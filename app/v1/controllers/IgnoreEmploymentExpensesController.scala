/*
 * Copyright 2021 HM Revenue & Customs
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
import cats.implicits._
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.IgnoreEmploymentExpensesRequestParser
import v1.hateoas.HateoasFactory
import v1.models.audit.{AuditEvent, AuditResponse, ExpensesAuditDetail}
import v1.models.errors._
import v1.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRawData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesHateoasData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesResponse.IgnoreEmploymentExpensesLinksFactory
import v1.services.{AuditService, EnrolmentsAuthService, IgnoreEmploymentExpensesService, MtdIdLookupService}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IgnoreEmploymentExpensesController @Inject()(val authService: EnrolmentsAuthService,
                                                   val lookupService: MtdIdLookupService,
                                                   parser: IgnoreEmploymentExpensesRequestParser,
                                                   service: IgnoreEmploymentExpensesService,
                                                   hateoasFactory: HateoasFactory,
                                                   auditService: AuditService,
                                                   cc: ControllerComponents,
                                                   val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
  extends AuthorisedController(cc) with BaseController with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "IgnoreEmploymentExpensesController", endpointName = "ignoreEmploymentExpenses")

  def handleRequest(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>

      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
        s"with correlationId : $correlationId")

      val rawData = IgnoreEmploymentExpensesRawData(nino, taxYear)
      val result =
        for {
          parsedRequest <- EitherT.fromEither[Future](parser.parseRequest(rawData))
          serviceResponse <- EitherT(service.ignore(parsedRequest))
          vendorResponse <- EitherT.fromEither[Future](
            hateoasFactory.wrap(serviceResponse.responseData, IgnoreEmploymentExpensesHateoasData(nino, taxYear)).asRight[ErrorWrapper])
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
              auditResponse = AuditResponse(httpStatus = OK, response = Right(Some(Json.toJson(vendorResponse))))
            )
          )

          Ok(Json.toJson(vendorResponse))
            .withApiHeaders(serviceResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(ExpensesAuditDetail(
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
      case NinoFormatError
           | BadRequestError
           | TaxYearFormatError
           | RuleTaxYearRangeInvalidError
           | RuleTaxYearNotSupportedError
           | RuleTaxYearNotEndedError => BadRequest(Json.toJson(errorWrapper))
      case DownstreamError                 => InternalServerError(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: ExpensesAuditDetail)
                             (implicit hc: HeaderCarrier,
                              ec: ExecutionContext): Future[AuditResult] = {

    val event = AuditEvent(
      auditType = "IgnoreEmploymentExpenses",
      transactionName = "ignore-employment-expenses",
      detail = details
    )

    auditService.auditEvent(event)
  }
}
