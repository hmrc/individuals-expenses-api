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
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v2.models.request.deleteEmploymentExpenses.DeleteEmploymentExpensesRequestData

import scala.concurrent.Future

class DeleteEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  trait Test { _: ConnectorTest =>
    def taxYear: TaxYear

    protected val connector: DeleteEmploymentExpensesConnector =
      new DeleteEmploymentExpensesConnector(
        http = mockHttpClient,
        appConfig = mockAppConfig
      )

    protected val request: DeleteEmploymentExpensesRequestData =
      DeleteEmploymentExpensesRequestData(
        nino = Nino(nino),
        taxYear = taxYear
      )

  }

  "DeleteEmploymentExpensesConnector" should {
    "return a 200 result on delete" when {
      "the downstream call is successful and not tax year specific" in new DesTest with Test {
        def taxYear: TaxYear                               = TaxYear.fromMtd("2017-18")
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willDelete(s"$baseUrl/income-tax/expenses/employments/$nino/2017-18") returns Future.successful(outcome)

        val result = await(connector.deleteEmploymentExpenses(request))

        result shouldBe outcome
      }

      "the downstream call is successful and is tax year specific" in new TysIfsTest with Test {
        def taxYear: TaxYear                               = TaxYear.fromMtd("2023-24")
        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willDelete(s"$baseUrl/income-tax/expenses/employments/23-24/$nino") returns Future.successful(outcome)

        val result = await(connector.deleteEmploymentExpenses(request))

        result shouldBe outcome
      }

    }

  }

}
