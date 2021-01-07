/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.http.Status._
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}
import play.api.http.HeaderNames.ACCEPT

class AmendOtherExpensesControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino: String = "AA123456A"
    val taxYear: String = "2021-22"

    val amount: BigDecimal = 5000.99

    val requestBodyJson: JsValue = Json.parse(
      s"""
         |{
         |  "paymentsToTradeUnionsForDeathBenefits": {
         |    "customerReference": "TRADE UNION PAYMENTS",
         |    "expenseAmount": $amount
         |  },
         |  "patentRoyaltiesPayments":{
         |    "customerReference": "ROYALTIES PAYMENTS",
         |    "expenseAmount": $amount
         |  }
         |}
         |""".stripMargin
    )

    val responseBody = Json.parse(
      s"""
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

    def desUri: String = s"/income-tax/expenses/other/$nino/$taxYear"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(uri)
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def errorBody(code: String): String =
      s"""
         |      {
         |        "code": "$code",
         |        "reason": "des message"
         |      }
    """.stripMargin
  }

  "Calling the amend endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.PUT, desUri, NO_CONTENT, JsObject.empty)
        }

        val response: WSResponse = await(request().put(requestBodyJson))
        response.status shouldBe OK
        response.json shouldBe responseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
      }
    }

    "return error according to spec" when {

      "validation error" when {
        s"an invalid NINO is provided" in new Test {
          override val nino: String = "INVALID_NINO"

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(NinoFormatError)
        }
        s"an invalid taxYear is provided" in new Test {
          override val taxYear: String = "INVALID_TAXYEAR"

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(TaxYearFormatError)
        }
        s"a taxYear before the minimum in sandbox of 2019-20 is provided" in new Test {
          override val taxYear: String = "2018-19"

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleTaxYearNotSupportedError)
        }
        s"an invalid amount is provided" in new Test {
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

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(ValueFormatError.copy(paths = Some(Seq("/paymentsToTradeUnionsForDeathBenefits/expenseAmount",
                                                                                    "/patentRoyaltiesPayments/expenseAmount"))))
        }
        s"an invalid customer reference is provided" in new Test {
          override val requestBodyJson: JsValue = Json.parse(
            s"""
               |{
               |  "paymentsToTradeUnionsForDeathBenefits": {
               |    "customerReference": "TRADE UNION PAYMENTS AND OTHER THINGS THAT LEAD TO A REALLY LONG NAME THAT I'M HOPING IS OVER NINETY CHARACTERS",
               |    "expenseAmount": $amount
               |  },
               |  "patentRoyaltiesPayments":{
               |    "customerReference": "ROYALTIES PAYMENTS AND OTHER THINGS THAT LEAD TO A REALLY LONG NAME THAT I'M HOPING IS OVER NINETY CHARACTERS",
               |    "expenseAmount": $amount
               |  }
               |}
               |""".stripMargin
          )

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(CustomerReferenceFormatError.copy(paths = Some(Seq("/paymentsToTradeUnionsForDeathBenefits/customerReference",
                                                                                                "/patentRoyaltiesPayments/customerReference"))))
        }
        s"a taxYear with range of greater than a year is provided" in new Test {
          override val taxYear: String = "2019-21"

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleTaxYearRangeInvalidError)
        }
        s"an empty body is provided" in new Test {
          override val requestBodyJson: JsValue = Json.parse("""{}""")

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleIncorrectOrEmptyBodyError)
        }
        s"a body missing mandatory fields is provided" in new Test {
          override val requestBodyJson: JsValue = Json.parse(
            """{
              | "paymentsToTradeUnionsForDeathBenefits": {},
              | "patentRoyaltiesPayments": {}
              |}""".stripMargin)

          override def setupStubs(): StubMapping = {
            AuditStub.audit()
            AuthStub.authorised()
            MtdIdLookupStub.ninoFound(nino)
          }

          val response: WSResponse = await(request().put(requestBodyJson))
          response.status shouldBe BAD_REQUEST
          response.json shouldBe Json.toJson(RuleIncorrectOrEmptyBodyError)
        }

      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.PUT, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request().put(requestBodyJson))
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "FORMAT_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, DownstreamError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, DownstreamError))

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
