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

package v1.endpoints

import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class CreateAmendEmploymentExpensesControllerISpec extends IntegrationBaseSpec {

  "Calling the amend endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new NonTysTest {

        override def setupStubs(): Unit =
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.json shouldBe hateoasResponse
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid TYS request is made" in new TysIfsTest {

        override def setupStubs(): Unit =
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.json shouldBe hateoasResponse
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      val validRequestBodyJson: JsValue = Json.parse(
        s"""
           |{
           |    "expenses": {
           |        "businessTravelCosts": 123.12,
           |        "jobExpenses": 123.12,
           |        "flatRateJobExpenses": 123.12,
           |        "professionalSubscriptions": 123.12,
           |        "hotelAndMealExpenses": 123.12,
           |        "otherAndCapitalAllowances": 123.12,
           |        "vehicleExpenses": 123.12,
           |        "mileageAllowanceRelief": 123.12
           |    }
           |}
           |""".stripMargin
      )

      "validation error" when {

        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestBody: JsValue,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {

          s"validation fails with ${expectedBody.code} error" in new NonTysTest {

            override val nino: String             = requestNino
            override val taxYear: String          = requestTaxYear
            override val requestBodyJson: JsValue = requestBody

            val response: WSResponse = await(request().post(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("AA1123A", "2019-20", validRequestBodyJson, BAD_REQUEST, NinoFormatError),
          ("AA123456A", "20199", validRequestBodyJson, BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2018-19", validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2019-21", validRequestBodyJson, BAD_REQUEST, RuleTaxYearRangeInvalidError),
          ("AA123456A", getCurrentTaxYear, validRequestBodyJson, BAD_REQUEST, RuleTaxYearNotEndedError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))

        s"an invalid amount is provided" in new NonTysTest {

          override val requestBodyJson: JsValue = Json.parse(
            s"""
               |{
               |    "expenses": {
               |        "businessTravelCosts": -1,
               |        "jobExpenses": -1,
               |        "flatRateJobExpenses": -1,
               |        "professionalSubscriptions": -1,
               |        "hotelAndMealExpenses": -1,
               |        "otherAndCapitalAllowances": -1,
               |        "vehicleExpenses": -1,
               |        "mileageAllowanceRelief": -1
               |    }
               |}
               |""".stripMargin
          )

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(ValueFormatError.copy(paths = Some(Seq(
            "/expenses/businessTravelCosts",
            "/expenses/jobExpenses",
            "/expenses/flatRateJobExpenses",
            "/expenses/professionalSubscriptions",
            "/expenses/hotelAndMealExpenses",
            "/expenses/otherAndCapitalAllowances",
            "/expenses/vehicleExpenses",
            "/expenses/mileageAllowanceRelief"
          ))))
        }

        s"an empty body is provided" in new NonTysTest {

          override val requestBodyJson: JsValue = Json.parse("""{}""")

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleIncorrectOrEmptyBodyError)
        }

        s"an empty expenses body is provided" in new NonTysTest {

          override val requestBodyJson: JsValue = Json.parse("""
              |{
              |    "expenses": {}
              |}
              |""".stripMargin)

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleIncorrectOrEmptyBodyError)
        }
      }

      "downstream service error" when {

        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {

            override def setupStubs(): Unit =
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val errors = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (UNPROCESSABLE_ENTITY, "INVALID_REQUEST_BEFORE_TAX_YEAR", BAD_REQUEST, RuleTaxYearNotEndedError),
          (NOT_FOUND, "INCOME_SOURCE_NOT_FOUND", NOT_FOUND, NotFoundError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, StandardDownstreamError)
        )

        val extraTysErrors = Seq(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    val nino: String = "AA123456A"

    def taxYear: String

    val amount: BigDecimal = 123.12

    def requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |    "expenses": {
         |        "businessTravelCosts": $amount,
         |        "jobExpenses": $amount,
         |        "flatRateJobExpenses": $amount,
         |        "professionalSubscriptions": $amount,
         |        "hotelAndMealExpenses": $amount,
         |        "otherAndCapitalAllowances": $amount,
         |        "vehicleExpenses": $amount,
         |        "mileageAllowanceRelief": $amount
         |    }
         |}
         |""".stripMargin
    )

    val hateoasResponse: JsValue = Json.parse(s"""
         |{
         |  "links": [
         |    {
         |      "href": "/individuals/expenses/employments/$nino/$taxYear",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/expenses/employments/$nino/$taxYear",
         |      "method": "PUT",
         |      "rel": "amend-employment-expenses"
         |    },
         |    {
         |      "href": "/individuals/expenses/employments/$nino/$taxYear",
         |      "method": "DELETE",
         |      "rel": "delete-employment-expenses"
         |    }
         |  ]
         |}
         |""".stripMargin)

    def uri: String = s"/employments/$nino/$taxYear"

    def downstreamUri: String

    def setupStubs(): Unit = ()

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.1.0+json"),
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

    def taxYear: String = "2021-22"

    def downstreamUri: String = s"/income-tax/expenses/employments/$nino/$taxYear"
  }

  private trait TysIfsTest extends Test {

    def taxYear: String = "2023-24"

    def downstreamUri: String = s"/income-tax/23-24/expenses/employments/$nino"

    override def request(): WSRequest = super.request().addHttpHeaders("suspend-temporal-validations" -> "true")
  }

}
