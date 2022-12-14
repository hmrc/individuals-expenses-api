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
import v1.mocks.MockHttpClient
import v1.models.domain.Nino
import v1.models.outcomes.ResponseWrapper
import v1.models.request.deleteOtherExpenses.DeleteOtherExpensesRequest

import scala.concurrent.Future

class DeleteOtherExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2017-18"
  val nino: String    = "AA123456A"

  class Test extends MockHttpClient with MockAppConfig {

    val connector: DeleteOtherExpensesConnector = new DeleteOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "deleteOtherExpenses" should {
    val request = DeleteOtherExpensesRequest(Nino(nino), taxYear)

    "return a 204 with no body" when {
      "the downstream call is successful" in new Test with IfsR5Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        willDelete(
          url = s"$baseUrl/income-tax/expenses/other/$nino/${request.taxYear}"
        )
          .returns(Future.successful(outcome))

        await(connector.deleteOtherExpenses(request)) shouldBe outcome
      }
    }
  }

}
