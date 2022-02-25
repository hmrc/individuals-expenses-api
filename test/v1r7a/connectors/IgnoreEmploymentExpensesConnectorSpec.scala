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

import mocks.MockAppConfig
import v1r7a.models.domain.Nino
import v1r7a.mocks.MockHttpClient
import uk.gov.hmrc.http.HeaderCarrier
import v1r7a.models.outcomes.ResponseWrapper
import v1r7a.models.request.ignoreEmploymentExpenses.{IgnoreEmploymentExpensesBody, IgnoreEmploymentExpensesRequest}

import scala.concurrent.Future

class IgnoreEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2021-22"
  val nino: String = "AA123456A"
  val body: IgnoreEmploymentExpensesBody = IgnoreEmploymentExpensesBody(true)

  class Test extends MockHttpClient with MockAppConfig {
    val connector: IgnoreEmploymentExpensesConnector = new IgnoreEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.desBaseUrl returns baseUrl
    MockAppConfig.desToken returns "des-token"
    MockAppConfig.desEnvironment returns "des-environment"
    MockAppConfig.desEnvironmentHeaders returns Some(allowedDesHeaders)
  }

  "ignore" should {
    val request = IgnoreEmploymentExpensesRequest(Nino(nino), taxYear)

    "put a body and return 204 no body" in new Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      implicit val hc: HeaderCarrier = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
      val requiredHeadersPut: Seq[(String, String)] = requiredDesHeaders ++ Seq("Content-Type" -> "application/json")

      MockHttpClient
        .put(
          url = s"$baseUrl/income-tax/expenses/employments/$nino/$taxYear",
          config = dummyDesHeaderCarrierConfig,
          body = body,
          requiredHeaders = requiredHeadersPut,
          excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
        ).returns(Future.successful(outcome))

      await(connector.ignore(request)) shouldBe outcome
    }
  }
}