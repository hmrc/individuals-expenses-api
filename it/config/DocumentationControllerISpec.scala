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

import io.swagger.v3.parser.OpenAPIV3Parser
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import support.IntegrationBaseSpec
import uk.gov.hmrc.auth.core.ConfidenceLevel

import scala.util.Try

class DocumentationControllerISpec extends IntegrationBaseSpec {

  val config: AppConfig                = app.injector.instanceOf[AppConfig]
  val confidenceLevel: ConfidenceLevel = config.confidenceLevelConfig.confidenceLevel

  private val apiDefinitionJson = Json.parse(
    s"""
      |{
      |   "scopes":[
      |      {
      |        "key":"read:self-assessment",
      |        "name":"View your Self Assessment information",
      |        "description":"Allow read access to self assessment data",
      |        "confidenceLevel": $confidenceLevel
      |      },
      |      {
      |        "key":"write:self-assessment",
      |        "name":"Change your Self Assessment information",
      |        "description":"Allow write access to self assessment data",
      |        "confidenceLevel": $confidenceLevel
      |      }
      |   ],
      |   "api":{
      |      "name":"Individuals Expenses (MTD)",
      |      "description":"An API for retrieving individual expenses data for Self Assessment",
      |      "context":"individuals/expenses",
      |      "categories":[
      |         "INCOME_TAX_MTD"
      |      ],
      |      "versions":[
      |         {
      |            "version":"1.0",
      |            "status":"BETA",
      |            "endpointsEnabled":true
      |         },
      |         {
      |            "version":"2.0",
      |            "status":"BETA",
      |            "endpointsEnabled":true
      |         }
      |      ]
      |   }
      |}
    """.stripMargin
  )

  "GET /api/definition" should {
    "return a 200 with the correct response body" in {
      val response = get("/api/definition")
      Json.parse(response.body) shouldBe apiDefinitionJson
    }
  }

  "an OAS documentation request" must {
    "return the V1 documentation that passes OAS V3 parser" in {
      val response = get("/api/conf/1.0/application.yaml")

      val body         = response.body[String]
      val parserResult = Try(new OpenAPIV3Parser().readContents(body))
      parserResult.isSuccess shouldBe true

      val openAPI = Option(parserResult.get.getOpenAPI).getOrElse(fail("openAPI wasn't defined"))
      openAPI.getOpenapi shouldBe "3.0.3"
      withClue("If v1.0 endpoints are enabled in application.conf, remove the [test only] from this test: ") {
        openAPI.getInfo.getTitle shouldBe "Individuals Expenses (MTD)"
      }
      openAPI.getInfo.getVersion shouldBe "1.0"
    }

    "return the V2 documentation that passes OAS V3 parser" in {
      val response = get("/api/conf/2.0/application.yaml")

      val body         = response.body[String]
      val parserResult = Try(new OpenAPIV3Parser().readContents(body))
      parserResult.isSuccess shouldBe true

      val openAPI = Option(parserResult.get.getOpenAPI).getOrElse(fail("openAPI wasn't defined"))
      openAPI.getOpenapi shouldBe "3.0.3"
      withClue("If v2.0 endpoints are enabled and released in production in application.conf, remove the [test only] from this test: ") {
        openAPI.getInfo.getTitle shouldBe "Individuals Expenses (MTD) [test only]"
      }
      openAPI.getInfo.getVersion shouldBe "2.0"
    }

    "return the expected endpoint description" when {
      "the relevant feature switch is enabled" in {
        val response = get("/api/conf/2.0/employment_expenses_retrieve.yaml")
        val body     = response.body[String]

        withClue("Depends on the oas-feature-example feature switch") {
          body should not include ("Gov-Test-Scenario headers are available only in")
          body should include("Gov-Test-Scenario headers is only available in")
        }
      }
    }
  }

  private def get(path: String): WSResponse = {
    val response: WSResponse = await(buildRequest(path).get())
    response.status shouldBe OK
    response
  }

}
