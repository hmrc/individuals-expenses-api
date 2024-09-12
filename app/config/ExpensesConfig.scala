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

import play.api.Configuration
import shared.config.{DownstreamConfig, FeatureSwitches}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class ExpensesConfig @Inject() (config: ServicesConfig, configuration: Configuration) {

  private def serviceKeyFor(serviceName: String) = s"microservice.services.$serviceName"

  protected def downstreamConfig(serviceName: String): DownstreamConfig = {
    val baseUrl = config.baseUrl(serviceName)

    val serviceKey = serviceKeyFor(serviceName)

    val env                = config.getString(s"$serviceKey.env")
    val token              = config.getString(s"$serviceKey.token")
    val environmentHeaders = configuration.getOptional[Seq[String]](s"$serviceKey.environmentHeaders")

    DownstreamConfig(baseUrl, env, token, environmentHeaders)
  }

  def ifsR5DownstreamConfig: DownstreamConfig = downstreamConfig("ifsR5")
  def ifsR6DownstreamConfig: DownstreamConfig = downstreamConfig("ifsR6")

  def featureSwitchConfig: Configuration = configuration.getOptional[Configuration](s"feature-switch").getOrElse(Configuration.empty)

  def featureSwitches: FeatureSwitches = ExpensesFeatureSwitches(featureSwitchConfig)

  def otherExpensesMinimumTaxYear: Int      = config.getInt("otherExpensesMinimumTaxYear")
  def employmentExpensesMinimumTaxYear: Int = config.getInt("employmentExpensesMinimumTaxYear")

}
