/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v2.controllers

import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import shared.config.MockAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.hateoas.Method.{DELETE, GET, PUT}
import shared.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import shared.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v2.controllers.validators.MockCreateAndAmendOtherExpensesValidatorFactory
import v2.models.request.createAndAmendOtherExpenses._
import v2.models.response.createAndAmendOtherExpenses.CreateAndAmendOtherExpensesHateoasData
import v2.services.MockCreateAndAmendOtherExpensesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAndAmendOtherExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateAndAmendOtherExpensesService
    with MockCreateAndAmendOtherExpensesValidatorFactory
    with MockAppConfig
    with MockHateoasFactory {

  private val taxYear = "2021-22"

  private val testHateoasLinks = List(
    Link(href = s"/individuals/expenses/other/$validNino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/other/$validNino/$taxYear", method = PUT, rel = "amend-expenses-other"),
    Link(href = s"/individuals/expenses/other/$validNino/$taxYear", method = DELETE, rel = "delete-expenses-other")
  )

  private val requestBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 1223.22)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 1223.22))
  )

  private val requestBodyJson = Json.parse("""
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223.22
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}
      |""".stripMargin)

  private val responseBodyJson = Json.parse(s"""
       |{
       |  "links": [
       |    {
       |      "href": "/individuals/expenses/other/$validNino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/expenses/other/$validNino/$taxYear",
       |      "method": "PUT",
       |      "rel": "amend-expenses-other"
       |    },
       |    {
       |      "href": "/individuals/expenses/other/$validNino/$taxYear",
       |      "method": "DELETE",
       |      "rel": "delete-expenses-other"
       |    }
       |  ]
       |}
       |""".stripMargin)

  private val requestData = CreateAndAmendOtherExpensesRequestData(Nino(validNino), TaxYear.fromMtd(taxYear), requestBody)

  "handleRequest" should {
    "return Ok" when {
      "the request received is valid" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

        MockCreateAndAmendOtherExpensesService
          .createAndAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), CreateAndAmendOtherExpensesHateoasData(validNino, taxYear))
          .returns(HateoasWrapper((), testHateoasLinks))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeAuditRequestBody = Some(requestBodyJson),
          maybeExpectedResponseBody = Some(responseBodyJson),
          maybeAuditResponseBody = Some(responseBodyJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {

        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "the service returns an error" in new Test {

        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false
        willUseValidator(returningSuccess(requestData))

        MockCreateAndAmendOtherExpensesService
          .createAndAmend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking[GenericAuditDetail] {

    val controller: CreateAndAmendOtherExpensesController = new CreateAndAmendOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockCreateAndAmendOtherExpensesValidatorFactory,
      service = mockService,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(validNino, taxYear)(fakePostRequest(requestBodyJson))

    def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendOtherExpenses",
        transactionName = "create-amend-other-expenses",
        detail = GenericAuditDetail(
          versionNumber = "2.0",
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> validNino, "taxYear" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
