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

import api.controllers.{AuthorisedController, EndpointLogContext}
import api.hateoas.HateoasFactory
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.errors._
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import cats.data.EitherT
import cats.implicits._
import config.{AppConfig, FeatureSwitches}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import utils.{IdGenerator, Logging}
import v1.controllers.requestParsers.IgnoreEmploymentExpensesRequestParser
import v1.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRawData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesHateoasData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesResponse.IgnoreEmploymentExpensesLinksFactory
import v1.services.IgnoreEmploymentExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IgnoreEmploymentExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                    val lookupService: MtdIdLookupService,
                                                    appConfig: AppConfig,
                                                    parser: IgnoreEmploymentExpensesRequestParser,
                                                    service: IgnoreEmploymentExpensesService,
                                                    hateoasFactory: HateoasFactory,
                                                    auditService: AuditService,
                                                    cc: ControllerComponents,
                                                    val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc)
    with Logging {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "IgnoreEmploymentExpensesController", endpointName = "ignoreEmploymentExpenses")

  def handleRequest(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val correlationId: String = idGenerator.generateCorrelationId
      logger.info(message = s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] " +
        s"with correlationId : $correlationId")

      val rawData = IgnoreEmploymentExpensesRawData(
        nino = nino,
        taxYear = taxYear,
        temporalValidationEnabled = FeatureSwitches()(appConfig).isTemporalValidationEnabled
      )

      val result =
        for {
          parsedRequest   <- EitherT.fromEither[Future](parser.parseRequest(rawData))
          serviceResponse <- EitherT(service.ignore(parsedRequest))
        } yield {
          val vendorResponse = hateoasFactory.wrap(serviceResponse.responseData, IgnoreEmploymentExpensesHateoasData(nino, taxYear))

          logger.info(
            s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
              s"Success response received with CorrelationId: ${serviceResponse.correlationId}")

          auditSubmission(
            GenericAuditDetail(
              userDetails = request.userDetails,
              params = Map("nino" -> nino, "taxYear" -> taxYear),
              request = None,
              `X-CorrelationId` = serviceResponse.correlationId,
              response = AuditResponse(httpStatus = OK, response = Right(Some(Json.toJson(vendorResponse))))
            )
          )

          Ok(Json.toJson(vendorResponse))
            .withApiHeaders(serviceResponse.correlationId)
        }

      result.leftMap { errorWrapper =>
        val resCorrelationId = errorWrapper.correlationId
        val result           = errorResult(errorWrapper).withApiHeaders(resCorrelationId)
        logger.warn(
          s"[${endpointLogContext.controllerName}][${endpointLogContext.endpointName}] - " +
            s"Error response received with CorrelationId: $resCorrelationId")

        auditSubmission(
          GenericAuditDetail(
            userDetails = request.userDetails,
            params = Map("nino" -> nino, "taxYear" -> taxYear),
            request = None,
            `X-CorrelationId` = resCorrelationId,
            response = AuditResponse(httpStatus = result.header.status, response = Left(errorWrapper.auditErrors))
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
            RuleTaxYearRangeInvalidError,
            RuleTaxYearNotSupportedError,
            RuleTaxYearNotEndedError) =>
        BadRequest(Json.toJson(errorWrapper))
      case StandardDownstreamError => InternalServerError(Json.toJson(errorWrapper))
      case NotFoundError           => NotFound(Json.toJson(errorWrapper))
    }
  }

  private def auditSubmission(details: GenericAuditDetail)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[AuditResult] = {

    val event = AuditEvent(
      auditType = "IgnoreEmploymentExpenses",
      transactionName = "ignore-employment-expenses",
      detail = details
    )

    auditService.auditEvent(event)
  }

}
