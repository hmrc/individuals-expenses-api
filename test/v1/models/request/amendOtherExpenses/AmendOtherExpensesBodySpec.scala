/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.request.amendOtherExpenses

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class AmendOtherExpensesBodySpec extends UnitSpec with JsonErrorValidators {

  val amendOtherExpensesBody = AmendOtherExpensesBody(Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"),2314.32)), Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"),2314.32)))
  val amendOtherExpensesBodyWithoutPatents = AmendOtherExpensesBody(Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"),2314.32)), None)
  val amendOtherExpensesBodyWithoutPayments = AmendOtherExpensesBody(None, Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"),2314.32)))


  val json = Json.parse(
    """{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 2314.32
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  val patentsMissingJson = Json.parse(
    """{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  val paymentsMissingJson = Json.parse(
    """{
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        amendOtherExpensesBody shouldBe json.as[AmendOtherExpensesBody]
      }
    }
  }
  "read from empty JSON with missing Patents" should {
    "convert JSON into an empty AmendOtherExpensesBody object" in {
      amendOtherExpensesBodyWithoutPatents shouldBe patentsMissingJson.as[AmendOtherExpensesBody]
    }
  }
  "read from empty JSON with missing Payments" should {
    "convert JSON into an empty AmendOtherExpensesBody object" in {
      amendOtherExpensesBodyWithoutPayments shouldBe paymentsMissingJson.as[AmendOtherExpensesBody]
    }
  }
  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(amendOtherExpensesBody) shouldBe json
      }
    }
    "write from an empty body" when {
      "passed a model missing patents" should {
        "return an empty JSON" in {
          Json.toJson(amendOtherExpensesBodyWithoutPatents) shouldBe patentsMissingJson
        }
      }
      "passed a model missing payments" should {
        "return an empty JSON" in {
          Json.toJson(amendOtherExpensesBodyWithoutPayments) shouldBe paymentsMissingJson
        }
      }
    }
  }
}
