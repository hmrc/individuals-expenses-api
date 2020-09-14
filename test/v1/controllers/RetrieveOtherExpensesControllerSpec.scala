/*
 * Copyright 2020 HM Revenue & Customs
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

import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockRetrieveOtherExpensesRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockRetrieveOtherExpensesService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, ExpensesAuditDetail}
import v1.models.errors.{BadRequestError, DownstreamError, ErrorWrapper, MtdError, NinoFormatError, NotFoundError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.hateoas.Method.{DELETE, GET, PUT}
import v1.models.outcomes.ResponseWrapper
import v1.models.response.retrieveOtherExpenses.{PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits, RetrieveOtherExpensesBody, RetrieveOtherExpensesHateoasData}
import v1.models.request.retrieveOtherExpenses.{RetrieveOtherExpensesRawData, RetrieveOtherExpensesRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveOtherExpensesControllerSpec
  extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockRetrieveOtherExpensesService
  with MockRetrieveOtherExpensesRequestParser
  with MockHateoasFactory
  with MockAuditService {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveOtherExpensesRequestParser,
      service = mockRetrieveOtherExpensesService,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  private val nino = "AA123456A"
  private val taxYear = "2019-20"
  private val correlationId = "X-123"

  private val rawData = RetrieveOtherExpensesRawData(nino, taxYear)
  private val requestData = RetrieveOtherExpensesRequest(Nino(nino), taxYear)

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = PUT, rel = "amend-expenses-other"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = DELETE, rel = "delete-expenses-other")
  )

  private val responseBody = RetrieveOtherExpensesBody(
    "2019-04-04T01:01:01Z",
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 4528.99)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.10))
  )

  val responseBodyJson = Json.parse(
    s"""
       |{
       |  "submittedOn": "2019-04-04T01:01:01Z",
       |  "paymentsToTradeUnionsForDeathBenefits": {
       |    "customerReference": "TRADE UNION PAYMENTS",
       |    "expenseAmount": 4528.99
       |  },
       |  "patentRoyaltiesPayments": {
       |    "customerReference": "ROYALTIES PAYMENTS",
       |    "expenseAmount": 2000.10
       |  },
       |  "links":[
       |      {
       |         "href":"/individuals/expenses/other/$nino/$taxYear",
       |         "method":"PUT",
       |         "rel":"amend-expenses-other"
       |      },
       |      {
       |         "href":"/individuals/expenses/other/$nino/$taxYear",
       |         "method":"GET",
       |         "rel":"self"
       |      },
       |      {
       |         "href":"/individuals/expenses/other/$nino/$taxYear",
       |         "method":"DELETE",
       |         "rel":"delete-expenses-other"
       |      }
       |   ]
       |}
       |""".stripMargin
  )

  def event(auditResponse: AuditResponse): AuditEvent[ExpensesAuditDetail] =
    AuditEvent(
      auditType = "RetrieveOtherExpenses",
      transactionName = "retrieve-expenses-other",
      detail = ExpensesAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        requestBody = None,
        `X-CorrelationId` = correlationId,
        auditResponse = auditResponse
      )
    )

  "handleRequest" should {
    "return Ok" when {
      "the request received is valid" in new Test {

        MockRetrieveOtherExpensesRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveOtherExpensesService
          .retrieveOtherExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseBody))))

        MockHateoasFactory
          .wrap(responseBody, RetrieveOtherExpensesHateoasData(nino, taxYear))
          .returns(HateoasWrapper(responseBody, testHateoasLinks))

        val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(responseBodyJson))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }
    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockRetrieveOtherExpensesRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(error.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (BadRequestError, BAD_REQUEST),
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" should {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockRetrieveOtherExpensesRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockRetrieveOtherExpensesService
              .retrieveOtherExpenses(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val input = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
          (DownstreamError, INTERNAL_SERVER_ERROR)
        )

        input.foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }
}