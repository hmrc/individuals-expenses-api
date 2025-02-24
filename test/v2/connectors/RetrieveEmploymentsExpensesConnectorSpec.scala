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

import common.domain.MtdSource
import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v2.fixtures.RetrieveEmploymentsExpensesFixtures._
import v2.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequestData

import scala.concurrent.Future

class RetrieveEmploymentsExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  trait Test {
    _: ConnectorTest =>

    def taxYear: String

    val connector: RetrieveEmploymentsExpensesConnector = new RetrieveEmploymentsExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val request: RetrieveEmploymentsExpensesRequestData =
      RetrieveEmploymentsExpensesRequestData(
        nino = Nino(nino),
        taxYear = TaxYear.fromMtd(taxYear),
        source = MtdSource.`user`
      )

  }

  "retrieveEmploymentExpenses" should {
    "return a result" when {
      "the downstream call is successful for a non-TYS tax year" in new IfsTest with Test {
        def taxYear: String = "2019-20"
        val outcome         = Right(ResponseWrapper(correlationId, responseModelUser))

        willGet(s"$baseUrl/income-tax/expenses/employments/$nino/2019-20?view=CUSTOMER")
          .returns(Future.successful(outcome))

        await(connector.retrieveEmploymentExpenses(request)) shouldBe outcome
      }

      "the downstream call is successful for a TYS tax year" in new IfsTest with Test {
        def taxYear: String = "2023-24"
        val outcome         = Right(ResponseWrapper(correlationId, responseModelUser))

        willGet(s"$baseUrl/income-tax/expenses/employments/23-24/$nino?view=CUSTOMER")
          .returns(Future.successful(outcome))

        await(connector.retrieveEmploymentExpenses(request)) shouldBe outcome
      }
    }
  }

}
