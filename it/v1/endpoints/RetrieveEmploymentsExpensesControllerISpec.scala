/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec
import v1.fixtures.RetrieveEmploymentsExpensesFixtures._
import v1.models.errors._
import v1.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class RetrieveEmploymentsExpensesControllerISpec extends IntegrationBaseSpec {

  "Calling the retrieve endpoint" should {
    "return a 200 status code" when {
      "valid latest request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> latestSourceDownstream), Status.OK, downstreamResponseJsonLatest)
        }

        val response: WSResponse = await(request(latestMtdUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe mtdResponseWithHateoasLinksLatest()
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid hmrcHeld request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(
            DownstreamStub.GET,
            downstreamUri,
            Map("view" -> hmrcHeldSourceDownstream),
            Status.OK,
            downstreamResponseJsonHmrcHeld)
        }

        val response: WSResponse = await(request(hmrcHeldMtdUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe mtdResponseWithHateoasLinksHmrcHeld
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid user request is made" in new NonTysTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> userSourceDownstream), Status.OK, downstreamResponseJsonCustomer)
        }

        val response: WSResponse = await(request(userMtdUri).get())
        response.status shouldBe Status.OK
        response.json shouldBe mtdResponseWithHateoasLinksUser
        response.header("X-CorrelationId").nonEmpty shouldBe true
        response.header("Content-Type") shouldBe Some("application/json")
      }

      "valid request is made for a Tax Year Specific (TYS) tax year" in new TysIfsTest {
        override def setupStubs(): Unit = {
          DownstreamStub.onSuccess(DownstreamStub.GET, downstreamUri, Map("view" -> latestSourceDownstream), Status.OK, downstreamResponseJsonLatest)
        }

        val response: WSResponse = await(request(latestMtdUri).get())
        response.status shouldBe Status.OK
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

        val input = Seq(
          ("Walrus", "2019-20", "latest", Status.BAD_REQUEST, NinoFormatError),
          ("AA123456A", "203100", "latest", Status.BAD_REQUEST, TaxYearFormatError),
          ("AA123456A", "2019-20", "Walrus", Status.BAD_REQUEST, SourceFormatError),
          ("AA123456A", "2017-18", "latest", Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
          ("AA123456A", "2018-20", "latest", Status.BAD_REQUEST, RuleTaxYearRangeInvalidError)
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
          (Status.BAD_REQUEST, "INVALID_TAXABLE_ENTITY_ID", Status.BAD_REQUEST, NinoFormatError),
          (Status.BAD_REQUEST, "INVALID_TAX_YEAR", Status.BAD_REQUEST, TaxYearFormatError),
          (Status.BAD_REQUEST, "INVALID_VIEW", Status.BAD_REQUEST, SourceFormatError),
          (Status.BAD_REQUEST, "INVALID_CORRELATIONID", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (Status.NOT_FOUND, "NO_DATA_FOUND", Status.NOT_FOUND, NotFoundError),
          (Status.UNPROCESSABLE_ENTITY, "INVALID_DATE_RANGE", Status.BAD_REQUEST, RuleTaxYearNotSupportedError),
          (Status.INTERNAL_SERVER_ERROR, "SERVER_ERROR", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (Status.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError)
        )

        val extraTysErrors = List(
          (Status.BAD_REQUEST, "INVALID_CORRELATION_ID", Status.INTERNAL_SERVER_ERROR, StandardDownstreamError),
          (Status.NOT_FOUND, "NOT_FOUND", Status.NOT_FOUND, NotFoundError),
          (Status.UNPROCESSABLE_ENTITY, "TAX_YEAR_NOT_SUPPORTED", Status.BAD_REQUEST, RuleTaxYearNotSupportedError)
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

    def latestMtdUri: String   = s"/employments/$nino/$mtdTaxYear?source=$latestSourceMtd"
    def hmrcHeldMtdUri: String = s"/employments/$nino/$mtdTaxYear?source=$hmrcHeldSourceMtd"
    def userMtdUri: String     = s"/employments/$nino/$mtdTaxYear?source=$userSourceMtd"

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
