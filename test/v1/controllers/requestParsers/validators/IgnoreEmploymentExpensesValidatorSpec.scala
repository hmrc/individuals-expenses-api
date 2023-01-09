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

package v1.controllers.requestParsers.validators

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.mocks.{MockCurrentDateTime, MockCurrentTaxYear}
import v1.models.errors._
import v1.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRawData

class IgnoreEmploymentExpensesValidatorSpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2019-20"
  private val date         = DateTime.parse("2020-08-05")

  class Test extends MockCurrentDateTime with MockCurrentTaxYear with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter       = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig           = mockAppConfig
    implicit val currentTaxYear: CurrentTaxYear = mockCurrentTaxYear

    val validator = new IgnoreEmploymentExpensesValidator()

    MockAppConfig.employmentExpensesMinimumTaxYear.returns(2020)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-08-05", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear
      .getCurrentTaxYear(date)
      .returns(2021)

  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, validTaxYear)) shouldBe Nil
      }
    }

    "return a path parameter error" when {
      "the nino is invalid" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData("Walrus", validTaxYear)) shouldBe List(NinoFormatError)
      }
      "the taxYear format is invalid" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, "2000")) shouldBe List(TaxYearFormatError)
      }
      "the taxYear range is invalid" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, "2017-20")) shouldBe List(RuleTaxYearRangeInvalidError)
      }
      "the taxYear is too early" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, "2017-18")) shouldBe List(RuleTaxYearNotSupportedError)
      }
      "the taxYear has not ended" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, "2023-24")) shouldBe List(RuleTaxYearNotEndedError)
      }
      "the taxYear has not ended but temporal validation is disabled" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData(validNino, "2023-24", temporalValidationEnabled = false)) shouldBe Nil
      }
      "all path parameters are invalid" in new Test {
        validator.validate(IgnoreEmploymentExpensesRawData("Walrus", "2000")) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }
  }

}
