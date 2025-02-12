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

import cats.implicits.catsSyntaxValidatedId
import shared.config.Deprecation.NotDeprecated
import shared.config.MockAppConfig
import shared.definition.APIStatus.BETA
import shared.definition._
import shared.mocks.MockHttpClient
import shared.routing.{Version2, Version3}
import shared.utils.UnitSpec

class ExpensesApiDefinitionFactorySpec extends UnitSpec with MockAppConfig {

  class Test extends MockHttpClient with MockAppConfig {
    MockedAppConfig.apiGatewayContext returns "individuals/expenses"
    val apiDefinitionFactory = new ExpensesApiDefinitionFactory(mockAppConfig)
  }

  "definition" when {
    "called" should {
      "return a valid Definition case class" in new Test {
        MockedAppConfig.apiStatus(Version2) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version2).returns(false).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version2).returns(NotDeprecated.valid).anyNumberOfTimes()
        MockedAppConfig.apiStatus(Version3) returns "BETA"
        MockedAppConfig.endpointsEnabled(Version3).returns(false).anyNumberOfTimes()
        MockedAppConfig.deprecationFor(Version3).returns(NotDeprecated.valid).anyNumberOfTimes()

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
                  status = BETA,
                  endpointsEnabled = false
                ),
                APIVersion(
                  version = Version3,
                  status = BETA,
                  endpointsEnabled = false
                )
              ),
              requiresTrust = None
            )
          )
      }
    }
  }

}
