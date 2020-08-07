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
    val source = "latest"

    val responseBody = Json.parse(
      s"""
         |{
         |    "submittedOn": "2020-12-12T12:12:12Z",
         |    "source": "latest",
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

    def uri: String = s"/employments/$nino/$taxYear?source=$source"
    def desUri: String = s"/expenses/employments/$nino/$taxYear?source=$source"

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

  "Calling the retrieve endpoint" should {

    "return a 200 status code" when {

      "any valid request is made" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DesStub.onSuccess(DesStub.GET, desUri, Status.OK, desResponseBody)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
        response.json shouldBe responseBody
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

            override def setupStubs(): StubMapping = {
              AuditStub.audit()
              AuthStub.authorised()
              MtdIdLookupStub.ninoFound(requestNino)
            }

            val response: WSResponse = await(request().get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          ("Walrus", "2019-20", "latest", Status.BAD_REQUEST, NinoFormatError),
          ("AA123456A", "203100", "latest", Status.BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2018-20", "wordsthatareincorrect", Status.BAD_REQUEST, SourceFormatError),
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

            val response: WSResponse = await(request().get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = Seq(
          (Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError),
          (Status.BAD_REQUEST, "FORMAT_TAX_YEAR", Status.BAD_REQUEST, TaxYearFormatError),
          (Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError),
          (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, DownstreamError),
          (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, DownstreamError)
        )

        input.foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }
}
