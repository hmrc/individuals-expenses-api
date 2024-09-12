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
import v2.models.request.ignoreEmploymentExpenses.{IgnoreEmploymentExpensesBody, IgnoreEmploymentExpensesRequestData}

import scala.concurrent.Future

class IgnoreEmploymentExpensesConnectorSpec extends ConnectorSpec {

  val body: IgnoreEmploymentExpensesBody = IgnoreEmploymentExpensesBody(true)

  trait Test { _: ConnectorTest =>

    val connector: IgnoreEmploymentExpensesConnector = new IgnoreEmploymentExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    val outcome = Right(ResponseWrapper(correlationId, ()))

  }

  "ignore" should {
    "return the expected response for a non-TYS request" when {
      "a valid request is made" in new IfsR6Test with Test {
        val request = IgnoreEmploymentExpensesRequestData(Nino("AA123456A"), TaxYear.fromMtd("2021-22"))

        willPut(
          url = s"$baseUrl/income-tax/expenses/employments/AA123456A/2021-22",
          body = body
        ).returns(Future.successful(outcome))

        await(connector.ignore(request)) shouldBe outcome
      }
    }

    "return the expected response for a TYS request" when {
      "a valid request is made" in new TysIfsTest with Test {
        val request = IgnoreEmploymentExpensesRequestData(Nino("AA123456A"), TaxYear.fromMtd("2023-24"))

        willPut(
          url = s"$baseUrl/income-tax/23-24/expenses/employments/AA123456A",
          body = body
        ).returns(Future.successful(outcome))

        await(connector.ignore(request)) shouldBe outcome
      }
    }
  }

}
