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

trait MockAppConfig extends MockFactory {

  val mockAppConfig: AppConfig = mock[AppConfig]

  object MockedAppConfig {

    // Downstream Config
    def desBaseUrl: CallHandler[String]                         = (mockAppConfig.desBaseUrl _: () => String).expects()
    def desToken: CallHandler[String]                           = (mockAppConfig.desToken _).expects()
    def desEnvironment: CallHandler[String]                     = (mockAppConfig.desEnv _).expects()
    def desEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (mockAppConfig.desEnvironmentHeaders _).expects()

    // IFS Config
    def ifsR5BaseUrl: CallHandler[String]                         = (mockAppConfig.ifsR5BaseUrl _: () => String).expects()
    def ifsR5Token: CallHandler[String]                           = (mockAppConfig.ifsR5Token _).expects()
    def ifsR5Environment: CallHandler[String]                     = (mockAppConfig.ifsR5Env _).expects()
    def ifsR5EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (mockAppConfig.ifsR5EnvironmentHeaders _).expects()

    def ifsR6BaseUrl: CallHandler[String]                         = (mockAppConfig.ifsR6BaseUrl _: () => String).expects()
    def ifsR6Token: CallHandler[String]                           = (mockAppConfig.ifsR6Token _).expects()
    def ifsR6Environment: CallHandler[String]                     = (mockAppConfig.ifsR6Env _).expects()
    def ifsR6EnvironmentHeaders: CallHandler[Option[Seq[String]]] = (mockAppConfig.ifsR6EnvironmentHeaders _).expects()

    def tysIfsBaseUrl: CallHandler[String]                         = (mockAppConfig.tysIfsBaseUrl _: () => String).expects()
    def tysIfsToken: CallHandler[String]                           = (mockAppConfig.tysIfsToken _).expects()
    def tysIfsEnvironment: CallHandler[String]                     = (mockAppConfig.tysIfsEnv _).expects()
    def tysIfsEnvironmentHeaders: CallHandler[Option[Seq[String]]] = (mockAppConfig.tysIfsEnvironmentHeaders _).expects()

    // MTD ID Lookup Config
    def mtdIdBaseUrl: CallHandler[String] = (mockAppConfig.mtdIdBaseUrl _: () => String).expects()

    // API Config
    def featureSwitches: CallHandler[Configuration] = (mockAppConfig.featureSwitches _: () => Configuration).expects()
    def apiGatewayContext: CallHandler[String]      = (mockAppConfig.apiGatewayContext _: () => String).expects()
    def apiStatus: CallHandler[String]              = (mockAppConfig.apiStatus: String => String).expects("1.0")
    def endpointsEnabled: CallHandler[Boolean]      = (mockAppConfig.endpointsEnabled: String => Boolean).expects("1.0")

    def confidenceLevelCheckEnabled: CallHandler[ConfidenceLevelConfig] =
      (mockAppConfig.confidenceLevelConfig _: () => ConfidenceLevelConfig).expects()

    // Business Rule Config
    def otherExpensesMinimumTaxYear: CallHandler[Int]      = (mockAppConfig.otherExpensesMinimumTaxYear _).expects()
    def employmentExpensesMinimumTaxYear: CallHandler[Int] = (mockAppConfig.employmentExpensesMinimumTaxYear _).expects()

  }

}
