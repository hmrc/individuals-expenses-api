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

package api.connectors

import mocks.{MockAppConfig, MockHttpClient}
import org.scalamock.handlers.CallHandler
import play.api.Configuration
import play.api.http.{HeaderNames, MimeTypes, Status}
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait ConnectorSpec extends UnitSpec with Status with MimeTypes with HeaderNames {

  lazy val baseUrl                   = "http://test-BaseUrl"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  val otherHeaders: Seq[(String, String)] = Seq(
    "Gov-Test-Scenario" -> "DEFAULT",
    "AnotherHeader"     -> "HeaderValue"
  )

  implicit val hc: HeaderCarrier    = HeaderCarrier(otherHeaders = otherHeaders)
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val dummyDownstreamHeaderCarrierConfig: HeaderCarrier.Config =
    HeaderCarrier.Config(
      Seq("^not-test-BaseUrl?$".r),
      Seq.empty[String],
      Some("individuals-expenses-api")
    )

  val requiredDesHeaders: Seq[(String, String)] = Seq(
    "Authorization"     -> "Bearer des-token",
    "Environment"       -> "des-environment",
    "User-Agent"        -> "individuals-expenses-api",
    "CorrelationId"     -> correlationId,
    "Gov-Test-Scenario" -> "DEFAULT"
  )

  val requiredIfsR5Headers: Seq[(String, String)] = Seq(
    "Authorization"     -> "Bearer ifs-r5-token",
    "Environment"       -> "ifs-r5-environment",
    "User-Agent"        -> "individuals-expenses-api",
    "CorrelationId"     -> correlationId,
    "Gov-Test-Scenario" -> "DEFAULT"
  )

  val requiredIfsR6Headers: Seq[(String, String)] = Seq(
    "Authorization"     -> "Bearer ifs-r6-token",
    "Environment"       -> "ifs-r6-environment",
    "User-Agent"        -> "individuals-expenses-api",
    "CorrelationId"     -> correlationId,
    "Gov-Test-Scenario" -> "DEFAULT"
  )

  val requiredTysIfsHeaders: Seq[(String, String)] = Seq(
    "Environment"   -> "TYS-IFS-environment",
    "Authorization" -> s"Bearer TYS-IFS-token",
    "CorrelationId" -> s"$correlationId"
  )

  val allowedDownstreamHeaders: Seq[String] = Seq(
    "Accept",
    "Gov-Test-Scenario",
    "Content-Type",
    "Location",
    "X-Request-Timestamp",
    "X-Session-Id"
  )

  val excludedHeaders: Seq[(String, String)] = Seq(
    "AnotherHeader" -> "HeaderValue"
  )

  protected trait ConnectorTest extends MockHttpClient with MockAppConfig {
    protected val baseUrl: String = "http://test-BaseUrl"

    implicit protected val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders)

    protected val requiredHeaders: Seq[(String, String)]

    protected def willGet[T](url: String): CallHandler[Future[T]] = {
      MockHttpClient
        .get(
          url = url,
          config = dummyDownstreamHeaderCarrierConfig,
          requiredHeaders = requiredHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willPost[BODY, T](url: String, body: BODY): CallHandler[Future[T]] = {
      MockHttpClient
        .post(
          url = url,
          config = dummyDownstreamHeaderCarrierConfig,
          body = body,
          requiredHeaders = requiredHeaders ++ Seq("Content-Type" -> "application/json"),
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willPut[BODY, T](url: String, body: BODY): CallHandler[Future[T]] = {
      MockHttpClient
        .put(
          url = url,
          config = dummyDownstreamHeaderCarrierConfig,
          body = body,
          requiredHeaders = requiredHeaders ++ Seq("Content-Type" -> "application/json"),
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        )
    }

    protected def willDelete[T](url: String): CallHandler[Future[T]] = {
      MockHttpClient
        .delete(
          url = url,
          config = dummyDownstreamHeaderCarrierConfig,
          requiredHeaders = requiredHeaders,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        )
    }

  }

  protected trait DesTest extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = requiredDesHeaders

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
    MockedAppConfig.desEnvironmentHeaders returns Some(allowedDownstreamHeaders)

    MockedAppConfig.featureSwitches returns Configuration("tys-api.enabled" -> false)

  }

  protected trait IfsR5Test extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = requiredIfsR5Headers

    MockedAppConfig.ifsR5BaseUrl returns baseUrl
    MockedAppConfig.ifsR5Token returns "ifs-r5-token"
    MockedAppConfig.ifsR5Environment returns "ifs-r5-environment"
    MockedAppConfig.ifsR5EnvironmentHeaders returns Some(allowedDownstreamHeaders)

    MockedAppConfig.featureSwitches returns Configuration("tys-api.enabled" -> false)

  }

  protected trait IfsR6Test extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = requiredIfsR6Headers

    MockedAppConfig.ifsR6BaseUrl returns baseUrl
    MockedAppConfig.ifsR6Token returns "ifs-r6-token"
    MockedAppConfig.ifsR6Environment returns "ifs-r6-environment"
    MockedAppConfig.ifsR6EnvironmentHeaders returns Some(allowedDownstreamHeaders)

    MockedAppConfig.featureSwitches returns Configuration("tys-api.enabled" -> false)

  }

  protected trait TysIfsTest extends ConnectorTest {

    protected lazy val requiredHeaders: Seq[(String, String)] = requiredTysIfsHeaders

    MockedAppConfig.tysIfsBaseUrl returns baseUrl
    MockedAppConfig.tysIfsToken returns "TYS-IFS-token"
    MockedAppConfig.tysIfsEnvironment returns "TYS-IFS-environment"
    MockedAppConfig.tysIfsEnvironmentHeaders returns Some(allowedDownstreamHeaders)

    MockedAppConfig.featureSwitches returns Configuration("tys-api.enabled" -> true)
  }

}
