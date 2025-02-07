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
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import shared.config.AppConfig
import shared.controllers._
import shared.routing.Version2
import shared.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v3.controllers.validators.CreateAndAmendEmploymentExpensesValidatorFactory
import v3.services.CreateAndAmendEmploymentExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CreateAndAmendEmploymentExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                            val lookupService: MtdIdLookupService,
                                                            validatorFactory: CreateAndAmendEmploymentExpensesValidatorFactory,
                                                            service: CreateAndAmendEmploymentExpensesService,
                                                            auditService: AuditService,
                                                            cc: ControllerComponents,
                                                            idGenerator: IdGenerator)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthorisedController(cc) {

  val endpointName = "create-amend-employment-expenses"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateAmendEmploymentExpensesController", endpointName)

  def handleRequest(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val temporalValidationEnabled = ExpensesFeatureSwitches()(appConfig).isTemporalValidationEnabled
      val validator                 = validatorFactory.validator(nino, taxYear, request.body, temporalValidationEnabled)

      val requestHandler = RequestHandler
        .withValidator(validator)
        .withService(service.createAndAmendEmploymentExpenses)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "CreateAmendEmploymentExpenses",
          transactionName = "create-amend-employment-expenses",
          apiVersion = Version2,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          requestBody = Some(request.body),
          includeResponse = true
        ))
        .withNoContentResult()

      requestHandler.handleRequest()
    }

}
