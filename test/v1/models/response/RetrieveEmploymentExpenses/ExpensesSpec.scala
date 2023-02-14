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

package v1.models.response.RetrieveEmploymentExpenses

import api.utils.JsonErrorValidators
import play.api.libs.json.Json
import support.UnitSpec
import v1.fixtures.RetrieveEmploymentsExpensesFixtures._
import v1.models.response.retrieveEmploymentExpenses.Expenses

class ExpensesSpec extends UnitSpec with JsonErrorValidators {

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        expensesModel shouldBe expensesJson.as[Expenses]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(expensesModel) shouldBe expensesJson
      }
    }
  }

}
