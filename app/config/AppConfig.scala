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

import com.typesafe.config.{Config, ConfigValue}
import play.api.{ConfigLoader, Configuration}
import shared.routing.Version
import shared.config.DownstreamConfig
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.util
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters._

trait AppConfig {

  // MTD ID Lookup Config
  def mtdIdBaseUrl: String

  def keyValuesJ: util.Map[String, ConfigValue]

  // Downstream Config
  def desBaseUrl: String
  def desToken: String
  def desEnv: String
  def desEnvironmentHeaders: Option[Seq[String]]

  lazy val desDownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = desBaseUrl, env = desEnv, token = desToken, environmentHeaders = desEnvironmentHeaders)

  def ifsR5Token: String
  def ifsR5BaseUrl: String
  def ifsR5Env: String
  def ifsR5EnvironmentHeaders: Option[Seq[String]]

  lazy val ifsR5DownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = ifsR5BaseUrl, env = ifsR5Env, token = ifsR5Token, environmentHeaders = ifsR5EnvironmentHeaders)

  def ifsR6Token: String
  def ifsR6BaseUrl: String
  def ifsR6Env: String
  def ifsR6EnvironmentHeaders: Option[Seq[String]]

  lazy val ifsR6DownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = ifsR6BaseUrl, env = ifsR6Env, token = ifsR6Token, environmentHeaders = ifsR6EnvironmentHeaders)

  // Tax Year Specific (TYS) IFS Config
  def tysIfsBaseUrl: String
  def tysIfsEnv: String
  def tysIfsToken: String
  def tysIfsEnvironmentHeaders: Option[Seq[String]]

  lazy val taxYearSpecificIfsDownstreamConfig: DownstreamConfig =
    DownstreamConfig(baseUrl = tysIfsBaseUrl, env = tysIfsEnv, token = tysIfsToken, environmentHeaders = tysIfsEnvironmentHeaders)

  // API Config
  def apiStatus(version: Version): String
  def apiGatewayContext: String
  def confidenceLevelConfig: ConfidenceLevelConfig

  // Business Rule Config
  def otherExpensesMinimumTaxYear: Int
  def employmentExpensesMinimumTaxYear: Int

  def featureSwitches: Configuration
  def endpointsEnabled(version: String): Boolean
  def endpointsEnabled(version: Version): Boolean

  def safeEndpointsEnabled(version: String): Boolean

  /** Currently only for OAS documentation.
    */
  def apiVersionReleasedInProduction(version: String): Boolean

  /** Currently only for OAS documentation.
    */
  def endpointReleasedInProduction(version: String, name: String): Boolean

  /** Defaults to false
    */
  def endpointAllowsSupportingAgents(endpointName: String): Boolean
}

@Singleton
class AppConfigImpl @Inject() (config: ServicesConfig, protected[config] val configuration: Configuration) extends AppConfig {

  val keyValuesJ: util.Map[String, ConfigValue] = configuration.entrySet.toMap.asJava

  // MTD ID Lookup Config
  val mtdIdBaseUrl: String = config.baseUrl("mtd-id-lookup")

  // Downstream Config
  val desBaseUrl: String                         = config.baseUrl("des")
  val desEnv: String                             = config.getString("microservice.services.des.env")
  val desToken: String                           = config.getString("microservice.services.des.token")
  val desEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.des.environmentHeaders")

  val ifsR5BaseUrl: String                         = config.baseUrl("ifsR5")
  val ifsR5Env: String                             = config.getString("microservice.services.ifsR5.env")
  val ifsR5Token: String                           = config.getString("microservice.services.ifsR5.token")
  val ifsR5EnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifsR5.environmentHeaders")

  val ifsR6BaseUrl: String                         = config.baseUrl("ifsR6")
  val ifsR6Env: String                             = config.getString("microservice.services.ifsR6.env")
  val ifsR6Token: String                           = config.getString("microservice.services.ifsR6.token")
  val ifsR6EnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.ifsR6.environmentHeaders")

  // Tax Year Specific (TYS) IFS Config
  val tysIfsBaseUrl: String                         = config.baseUrl("tys-ifs")
  val tysIfsEnv: String                             = config.getString("microservice.services.tys-ifs.env")
  val tysIfsToken: String                           = config.getString("microservice.services.tys-ifs.token")
  val tysIfsEnvironmentHeaders: Option[Seq[String]] = configuration.getOptional[Seq[String]]("microservice.services.tys-ifs.environmentHeaders")

  // API Config
  val apiGatewayContext: String                    = config.getString("api.gateway.context")
  def apiStatus(version: Version): String          = config.getString(s"api.$version.status")
  def featureSwitches: Configuration               = configuration.getOptional[Configuration](s"feature-switch").getOrElse(Configuration.empty)
  val confidenceLevelConfig: ConfidenceLevelConfig = configuration.get[ConfidenceLevelConfig](s"api.confidence-level-check")

  // Business Rule Config
  val otherExpensesMinimumTaxYear: Int      = config.getInt("otherExpensesMinimumTaxYear")
  val employmentExpensesMinimumTaxYear: Int = config.getInt("employmentExpensesMinimumTaxYear")

  def endpointsEnabled(version: String): Boolean  = config.getBoolean(s"api.$version.endpoints.enabled")
  def endpointsEnabled(version: Version): Boolean = config.getBoolean(s"api.${version.name}.endpoints.enabled")

  def safeEndpointsEnabled(version: String): Boolean =
    configuration
      .getOptional[Boolean](s"api.$version.endpoints.enabled")
      .getOrElse(false)

  def apiVersionReleasedInProduction(version: String): Boolean = config.getBoolean(s"api.$version.endpoints.api-released-in-production")

  def endpointReleasedInProduction(version: String, name: String): Boolean = {
    val versionReleasedInProd = apiVersionReleasedInProduction(version)
    val path                  = s"api.$version.endpoints.released-in-production.$name"

    val conf = configuration.underlying
    if (versionReleasedInProd && conf.hasPath(path)) config.getBoolean(path) else versionReleasedInProd
  }

  def endpointAllowsSupportingAgents(endpointName: String): Boolean =
    supportingAgentEndpoints.getOrElse(endpointName, false)

  private val supportingAgentEndpoints: Map[String, Boolean] =
    configuration
      .getOptional[Map[String, Boolean]]("api.supporting-agent-endpoints")
      .getOrElse(Map.empty)

}

case class ConfidenceLevelConfig(confidenceLevel: ConfidenceLevel, definitionEnabled: Boolean, authValidationEnabled: Boolean)

object ConfidenceLevelConfig {

  implicit val configLoader: ConfigLoader[ConfidenceLevelConfig] = (rootConfig: Config, path: String) => {
    val config = rootConfig.getConfig(path)
    ConfidenceLevelConfig(
      confidenceLevel = ConfidenceLevel.fromInt(config.getInt("confidence-level")).getOrElse(ConfidenceLevel.L200),
      definitionEnabled = config.getBoolean("definition.enabled"),
      authValidationEnabled = config.getBoolean("auth-validation.enabled")
    )
  }

}
