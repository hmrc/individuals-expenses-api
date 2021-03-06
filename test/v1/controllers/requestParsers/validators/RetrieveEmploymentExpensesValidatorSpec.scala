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

package v1.controllers.requestParsers.validators

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.mocks.{MockCurrentDateTime, MockCurrentTaxYear}
import v1.models.errors.{NinoFormatError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, SourceFormatError, TaxYearFormatError}
import v1.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRawData

class RetrieveEmploymentExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2021-22"
  private val date = DateTime.parse("2020-08-05")

  class Test extends MockCurrentDateTime with MockCurrentTaxYear with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig
    implicit val currentTaxYear: CurrentTaxYear = mockCurrentTaxYear

    val validator = new RetrieveEmploymentExpensesValidator()

    MockedAppConfig.employmentExpensesMinimumTaxYear.returns(2020)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-08-05", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear.getCurrentTaxYear(date)
      .returns(2021)
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied with the latest data" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "latest")) shouldBe Nil
      }
      "a valid request is supplied with HMRC held data" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "hmrcHeld")) shouldBe Nil
      }
      "a valid request is supplied with user data" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "user")) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData("A12344A", validTaxYear, "latest")) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, "20178", "latest")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return SourceFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, "Walrus")) shouldBe
          List(SourceFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(
          RetrieveEmploymentsExpensesRawData(validNino, "2019-21", "latest")) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "a taxYear below the minimum required is supplied" in new Test {
        validator.validate(
          RetrieveEmploymentsExpensesRawData(validNino, "2018-19", "latest")) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in new Test {
        validator.validate(RetrieveEmploymentsExpensesRawData("A12344A", "20178", "Walrus")) shouldBe
          List(NinoFormatError, TaxYearFormatError, SourceFormatError)
      }
    }
  }
}
