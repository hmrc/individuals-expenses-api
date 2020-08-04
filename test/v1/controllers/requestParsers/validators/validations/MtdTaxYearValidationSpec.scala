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

package v1.controllers.requestParsers.validators.validations

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.scalamock.handlers.CallHandler
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors.{RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError}
import v1.models.utils.JsonErrorValidators

class MtdTaxYearValidationSpec extends UnitSpec with JsonErrorValidators {

  class Test extends MockCurrentDateTime with MockAppConfig {
    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    def setupTimeProvider(date: String): CallHandler[DateTime] =
      MockCurrentDateTime.getCurrentDate
        .returns(DateTime.parse(date, dateTimeFormatter))

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2019)
  }

  "validate" should {
    "return no errors" when {
      "the minimum allowed tax year is supplied" in new Test {

        setupTimeProvider("2019-04-06")

        val validTaxYear = "2019-20"
        val validationResult = MtdTaxYearValidation.validate(validTaxYear)
        validationResult.isEmpty shouldBe true
      }
      "the supplied tax year has not yet ended, with checkCurrentTaxYear set to false" in new Test {

        setupTimeProvider("2019-04-06")

        private val invalidTaxYear = "2019-20"
        private val validationResult = MtdTaxYearValidation.validate(invalidTaxYear, false)

        validationResult.isEmpty shouldBe true
      }
    }

    "return the given error" when {
      "a tax year below 2021-22 is supplied" in new Test {

        setupTimeProvider("2019-04-06")

        val invalidTaxYear = "2015-16"
        val validationResult = MtdTaxYearValidation.validate(invalidTaxYear)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe RuleTaxYearNotSupportedError
      }
      "the supplied tax year has not yet ended, with checkCurrentTaxYear set to true" in new Test {

        setupTimeProvider("2019-04-06")

        private val invalidTaxYear = "2019-20"
        private val validationResult = MtdTaxYearValidation.validate(invalidTaxYear, true)

        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe RuleTaxYearNotEndedError
      }
    }
  }
}
