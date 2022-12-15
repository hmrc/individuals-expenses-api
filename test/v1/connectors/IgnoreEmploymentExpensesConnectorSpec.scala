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
import v1.models.request.ignoreEmploymentExpenses.{IgnoreEmploymentExpensesBody, IgnoreEmploymentExpensesRequest}

import scala.concurrent.Future

class IgnoreEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String                    = "2021-22"
  val nino: String                       = "AA123456A"
  val body: IgnoreEmploymentExpensesBody = IgnoreEmploymentExpensesBody(true)

  class Test { _: ConnectorTest =>

    val connector: IgnoreEmploymentExpensesConnector = new IgnoreEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "ignore" should {
    val request = IgnoreEmploymentExpensesRequest(Nino(nino), taxYear)

    "put a body and return 204 no body" in new Test with IfsR6Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      willPut(
        url = s"$baseUrl/income-tax/expenses/employments/$nino/$taxYear",
        body = body
      )
        .returns(Future.successful(outcome))

      await(connector.ignore(request)) shouldBe outcome
    }
  }

}
