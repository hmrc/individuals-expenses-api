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
import v1.mocks.MockIdGenerator
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockRetrieveEmploymentsExpensesRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockRetrieveEmploymentsExpensesService}
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, ExpensesAuditDetail}
import v1.models.domain.MtdSource
import v1.models.errors._
import v1.models.hateoas.Method.{DELETE, GET, PUT}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}
import v1.models.response.retrieveEmploymentExpenses.{Expenses, RetrieveEmploymentsExpensesHateoasData, RetrieveEmploymentsExpensesResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveEmploymentsExpensesControllerSpec
  extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveEmploymentsExpensesService
    with MockRetrieveEmploymentsExpensesRequestParser
    with MockHateoasFactory
    with MockAuditService
    with MockIdGenerator {


  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveEmploymentsExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveEmploymentsExpensesRequestParser,
      service = mockRetrieveEmploymentsExpensesService,
      hateoasFactory = mockHateoasFactory,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  private val nino = "AA123456A"
  private val taxYear = "2019-20"
  private val source = MtdSource.`latest`
  private val correlationId = "X-123"

  private val rawData = RetrieveEmploymentsExpensesRawData(nino, taxYear, source.toString)
  private val requestData = RetrieveEmploymentsExpensesRequest(Nino(nino), taxYear, source)

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = PUT, rel = "amend-employment-expenses"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = DELETE, rel = "delete-employment-expenses"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear/ignore", method = PUT, rel = "ignore-employment-expenses")
  )

  private val responseBody = RetrieveEmploymentsExpensesResponse(Some("2020-12-12T12:12:12Z"),
    Some(123.12),
    Some(MtdSource.`latest`),
    Some("2020-07-13T20:37:27Z"),
    Some(Expenses(Some(123.12),Some(123.12),Some(123.12),Some(123.12),Some(123.12),Some(123.12),Some(123.12),Some(123.12)))
  )

  private val latestResponseBody = Json.parse(
    s"""
       |{
       |		"submittedOn": "2020-12-12T12:12:12Z",
       |		"totalExpenses": 123.12,
       |  	"source": "latest",
       |    "dateIgnored": "2020-07-13T20:37:27Z",
       |		"expenses": {
       |			"businessTravelCosts": 123.12,
       |			"jobExpenses": 123.12,
       |			"flatRateJobExpenses": 123.12,
       |			"professionalSubscriptions": 123.12,
       |			"hotelAndMealExpenses": 123.12,
       |			"otherAndCapitalAllowances": 123.12,
       |			"vehicleExpenses": 123.12,
       |			"mileageAllowanceRelief": 123.12
       |		},
       |		"links": [{
       |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |				"method": "PUT",
       |				"rel": "amend-employment-expenses"
       |			},
       |			{
       |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |				"method": "GET",
       |				"rel": "self"
       |			},
       |			{
       |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |				"method": "DELETE",
       |				"rel": "delete-employment-expenses"
       |			},
       |			{
       |				"href": "/individuals/expenses/employments/AA123456A/2019-20/ignore",
       |				"method": "PUT",
       |				"rel": "ignore-employment-expenses"
       |			}
       |		]
       |	}
       |""".stripMargin
  )

  def event(auditResponse: AuditResponse): AuditEvent[ExpensesAuditDetail] =
    AuditEvent(
      auditType = "RetrieveEmploymentExpenses",
      transactionName = "retrieve-employment-expenses",
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

        MockRetrieveEmploymentsExpensesRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveEmploymentsExpensesService
          .retrieveEmploymentsExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseBody))))

        MockHateoasFactory
          .wrap(responseBody, RetrieveEmploymentsExpensesHateoasData(nino, taxYear, source.toString))
          .returns(HateoasWrapper(responseBody, testHateoasLinks))

        val result: Future[Result] = controller.handleRequest(nino, taxYear, source.toString)(fakeRequest)
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(latestResponseBody))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockRetrieveEmploymentsExpensesRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

            val result: Future[Result] = controller.handleRequest(nino, taxYear, source.toString)(fakeRequest)

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
          (SourceFormatError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" should {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockRetrieveEmploymentsExpensesRequestParser
              .parse(rawData)
              .returns(Right(requestData))

            MockRetrieveEmploymentsExpensesService
              .retrieveEmploymentsExpenses(requestData)
              .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

            val result: Future[Result] = controller.handleRequest(nino, taxYear, source.toString)(fakeRequest)

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
