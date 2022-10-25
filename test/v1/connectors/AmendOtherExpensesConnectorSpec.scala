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
import uk.gov.hmrc.http.HeaderCarrier
import v1.mocks.MockHttpClient
import v1.models.domain.Nino
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendOtherExpenses.{AmendOtherExpensesBody, AmendOtherExpensesRequest, PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits}

import scala.concurrent.Future

class AmendOtherExpensesConnectorSpec extends ConnectorSpec {

  val taxYear: String = "2017-18"
  val nino: String    = "AA123456A"

  val body: AmendOtherExpensesBody = AmendOtherExpensesBody(
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

  class Test extends MockHttpClient with MockAppConfig {

    val connector: AmendOtherExpensesConnector = new AmendOtherExpensesConnector(
      http = mockHttpClient,
      appConfig = mockAppConfig
    )

    MockAppConfig.ifsR5BaseUrl returns baseUrl
    MockAppConfig.ifsR5Token returns "ifs-token"
    MockAppConfig.ifsR5Environment returns "ifs-environment"
    MockAppConfig.ifsR5EnvironmentHeaders returns Some(allowedDownstreamHeaders)
  }

  "amend" should {
    val request = AmendOtherExpensesRequest(Nino(nino), taxYear, body)

    "put a body and return 204 no body" in new Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))

      implicit val hc: HeaderCarrier                = HeaderCarrier(otherHeaders = otherHeaders ++ Seq("Content-Type" -> "application/json"))
      val requiredHeadersPut: Seq[(String, String)] = requiredIfsHeaders ++ Seq("Content-Type" -> "application/json")

      MockHttpClient
        .put(
          url = s"$baseUrl/income-tax/expenses/other/$nino/$taxYear",
          config = dummyDownstreamHeaderCarrierConfig,
          body = body,
          requiredHeaders = requiredHeadersPut,
          excludedHeaders = excludedHeaders
        )
        .returns(Future.successful(outcome))

      await(connector.amend(request)) shouldBe outcome
    }
  }

}
