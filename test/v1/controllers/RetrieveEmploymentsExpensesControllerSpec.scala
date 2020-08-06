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
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.des.DesSource
import v1.models.domain.MtdSource
import v1.models.errors.{BadRequestError, DownstreamError, ErrorWrapper, MtdError, NinoFormatError, NotFoundError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.hateoas.Method.GET
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}
import v1.models.response.retrieveEmploymentExpenses.{Expenses, RetrieveEmploymentsExpensesResponse}

import scala.concurrent.Future

class RetrieveEmploymentsExpensesControllerSpec
  extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockRetrieveEmploymentExpensesService
  with MockRetrieveEmploymentExpensesRequestParser
  with MockHateoasFactory
  with MockAuditService {


  trait Test {
    val hc = HeaderCarrier()

    val controller = new RetrieveOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveEmploymentsExpensesRequestParser,
      service = mockRetrieveEmploymentsExpensesService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
    )

    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
  }

  private val nino = "AA123456A"
  private val taxYear = "2019-20"
  private val source = DesSource.`LATEST`
  private val correlationId = "X-123"

  private val rawData = RetrieveEmploymentsExpensesRawData(nino, taxYear, source.toString)
  private val requestData = RetrieveEmploymentsExpensesRequest(Nino(nino), taxYear, source)

  private val testHateoasLink = Link(href = s"individuals/expenses/employment/$nino/$taxYear?source=$source", method = GET, rel = "self")

  private val responseBody = RetrieveEmploymentsExpensesResponse(Some("2020-07-13T20:37:27Z"),
    Some(1000.25),
    Some(MtdSource.`latest`),
    Some("2020-07-13T20:37:27Z"),
    Some(Expenses(Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25)))
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
          .wrap(responseBody, RetrieveEmploymentsExpensesHateoasData(nino, taxYear, source))
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

            MockRetrieveEmploymentsExpensesRequestParser
              .parse(rawData)
              .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

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
