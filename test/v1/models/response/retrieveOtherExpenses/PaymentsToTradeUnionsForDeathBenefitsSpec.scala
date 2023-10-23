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

package v1.models.response.retrieveOtherExpenses

import api.models.utils.JsonErrorValidators
import play.api.libs.json.Json
import support.UnitSpec

class PaymentsToTradeUnionsForDeathBenefitsSpec extends UnitSpec with JsonErrorValidators {

  val paymentsToTradeUnionsForDeathBenefits      = PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2314.32)
  val emptyPaymentsToTradeUnionsForDeathBenefits = PaymentsToTradeUnionsForDeathBenefits(None, 2314.32)

  val json = Json.parse(
    """{
      |  "customerReference": "TRADE UNION PAYMENTS",
      |  "expenseAmount": 2314.32
      |}""".stripMargin
  )

  val noReferenceJson = Json.parse(
    """{
      |  "expenseAmount": 2314.32
      |}""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        paymentsToTradeUnionsForDeathBenefits shouldBe json.as[PaymentsToTradeUnionsForDeathBenefits]
      }
    }
  }

  "read from empty JSON" should {
    "convert empty MTD JSON into an empty PaymentsToTradeUnionsForDeathBenefits object" in {
      emptyPaymentsToTradeUnionsForDeathBenefits shouldBe noReferenceJson.as[PaymentsToTradeUnionsForDeathBenefits]
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(paymentsToTradeUnionsForDeathBenefits) shouldBe json
      }
    }
  }

  "written from an empty body" when {
    "passed an empty model" should {
      "return an no reference JSON" in {
        Json.toJson(emptyPaymentsToTradeUnionsForDeathBenefits) shouldBe noReferenceJson
      }
    }
  }

}
