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
import v1.models.response.retrieveEmploymentExpenses._
import v1.models.utils.JsonErrorValidators

class RetrieveEmploymentExpensesSpec extends UnitSpec with JsonErrorValidators {

  val retrieveEmploymentExpensesBody =
    RetrieveEmploymentExpensesResponse(Some("2020-07-13T20:37:27Z"),
      Some(1000.25),
      Some("latest"),
      Some("2020-07-13T20:37:27Z"),
      Some(Expenses(Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25),Some(1000.25))))

  val json = Json.parse(
    """{
      |    "submittedOn": "2020-07-13T20:37:27Z",
      |    "totalExpenses": 1000.25,
      |    "source": "LATEST",
      |    "dateIgnored": "2020-07-13T20:37:27Z",
      |    "expenses": {
      |        "businessTravelCosts": 1000.25,
      |        "jobExpenses": 1000.25,
      |        "flatRateJobExpenses": 1000.25,
      |        "professionalSubscriptions": 1000.25,
      |        "hotelAndMealExpenses": 1000.25,
      |        "otherAndCapitalAllowances": 1000.25,
      |        "vehicleExpenses": 1000.25,
      |        "mileageAllowanceRelief": 1000.25
      |    }
      |}""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        retrieveEmploymentExpensesBody shouldBe json.as[RetrieveEmploymentExpensesResponse]
      }
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(retrieveEmploymentExpensesBody) shouldBe json
      }
    }
  }
}
