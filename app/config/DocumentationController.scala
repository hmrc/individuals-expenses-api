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

package config

import config.rewriters.DocumentationRewriters
import controllers.RewriteableAssets
import definition.ApiDefinitionFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logging

import javax.inject.{Inject, Singleton}

@Singleton
class DocumentationController @Inject() (
    selfAssessmentApiDefinition: ApiDefinitionFactory,
    docRewriters: DocumentationRewriters,
    assets: RewriteableAssets,
    cc: ControllerComponents
) extends BackendController(cc)
    with Logging {

  def definition(): Action[AnyContent] = Action {
    Ok(Json.toJson(selfAssessmentApiDefinition.definition))
  }

  def asset(version: String, filename: String): Action[AnyContent] = {
    val path      = s"/public/api/conf/$version"
    val rewriters = docRewriters.rewriteables.collect { case (check, rewriter) if check(version, filename) => rewriter }
    assets.rewriteableAt(path, filename, rewriters)
  }

}
