/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.http.StringContextOps
import v2.models.request.createAndAmendOtherExpenses._

import scala.concurrent.Future

class CreateAndAmendOtherExpensesConnectorSpec extends ConnectorSpec {

  "CreateAndAmendOtherExpensesConnector" should {
    "return the expected response for a non-TYS request" when {
      "a valid request is made" in new IfsTest with Test {
        def taxYear: String = "2021-22"

        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = url"$baseUrl/income-tax/expenses/other/$nino/2021-22",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.createAndAmend(request)) shouldBe outcome
      }
    }
    "return the expected response for a TYS request" when {
      "a valid request is made" in new IfsTest with Test {
        def taxYear: String = "2023-24"

        val outcome: Right[Nothing, ResponseWrapper[Unit]] = Right(ResponseWrapper(correlationId, ()))

        willPut(
          url = url"$baseUrl/income-tax/expenses/other/23-24/$nino",
          body = body
        )
          .returns(Future.successful(outcome))

        await(connector.createAndAmend(request)) shouldBe outcome
      }
    }
  }

  trait Test {
    _: ConnectorTest =>

    val nino: String = "AA123456A"
    def taxYear: String

    val connector: CreateAndAmendOtherExpensesConnector = new CreateAndAmendOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

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

    lazy val request: CreateAndAmendOtherExpensesRequestData = CreateAndAmendOtherExpensesRequestData(Nino(nino), TaxYear.fromMtd(taxYear), body)
  }

}
