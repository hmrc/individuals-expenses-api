/*
 * Copyright 2021 HM Revenue & Customs
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

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import play.api.{ConfigLoader, Configuration}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait AppConfig {

  // MTD ID Lookup Config
  def mtdIdBaseUrl: String

  // Downstream Config
  def desBaseUrl: String
  def desToken: String
  def desEnv: String
  def desEnvironmentHeaders: Option[Seq[String]]
  def ifsToken: String
  def ifsBaseUrl: String
  def ifsEnv: String
  def ifsEnvironmentHeaders: Option[Seq[String]]

  // API Config
  def apiStatus(version: String): String
  def apiGatewayContext: String
  def confidenceLevelConfig: ConfidenceLevelConfig

  // Business Rule Config
  def otherExpensesMinimumTaxYear: Int
  def employmentExpensesMinimumTaxYear: Int

  def featureSwitch: Option[Configuration]
  def endpointsEnabled(version: String): Boolean

}

@Singleton
class AppConfigImpl @Inject()(config: ServicesConfig, configuration: Configuration) extends AppConfig {

  // MTD ID Lookup Config
  val mtdIdBaseUrl: String = config.baseUrl("mtd-id-lookup")

  // Downstream Config
  val desBaseUrl: String = config.baseUrl("des")
  val desEnv: String = config.getString("microservice.services.des.env")
  val desToken: String = config.getString("microservice.services.des.token")
  val desEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.des.environmentHeaders")

  val ifsBaseUrl: String = config.baseUrl("ifs")
  val ifsEnv: String = config.getString("microservice.services.ifs.env")
  val ifsToken: String = config.getString("microservice.services.ifs.token")
  val ifsEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifs.environmentHeaders")

  // API Config
  val apiGatewayContext: String = config.getString("api.gateway.context")
  def apiStatus(version: String): String = config.getString(s"api.$version.status")
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")

  // Business Rule Config
  val otherExpensesMinimumTaxYear: Int = config.getInt("otherExpensesMinimumTaxYear")
  val employmentExpensesMinimumTaxYear: Int = config.getInt("employmentExpensesMinimumTaxYear")

  def featureSwitch: Option[Configuration] = configuration.getOptional[Configuration](s"feature-switch")

  def endpointsEnabled(version: String): Boolean = config.getBoolean(s"api.$version.endpoints.enabled")

}

case class ConfidenceLevelConfig(definitionEnabled: Boolean, authValidationEnabled: Boolean)
object ConfidenceLevelConfig {
  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }
}
