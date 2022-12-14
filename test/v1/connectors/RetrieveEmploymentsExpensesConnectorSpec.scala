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

import mocks.MockAppConfig
import v1.fixtures.RetrieveEmploymentsExpensesFixtures._
import v1.mocks.MockHttpClient
import v1.models.domain.{MtdSource, Nino}
import v1.models.outcomes.ResponseWrapper
import v1.models.request.TaxYear
import v1.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequest

import scala.concurrent.Future

class RetrieveEmploymentsExpensesConnectorSpec extends ConnectorSpec {

  val nino: String      = "AA123456A"
  val source: MtdSource = MtdSource.`user`

  def makeRequest(taxYear: String): RetrieveEmploymentsExpensesRequest =
    RetrieveEmploymentsExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear), source)

  val nonTysRequest = makeRequest("2019-20")
  val tysRequest    = makeRequest("2023-24")

  trait Test extends MockHttpClient with MockAppConfig {

    val connector: RetrieveEmploymentsExpensesConnector = new RetrieveEmploymentsExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "retrieveEmploymentExpenses" should {
    "return a result" when {
      "the downstream call is successful for a non-TYS tax year" in new Test with IfsR6Test {
        val outcome = Right(ResponseWrapper(correlationId, responseModelUser))

        willGet(s"$baseUrl/income-tax/expenses/employments/$nino/2019-20?view=CUSTOMER")
          .returns(Future.successful(outcome))

        await(connector.retrieveEmploymentExpenses(nonTysRequest)) shouldBe outcome
      }

      "the downstream call is successful for a TYS tax year" in new Test with TysIfsTest {
        val outcome = Right(ResponseWrapper(correlationId, responseModelUser))

        willGet(s"$baseUrl/income-tax/expenses/employments/23-24/$nino?view=CUSTOMER")
          .returns(Future.successful(outcome))

        await(connector.retrieveEmploymentExpenses(tysRequest)) shouldBe outcome
      }
    }
  }

}
