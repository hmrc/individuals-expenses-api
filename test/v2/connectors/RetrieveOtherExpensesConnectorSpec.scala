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

package v2.connectors

import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear, Timestamp}
import shared.models.outcomes.ResponseWrapper
import uk.gov.hmrc.http.StringContextOps
import v2.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequestData
import v2.models.response.retrieveOtherExpenses.{PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits, RetrieveOtherExpensesResponse}

import scala.concurrent.Future

class RetrieveOtherExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  val retrieveOtherExpensesResponse: RetrieveOtherExpensesResponse =
    RetrieveOtherExpensesResponse(
      Timestamp("2019-04-04T01:01:01Z"),
      Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 5433.54)),
      Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 98765.12))
    )

  trait Test {
    _: ConnectorTest =>

    def taxYear: String

    val connector: RetrieveOtherExpensesConnector = new RetrieveOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    lazy val request: RetrieveOtherExpensesRequestData = RetrieveOtherExpensesRequestData(Nino(nino), TaxYear.fromMtd(taxYear))

  }

  "RetrieveOtherExpensesConnector" should {

    "return a result" when {

      "the downstream call is successful" in new IfsTest with Test {

        def taxYear: String = "2021-22"
        val outcome: Right[Nothing, ResponseWrapper[RetrieveOtherExpensesResponse]] = Right(ResponseWrapper(correlationId, retrieveOtherExpensesResponse))

        willGet(url = url"$baseUrl/income-tax/expenses/other/$nino/2021-22")
          .returns(Future.successful(outcome))

        await(connector.retrieveOtherExpenses(request)) shouldBe outcome
      }

      "the downstream call is successful for a TYS tax year" in new IfsTest with Test {

        def taxYear: String = "2023-24"
        val outcome: Right[Nothing, ResponseWrapper[RetrieveOtherExpensesResponse]] = Right(ResponseWrapper(correlationId, retrieveOtherExpensesResponse))

        willGet(url = url"$baseUrl/income-tax/expenses/other/23-24/$nino")
          .returns(Future.successful(outcome))

        await(connector.retrieveOtherExpenses(request)) shouldBe outcome
      }
    }
  }

}
