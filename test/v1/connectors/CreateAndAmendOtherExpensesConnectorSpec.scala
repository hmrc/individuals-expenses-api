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
import v1.models.request.createAndAmendOtherExpenses.{
  CreateAndAmendOtherExpensesBody,
  CreateAndAmendOtherExpensesRequest,
  PatentRoyaltiesPayments,
  PaymentsToTradeUnionsForDeathBenefits
}

import scala.concurrent.Future

class CreateAndAmendOtherExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2017-18"
  val nino: String    = "AA123456A"

  val body: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(
      PaymentsToTradeUnionsForDeathBenefits(
        Some("TRADE UNION PAYMENTS"),
        2000.99
      )),
    Some(
      PatentRoyaltiesPayments(
        Some("ROYALTIES PAYMENTS"),
        2000.99
      ))
  )

  trait Test { _: ConnectorTest =>

    val connector: CreateAndAmendOtherExpensesConnector = new CreateAndAmendOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

  }

  "amend" should {
    val request = CreateAndAmendOtherExpensesRequest(Nino(nino), taxYear, body)

    "put a body and return 204 no body" in new IfsR5Test with Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      willPut(
        url = s"$baseUrl/income-tax/expenses/other/$nino/$taxYear",
        body = body
      )
        .returns(Future.successful(outcome))

      await(connector.createAndAmend(request)) shouldBe outcome
    }
  }

}
