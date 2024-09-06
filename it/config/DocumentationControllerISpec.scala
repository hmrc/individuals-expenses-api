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
import routing.Version2
import support.IntegrationBaseSpec

import scala.util.Try

class DocumentationControllerISpec extends IntegrationBaseSpec {

  private val config          = app.injector.instanceOf[AppConfig]
  private val confidenceLevel = config.confidenceLevelConfig.confidenceLevel

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
    List(Version2).foreach { version =>
      s"return the documentation for $version" in {
        val response = get(s"/api/conf/${version.name}/application.yaml")

        val body         = response.body
        val parserResult = Try(new OpenAPIV3Parser().readContents(body)).getOrElse(fail("openAPI couldn't read contents"))

        val openAPI = Option(parserResult.getOpenAPI).getOrElse(fail("openAPI wasn't defined"))
        openAPI.getOpenapi shouldBe "3.0.3"
        withClue(s"If v${version.name} endpoints are enabled in application.conf, remove the [test only] from this test: ") {
          openAPI.getInfo.getTitle shouldBe "Individuals Expenses (MTD)"
        }
        openAPI.getInfo.getVersion shouldBe version.toString
      }

      s"return the documentation with the correct accept header for version $version" in {
        val response = get(s"/api/conf/${version.name}/common/headers.yaml")
        val body     = response.body

        val headerRegex = """(?s).*?application/vnd\.hmrc\.(\d+\.\d+)\+json.*?""".r
        val header      = headerRegex.findFirstMatchIn(body)
        header.isDefined shouldBe true

        val versionFromHeader = header.get.group(1)
        versionFromHeader shouldBe version.name

      }
    }

    "return the expected endpoint description" when {
      "the relevant feature switch is enabled" in {
        val response = get("/api/conf/2.0/employment_expenses_retrieve.yaml")
        val body     = response.body

        withClue("Depends on the oas-feature-example feature switch") {
          body should not include "Gov-Test-Scenario headers are available only in"
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
