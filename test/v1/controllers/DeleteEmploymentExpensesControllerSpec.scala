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
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.outcomes.ResponseWrapper
import play.api.libs.json.JsValue
import play.api.mvc.Result
import v1.controllers.validators.MockDeleteEmploymentExpensesValidatorFactory
import v1.models.request.deleteEmploymentExpenses.DeleteEmploymentExpensesRequestData
import v1.services.MockDeleteEmploymentExpensesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeleteEmploymentExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockDeleteEmploymentExpensesService
    with MockDeleteEmploymentExpensesValidatorFactory {

  private val taxYear = "2019-20"

  private val requestData = DeleteEmploymentExpensesRequestData(Nino(nino), TaxYear.fromMtd(taxYear))

  "handleRequest" should {
    "return a successful response with status 204 (No Content)" when {
      "a valid request is supplied" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteEmploymentExpensesService
          .delete(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        runOkTestWithAudit(expectedStatus = NO_CONTENT)
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        willUseValidator(returning(NinoFormatError))

        runErrorTestWithAudit(NinoFormatError)
      }

      "service returns an error" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockDeleteEmploymentExpensesService
          .delete(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new DeleteEmploymentExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockDeleteEmploymentExpensesValidatorFactory,
      service = mockDeleteEmploymentExpensesService,
      auditService = mockAuditService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(nino, taxYear)(fakeDeleteRequest)

    def event(auditResponse: AuditResponse, maybeRequestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "DeleteEmploymentExpenses",
        transactionName = "delete-employment-expenses",
        detail = GenericAuditDetail(
          versionNumber = "1.0",
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          requestBody = maybeRequestBody,
          `X-CorrelationId` = correlationId,
          auditResponse = auditResponse
        )
      )

  }

}
