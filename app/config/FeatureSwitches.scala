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

case class FeatureSwitches(featureSwitchConfig: Configuration) {

  private val versionRegex = """(\d)\.\d""".r

  def isVersionEnabled(version: String): Boolean = {
    val maybeVersion: Option[String] =
      version match {
        case versionRegex(v) => Some(v)
        case _               => None
      }

    val enabled = for {
      versionNo <- maybeVersion
      enabled   <- featureSwitchConfig.getOptional[Boolean](s"version-$versionNo.enabled")
    } yield enabled

    enabled.getOrElse(false)
  }

  val isTaxYearSpecificApiEnabled: Boolean = isEnabled("tys-api")

  /** Is this feature enabled in the current environment?
    */
  val isOasFeatureExampleEnabled: Boolean = isEnabled("oas-feature-example")

  /** Is this feature released (available) in production?
    */
  val isOasFeatureExampleReleased: Boolean = isReleasedInProduction("oas-feature-example")

  def isTemporalValidationEnabled(implicit request: Request[_]): Boolean = {
    if (isEnabled("allowTemporalValidationSuspension")) {
      request.headers.get("suspend-temporal-validations").forall(!BooleanUtils.toBoolean(_))
    } else {
      true
    }
  }

  def isEnabled(key: String): Boolean              = isConfigTrue(key + ".enabled")
  def isReleasedInProduction(key: String): Boolean = isConfigTrue(key + ".released")

  private def isConfigTrue(key: String): Boolean = featureSwitchConfig.getOptional[Boolean](key).getOrElse(true)
}

object FeatureSwitches {
  def apply()(implicit appConfig: AppConfig): FeatureSwitches = FeatureSwitches(appConfig.featureSwitches)
}
