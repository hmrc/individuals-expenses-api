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

package v3.connectors

import shared.connectors.ConnectorSpec
import shared.models.domain.{Nino, TaxYear}
import shared.models.outcomes.ResponseWrapper
import v3.models.request.deleteOtherExpenses.DeleteOtherExpensesRequestData

import scala.concurrent.Future

class DeleteOtherExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  trait Test {
    _: ConnectorTest =>
    def taxYear: TaxYear
    protected val request = DeleteOtherExpensesRequestData(Nino(nino), taxYear)

    protected val connector: DeleteOtherExpensesConnector = new DeleteOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "deleteOtherExpenses" should {
    "return a 204 with no body" when {
      "the downstream call is successful" in new IfsTest with Test {
        def taxYear: TaxYear = TaxYear.fromMtd("2017-18")
        val outcome          = Right(ResponseWrapper(correlationId, ()))

        willDelete(s"$baseUrl/income-tax/expenses/other/$nino/2017-18").returns(Future.successful(outcome))

        await(connector.deleteOtherExpenses(request)) shouldBe outcome
      }
    }
    "a valid request is called for a Tax Year Specific tax year" in new IfsTest with Test {
      def taxYear: TaxYear = TaxYear.fromMtd("2023-24")
      val outcome          = Right(ResponseWrapper(correlationId, ()))

      willDelete(s"$baseUrl/income-tax/expenses/other/23-24/$nino").returns(Future.successful(outcome))

      await(connector.deleteOtherExpenses(request)) shouldBe outcome
    }
  }

}
