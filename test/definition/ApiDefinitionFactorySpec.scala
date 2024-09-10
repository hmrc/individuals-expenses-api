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

package definition

import api.mocks.MockHttpClient
import config.ConfidenceLevelConfig
import definition.APIStatus.{ALPHA, BETA}
import config.MockAppConfig
import routing.Version2
import support.UnitSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel

class ApiDefinitionFactorySpec extends UnitSpec {

  class Test extends MockHttpClient with MockAppConfig {
    val apiDefinitionFactory = new ApiDefinitionFactory(mockAppConfig)
    MockedAppConfig.apiGatewayContext returns "individuals/expenses"
  }

  private val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L200

  "definition" when {
    "called" should {
      "return a valid Definition case class when confidence level 200 checking is enforced" in {
        testDefinitionWithConfidence(ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = true, authValidationEnabled = true))
      }

      "return a valid Definition case class when confidence level checking 50 is enforced" in {
        testDefinitionWithConfidence(
          ConfidenceLevelConfig(confidenceLevel = confidenceLevel, definitionEnabled = false, authValidationEnabled = false))
      }

      def testDefinitionWithConfidence(confidenceLevelConfig: ConfidenceLevelConfig): Unit = new Test {
        MockedAppConfig.apiStatus(Version2) returns "ALPHA"
        MockedAppConfig.endpointsEnabled(Version2) returns false
        MockedAppConfig.confidenceLevelConfig.returns(confidenceLevelConfig).anyNumberOfTimes()

        apiDefinitionFactory.definition shouldBe
          Definition(
            api = APIDefinition(
              name = "Individuals Expenses (MTD)",
              description = "An API for retrieving individual expenses data for Self Assessment",
              context = "individuals/expenses",
              categories = List("INCOME_TAX_MTD"),
              versions = List(
                APIVersion(
                  version = Version2,
                  status = ALPHA,
                  endpointsEnabled = false
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

  "confidenceLevel" when {
    List(
      (true, ConfidenceLevel.L250, ConfidenceLevel.L250),
      (true, ConfidenceLevel.L200, ConfidenceLevel.L200),
      (false, ConfidenceLevel.L200, ConfidenceLevel.L50)
    ).foreach { case (definitionEnabled, configCL, expectedDefinitionCL) =>
      s"confidence-level-check.definition.enabled is $definitionEnabled and confidence-level = $configCL" should {
        s"return confidence level $expectedDefinitionCL" in new Test {
          MockedAppConfig.confidenceLevelConfig returns ConfidenceLevelConfig(
            confidenceLevel = configCL,
            definitionEnabled = definitionEnabled,
            authValidationEnabled = true)
          apiDefinitionFactory.confidenceLevel shouldBe expectedDefinitionCL
        }
      }
    }
  }

  "buildAPIStatus" when {
    "the 'apiStatus' parameter is present and valid" should {
      "return the correct status" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        apiDefinitionFactory.buildAPIStatus(version = Version2) shouldBe BETA
      }
    }

    "the 'apiStatus' parameter is present and invalid" should {
      "default to alpha" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "ALPHA"
        apiDefinitionFactory.buildAPIStatus(version = Version2) shouldBe ALPHA
      }
    }
  }

}
