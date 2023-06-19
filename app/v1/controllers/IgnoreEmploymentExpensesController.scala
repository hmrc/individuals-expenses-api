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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import utils.IdGenerator
import v1.controllers.requestParsers.IgnoreEmploymentExpensesRequestParser
import v1.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRawData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesHateoasData
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesResponse.IgnoreEmploymentExpensesLinksFactory
import v1.services.IgnoreEmploymentExpensesService

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class IgnoreEmploymentExpensesController @Inject() (val authService: EnrolmentsAuthService,
                                                    val lookupService: MtdIdLookupService,
                                                    appConfig: AppConfig,
                                                    parser: IgnoreEmploymentExpensesRequestParser,
                                                    service: IgnoreEmploymentExpensesService,
                                                    auditService: AuditService,
                                                    hateoasFactory: HateoasFactory,
                                                    cc: ControllerComponents,
                                                    val idGenerator: IdGenerator)(implicit ec: ExecutionContext)
    extends AuthorisedController(cc) {

  implicit val endpointLogContext: EndpointLogContext =
    EndpointLogContext(controllerName = "IgnoreEmploymentExpensesController", endpointName = "ignoreEmploymentExpenses")

  def handleRequest(nino: String, taxYear: String): Action[AnyContent] =
    authorisedAction(nino).async { implicit request =>
      implicit val ctx: RequestContext = RequestContext.from(idGenerator, endpointLogContext)

      val rawData = IgnoreEmploymentExpensesRawData(
        nino = nino,
        taxYear = taxYear,
        temporalValidationEnabled = FeatureSwitches()(appConfig).isTemporalValidationEnabled
      )
      val requestHandler = RequestHandler
        .withParser(parser)
        .withService(service.ignore)
        .withAuditing(AuditHandler(
          auditService = auditService,
          auditType = "IgnoreEmploymentExpenses",
          transactionName = "ignore-employment-expenses",
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          includeResponse = true
        ))
        .withHateoasResult(hateoasFactory)(IgnoreEmploymentExpensesHateoasData(nino, taxYear))

      requestHandler.handleRequest(rawData)

    }

}
