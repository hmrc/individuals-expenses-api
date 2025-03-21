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

import play.api.mvc.{Action, AnyContent, ControllerComponents}
import shared.config.AppConfig
import shared.controllers._
import shared.services.{EnrolmentsAuthService, MtdIdLookupService}
import shared.utils.IdGenerator
import v3.controllers.validators.RetrieveEmploymentExpensesValidatorFactory
import v3.services.RetrieveEmploymentsExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveEmploymentsExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                       val lookupService: MtdIdLookupService,
                                                       validatorFactory: RetrieveEmploymentExpensesValidatorFactory,
                                                       service: RetrieveEmploymentsExpensesService,
                                                       cc: ControllerComponents,
                                                       val idGenerator: IdGenerator)(implicit appConfig: AppConfig, ec: ExecutionContext)
    extends AuthorisedController(cc) {

  val endpointName = "retrieve-employment-expenses"

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "RetrieveEmploymentsExpensesController", endpointName)

  def handleRequest(nino: String, taxYear: String, source: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, taxYear, source)

      val requestHandler = RequestHandler
        .withValidator(validator)
        .withService(service.retrieveEmploymentsExpenses)
        .withPlainJsonResult()

      requestHandler.handleRequest()

    }

}
