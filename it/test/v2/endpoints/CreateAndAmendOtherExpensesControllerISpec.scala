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

import common.error.CustomerReferenceFormatError
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import shared.models.errors._
import shared.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import shared.support.IntegrationBaseSpec
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class CreateAndAmendOtherExpensesControllerISpec extends IntegrationBaseSpec {

  "Calling the create and amend endpoint" should {
    "return a 200 status code" when {
      "any valid request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.json shouldBe responseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }

      "any valid request with a Tax Year Specific (TYS) tax year is made" in new TysIfsTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.json shouldBe responseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {
      "validation error" when {
        s"an invalid NINO is provided" in new NonTysTest {
          override val nino: String = "INVALID_NINO"

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(NinoFormatError)
        }
        s"an invalid taxYear is provided" in new NonTysTest {
          override val taxYear: String = "INVALID_TAXYEAR"

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(TaxYearFormatError)
        }
        s"a taxYear before the minimum in sandbox of 2021-22 is provided" in new NonTysTest {
          override val taxYear: String = "2018-19"

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleTaxYearNotSupportedError)
        }
        s"an invalid amount is provided" in new NonTysTest {
          override val requestBodyJson: JsValue = Json.parse(
            s"""
               |{
               |  "paymentsToTradeUnionsForDeathBenefits": {
               |    "customerReference": "TRADE UNION PAYMENTS",
               |    "expenseAmount": -1
               |  },
               |  "patentRoyaltiesPayments":{
               |    "customerReference": "ROYALTIES PAYMENTS",
               |    "expenseAmount": -1
               |  }
               |}
               |""".stripMargin
          )

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(ValueFormatError.copy(paths =
            Some(List("/paymentsToTradeUnionsForDeathBenefits/expenseAmount", "/patentRoyaltiesPayments/expenseAmount"))))
        }
        s"an invalid customer reference is provided" in new NonTysTest {
          override val requestBodyJson: JsValue = Json.parse(
            s"""
               |{
               |  "paymentsToTradeUnionsForDeathBenefits": {
               |    "customerReference": "TRADE UNION PAYMENTS AND OTHER THINGS THAT LEAD TO A REALLY LONG NAME THAT I'M HOPING IS OVER NINETY CHARACTERS",
               |    "expenseAmount": 5000.99
               |  },
               |  "patentRoyaltiesPayments":{
               |    "customerReference": "ROYALTIES PAYMENTS AND OTHER THINGS THAT LEAD TO A REALLY LONG NAME THAT I'M HOPING IS OVER NINETY CHARACTERS",
               |    "expenseAmount": 5000.99
               |  }
               |}
               |""".stripMargin
          )

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(CustomerReferenceFormatError.copy(paths =
            Some(List("/paymentsToTradeUnionsForDeathBenefits/customerReference", "/patentRoyaltiesPayments/customerReference"))))
        }
        s"a taxYear with range of greater than a year is provided" in new NonTysTest {
          override val taxYear: String = "2019-21"

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleTaxYearRangeInvalidError)
        }
        s"an empty body is provided" in new NonTysTest {
          override val requestBodyJson: JsValue = Json.parse("""{}""")

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleIncorrectOrEmptyBodyError)
        }
        s"a body missing mandatory fields is provided" in new NonTysTest {
          override val requestBodyJson: JsValue = Json.parse("""{
              | "paymentsToTradeUnionsForDeathBenefits": {},
              | "patentRoyaltiesPayments": {}
              |}""".stripMargin)

          override def setupStubs(): Unit = {}

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(
            RuleIncorrectOrEmptyBodyError.withPaths(
              List(
                "/patentRoyaltiesPayments/expenseAmount",
                "/paymentsToTradeUnionsForDeathBenefits/expenseAmount"
              )))
        }
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {

            override def setupStubs(): Unit = {
              DownstreamStub.onError(DownstreamStub.PUT, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val errors = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (BAD_REQUEST, "INVALID_PAYLOAD", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY", INTERNAL_SERVER_ERROR, InternalError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = List(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => serviceErrorTest.tupled(args))
      }
    }
  }

  private trait Test {

    val nino: String = "AA123456A"

    val requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |  "paymentsToTradeUnionsForDeathBenefits": {
         |    "customerReference": "TRADE UNION PAYMENTS",
         |    "expenseAmount": 5000.99
         |  },
         |  "patentRoyaltiesPayments":{
         |    "expenseAmount": 5000.99
         |  }
         |}
         |""".stripMargin
    )

    val responseBody: JsValue = Json.parse(s"""
         |{
         |  "links": [
         |    {
         |      "href": "/individuals/expenses/other/$nino/$taxYear",
         |      "method": "GET",
         |      "rel": "self"
         |    },
         |    {
         |      "href": "/individuals/expenses/other/$nino/$taxYear",
         |      "method": "PUT",
         |      "rel": "amend-expenses-other"
         |    },
         |    {
         |      "href": "/individuals/expenses/other/$nino/$taxYear",
         |      "method": "DELETE",
         |      "rel": "delete-expenses-other"
         |    }
         |  ]
         |}
         |""".stripMargin)

    def uri: String = s"/other/$nino/$taxYear"

    def taxYear: String

    def downstreamUri: String

    def setupStubs(): Unit

    def request(): WSRequest = {
      AuditStub.audit()
      AuthStub.authorised()
      MtdIdLookupStub.ninoFound(nino)
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders(
          (ACCEPT, "application/vnd.hmrc.2.0+json"),
          (AUTHORIZATION, "Bearer 123") // some bearer token
        )
    }

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "downstream message"
         |      }
    """.stripMargin

  }

  private trait NonTysTest extends Test {
    def taxYear: String = "2021-22"

    def downstreamUri: String = s"/income-tax/expenses/other/$nino/2021-22"
  }

  private trait TysIfsTest extends Test {
    def taxYear: String = "2023-24"

    def downstreamUri: String = s"/income-tax/expenses/other/23-24/$nino"
  }

}
