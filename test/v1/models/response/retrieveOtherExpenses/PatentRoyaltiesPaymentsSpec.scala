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

class PatentRoyaltiesPaymentsSpec extends UnitSpec with JsonErrorValidators {

  val patentRoyaltiesPayments      = PatentRoyaltiesPayments(Some("ROYALTIES PAYMENT"), 2314.32)
  val emptyPatentRoyaltiesPayments = PatentRoyaltiesPayments(None, 2314.32)

  val json = Json.parse(
    """{
      |  "customerReference": "ROYALTIES PAYMENT",
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
        patentRoyaltiesPayments shouldBe json.as[PatentRoyaltiesPayments]
      }
    }
  }

  "read from empty JSON" should {
    "convert empty MTD JSON into an empty PatentRoyaltiesPayments object" in {
      emptyPatentRoyaltiesPayments shouldBe noReferenceJson.as[PatentRoyaltiesPayments]
    }
  }

  "writes" when {
    "passed valid model" should {
      "return valid JSON" in {
        Json.toJson(patentRoyaltiesPayments) shouldBe json
      }
    }
  }

  "write from an empty body" when {
    "passed an empty model" should {
      "return an no reference JSON" in {
        Json.toJson(emptyPatentRoyaltiesPayments) shouldBe noReferenceJson
      }
    }
  }

}
