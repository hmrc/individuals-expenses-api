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

import api.controllers._
import api.hateoas.HateoasFactory
import api.services.{AuditService, EnrolmentsAuthService, MtdIdLookupService}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import routing.{Version, Version1}
import utils.IdGenerator
import v1.controllers.validators.CreateAndAmendOtherExpensesValidatorFactory
import v1.models.response.createAndAmendOtherExpenses.CreateAndAmendOtherExpensesHateoasData
import v1.models.response.createAndAmendOtherExpenses.CreateAndAmendOtherExpensesResponse.CreateAndAmendOtherExpensesLinksFactory
import v1.services.CreateAndAmendOtherExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CreateAndAmendOtherExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                       val lookupService: MtdIdLookupService,
                                                       validatorFactory: CreateAndAmendOtherExpensesValidatorFactory,
                                                       service: CreateAndAmendOtherExpensesService,
                                                       auditService: AuditService,
                                                       hateoasFactory: HateoasFactory,
                                                       cc: ControllerComponents,
                                                       val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateAndAmendOtherExpensesController", endpointName = "createAndAmendOtherExpenses")

  def handleRequest(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val validator = validatorFactory.validator(nino, taxYear, request.body)
      val requestHandler = RequestHandler
        .withValidator(validator)
        .withService(service.createAndAmend)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "CreateAmendOtherExpenses",
          transactionName = "create-amend-other-expenses",
          apiVersion = Version.from(request, orElse = Version1),
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          requestBody = Some(request.body),
          includeResponse = true
        ))
        .withHateoasResult(hateoasFactory)(CreateAndAmendOtherExpensesHateoasData(nino, taxYear))

      requestHandler.handleRequest()
    }

}
