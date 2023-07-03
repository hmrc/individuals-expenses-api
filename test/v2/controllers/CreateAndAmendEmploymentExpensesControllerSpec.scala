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

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.mocks.hateoas.MockHateoasFactory
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors
import api.models.errors._
import api.models.hateoas.Method.{DELETE, GET, PUT}
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import v2.mocks.requestParsers.MockCreateAndAmendEmploymentExpensesRequestParser
import v2.mocks.services.MockCreateAndAmendEmploymentExpensesService
import v2.models.request.createAndAmendEmploymentExpenses._
import v2.models.response.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAndAmendEmploymentExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockAppConfig
    with MockCreateAndAmendEmploymentExpensesService
    with MockCreateAndAmendEmploymentExpensesRequestParser
    with MockHateoasFactory {

  private val taxYear = "2021-22"

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = PUT, rel = "amend-employment-expenses"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = DELETE, rel = "delete-employment-expenses")
  )

  private val requestBody = CreateAndAmendEmploymentExpensesBody(
    Expenses(
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12)
    )
  )

  private val requestBodyJson = Json.parse("""
      |{
      |    "expenses": {
      |        "businessTravelCosts": 123.12,
      |        "jobExpenses": 123.12,
      |        "flatRateJobExpenses": 123.12,
      |        "professionalSubscriptions": 123.12,
      |        "hotelAndMealExpenses": 123.12,
      |        "otherAndCapitalAllowances": 123.12,
      |        "vehicleExpenses": 123.12,
      |        "mileageAllowanceRelief": 123.12
      |    }
      |}
      |""".stripMargin)

  private val responseBodyJson = Json.parse(s"""
       |{
       |  "links": [
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "PUT",
       |      "rel": "amend-employment-expenses"
       |    },
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "DELETE",
       |      "rel": "delete-employment-expenses"
       |    }
       |  ]
       |}
       |""".stripMargin)

  private val rawData     = CreateAndAmendEmploymentExpensesRawData(nino, taxYear, requestBodyJson)
  private val requestData = CreateAndAmendEmploymentExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear), requestBody)

  "handleRequest" should {
    "return OK" when {
      "the request received is valid" in new Test {

        MockCreateAndAmendEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockCreateAndAmendEmploymentExpensesService
          .amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), CreateAndAmendEmploymentExpensesHateoasData(nino, taxYear))
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

        MockCreateAndAmendEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Left(errors.ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "the service returns an error" in new Test {

        MockCreateAndAmendEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockCreateAndAmendEmploymentExpensesService
          .amend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new CreateAndAmendEmploymentExpensesController(
      appConfig = mockAppConfig,
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRequestParser,
      service = mockService,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockAppConfig.featureSwitches.returns(Configuration("allowTemporalValidationSuspension.enabled" -> true)).anyNumberOfTimes()

    protected def callController(): Future[Result] = controller.handleRequest(nino, taxYear)(fakePutRequest(requestBodyJson))

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendEmploymentExpenses",
        transactionName = "create-amend-employment-expenses",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = requestBody,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

}
