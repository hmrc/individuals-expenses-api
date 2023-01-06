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

package v1.connectors

import v1.models.domain.Nino
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendEmploymentExpenses.{AmendEmploymentExpensesBody, AmendEmploymentExpensesRequest, Expenses}

import scala.concurrent.Future

class AmendEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2021-22"
  val nino: String    = "AA123456A"

  val body: AmendEmploymentExpensesBody = AmendEmploymentExpensesBody(
    Expenses(
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12),
      Some(123.12)
    )
  )

  trait Test { _: ConnectorTest =>

    val connector: AmendEmploymentExpensesConnector = new AmendEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "amend" should {
    val request = AmendEmploymentExpensesRequest(Nino(nino), taxYear, body)

    "put a body and return 204 no body" in new IfsR6Test with Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      willPut(
        url = s"$baseUrl/income-tax/expenses/employments/$nino/$taxYear",
        body = body
      )
        .returns(Future.successful(outcome))

      await(connector.amend(request)) shouldBe outcome
    }
  }

}
