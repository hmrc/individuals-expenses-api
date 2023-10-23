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

import api.models.errors.{NinoFormatError, RuleTaxYearNotSupportedError, RuleTaxYearRangeInvalidError, TaxYearFormatError}
import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRawData

class RetrieveOtherExpensesValidatorSpec extends UnitSpec {

  private val validNino    = "AA123456A"
  private val validTaxYear = "2021-22"
//  private val date         = DateTime.parse("2020-08-05")

  class Test extends MockAppConfig {

    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new RetrieveOtherExpensesValidator()

    MockAppConfig.otherExpensesMinimumTaxYear.returns(2022)

//    MockCurrentDateTime.getCurrentDate
//      .returns(DateTime.parse("2020-07-11", dateTimeFormatter))
//      .anyNumberOfTimes()
//
//    MockCurrentTaxYear
//      .getCurrentTaxYear(date)
//      .returns(2021)

  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(RetrieveOtherExpensesRawData(validNino, validTaxYear)) shouldBe Nil
      }
    }

    "return NinoFormatError error" when {
      "an invalid nino is supplied" in new Test {
        validator.validate(RetrieveOtherExpensesRawData("A12344A", validTaxYear)) shouldBe
          List(NinoFormatError)
      }
    }

    "return TaxYearFormatError error" when {
      "an invalid tax year is supplied" in new Test {
        validator.validate(RetrieveOtherExpensesRawData(validNino, "20178")) shouldBe
          List(TaxYearFormatError)
      }
    }

    "return RuleTaxYearRangeInvalid error" when {
      "an out of range tax year is supplied" in new Test {
        validator.validate(RetrieveOtherExpensesRawData(validNino, "2019-21")) shouldBe
          List(RuleTaxYearRangeInvalidError)
      }
    }

    "return RuleTaxYearNotSupportedError error" when {
      "a below minimum taxYear is provided" in new Test {
        validator.validate(RetrieveOtherExpensesRawData(validNino, "2018-19")) shouldBe
          List(RuleTaxYearNotSupportedError)
      }
    }

    "return multiple errors" when {
      "request supplied has multiple errors" in new Test {
        validator.validate(RetrieveOtherExpensesRawData("A12344A", "20178")) shouldBe
          List(NinoFormatError, TaxYearFormatError)
      }
    }
  }

}
