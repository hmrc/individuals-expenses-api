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
import v1.models.request.CreateAmendEmploymentExpenses.{AmendEmploymentExpensesBody, CreateAmendEmploymentExpensesRequest, Expenses}
import v1.models.request.TaxYear

import scala.concurrent.Future

class CreateAmendEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

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

  trait Test {
    _: ConnectorTest =>

    def taxYear: TaxYear

    val connector: CreateAmendEmploymentExpensesConnector = new CreateAmendEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val request: CreateAmendEmploymentExpensesRequest = CreateAmendEmploymentExpensesRequest(Nino(nino), taxYear, body)

  }

  "CreateAmendEmploymentExpensesConnector" when {

    "amend" should {

      "put a body and return 204 no body" in new IfsR6Test with Test {

        def taxYear: TaxYear = TaxYear.fromMtd("2021-22")

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/expenses/employments/$nino/2021-22",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.createAmendEmploymentExpenses(request)) shouldBe outcome

      }

      "put a body and return 204 no body for a TYS request" in new TysIfsTest with Test {

        def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

        val outcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/23-24/expenses/employments/$nino",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.createAmendEmploymentExpenses(request)) shouldBe outcome

      }
    }

  }

}
