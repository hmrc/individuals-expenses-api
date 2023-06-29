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

import org.apache.commons.lang3.BooleanUtils
import play.api.Configuration
import play.api.mvc.Request

import scala.util.matching.Regex

case class FeatureSwitches(featureSwitchConfig: Configuration) {

  val isTaxYearSpecificApiEnabled: Boolean = isEnabled("tys-api.enabled")

  val openApiFeatures: Seq[OpenApiFeature] = List(
    OpenApiFeatureTest
  ).filter { feature: OpenApiFeature => isEnabled(feature.key + ".enabled") }

  def isTemporalValidationEnabled(implicit request: Request[_]): Boolean = {
    if (isEnabled("allowTemporalValidationSuspension.enabled")) {
      request.headers.get("suspend-temporal-validations").forall(!BooleanUtils.toBoolean(_))
    } else {
      true
    }
  }

  private def isEnabled(key: String): Boolean = featureSwitchConfig.getOptional[Boolean](key).getOrElse(true)
}

object FeatureSwitches {
  def apply()(implicit appConfig: AppConfig): FeatureSwitches = FeatureSwitches(appConfig.featureSwitches)
}

trait OpenApiFeature {
  val key: String
  val version: String
  val fileMatchers: Seq[Regex]

  def matches(requestedVersion: String, filename: String): Boolean = requestedVersion == version && matches(filename)

  private[config] def matches(filename: String): Boolean = fileMatchers.exists(_.findFirstIn(filename).isDefined)
}

case object OpenApiFeatureTest extends OpenApiFeature {
  val key     = "openApiFeatureTest"
  val version = "1.0"

  val fileMatchers = List(
    "^employment_expenses_retrieve\\.yaml$".r,
    "^other_expenses_retrieve\\.yaml$".r
  )

}
