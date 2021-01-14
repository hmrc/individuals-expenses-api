/*
 * Copyright 2021 HM Revenue & Customs
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

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequest
import v1.models.response.retrieveOtherExpenses.{PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits, RetrieveOtherExpensesResponse}

import scala.concurrent.Future

class RetrieveOtherExpensesConnectorSpec extends ConnectorSpec {

  private val nino = Nino("AA123456A")
  private val taxYear = "2019-20"

  class Test extends MockHttpClient with MockAppConfig {
    val connector: RetrieveOtherExpensesConnector = new RetrieveOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnvironment returns "des-environment"
  }

  "retrieve business details" should {
    val request = RetrieveOtherExpensesRequest(nino, taxYear)
    "return a result" when {
      "the downstream call is successful" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, RetrieveOtherExpensesResponse(
          "2019-04-04T01:01:01Z",
          Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 5433.54)),
          Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 98765.12))
        )))

        MockedHttpClient
          .get(
            url = s"$baseUrl/income-tax/expenses/other/${request.nino}/${request.taxYear}",
            requiredHeaders = requiredHeaders :_*
          )
          .returns(Future.successful(outcome))

        await(connector.retrieveOtherExpenses(request)) shouldBe outcome
      }
    }
  }
}
