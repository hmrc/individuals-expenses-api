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

package v1.connectors

import v1.models.domain.Nino
import v1.models.outcomes.ResponseWrapper
import v1.models.request.TaxYear
import v1.models.request.amendOtherExpenses._

import scala.concurrent.Future

class AmendOtherExpensesConnectorSpec extends ConnectorSpec {

  "amend" should {
    "return the expected response for a non-TYS request" when {
      "a valid request is made" in new IfsR5Test with Test {
        def taxYear: String = "2021-22"

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/expenses/other/$nino/2021-22",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.amend(request)) shouldBe outcome
      }
    }
    "return the expected response for a TYS request" when {
      "a valid request is made" in new TysIfsTest with Test {
        def taxYear: String = "2023-24"

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/expenses/other/23-24/$nino",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.amend(request)) shouldBe outcome
      }
    }
  }

  trait Test {
    _: ConnectorTest =>

    val nino: String = "AA123456A"
    def taxYear: String

    val connector: AmendOtherExpensesConnector = new AmendOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val body: AmendOtherExpensesBody = AmendOtherExpensesBody(
      Some(
        PaymentsToTradeUnionsForDeathBenefits(
          Some("TRADE UNION PAYMENTS"),
          2000.99
        )),
      Some(
        PatentRoyaltiesPayments(
          Some("ROYALTIES PAYMENTS"),
          2000.99
        ))
    )

    lazy val request: AmendOtherExpensesRequest = AmendOtherExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear), body)
  }

}
