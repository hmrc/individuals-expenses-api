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
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockIdGenerator
import v1.mocks.hateoas.MockHateoasFactory
import v1.mocks.requestParsers.MockDeleteOtherExpensesRequestDataParser
import v1.mocks.services._
import v1.models.audit.{AuditError, AuditEvent, AuditResponse, ExpensesAuditDetail}
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.TaxYear
import v1.models.request.deleteOtherExpenses.{DeleteOtherExpensesRawData, DeleteOtherExpensesRequest}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteOtherExpensesControllerSpec
    extends ControllerBaseSpec
    with MockEnrolmentsAuthService
    with MockMtdIdLookupService
    with MockDeleteOtherExpensesService
    with MockDeleteOtherExpensesRequestDataParser
    with MockHateoasFactory
    with MockAuditService
    with MockIdGenerator {

  trait Test {
    val hc = HeaderCarrier()

    val controller = new DeleteOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRequestDataParser,
      service = mockDeleteOtherExpensesService,
      auditService = mockAuditService,
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

  def event(auditResponse: AuditResponse): AuditEvent[ExpensesAuditDetail] =
    AuditEvent(
      auditType = "DeleteOtherExpenses",
      transactionName = "delete-other-expenses",
      detail = ExpensesAuditDetail(
        userType = "Individual",
        agentReferenceNumber = None,
        params = Map("nino" -> nino, "taxYear" -> taxYear),
        requestBody = None,
        `X-CorrelationId` = correlationId,
        auditResponse = auditResponse
      )
    )

  private val rawData     = DeleteOtherExpensesRawData(nino, taxYear)
  private val requestData = DeleteOtherExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear))

  "handleRequest" should {
    "return NoContent" when {
      "the request received is valid" in new Test {

        MockDeleteOtherExpensesRequestDataParser
          .parse(rawData)
          .returns(Right(requestData))

        MockDeleteOtherExpensesService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

        status(result) shouldBe NO_CONTENT
        header("X-CorrelationId", result) shouldBe Some(correlationId)

        val auditResponse: AuditResponse = AuditResponse(NO_CONTENT, None, None)
        MockedAuditService.verifyAuditEvent(event(auditResponse)).once
      }
    }

    "return the error as per spec" when {
      "parser errors occur" should {
        def errorsFromParserTester(error: MtdError, expectedStatus: Int): Unit = {
          s"a ${error.code} error is returned from the parser" in new Test {

            MockDeleteOtherExpensesRequestDataParser
              .parse(rawData)
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
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (RuleTaxYearNotSupportedError, BAD_REQUEST)
        )

        input.foreach(args => (errorsFromParserTester _).tupled(args))
      }

      "service errors occur" should {
        def serviceErrors(mtdError: MtdError, expectedStatus: Int): Unit = {
          s"a $mtdError error is returned from the service" in new Test {

            MockDeleteOtherExpensesRequestDataParser
              .parse(rawData)
              .returns(Right(requestData))

            MockDeleteOtherExpensesService
              .delete(requestData)
              .returns(Future.successful(Left(ErrorWrapper(correlationId, mtdError))))

            val result: Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

            status(result) shouldBe expectedStatus
            contentAsJson(result) shouldBe Json.toJson(mtdError)
            header("X-CorrelationId", result) shouldBe Some(correlationId)

            val auditResponse: AuditResponse = AuditResponse(expectedStatus, Some(Seq(AuditError(mtdError.code))), None)
            MockedAuditService.verifyAuditEvent(event(auditResponse)).once
          }
        }

        val errors = Seq(
          (NinoFormatError, BAD_REQUEST),
          (TaxYearFormatError, BAD_REQUEST),
          (RuleTaxYearRangeInvalidError, BAD_REQUEST),
          (NotFoundError, NOT_FOUND),
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
