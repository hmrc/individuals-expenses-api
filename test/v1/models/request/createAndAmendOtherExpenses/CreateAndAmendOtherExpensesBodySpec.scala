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

package v1.models.request.createAndAmendOtherExpenses

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class CreateAndAmendOtherExpensesBodySpec extends UnitSpec with JsonErrorValidators {

  val createAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2314.32)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2314.32)))

  val createAndAmendOtherExpensesBodyWithoutPatents =
    CreateAndAmendOtherExpensesBody(Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2314.32)), None)

  val createAndAmendOtherExpensesBodyWithoutPayments =
    CreateAndAmendOtherExpensesBody(None, Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2314.32)))

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
        createAndAmendOtherExpensesBody shouldBe json.as[CreateAndAmendOtherExpensesBody]
      }
    }
  }

  "read from empty JSON with missing Patents" should {
    "convert JSON into an empty CreateAndAmendOtherExpensesBody object" in {
      createAndAmendOtherExpensesBodyWithoutPatents shouldBe patentsMissingJson.as[CreateAndAmendOtherExpensesBody]
    }
  }

  "read from empty JSON with missing Payments" should {
    "convert JSON into an empty CreateAndAmendOtherExpensesBody object" in {
      createAndAmendOtherExpensesBodyWithoutPayments shouldBe paymentsMissingJson.as[CreateAndAmendOtherExpensesBody]
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(createAndAmendOtherExpensesBody) shouldBe json
      }
    }
    "write from an empty body" when {
      "passed a model missing patents" should {
        "return an empty JSON" in {
          Json.toJson(createAndAmendOtherExpensesBodyWithoutPatents) shouldBe patentsMissingJson
        }
      }
      "passed a model missing payments" should {
        "return an empty JSON" in {
          Json.toJson(createAndAmendOtherExpensesBodyWithoutPayments) shouldBe paymentsMissingJson
        }
      }
    }
  }

}
