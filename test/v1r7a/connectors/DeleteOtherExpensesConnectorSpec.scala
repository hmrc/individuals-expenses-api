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
import v1r7a.models.outcomes.ResponseWrapper
import v1r7a.models.request.deleteOtherExpenses.DeleteOtherExpensesRequest

import scala.concurrent.Future

class DeleteOtherExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2017-18"
  val nino: String = "AA123456A"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: DeleteOtherExpensesConnector = new DeleteOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifsBaseUrl returns baseUrl
    MockAppConfig.ifsToken returns "ifs-token"
    MockAppConfig.ifsEnvironment returns "ifs-environment"
    MockAppConfig.ifsEnvironmentHeaders returns Some(allowedIfsHeaders)
  }

  "delete" should {
    val request = DeleteOtherExpensesRequest(Nino(nino), taxYear)

    "return a 204 with no body" when {
      "the downstream call is successful" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockHttpClient
          .delete(
            url = s"$baseUrl/income-tax/expenses/other/$nino/${request.taxYear}",
            config = dummyIfsHeaderCarrierConfig,
            requiredHeaders = requiredIfsHeaders,
            excludedHeaders = Seq("AnotherHeader" -> "HeaderValue")
          )
          .returns(Future.successful(outcome))

        await(connector.deleteOtherExpenses(request)) shouldBe outcome
      }
    }
  }
}
