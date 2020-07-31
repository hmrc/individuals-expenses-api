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

package v1.models.response.RetrieveEmploymentExpenses

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.response.retrieveEmploymentExpenses.Expenses
import v1.models.utils.JsonErrorValidators

class ExpensesSpec extends UnitSpec with JsonErrorValidators {

  val expensesBody = Expenses(Some(1000.25), Some(1000.25), Some(1000.25), Some(1000.25), Some(1000.25), Some(1000.25), Some(1000.25), Some(1000.25))

  val json = Json.parse(
    """{
      |  "businessTravelCosts": 1000.25,
      |  "jobExpenses": 1000.25,
      |  "flatRateJobExpenses": 1000.25,
      |  "professionalSubscriptions": 1000.25,
      |  "hotelAndMealExpenses": 1000.25,
      |  "otherAndCapitalAllowances": 1000.25,
      |  "vehicleExpenses": 1000.25,
      |  "mileageAllowanceRelief": 1000.25
      |}""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        expensesBody shouldBe json.as[Expenses]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(expensesBody) shouldBe json
      }
    }
  }
}
