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
import v1.mocks.requestParsers.MockAmendEmploymentExpensesRequestParser
import v1.mocks.services.{MockAmendEmploymentExpensesService, MockAuditService, MockEnrolmentsAuthService, MockMtdIdLookupService}
import v1.models.errors._
import v1.models.hateoas.{HateoasWrapper, Link}
import v1.models.hateoas.Method.GET
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendEmploymentExpenses._
import v1.models.response.amendEmploymentExpenses.AmendEmploymentExpensesHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmendEmploymentExpensesControllerSpec
  extends ControllerBaseSpec
  with MockEnrolmentsAuthService
  with MockMtdIdLookupService
  with MockAmendEmploymentExpensesService
  with MockAmendEmploymentExpensesRequestParser
  with MockHateoasFactory
  with MockAuditService {


    private val nino = "AA123456A"
    private val taxYear = "2021-22"
    private val correlationId = "X-123"
    private val testHateoasLink = Link(href = s"individuals/expenses/employments/$nino/$taxYear", method = GET, rel = "self")
    private val requestBody = AmendEmploymentExpensesBody(
      Expenses(
        Some(123.12),
        Some(123.12),
        Some(123.12),
        Some(123.12),
        Some(123.12),
        Some(123.12),
        Some(123.12),
        Some(123.12),
      )
    )

    private val requestBodyJson = Json.parse(
      """
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

    private val rawData = AmendEmploymentExpensesRawData(nino, taxYear, requestBodyJson)
    private val requestData = AmendEmploymentExpensesRequest(Nino(nino), taxYear , requestBody)

    trait Test {
      val hc = HeaderCarrier()

      val controller = new AmendEmploymentExpensesController(
        authService = mockEnrolmentsAuthService,
        lookupService = mockMtdIdLookupService,
        parser = mockRequestParser,
        service = mockService,
        hateoasFactory = mockHateoasFactory,
        cc = cc,
      )

      MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
      MockedEnrolmentsAuthService.authoriseUser()
    }

    "handleRequest" should {
      "return Ok" when {
        "the request received is valid" in new Test {

          MockAmendEmploymentExpensesRequestParser
            .parseRequest(rawData)
            .returns(Right(requestData))

          MockAmendEmploymentExpensesService
            .amend(requestData)
            .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

          MockHateoasFactory
            .wrap((), AmendEmploymentExpensesHateoasData(nino, taxYear))
            .returns(HateoasWrapper((), Seq(testHateoasLink)))

          val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakePostRequest(requestBodyJson))
          status(result) shouldBe OK
          header("X-CorrelationId", result) shouldBe Some(correlationId)
        }
      }
      "return the error as per spec" when {
        "parser errors occur" should {
          def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
            s"a ${error.code} error is returned from the parser" in new Test {

              MockAmendEmploymentExpensesRequestParser
                .parseRequest(rawData)
                .returns(Left(ErrorWrapper(Some(correlationId), error, None)))

              val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakePostRequest(requestBodyJson))

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
            (RuleIncorrectOrEmptyBodyError, BAD_REQUEST),
            (RuleTaxYearNotSupportedError, BAD_REQUEST),
            (RuleTaxYearNotEndedError, BAD_REQUEST),
            (ValueFormatError, BAD_REQUEST)
          )

          input.foreach(args => (errorsFromParserTester _).tupled(args))
        }

        "service errors occur" should {
          def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
            s"a $mtdError error is returned from the service" in new Test {

              MockAmendEmploymentExpensesRequestParser
                .parseRequest(rawData)
                .returns(Right(requestData))

              MockAmendEmploymentExpensesService
                .amend(requestData)
                .returns(Future.successful(Left(ErrorWrapper(Some(correlationId), mtdError))))

              val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakePostRequest(requestBodyJson))

              status(result) shouldBe expectedStatus
              contentAsJson(result) shouldBe Json.toJson(mtdError)
              header("X-CorrelationId", result) shouldBe Some(correlationId)
            }
          }

          val input = Seq(
            (NinoFormatError, BAD_REQUEST),
            (RuleTaxYearNotEndedError, BAD_REQUEST),
            (NotFoundError, NOT_FOUND),
            (DownstreamError, INTERNAL_SERVER_ERROR)
          )

          input.foreach(args => (serviceErrors _).tupled(args))
        }
      }
    }
  }
