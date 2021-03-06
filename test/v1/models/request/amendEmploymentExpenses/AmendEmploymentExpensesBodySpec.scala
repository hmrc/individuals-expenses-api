/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.models.request.amendEmploymentExpenses

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.utils.JsonErrorValidators

class AmendEmploymentExpensesBodySpec extends UnitSpec with JsonErrorValidators {

  val amendEmploymentExpensesBody = AmendEmploymentExpensesBody(Expenses(Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12)))

  val json = Json.parse(
    """
      |{
      |    "expenses": {
      |        "businessTravelCosts": 123.12,
      |        "jobExpenses": 123.12,
      |        "flatRateJobExpenses": 123.12,
      |        "professionalSubscriptions": 123.12,
      |        "hotelAndMealExpenses": 123.12,
      |        "otherAndCapitalAllowances": 123.12,
      |        "vehicleExpenses": 123.12,
      |        "mileageAllowanceRelief": 123.12
      |    }
      |}
      |""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        amendEmploymentExpensesBody shouldBe json.as[AmendEmploymentExpensesBody]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(amendEmploymentExpensesBody) shouldBe json
      }
    }
  }

  "isIncorrectOrEmptyBody" should {
    "return true" when {
      "expenses is empty" in {
        val expenses = Expenses(None, None, None, None, None, None, None, None)
        AmendEmploymentExpensesBody(expenses).isIncorrectOrEmptyBody shouldBe true
      }
      "expenses is not empty" in {
        val expenses = Expenses(Some(1), None, None, None, None, None, None, None)
        AmendEmploymentExpensesBody(expenses).isIncorrectOrEmptyBody shouldBe false
      }
    }
  }
}

