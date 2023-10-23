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

package v1.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.hateoas.{HateoasWrapper, Link, MockHateoasFactory}
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.hateoas.Method.{DELETE, GET, PUT}
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import v1.mocks.requestParsers.MockCreateAndAmendOtherExpensesRequestParser
import v1.mocks.services._
import v1.models.request.createAndAmendOtherExpenses._
import v1.models.response.createAndAmendOtherExpenses.CreateAndAmendOtherExpensesHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateAndAmendOtherExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockCreateAndAmendOtherExpensesService
    with MockCreateAndAmendOtherExpensesRequestParser
    with MockHateoasFactory {

  private val taxYear = "2021-22"

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = PUT, rel = "amend-expenses-other"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = DELETE, rel = "delete-expenses-other")
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
       |      "href": "/individuals/expenses/other/$nino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/expenses/other/$nino/$taxYear",
       |      "method": "PUT",
       |      "rel": "amend-expenses-other"
       |    },
       |    {
       |      "href": "/individuals/expenses/other/$nino/$taxYear",
       |      "method": "DELETE",
       |      "rel": "delete-expenses-other"
       |    }
       |  ]
       |}
       |""".stripMargin)

  private val rawData     = CreateAndAmendOtherExpensesRawData(nino, taxYear, requestBodyJson)
  private val requestData = CreateAndAmendOtherExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear), requestBody)

  "handleRequest" should {
    "return Ok" when {
      "the request received is valid" in new Test {

        MockCreateAndAmendOtherExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockCreateAndAmendOtherExpensesService
          .createAndAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), CreateAndAmendOtherExpensesHateoasData(nino, taxYear))
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

        MockCreateAndAmendOtherExpensesRequestParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError, Some(requestBodyJson))
      }

      "the service returns an error" in new Test {

        MockCreateAndAmendOtherExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockCreateAndAmendOtherExpensesService
          .createAndAmend(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError, maybeAuditRequestBody = Some(requestBodyJson))
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new CreateAndAmendOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRequestParser,
      service = mockService,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(nino, taxYear)(fakePutRequest(requestBodyJson))

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "CreateAmendOtherExpenses",
        transactionName = "create-amend-other-expenses",
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
