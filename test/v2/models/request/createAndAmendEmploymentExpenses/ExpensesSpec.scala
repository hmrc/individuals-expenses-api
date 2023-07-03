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

package v2.models.request.createAndAmendEmploymentExpenses

import api.utils.JsonErrorValidators
import play.api.libs.json.Json
import support.UnitSpec

class ExpensesSpec extends UnitSpec with JsonErrorValidators {

  val expensesBody: Expenses =
    Expenses(Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12))

  private val json = Json.parse(
    """
      |{
      |    "businessTravelCosts": 123.12,
      |    "jobExpenses": 123.12,
      |    "flatRateJobExpenses": 123.12,
      |    "professionalSubscriptions": 123.12,
      |    "hotelAndMealExpenses": 123.12,
      |    "otherAndCapitalAllowances": 123.12,
      |    "vehicleExpenses": 123.12,
      |    "mileageAllowanceRelief": 123.12
      |}
      |""".stripMargin
  )

  val minModel: Expenses = Expenses(None, None, None, None, None, None, None, None)

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

  "isEmpty" should {
    "return true" when {
      "all fields are empty" in {
        minModel.isEmpty shouldBe true
      }
    }
    "return false" when {
      "businessTravelCosts is not empty" in {
        minModel.copy(businessTravelCosts = Some(1)).isEmpty shouldBe false
      }
      "jobExpenses is not empty" in {
        minModel.copy(jobExpenses = Some(1)).isEmpty shouldBe false
      }
      "flatRateJobExpenses is not empty" in {
        minModel.copy(flatRateJobExpenses = Some(1)).isEmpty shouldBe false
      }
      "professionalSubscriptions is not empty" in {
        minModel.copy(professionalSubscriptions = Some(1)).isEmpty shouldBe false
      }
      "hotelAndMealExpenses is not empty" in {
        minModel.copy(hotelAndMealExpenses = Some(1)).isEmpty shouldBe false
      }
      "otherAndCapitalAllowances is not empty" in {
        minModel.copy(otherAndCapitalAllowances = Some(1)).isEmpty shouldBe false
      }
      "vehicleExpenses is not empty" in {
        minModel.copy(vehicleExpenses = Some(1)).isEmpty shouldBe false
      }
      "mileageAllowanceRelief is not empty" in {
        minModel.copy(mileageAllowanceRelief = Some(1)).isEmpty shouldBe false
      }
    }
  }

}
