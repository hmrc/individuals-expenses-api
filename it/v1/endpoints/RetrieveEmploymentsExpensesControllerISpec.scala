/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import support.IntegrationBaseSpec
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DesStub, MtdIdLookupStub}

class RetrieveEmploymentsExpensesControllerISpec extends IntegrationBaseSpec {

  private trait Test {

    val nino = "AA123456A"
    val taxYear = "2019-20"
    val latestSource = "latest"
    val hmrcHeldSource = "hmrcHeld"
    val userSource = "user"
    val desSource = "LATEST"
    val hmrcHeldDesSource = "HMRC-HELD"
    val userDesSource = "CUSTOMER"

    val latestResponseBody = Json.parse(
      s"""
         |{
         |		"submittedOn": "2020-12-12T12:12:12Z",
         |		"source": "latest",
         |		"totalExpenses": 123.12,
         |		"expenses": {
         |			"businessTravelCosts": 123.12,
         |			"jobExpenses": 123.12,
         |			"flatRateJobExpenses": 123.12,
         |			"professionalSubscriptions": 123.12,
         |			"hotelAndMealExpenses": 123.12,
         |			"otherAndCapitalAllowances": 123.12,
         |			"vehicleExpenses": 123.12,
         |			"mileageAllowanceRelief": 123.12
         |		},
         |		"links": [{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "PUT",
         |				"rel": "amend-employment-expenses"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "GET",
         |				"rel": "self"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "DELETE",
         |				"rel": "delete-employment-expenses"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20/ignore",
         |				"method": "PUT",
         |				"rel": "ignore-employment-expenses"
         |			}
         |		]
         |	}
         |""".stripMargin
    )

    val hmrcHeldResponseBody = Json.parse(
      s"""
         |{
         |		"submittedOn": "2020-12-12T12:12:12Z",
         |		"source": "hmrcHeld",
         |		"totalExpenses": 123.12,
         |		"expenses": {
         |			"businessTravelCosts": 123.12,
         |			"jobExpenses": 123.12,
         |			"flatRateJobExpenses": 123.12,
         |			"professionalSubscriptions": 123.12,
         |			"hotelAndMealExpenses": 123.12,
         |			"otherAndCapitalAllowances": 123.12,
         |			"vehicleExpenses": 123.12,
         |			"mileageAllowanceRelief": 123.12
         |		},
         |		"links": [{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "GET",
         |				"rel": "self"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20/ignore",
         |				"method": "PUT",
         |				"rel": "ignore-employment-expenses"
         |			}
         |		]
         |	}
         |""".stripMargin
    )

    val userResponseBody = Json.parse(
      s"""
         |{
         |		"submittedOn": "2020-12-12T12:12:12Z",
         |		"source": "user",
         |		"totalExpenses": 123.12,
         |		"expenses": {
         |			"businessTravelCosts": 123.12,
         |			"jobExpenses": 123.12,
         |			"flatRateJobExpenses": 123.12,
         |			"professionalSubscriptions": 123.12,
         |			"hotelAndMealExpenses": 123.12,
         |			"otherAndCapitalAllowances": 123.12,
         |			"vehicleExpenses": 123.12,
         |			"mileageAllowanceRelief": 123.12
         |		},
         |		"links": [{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "PUT",
         |				"rel": "amend-employment-expenses"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "GET",
         |				"rel": "self"
         |			},
         |			{
         |				"href": "/individuals/expenses/employments/AA123456A/2019-20",
         |				"method": "DELETE",
         |				"rel": "delete-employment-expenses"
         |			}
         |		]
         |	}
         |""".stripMargin
    )

    val desResponseBody = Json.parse(
      s"""
         |{
         |    "submittedOn": "2020-12-12T12:12:12Z",
         |    "source": "LATEST",
         |    "totalExpenses": 123.12,
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
         |""".stripMargin)

    val hmrcHeldDesResponseBody = Json.parse(
      s"""
         |{
         |    "submittedOn": "2020-12-12T12:12:12Z",
         |    "source": "HMRC HELD",
         |    "totalExpenses": 123.12,
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
         |""".stripMargin)

    val userDesResponseBody = Json.parse(
      s"""
         |{
         |    "submittedOn": "2020-12-12T12:12:12Z",
         |    "source": "CUSTOMER",
         |    "totalExpenses": 123.12,
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
         |""".stripMargin)

    def latestUri: String = s"/employments/$nino/$taxYear?source=$latestSource"
    def hmrcHeldUri: String = s"/employments/$nino/$taxYear?source=$hmrcHeldSource"
    def userUri: String = s"/employments/$nino/$taxYear?source=$userSource"
    def desUri: String = s"/income-tax/expenses/employments/$nino/$taxYear"

    def setupStubs(): StubMapping

    def request(uri: String): WSRequest = {
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

  "Calling the retrieve endpoint" should {

    "return a 200 status code" when {

      "valid latest request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUri, Map("view" -> desSource) ,Status.OK, desResponseBody)
        }

        val response: WSResponse = await(request(latestUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe latestResponseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "valid hmrcHeld request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUri, Map("view" -> hmrcHeldDesSource) ,Status.OK, hmrcHeldDesResponseBody)
        }

        val response: WSResponse = await(request(hmrcHeldUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe hmrcHeldResponseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
      "valid user request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUri, Map("view" -> userDesSource) ,Status.OK, userDesResponseBody)
        }

        val response: WSResponse = await(request(userUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe userResponseBody
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String, requestTaxYear: String, requestSource: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new Test {

            override val nino: String = requestNino
            override val taxYear: String = requestTaxYear
            override val latestSource: String = requestSource

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(requestNino)
            }

            val response: WSResponse = await(request(latestUri).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("Walrus", "2019-20", "latest", Status.BAD_REQUEST, NinoFormatError),
          ("AA123456A", "203100", "latest", Status.BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "Walrus", Status.BAD_REQUEST, SourceFormatError),
          ("AA123456A", "2017-18", "latest", Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2018-20", "latest", Status.BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "des service error" when {
        def serviceErrorTest(desStatus: Int, desCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"des returns an $desCode error and status $desStatus" in new Test {

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(nino)
              DesStub.onError(DesStub.GET, desUri, desStatus, errorBody(desCode))
            }

            val response: WSResponse = await(request(latestUri).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          (Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError),
          (Status.BAD_REQUEST, "INVALID_TAX_YEAR", Status.BAD_REQUEST, TaxYearFormatError),
          (Status.NOT_FOUND, "NO_DATA_FOUND", Status.NOT_FOUND, NotFoundError),
          (Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.BAD_REQUEST, "INVALID_DATE_RANGE", Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
          (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
