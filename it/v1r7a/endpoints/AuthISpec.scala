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
 * WITHOUT WARRANTIED OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1r7a.endpoints

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSRequest, WSResponse}
import support.V1R7aIntegrationSpec
import v1r7a.stubs.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}

class AuthISpec extends V1R7aIntegrationSpec {

  private trait Test {
    val nino          = "AA123456A"
    val taxYear       = "2021-22"

    def setupStubs(): StubMapping

    def request(): WSRequest = {
      setupStubs()
      buildRequest(s"/other/$nino/$taxYear")
        .withHttpHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
    }

    def desUri: String = s"/income-tax/expenses/other/$nino/$taxYear"

    val desResponse: JsValue = Json.parse(
      """
        |{
        |  "submittedOn": "2019-04-04T01:01:01Z",
        |  "paymentsToTradeUnionsForDeathBenefits": {
        |    "customerReference": "TRADE UNION PAYMENTS",
        |    "expenseAmount": 4528.99
        |  },
        |  "patentRoyaltiesPayments": {
        |    "customerReference": "ROYALTIES PAYMENTS",
        |    "expenseAmount": 3015.50
        |  }
        |}
        |""".stripMargin)
  }

  "Calling the retrieve other expenses endpoint" when {

    "the NINO cannot be converted to a MTD ID" should {

      "return 500" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.internalServerError(nino)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is authorised" should {

      "return 200" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          AuthStub.authorised()
          MtdIdLookupStub.ninoFound(nino)
          DownstreamStub.onSuccess(DownstreamStub.GET, desUri, Status.OK, desResponse)
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.OK
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is NOT logged in" should {

      "return 403" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedNotLoggedIn()
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.FORBIDDEN
      }
    }

    "an MTD ID is successfully retrieve from the NINO and the user is NOT authorised" should {

      "return 403" in new Test {
        override val nino: String = "AA123456A"

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedOther()
        }

        val response: WSResponse = await(request().get())
        response.status shouldBe Status.FORBIDDEN
      }
    }

  }

}
