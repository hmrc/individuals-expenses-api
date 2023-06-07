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

package v1.models.response.retrieveEmploymentExpenses

import api.utils.JsonErrorValidators
import support.UnitSpec
import v1.fixtures.RetrieveEmploymentsExpensesFixtures._

class RetrieveEmploymentsExpensesSpec extends UnitSpec with JsonErrorValidators {

  "reads" when {
    "passed valid JSON" should {
      "return a valid model for latest" in {
        responseModelLatest shouldBe downstreamResponseJsonLatest.as[RetrieveEmploymentsExpensesResponse]
      }
      "return a valid model for customer" in {
        responseModelUser shouldBe downstreamResponseJsonCustomer.as[RetrieveEmploymentsExpensesResponse]
      }
      "return a valid model for hmrcHeld" in {
        responseModelHmrcHeld shouldBe downstreamResponseJsonHmrcHeld.as[RetrieveEmploymentsExpensesResponse]
      }
    }
  }

  "writes" when {
    "passed a response object" should {
      "return valid JSON for latest" in {
        responseModelLatest.toJson shouldBe mtdResponseJsonLatest
      }
      "return valid JSON for user" in {
        responseModelUser.toJson shouldBe mtdResponseJsonUser
      }
      "return valid JSON for hmrcHeld" in {
        responseModelHmrcHeld.toJson shouldBe mtdResponseJsonHmrcHeld
      }
    }
  }

}
