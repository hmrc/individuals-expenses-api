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

import mocks.MockAppConfig
import support.UnitSpec

class EndpointSummaryGroupRewriterSpec extends UnitSpec with MockAppConfig {

  val rewriter = new EndpointSummaryGroupRewriter(mockAppConfig)

  "check and rewrite for the 'workaround' grouped endpoints yaml file" should {
    // e.g. employment_expenses.yaml which points to employment_expenses_create_and_amend.yaml etc,
    // but must include the endpoint summary as an OAS renderer workaround.

    val (check, rewrite) = rewriter.rewriteGroupedEndpointSummaries.asTuple

    "indicate rewrite needed" in {
      val result = check("any-version", "employment_expenses.yaml")
      result shouldBe true
    }

    "indicate rewrite not needed" in {
      val result = check("1.0", "employment_expenses.json")
      result shouldBe false
    }

    "rewrite all summaries in the group yaml file" in {
      MockAppConfig.apiVersionReleasedInProduction("1.0") returns true

      MockAppConfig.endpointReleasedInProduction("1.0", "employment-expenses-create-and-amend") returns false
      MockAppConfig.endpointReleasedInProduction("1.0", "employment-expenses-retrieve") returns true
      MockAppConfig.endpointReleasedInProduction("1.0", "employment-expenses-delete") returns false

      val yaml =
        """
          |put:
          |  $ref: "./employment_expenses_create_and_amend.yaml"
          |  summary: Create and Amend Employment Expenses{{#maybeTestOnly "2.0 employment-expenses-create-and-amend"}}
          |  security:
          |    - User-Restricted:
          |        - write:self-assessment
          |
          |
          |get:
          |  $ref: "./employment_expenses_retrieve.yaml"
          |  summary: Retrieve Employment Expenses{{#maybeTestOnly "2.0 employment-expenses-retrieve"}}
          |  security:
          |    - User-Restricted:
          |        - read:self-assessment
          |  parameters:
          |    - $ref: './common/queryParameters.yaml#/components/parameters/source'
          |
          |delete:
          |  $ref: "./employment_expenses_delete.yaml"
          |  summary: Delete Employment Expenses{{#maybeTestOnly "2.0 employment-expenses-delete"}}
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

      val result = rewrite(path = "/public/api/conf/1.0", filename = "employment_expenses.yaml", yaml)
      result shouldBe expected
    }
  }

}
