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

package v1r7a.connectors

import config.AppConfig
import mocks.MockAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v1r7a.mocks.MockHttpClient
import v1r7a.models.outcomes.ResponseWrapper

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {

  // WLOG
  case class Result(value: Int)

  // WLOG
  val body = "body"
  val queryParams: Seq[(String, String)] = Seq("aParam" -> "aValue")
  val outcome = Right(ResponseWrapper(correlationId, Result(2)))

  val url = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class DesTest(desEnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {
    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns desEnvironmentHeaders
  }

  "BaseDesConnector" when {
    val requiredHeaders: Seq[(String, String)] = Seq(
      "Environment" -> "des-environment",
      "Authorization" -> s"Bearer des-token",
      "User-Agent" -> "self-assessment-accounts-api",
      "CorrelationId" -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    val excludedHeaders: Seq[(String, String)] = Seq(
      "AnotherHeader" -> "HeaderValue"
    )

    "making a HTTP request to a downstream service (i.e DES)" must {
      desTestHttpMethods(dummyDesHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedDesHeaders))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment" -> "des-environment",
          "Authorization" -> s"Bearer des-token",
          "User-Agent" -> "self-assessment-accounts-api",
          "CorrelationId" -> correlationId,
        )

        desTestHttpMethods(dummyDesHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }
  }

  def desTestHttpMethods(config: HeaderCarrier.Config,
                         requiredHeaders: Seq[(String, String)],
                         excludedHeaders: Seq[(String, String)],
                         desEnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers" when {
      "GET" in new DesTest(desEnvironmentHeaders) {
        MockHttpClient
          .get(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.get(BackendUri.DesUri[Result](url))) shouldBe outcome
      }

      "POST" in new DesTest(desEnvironmentHeaders) {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient.post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, BackendUri.DesUri[Result](url))) shouldBe outcome
      }

      "PUT" in new DesTest(desEnvironmentHeaders) {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient.put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.put(body, BackendUri.DesUri[Result](url))) shouldBe outcome
      }

      "DELETE" in new DesTest(desEnvironmentHeaders) {
        MockHttpClient
          .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.delete(BackendUri.DesUri[Result](url))) shouldBe outcome
      }
    }
  }

  class IfsTest(ifsEnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {
    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns ifsEnvironmentHeaders
  }

  "BaseDownstreamConnector" when {
    val requiredHeaders: Seq[(String, String)] = Seq(
      "Environment" -> "ifs-environment",
      "Authorization" -> s"Bearer ifs-token",
      "User-Agent" -> "self-assessment-accounts-api",
      "CorrelationId" -> correlationId,
      "Gov-Test-Scenario" -> "DEFAULT"
    )

    val excludedHeaders: Seq[(String, String)] = Seq(
      "AnotherHeader" -> "HeaderValue"
    )

    "making a HTTP request to a downstream service (i.e IFS)" must {
      ifsTestHttpMethods(dummyIfsHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedIfsHeaders))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment" -> "ifs-environment",
          "Authorization" -> s"Bearer ifs-token",
          "User-Agent" -> "self-assessment-accounts-api",
          "CorrelationId" -> correlationId,
        )

        ifsTestHttpMethods(dummyIfsHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }
  }

  def ifsTestHttpMethods(config: HeaderCarrier.Config,
                         requiredHeaders: Seq[(String, String)],
                         excludedHeaders: Seq[(String, String)],
                         ifsEnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers" when {
      "POST" in new IfsTest(ifsEnvironmentHeaders) {
        implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient.post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, BackendUri.IfsUri[Result](url))) shouldBe outcome
      }

      "complete the request successfully with the required headers" when {
        "GET" in new IfsTest(ifsEnvironmentHeaders) {
          MockHttpClient
            .get(absoluteUrl, config, requiredHeaders, excludedHeaders)
            .returns(Future.successful(outcome))

          await(connector.get(BackendUri.IfsUri[Result](url))) shouldBe outcome
        }

        "PUT" in new IfsTest(ifsEnvironmentHeaders) {
          implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
          val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

          MockHttpClient.put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
            .returns(Future.successful(outcome))

          await(connector.put(body, BackendUri.IfsUri[Result](url))) shouldBe outcome
        }

        "DELETE" in new IfsTest(ifsEnvironmentHeaders) {
          MockHttpClient
            .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
            .returns(Future.successful(outcome))

          await(connector.delete(BackendUri.IfsUri[Result](url))) shouldBe outcome
        }
      }
    }
  }

}