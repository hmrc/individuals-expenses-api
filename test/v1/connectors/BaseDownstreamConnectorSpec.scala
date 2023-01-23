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

package v1.connectors

import config.AppConfig
import mocks.MockAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReads}
import v1.connectors.DownstreamUri._
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper

import scala.concurrent.Future

class BaseDownstreamConnectorSpec extends ConnectorSpec {

  // WLOG
  case class Result(value: Int)

  // WLOG
  val body                               = "body"
  val queryParams: Seq[(String, String)] = Seq("aParam" -> "aValue")
  val outcome                            = Right(ResponseWrapper(correlationId, Result(2)))

  val url         = "some/url?param=value"
  val absoluteUrl = s"$baseUrl/$url"

  implicit val httpReads: HttpReads[DownstreamOutcome[Result]] = mock[HttpReads[DownstreamOutcome[Result]]]

  class DesTest(desEnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
    MockedAppConfig.desEnvironmentHeaders returns desEnvironmentHeaders
  }

  class IfsR5Test(ifsR5EnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.ifsR5BaseUrl returns baseUrl
    MockedAppConfig.ifsR5Token returns "ifs-token"
    MockedAppConfig.ifsR5Environment returns "ifs-environment"
    MockedAppConfig.ifsR5EnvironmentHeaders returns ifsR5EnvironmentHeaders
  }

  class IfsR6Test(ifsR6EnvironmentHeaders: Option[Seq[String]]) extends MockHttpClient with MockAppConfig {

    val connector: BaseDownstreamConnector = new BaseDownstreamConnector {
      val http: HttpClient     = mockHttpClient
      val appConfig: AppConfig = mockAppConfig
    }

    MockedAppConfig.ifsR6BaseUrl returns baseUrl
    MockedAppConfig.ifsR6Token returns "ifs-token"
    MockedAppConfig.ifsR6Environment returns "ifs-environment"
    MockedAppConfig.ifsR6EnvironmentHeaders returns ifsR6EnvironmentHeaders
  }

  "BaseDownstreamConnector" when {
    "making a HTTP request to a downstream service (i.e DES)" must {
      val requiredHeaders: Seq[(String, String)] = Seq(
        "Environment"       -> "des-environment",
        "Authorization"     -> "Bearer des-token",
        "User-Agent"        -> "individuals-expenses-api",
        "CorrelationId"     -> correlationId,
        "Gov-Test-Scenario" -> "DEFAULT"
      )

      desTestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedDownstreamHeaders))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment"   -> "des-environment",
          "Authorization" -> "Bearer des-token",
          "User-Agent"    -> "individuals-expenses-api",
          "CorrelationId" -> correlationId
        )

        desTestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
      }
    }

    "making a HTTP request to a downstream service (i.e IF)" must {
      val requiredHeaders: Seq[(String, String)] = Seq(
        "Environment"       -> "ifs-environment",
        "Authorization"     -> "Bearer ifs-token",
        "User-Agent"        -> "individuals-expenses-api",
        "CorrelationId"     -> correlationId,
        "Gov-Test-Scenario" -> "DEFAULT"
      )

      ifsR5TestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedDownstreamHeaders))
      ifsR6TestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, excludedHeaders, Some(allowedDownstreamHeaders))

      "exclude all `otherHeaders` when no external service header allow-list is found" should {
        val requiredHeaders: Seq[(String, String)] = Seq(
          "Environment"   -> "ifs-environment",
          "Authorization" -> "Bearer ifs-token",
          "User-Agent"    -> "individuals-expenses-api",
          "CorrelationId" -> correlationId
        )

        ifsR5TestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
        ifsR6TestHttpMethods(dummyDownstreamHeaderCarrierConfig, requiredHeaders, otherHeaders, None)
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

        await(connector.get(DesUri[Result](url))) shouldBe outcome
      }

      "POST" in new DesTest(desEnvironmentHeaders) {
        implicit val hc: HeaderCarrier                 = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, DesUri[Result](url))) shouldBe outcome
      }

      "PUT" in new DesTest(desEnvironmentHeaders) {
        implicit val hc: HeaderCarrier                = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.put(body, DesUri[Result](url))) shouldBe outcome
      }

      "DELETE" in new DesTest(desEnvironmentHeaders) {
        MockHttpClient
          .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.delete(DesUri[Result](url))) shouldBe outcome
      }
    }
  }

  def ifsR5TestHttpMethods(config: HeaderCarrier.Config,
                           requiredHeaders: Seq[(String, String)],
                           excludedHeaders: Seq[(String, String)],
                           ifsR5EnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers for IfsR5" when {
      "POST" in new IfsR5Test(ifsR5EnvironmentHeaders) {
        implicit val hc: HeaderCarrier                 = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, IfsR5Uri[Result](url))) shouldBe outcome
      }

      "GET" in new IfsR5Test(ifsR5EnvironmentHeaders) {
        MockHttpClient
          .get(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.get(IfsR5Uri[Result](url))) shouldBe outcome
      }

      "PUT" in new IfsR5Test(ifsR5EnvironmentHeaders) {
        implicit val hc: HeaderCarrier                = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.put(body, IfsR5Uri[Result](url))) shouldBe outcome
      }

      "DELETE" in new IfsR5Test(ifsR5EnvironmentHeaders) {
        MockHttpClient
          .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.delete(IfsR5Uri[Result](url))) shouldBe outcome
      }
    }
  }

  def ifsR6TestHttpMethods(config: HeaderCarrier.Config,
                           requiredHeaders: Seq[(String, String)],
                           excludedHeaders: Seq[(String, String)],
                           ifsR6EnvironmentHeaders: Option[Seq[String]]): Unit = {

    "complete the request successfully with the required headers for IfsR6" when {
      "POST" in new IfsR6Test(ifsR6EnvironmentHeaders) {
        implicit val hc: HeaderCarrier                 = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPost: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .post(absoluteUrl, config, body, requiredHeadersPost, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.post(body, IfsR6Uri[Result](url))) shouldBe outcome
      }

      "GET" in new IfsR6Test(ifsR6EnvironmentHeaders) {
        MockHttpClient
          .get(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.get(IfsR6Uri[Result](url))) shouldBe outcome
      }

      "PUT" in new IfsR6Test(ifsR6EnvironmentHeaders) {
        implicit val hc: HeaderCarrier                = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
        val requiredHeadersPut: Seq[(String, String)] = requiredHeaders ++ Seq("Content-Type" -> "application/json")

        MockHttpClient
          .put(absoluteUrl, config, body, requiredHeadersPut, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.put(body, IfsR6Uri[Result](url))) shouldBe outcome
      }

      "DELETE" in new IfsR6Test(ifsR6EnvironmentHeaders) {
        MockHttpClient
          .delete(absoluteUrl, config, requiredHeaders, excludedHeaders)
          .returns(Future.successful(outcome))

        await(connector.delete(IfsR6Uri[Result](url))) shouldBe outcome
      }
    }
  }

}
