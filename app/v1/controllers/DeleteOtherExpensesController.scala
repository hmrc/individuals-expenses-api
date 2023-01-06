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

import cats.data.EitherT
import cats.instances.future._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.DeleteOtherExpensesRequestParser
import v1.models.audit.{AuditEvent, AuditResponse, ExpensesAuditDetail}
import v1.models.errors._
import v1.models.request.deleteOtherExpenses.DeleteOtherExpensesRawData
import v1.services.{AuditService, DeleteOtherExpensesService, EnrolmentsAuthService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteOtherExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                               val lookupService: MtdIdLookupService,
                                               parser: DeleteOtherExpensesRequestParser,
                                               service: DeleteOtherExpensesService,
                                               auditService: AuditService,
                                               cc: ControllerComponents,
                                               val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with BaseController
    with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "DeleteOtherExpensesController", endpointName = "delete-expenses-other")

  def handleRequest(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
        s"with correlationId : $correlationId")

      val rawData = DeleteOtherExpensesRawData(nino, taxYear)
      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](parser.parseRequest(rawData))
          serviceResponse <- EitherT(service.deleteOtherExpenses(parsedRequest))
        } yield {
          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          auditSubmission(
            ExpensesAuditDetail(
              userDetails = request.userDetails,
              params = Map("nino" -> nino, "taxYear" -> taxYear),
              requestBody = None,
              `X-CorrelationId` = serviceResponse.correlationId,
              auditResponse = AuditResponse(httpStatus = NO_CONTENT, None, None)
            )
          )

          NoContent.withApiHeaders(serviceResponse.correlationId)

        }
      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result           = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(
          ExpensesAuditDetail(
            userDetails = request.userDetails,
            params = Map("nino" -> nino, "taxYear" -> taxYear),
            requestBody = None,
            `X-CorrelationId` = resCorrelationId,
            auditResponse = AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
          ))

        result
      }.merge
    }

  private def errorResult(errorWrapper: ErrorWrapper) = {
    (errorWrapper.error: @unchecked) match {
      case _
          if errorWrapper.containsAnyOf(
            NinoFormatError,
            BadRequestError,
            TaxYearFormatError,
            RuleTaxYearNotSupportedError,
            RuleTaxYearRangeInvalidError) =>
        BadRequest(Json.toJson(errorWrapper))
      case StandardDownstreamError => InternalServerError(Json.toJson(errorWrapper))
      case NotFoundError           => NotFound(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: ExpensesAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val event = AuditEvent(
      auditType = "DeleteOtherExpenses",
      transactionName = "delete-other-expenses",
      detail = details
    )

    auditService.auditEvent(event)
  }

}
