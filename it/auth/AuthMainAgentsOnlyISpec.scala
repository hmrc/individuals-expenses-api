/*
 * Copyright 2024 HM Revenue & Customs
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

package auth

import api.models.errors.{ClientOrAgentNotAuthorisedError, InternalError}
import api.services.{AuditStub, AuthStub, DownstreamStub, MtdIdLookupStub}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.HeaderNames.ACCEPT
import play.api.http.Status.{FORBIDDEN, INTERNAL_SERVER_ERROR, OK, NO_CONTENT}
import play.api.libs.json.{JsValue, JsObject}
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.test.Helpers.AUTHORIZATION
import support.IntegrationBaseSpec

class AuthMainAgentsOnlyISpec extends IntegrationBaseSpec {

  /** The API's latest version, e.g. "1.0".
    */
  val callingApiVersion = "1.0"

  protected val nino = "AA123456A"

  /** As the IT supplies the "supported" config below, this can be any endpoint IF there's no actual "main agents only" endpoint in the API.
    */
  val supportingAgentsNotAllowedEndpoint = "ignore-employed-expenses"

  private val taxYearStr = "2019-20"

  def sendMtdRequest(request: WSRequest): WSResponse = await(request.post(JsObject.empty))

  val mtdUrl = s"/employments/$nino/$taxYearStr/ignore"

  val downstreamUri: String = s"/income-tax/expenses/employments/$nino/2019-20"

  val maybeDownstreamResponseJson: Option[JsValue] = Some(JsObject.empty)

  protected val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  protected val downstreamSuccessStatus: Int = OK

  protected val expectedMtdSuccessStatus: Int = OK

  /** One endpoint where supporting agents are allowed.
    */
  override def servicesConfig: Map[String, Any] =
    Map(
      s"api.supporting-agent-endpoints.$supportingAgentsNotAllowedEndpoint" -> "false"
    ) ++ super.servicesConfig

  "Calling an endpoint that only allows primary agents" when {
    "the client is the primary agent" should {
      "return a success response" in new Test {

        override def setupStubs(): StubMapping = {
          AuthStub.resetAll()
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)

          AuthStub.authorisedWithAgentAffinityGroup()
          AuthStub.authorisedWithPrimaryAgentEnrolment()

          DownstreamStub.onSuccess(DownstreamStub.PUT, downstreamUri, NO_CONTENT, JsObject.empty)

        }
        val response: WSResponse = sendMtdRequest(request())
        response.status shouldBe expectedMtdSuccessStatus
      }
    }

    "the client is a supporting agent" should {
      "return a 403 response" in new Test {

        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)

          AuthStub.authorisedWithAgentAffinityGroup()
          AuthStub.unauthorisedForPrimaryAgentEnrolment()
        }

        val response: WSResponse = sendMtdRequest(request())

        response.status shouldBe FORBIDDEN
        response.body should include(ClientOrAgentNotAuthorisedError.message)
      }
    }
  }

  "Calling an endpoint" when {

    "MTD ID lookup succeeds but the user isn't logged in" should {

      "return a 403 response" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedNotLoggedIn()
        }

        val response: WSResponse = sendMtdRequest(request())
        response.status shouldBe FORBIDDEN
      }
    }

    "MTD ID lookup succeeds but the user isn't authorised to access it" should {

      "return a 403 response" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.ninoFound(nino)
          AuthStub.unauthorisedOther()
        }

        val response: WSResponse = sendMtdRequest(request())
        response.status shouldBe FORBIDDEN
        response.body should include(ClientOrAgentNotAuthorisedError.message)
      }
    }

    "MTD ID lookup fails with a 500" should {

      "return a 500 response" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.error(nino, INTERNAL_SERVER_ERROR)
        }

        val response: WSResponse = sendMtdRequest(request())
        response.status shouldBe INTERNAL_SERVER_ERROR
        response.body should include(InternalError.message)
      }
    }

    "MTD ID lookup fails with a 403" should {

      "return a 403 response" in new Test {
        override def setupStubs(): StubMapping = {
          AuditStub.audit()
          MtdIdLookupStub.error(nino, FORBIDDEN)
        }

        val response: WSResponse = sendMtdRequest(request())
        response.status shouldBe FORBIDDEN
        response.body should include(ClientOrAgentNotAuthorisedError.message)
      }
    }
  }

  protected trait Test {

    def setupStubs(): StubMapping

    protected def request(): WSRequest = {
      AuthStub.resetAll()
      setupStubs()
      buildRequest(mtdUrl)
        .withHttpHeaders(
          (ACCEPT, s"application/vnd.hmrc.$callingApiVersion+json"),
          (AUTHORIZATION, "Bearer 123")
        )
    }

  }

}
