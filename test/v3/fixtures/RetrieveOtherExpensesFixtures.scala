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

package v3.fixtures

import shared.models.domain.Timestamp
import play.api.libs.json.{JsObject, JsValue, Json}
import v3.models.response.retrieveOtherExpenses.{PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits, RetrieveOtherExpensesResponse}

object RetrieveOtherExpensesFixtures {

  val responseModel = RetrieveOtherExpensesResponse(
    submittedOn = Timestamp("2019-04-04T01:01:01Z"),
    paymentsToTradeUnionsForDeathBenefits = Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 4528.99)),
    patentRoyaltiesPayments = Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.10))
  )

  val retrieveOtherExpensesMtdResponse: JsValue =
    Json
      .parse(
        s"""
         |{
         |  "submittedOn": "2019-04-04T01:01:01.000Z",
         |  "paymentsToTradeUnionsForDeathBenefits": {
         |    "customerReference": "TRADE UNION PAYMENTS",
         |    "expenseAmount": 4528.99
         |  },
         |  "patentRoyaltiesPayments": {
         |    "customerReference": "ROYALTIES PAYMENTS",
         |    "expenseAmount": 2000.10
         |  }
         |}
         |""".stripMargin
      )
      .as[JsObject]

}
