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

package v2.endpoints

import api.models.errors._
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import support.IntegrationBaseSpec

class IgnoreEmploymentExpensesControllerISpec extends IntegrationBaseSpec {

  "Calling the ignore endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new NonTysTest {

        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe OK
        response.json shouldBe responseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid request is made for a Tax Year Specific (TYS) tax year" in new TysIfsTest {

        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().post(requestBody))
        response.status shouldBe OK
        response.json shouldBe responseBody

        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {

        def parserErrorTest(newNino: String, newTaxYear: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"parser returns ${expectedBody.code}" in new NonTysTest {

            override val nino: String    = newNino
            override val taxYear: String = newTaxYear

            val response: WSResponse = await(request().post(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          ("AA123456ABCDEF", "2019-20", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "201920", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2016-17", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(args => (parserErrorTest _).tupled(args))

      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {

            override def setupStubs(): Unit = {
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().post(requestBody))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val errors = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_BEFORE_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (NOT_FOUND, "INCOME_SOURCE_NOT_FOUND", NOT_FOUND, NotFoundError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, StandardDownstreamError)
        )

        val extraTysErrors = List(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    val nino: String = "AA123456A"

    val taxYear: String
    val downstreamUri: String

    val requestBody: JsValue = Json.parse("{}")

    lazy val responseBody: JsValue = Json.parse(s"""
         |{
         |  "links": [
         |    {
         |      "href": "/individuals/expenses/employments/$nino/$taxYear",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/expenses/employments/$nino/$taxYear",
         |      "method": "DELETE",
         |      "rel": "delete-employment-expenses"
         |    }
         |  ]
         |}
         |""".stripMargin)

    def setupStubs(): Unit = {}

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()

      buildRequest(s"/employments/$nino/$taxYear/ignore")
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

    def errorBody(code: String): String =
      s"""
         |{
         |  "failures": [
         |    {
         |      "code": "$code",
         |      "reason": "downstream message"
         |    }
         |  ]
         |}
    """.stripMargin

  }

  private trait NonTysTest extends Test {
    val taxYear: String = "2019-20"

    val downstreamUri: String = s"/income-tax/expenses/employments/$nino/2019-20"
  }

  private trait TysIfsTest extends Test {
    val taxYear: String = "2023-24"

    val downstreamUri: String = s"/income-tax/23-24/expenses/employments/$nino"

    override def request(): WSRequest =
      super.request().addHttpHeaders("suspend-temporal-validations" -> "true")

  }

}
