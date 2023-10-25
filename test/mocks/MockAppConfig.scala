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

package mocks

import config.{AppConfig, ConfidenceLevelConfig}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.Configuration
import routing.Version

trait MockAppConfig extends MockFactory {

  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {

    // Downstream Config
    def desBaseUrl: CallHandler[String]                         = (() => mockAppConfig.desBaseUrl).expects()
    def desToken: CallHandler[String]                           = (() => mockAppConfig.desToken).expects()
    def desEnvironment: CallHandler[String]                     = (() => mockAppConfig.desEnv).expects()
    def desEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.desEnvironmentHeaders).expects()

    // IFS Config
    def ifsR5BaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifsR5BaseUrl).expects()
    def ifsR5Token: CallHandler[String]                           = (() => mockAppConfig.ifsR5Token).expects()
    def ifsR5Environment: CallHandler[String]                     = (() => mockAppConfig.ifsR5Env).expects()
    def ifsR5EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifsR5EnvironmentHeaders).expects()

    def ifsR6BaseUrl: CallHandler[String]                         = (() => mockAppConfig.ifsR6BaseUrl).expects()
    def ifsR6Token: CallHandler[String]                           = (() => mockAppConfig.ifsR6Token).expects()
    def ifsR6Environment: CallHandler[String]                     = (() => mockAppConfig.ifsR6Env).expects()
    def ifsR6EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.ifsR6EnvironmentHeaders).expects()

    def tysIfsBaseUrl: CallHandler[String]                         = (() => mockAppConfig.tysIfsBaseUrl).expects()
    def tysIfsToken: CallHandler[String]                           = (() => mockAppConfig.tysIfsToken).expects()
    def tysIfsEnvironment: CallHandler[String]                     = (() => mockAppConfig.tysIfsEnv).expects()
    def tysIfsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (() => mockAppConfig.tysIfsEnvironmentHeaders).expects()

    // MTD ID Lookup Config
    def mtdIdBaseUrl: CallHandler[String] = (() => mockAppConfig.mtdIdBaseUrl).expects()

    // API Config
    def featureSwitches: CallHandler[Configuration] = (() => mockAppConfig.featureSwitches).expects()
    def apiGatewayContext: CallHandler[String]      = (() => mockAppConfig.apiGatewayContext).expects()

    def apiStatus(version: Version): CallHandler[String]         = (mockAppConfig.apiStatus(_: Version)).expects(version)
    def endpointsEnabled(version: String): CallHandler[Boolean]  = (mockAppConfig.endpointsEnabled(_: String)).expects(version)
    def endpointsEnabled(version: Version): CallHandler[Boolean] = (mockAppConfig.endpointsEnabled(_: Version)).expects(version)

    def apiVersionReleasedInProduction(version: String): CallHandler[Boolean] =
      (mockAppConfig.apiVersionReleasedInProduction: String => Boolean).expects(version)

    def endpointReleasedInProduction(version: String, key: String): CallHandler[Boolean] =
      (mockAppConfig.endpointReleasedInProduction: (String, String) => Boolean).expects(version, key)

    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] =
      (() => mockAppConfig.confidenceLevelConfig).expects()

    // Business Rule Config
    def otherExpensesMinimumTaxYear: CallHandler[Int]      = (() => mockAppConfig.otherExpensesMinimumTaxYear).expects()
    def employmentExpensesMinimumTaxYear: CallHandler[Int] = (() => mockAppConfig.employmentExpensesMinimumTaxYear).expects()

  }

}
