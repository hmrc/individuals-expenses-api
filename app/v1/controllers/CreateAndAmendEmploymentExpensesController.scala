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
import config.{AppConfig, FeatureSwitches}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import utils.IdGenerator
import v1.controllers.requestValidators.CreateAmendEmploymentExpensesRequestValidator
import v1.models.request.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesRawData
import v1.models.response.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesHateoasData
import v1.models.response.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesResponse.AmendOrderLinksFactory
import v1.services.CreateAndAmendEmploymentExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class CreateAndAmendEmploymentExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                            val lookupService: MtdIdLookupService,
                                                            appConfig: AppConfig,
                                                            validator: CreateAmendEmploymentExpensesRequestValidator,
                                                            service: CreateAndAmendEmploymentExpensesService,
                                                            auditService: AuditService,
                                                            hateoasFactory: HateoasFactory,
                                                            cc: ControllerComponents,
                                                            idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "CreateAmendEmploymentExpensesController", endpointName = "createAmendEmploymentExpenses")

  def handleRequest(nino: String, taxYear: String): Action[JsValue] =
    authorisedAction(nino).async(parse.json) { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = CreateAndAmendEmploymentExpensesRawData(
        nino = nino,
        taxYear = taxYear,
        body = request.body,
        temporalValidationEnabled = FeatureSwitches()(appConfig).isTemporalValidationEnabled
      )
      val requestHandler = RequestHandler
        .withValidator(validator)
        .withService(service.createAndAmendEmploymentExpenses)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "CreateAmendEmploymentExpenses",
          transactionName = "create-amend-employment-expenses",
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          requestBody = Some(request.body),
          includeResponse = true
        ))
        .withHateoasResult(hateoasFactory)(CreateAndAmendEmploymentExpensesHateoasData(nino, taxYear))

      requestHandler.handleRequest(rawData)
    }

}
