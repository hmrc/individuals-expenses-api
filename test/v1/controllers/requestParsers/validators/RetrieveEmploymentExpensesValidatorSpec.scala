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
import v1.models.errors.{NinoFormatError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, SourceFormatError, TaxYearFormatError}
import v1.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRawData

class RetrieveEmploymentExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2021-22"

  val validator = new RetrieveEmploymentExpensesValidator()

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied with the latest data" in {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "latest")) shouldBe Nil
      }
      "a valid request is supplied with HMRC held data" in {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "hmrcHeld")) shouldBe Nil
      }
      "a valid request is supplied with user data" in {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "user")) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in {
        validator.validate(RetrieveEmploymentsExpensesRawData("A12344A", validTaxYear, "latest")) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, "20178", "latest")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return SourceFormatError error" when {
      "an invalid tax year is supplied" in {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "Walrus")) shouldBe
          List(SourceFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid error" when {
      "an out of range tax year is supplied" in {
        validator.validate(
          RetrieveEmploymentsExpensesRawData(validNino, "2019-21", "latest")) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "an out of range tax year is supplied" in {
        validator.validate(
          RetrieveEmploymentsExpensesRawData(validNino, "2018-19", "latest")) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in {
        validator.validate(RetrieveEmploymentsExpensesRawData("A12344A", "20178", "Walrus")) shouldBe
          List(NinoFormatError, TaxYearFormatError, SourceFormatError)
      }
    }
  }
}
