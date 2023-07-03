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

import api.connectors.{ConnectorSpec, DownstreamOutcome}
import api.models.domain.{Nino, TaxYear}
import api.models.outcomes.ResponseWrapper
import v2.models.request.createAndAmendEmploymentExpenses.{CreateAndAmendEmploymentExpensesBody, CreateAndAmendEmploymentExpensesRequest, Expenses}

import scala.concurrent.Future

class CreateAndAmendEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val nino: String = "AA123456A"

  val body: CreateAndAmendEmploymentExpensesBody = CreateAndAmendEmploymentExpensesBody(
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

    val connector: CreateAndAmendEmploymentExpensesConnector = new CreateAndAmendEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val request: CreateAndAmendEmploymentExpensesRequest = CreateAndAmendEmploymentExpensesRequest(Nino(nino), taxYear, body)

  }

  "CreateAmendEmploymentExpensesConnector" when {

    "amend" should {

      "put a body and return 204 no body" in new IfsR6Test with Test {

        def taxYear: TaxYear = TaxYear.fromMtd("2021-22")

        val expectedOutcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/expenses/employments/$nino/2021-22",
          body = body
        )
          .returns(Future.successful(expectedOutcome))

        val result: DownstreamOutcome[Unit] = await(connector.createAmendEmploymentExpenses(request))

        result shouldBe expectedOutcome

      }

      "put a body and return 204 no body for a TYS request" in new TysIfsTest with Test {

        def taxYear: TaxYear = TaxYear.fromMtd("2023-24")

        val expectedOutcome = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = s"$baseUrl/income-tax/23-24/expenses/employments/$nino",
          body = body
        )
          .returns(Future.successful(expectedOutcome))

        val result: DownstreamOutcome[Unit] = await(connector.createAmendEmploymentExpenses(request))

        result shouldBe expectedOutcome

      }
    }

  }

}
