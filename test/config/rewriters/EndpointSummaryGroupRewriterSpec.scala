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

package config.rewriters

import config.rewriters.EndpointSummaryGroupRewriter.rewriteGroupedEndpointSummaries
import mocks.MockAppConfig
import support.UnitSpec

class EndpointSummaryGroupRewriterSpec extends UnitSpec with MockAppConfig {

  "check and rewrite for the 'workaround' grouped endpoints yaml file" should {
    // e.g. employment_expenses.yaml which points to employment_expenses_create_and_amend.yaml etc,
    // but must include the endpoint summary as an OAS renderer workaround.

    val (check, rewrite) = rewriteGroupedEndpointSummaries

    "indicate rewrite needed" in {
      MockAppConfig.endpointSwitches("1.0") returns Map("employment-expenses-create-and-amend.enabled" -> false)

      val result = check("1.0", "employment_expenses.yaml", mockAppConfig)
      result shouldBe true
    }

    "indicate rewrite not needed" in {
      MockAppConfig.endpointSwitches("1.0") returns Map(
        "employment-expenses-create-and-amend.enabled" -> true
      )
      val result = check("1.0", "employment_expenses.yaml", mockAppConfig)
      result shouldBe false
    }

    "indicate rewrite not needed given no endpoint API config entries" in {
      MockAppConfig.endpointSwitches("1.0") returns Map.empty
      val result = check("1.0", "employment_expenses.enabled.yaml", mockAppConfig)
      result shouldBe false
    }

    "rewrite all summaries in the group yaml file" in {
      MockAppConfig.endpointSwitches("1.0") returns Map(
        "employment-expenses-create-and-amend" -> false,
        "employment-expenses-retrieve"         -> true,
        "employment-expenses-delete"           -> false
      )

      val yaml =
        """
          |put:
          |  $ref: "./employment_expenses_create_and_amend.yaml"
          |  summary: Create and Amend Employment Expenses
          |  security:
          |    - User-Restricted:
          |        - write:self-assessment
          |
          |
          |get:
          |  $ref: "./employment_expenses_retrieve.yaml"
          |  summary: Retrieve Employment Expenses
          |  security:
          |    - User-Restricted:
          |        - read:self-assessment
          |  parameters:
          |    - $ref: './common/queryParameters.yaml#/components/parameters/source'
          |
          |delete:
          |  $ref: "./employment_expenses_delete.yaml"
          |  summary: Delete Employment Expenses
          |  security:
          |    - User-Restricted:
          |        - write:self-assessment
          |
          |""".stripMargin

      val expected =
        """
          |put:
          |  $ref: "./employment_expenses_create_and_amend.yaml"
          |  summary: "Create and Amend Employment Expenses [test only]"
          |  security:
          |    - User-Restricted:
          |        - write:self-assessment
          |
          |
          |get:
          |  $ref: "./employment_expenses_retrieve.yaml"
          |  summary: Retrieve Employment Expenses
          |  security:
          |    - User-Restricted:
          |        - read:self-assessment
          |  parameters:
          |    - $ref: './common/queryParameters.yaml#/components/parameters/source'
          |
          |delete:
          |  $ref: "./employment_expenses_delete.yaml"
          |  summary: "Delete Employment Expenses [test only]"
          |  security:
          |    - User-Restricted:
          |        - write:self-assessment
          |
          |""".stripMargin

      val result = rewrite(path = "/public/api/conf/1.0", filename = "employment_expenses.yaml", mockAppConfig, yaml)
      result shouldBe expected

    }
  }

}
