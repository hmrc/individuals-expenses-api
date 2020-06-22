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

package v1.controllers.requestParsers.validators

import support.UnitSpec
import v1.models.errors.{NinoFormatError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRawData

class RetrieveOtherExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"

  val validator = new RetrieveOtherExpensesValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(RetrieveOtherExpensesRawData(validNino, validTaxYear)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(RetrieveOtherExpensesRawData("A12344A", validTaxYear)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(RetrieveOtherExpensesRawData(validNino, "20178")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid error" when {
      "an out of range tax year is supplied" in {
        validator.validate(
          RetrieveOtherExpensesRawData(validNino, "2019-21")) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(RetrieveOtherExpensesRawData("A12344A", "20178")) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }
}
