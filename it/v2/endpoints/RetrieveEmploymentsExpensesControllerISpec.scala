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
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import support.IntegrationBaseSpec
import v1.fixtures.RetrieveEmploymentsExpensesFixtures._

class RetrieveEmploymentsExpensesControllerISpec extends IntegrationBaseSpec {

  "Calling the retrieve endpoint" should {
    "return a 200 status code" when {
      "valid latest request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> latestSourceDownstream), OK, downstreamResponseJsonLatest)
        }

        val response: WSResponse = await(request(latestMtdUri).get())
        response.status shouldBe OK
        response.json shouldBe mtdResponseWithHateoasLinksLatest()
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid hmrcHeld request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> hmrcHeldSourceDownstream), OK, downstreamResponseJsonHmrcHeld)
        }

        val response: WSResponse = await(request(hmrcHeldMtdUri).get())
        response.status shouldBe OK
        response.json shouldBe mtdResponseWithHateoasLinksHmrcHeld
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid user request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> userSourceDownstream), OK, downstreamResponseJsonCustomer)
        }

        val response: WSResponse = await(request(userMtdUri).get())
        response.status shouldBe OK
        response.json shouldBe mtdResponseWithHateoasLinksUser
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid request is made for a Tax Year Specific (TYS) tax year" in new TysIfsTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> latestSourceDownstream), OK, downstreamResponseJsonLatest)
        }

        val response: WSResponse = await(request(latestMtdUri).get())
        response.status shouldBe OK
        response.json shouldBe mtdResponseWithHateoasLinksLatest(mtdTaxYear)
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }
    }

    "return error according to spec" when {

      "validation error" when {
        def validationErrorTest(requestNino: String,
                                requestTaxYear: String,
                                requestSource: String,
                                expectedStatus: Int,
                                expectedBody: MtdError): Unit = {
          s"validation fails with ${expectedBody.code} error" in new NonTysTest {

            override val nino: String            = requestNino
            override val mtdTaxYear: String      = requestTaxYear
            override val latestSourceMtd: String = requestSource

            val response: WSResponse = await(request(latestMtdUri).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val input = List(
          ("Walrus", "2019-20", "latest", BAD_REQUEST, NinoFormatError),
          ("AA123456A", "203100", "latest", BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "Walrus", BAD_REQUEST, SourceFormatError),
          ("AA123456A", "2017-18", "latest", BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2018-20", "latest", BAD_REQUEST, RuleTaxYearRangeInvalidError)
        )

        input.foreach(args => (validationErrorTest _).tupled(args))
      }

      "downstream service error" when {
        def serviceErrorTest(downstreamStatus: Int, downstreamCode: String, expectedStatus: Int, expectedBody: MtdError): Unit = {
          s"downstream returns an $downstreamCode error and status $downstreamStatus" in new NonTysTest {
            override def setupStubs(): Unit = {
              DownstreamStub.onError(DownstreamStub.GET, downstreamUri, downstreamStatus, errorBody(downstreamCode))
            }

            val response: WSResponse = await(request(latestMtdUri).get())
            response.status shouldBe expectedStatus
            response.json shouldBe Json.toJson(expectedBody)
          }
        }

        val errors = List(
          (BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", BAD_REQUEST, NinoFormatError),
          (BAD_REQUEST, "INVALID_TAX_YEAR", BAD_REQUEST, TaxYearFormatError),
          (BAD_REQUEST, "INVALID_VIEW", BAD_REQUEST, SourceFormatError),
          (BAD_REQUEST, "INVALID_CORRELATIONID", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "NO_DATA_FOUND", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "INVALID_DATE_RANGE", BAD_REQUEST, RuleTaxYearNotSupportedError),
          (INTERNAL_SERVER_ERROR, "SERVER_ERROR", INTERNAL_SERVER_ERROR, InternalError),
          (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", INTERNAL_SERVER_ERROR, InternalError)
        )

        val extraTysErrors = List(
          (BAD_REQUEST, "INVALID_CORRELATION_ID", INTERNAL_SERVER_ERROR, InternalError),
          (NOT_FOUND, "NOT_FOUND", NOT_FOUND, NotFoundError),
          (UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", BAD_REQUEST, RuleTaxYearNotSupportedError)
        )

        (errors ++ extraTysErrors).foreach(args => (serviceErrorTest _).tupled(args))
      }
    }
  }

  private trait Test {

    val nino = "AA123456A"

    val latestSourceMtd        = "latest"
    val latestSourceDownstream = "LATEST"

    val hmrcHeldSourceMtd        = "hmrcHeld"
    val hmrcHeldSourceDownstream = "HMRC-HELD"

    val userSourceMtd        = "user"
    val userSourceDownstream = "CUSTOMER"

    def latestMtdUri: String = s"/employments/$nino/$mtdTaxYear?source=$latestSourceMtd"

    def hmrcHeldMtdUri: String = s"/employments/$nino/$mtdTaxYear?source=$hmrcHeldSourceMtd"

    def userMtdUri: String = s"/employments/$nino/$mtdTaxYear?source=$userSourceMtd"

    def mtdTaxYear: String

    def downstreamUri: String

    def setupStubs(): Unit = {}

    def request(uri: String): WSRequest = {
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
         |{
         |  "failures": [
         |    {
         |      "code": "$code",
         |      "reason": "downstream error message"
         |    }
         |  ]
         |}
    """.stripMargin

  }

  private trait NonTysTest extends Test {
    def mtdTaxYear: String = "2019-20"

    def downstreamUri: String = s"/income-tax/expenses/employments/$nino/2019-20"
  }

  private trait TysIfsTest extends Test {
    def mtdTaxYear: String = "2023-24"

    def downstreamUri: String = s"/income-tax/expenses/employments/23-24/$nino"
  }

}
