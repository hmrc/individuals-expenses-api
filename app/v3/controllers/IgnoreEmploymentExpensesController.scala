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

package v3.controllers

import config.ExpensesFeatureSwitches
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.config.AppConfig
import shared.controllers._
import shared.routing.Version3
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v3.controllers.validators.IgnoreEmploymentExpensesValidatorFactory
import v3.services.IgnoreEmploymentExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IgnoreEmploymentExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                    val lookupService: MtdIdLookupService,
                                                    validatorFactory: IgnoreEmploymentExpensesValidatorFactory,
                                                    service: IgnoreEmploymentExpensesService,
                                                    auditService: AuditService,
                                                    cc: ControllerComponents,
                                                    val idGenerator: IdGenerator)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthorisedController(cc) {

  val endpointName = "ignore-employment-expenses"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "IgnoreEmploymentExpensesController", endpointName)

  def handleRequest(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val temporalValidationEnabled = ExpensesFeatureSwitches()(appConfig).isTemporalValidationEnabled
      val validator                 = validatorFactory.validator(nino, taxYear, temporalValidationEnabled)

      val requestHandler = RequestHandler
        .withValidator(validator)
        .withService(service.ignore)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "IgnoreEmploymentExpenses",
          apiVersion = Version3,
          transactionName = "ignore-employment-expenses",
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          includeResponse = true
        ))
        .withNoContentResult()

      requestHandler.handleRequest()

    }

}
