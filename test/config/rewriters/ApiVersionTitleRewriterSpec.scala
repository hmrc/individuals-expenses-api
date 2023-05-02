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

import config.rewriters.ApiVersionTitleRewriter.rewriteApiVersionTitle
import mocks.MockAppConfig
import support.UnitSpec

class ApiVersionTitleRewriterSpec extends UnitSpec with MockAppConfig {

  "check and rewrite" when {
    val (check, rewrite) = rewriteApiVersionTitle

    "check() is given application.yaml with API endpoints disabled (assuming in production)" should {
      "indicate rewrite needed" in {
        MockAppConfig.endpointsEnabled("1.0") returns false
        val result = check("1.0", "application.yaml", mockAppConfig)
        result shouldBe true
      }
    }

    "check() is given any other combination" should {
      "indicate rewrite not needed" in {
        MockAppConfig.endpointsEnabled("1.0") returns true
        val result = check("1.0", "application.yaml", mockAppConfig)
        result shouldBe false
      }
      "also indicate rewrite not needed" in {
        val result = check("1.0", "any_other_file.yaml", mockAppConfig)
        result shouldBe false
      }
    }

    "the title already contains [test only]" should {
      "return the title unchanged" in {
        val title  = """  title: "[tesT oNLy] API title (MTD)""""
        val result = rewrite("", "", mockAppConfig, title)
        result shouldBe title
      }
    }

    "the yaml title is ready to be rewritten" should {
      "return the rewritten title" in {
        val yaml =
          """
            |openapi: "3.0.3"
            |
            |info:
            |  version: "1.0"
            |  title: Individuals Expenses (MTD)
            |  description: |
            |    # Send fraud prevention data
            |    HMRC monitors transactions to help protect your customers' confidential data from criminals and fraudsters.
            |
            |servers:
            |  - url: https://test-api.service.hmrc.gov.uk
            |""".stripMargin

        val expected =
          """
            |openapi: "3.0.3"
            |
            |info:
            |  version: "1.0"
            |  title: "Individuals Expenses (MTD) [test only]"
            |  description: |
            |    # Send fraud prevention data
            |    HMRC monitors transactions to help protect your customers' confidential data from criminals and fraudsters.
            |
            |servers:
            |  - url: https://test-api.service.hmrc.gov.uk
            |""".stripMargin

        val result = rewrite("", "", mockAppConfig, yaml)
        result shouldBe expected
      }
    }

    "the yaml title is in quotes" should {
      "return the rewritten title" in {
        val result = rewrite("", "", mockAppConfig, """  title: "API title (MTD)"""")
        result shouldBe """  title: "API title (MTD) [test only]""""
      }
    }
  }

}
