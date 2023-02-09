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

import api.models.audit.{AuditError, AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{BadRequestError, ErrorWrapper, MtdError, NinoFormatError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, StandardDownstreamError, TaxYearFormatError}
import api.models.hateoas.{HateoasWrapper, Link}
import mocks.MockAppConfig
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockIdGenerator
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockIgnoreEmploymentExpensesRequestParser
import v1.mocks.services.{MockAuditService, MockEnrolmentsAuthService, MockIgnoreEmploymentExpensesService, MockMtdIdLookupService}
import v1.models.errors._
import api.models.hateoas.Method.{DELETE, GET}
import api.models.outcomes.ResponseWrapper
import v1.models.request.ignoreEmploymentExpenses._
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IgnoreEmploymentExpensesControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockIgnoreEmploymentExpensesService
    with MockIgnoreEmploymentExpensesRequestParser
    with MockHateoasFactory
    with MockAuditService
    with MockAppConfig
    with MockIdGenerator {

  private val nino          = "AA123456A"
  private val taxYear       = "2019-20"
  private val correlationId = "X-123"

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = DELETE, rel = "delete-employment-expenses")
  )

  val responseBody: JsValue = Json.parse(s"""
       |{
       |  "links": [
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "DELETE",
       |      "rel": "delete-employment-expenses"
       |    }
       |  ]
       |}
       |""".stripMargin)

  def event(auditResponse: AuditResponse): AuditEvent[GenericAuditDetail] =
    AuditEvent(
      auditType = "IgnoreEmploymentExpenses",
      transactionName = "ignore-employment-expenses",
      detail = GenericAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        request = None,
        `X-CorrelationId` = correlationId,
        response = auditResponse
      )
    )

  private val rawData     = IgnoreEmploymentExpensesRawData(nino, taxYear)
  private val requestData = IgnoreEmploymentExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear))

  trait Test {
    val hc = HeaderCarrier()

    val controller = new IgnoreEmploymentExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      appConfig = mockAppConfig,
      parser = mockRequestParser,
      service = mockService,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockedAppConfig.featureSwitches.returns(Configuration("allowTemporalValidationSuspension.enabled" -> true)).anyNumberOfTimes()
    MockedMtdIdLookupService.lookup(nino).returns(Future.successful(Right("test-mtd-id")))
    MockedEnrolmentsAuthService.authoriseUser()
    MockIdGenerator.generateCorrelationId.returns(correlationId)
  }

  "handleRequest" should {
    "return Ok" when {
      "the request received is valid" in new Test {

        MockIgnoreEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockIgnoreEmploymentExpensesService
          .ignore(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), IgnoreEmploymentExpensesHateoasData(nino, taxYear))
          .returns(HateoasWrapper((), testHateoasLinks))

        val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)
        status(result) shouldBe OK
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(OK, None, Some(responseBody))
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockIgnoreEmploymentExpensesRequestParser
              .parseRequest(rawData)
              .returns(Left(ErrorWrapper(correlationId, error, None)))

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
          (RuleTaxYearNotSupportedError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" should {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockIgnoreEmploymentExpensesRequestParser
              .parseRequest(rawData)
              .returns(Right(requestData))

            MockIgnoreEmploymentExpensesService
              .ignore(requestData)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val errors = List(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearNotEndedError, BAD_REQUEST),
          (StandardDownstreamError, INTERNAL_SERVER_ERROR)
        )

        val extraTysErrors = List(
          (RuleTaxYearNotSupportedError, BAD_REQUEST)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrors _).tupled(args))
      }
    }
  }

}
