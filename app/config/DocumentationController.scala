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

import config.DocumentationController.filenameWithFeatureName
import controllers.Assets
import definition.ApiDefinitionFactory
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.Logging

import javax.inject.{Inject, Singleton}

@Singleton
class DocumentationController @Inject() (selfAssessmentApiDefinition: ApiDefinitionFactory, cc: ControllerComponents, assets: Assets)(implicit
    appConfig: AppConfig)
    extends BackendController(cc)
    with Logging {

  private val openApiFeatures: Seq[OpenApiFeature] = FeatureSwitches().openApiFeatures

  def definition(): Action[AnyContent] = Action {
    Ok(Json.toJson(selfAssessmentApiDefinition.definition))
  }

  def file(version: String, filename: String): Action[AnyContent] = {
    assets.at(s"/public/api/conf/$version", fileToReturn(version, filename))
  }

  private[config] def fileToReturn(version: String, filename: String): String =
    openApiFeatures.find(_.matches(version, filename)) match {
      case Some(feature) => filenameWithFeatureName(filename, feature)
      case None          => filename
    }

}

object DocumentationController {

  private[config] def filenameWithFeatureName(filename: String, feature: OpenApiFeature): String = {
    val dotIdx = filename.lastIndexOf(".")
    if (dotIdx == -1) {
      s"${filename}_${feature.key}"
    } else {
      val ext  = filename.substring(dotIdx)
      val main = filename.substring(0, dotIdx)
      s"${main}_${feature.key}$ext"
    }
  }

}
