/*
 * Copyright 2022 HM Revenue & Customs
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
import v1.models.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockIdGenerator
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockRetrieveOtherExpensesRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService, MockRetrieveOtherExpensesService}
import v1.models.errors._
import v1.models.hateoas.Method.GET
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveOtherExpenses.{RetrieveOtherExpensesRawData, RetrieveOtherExpensesRequest}
import v1.models.response.retrieveOtherExpenses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveOtherExpensesControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockRetrieveOtherExpensesService
    with MockRetrieveOtherExpensesRequestParser
    with MockHateoasFactory
    with MockAuditService
    with MockIdGenerator {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveOtherExpensesRequestParser,
      service = mockRetrieveOtherExpensesService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  private val nino          = "AA123456A"
  private val taxYear       = "2019-20"
  private val correlationId = "X-123"

  private val rawData     = RetrieveOtherExpensesRawData(nino, taxYear)
  private val requestData = RetrieveOtherExpensesRequest(Nino(nino), taxYear)

  private val testHateoasLink = Link(href = s"individuals/expenses/other/$nino/$taxYear", method = GET, rel = "self")

  private val responseBody = RetrieveOtherExpensesResponse(
    "2019-04-04T01:01:01Z",
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 4528.99)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.10))
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
          .returns(HateoasWrapper(responseBody, Seq(testHateoasLink)))

        val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)
      }
    }

    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockRetrieveOtherExpensesRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(error)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
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
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)
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
