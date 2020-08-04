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

import play.api.libs.json.Json
import support.UnitSpec
import v1.models.errors._
import v1.models.request.amendOtherExpenses.AmendOtherExpensesRawData

class IgnoreEmploymentExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2019-20"
  private val requestBodyJson = Json.parse(
    """
      |{
      |  "ignoreExpenses": "true"
      |}
      |""".stripMargin)

  private val emptyJson = Json.parse(
    """{}""".stripMargin
  )

  val validator = new AmendOtherExpensesValidator


  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJson)) shouldBe Nil
      }
    }

    "return a path parameter error" when {
      "the nino is invalid" in {
        validator.validate(AmendOtherExpensesRawData("Walrus", validTaxYear, requestBodyJson)) shouldBe List(NinoFormatError)
      }
      "the taxYear format is invalid" in {
        validator.validate(AmendOtherExpensesRawData(validNino, "2000", requestBodyJson)) shouldBe List(TaxYearFormatError)
      }
      "the taxYear range is invalid" in {
        validator.validate(AmendOtherExpensesRawData(validNino, "2017-20", requestBodyJson)) shouldBe List(RuleTaxYearRangeInvalidError)
      }
      "the taxYear is too early" in {
        validator.validate(AmendOtherExpensesRawData(validNino, "2017-20", requestBodyJson)) shouldBe List(RuleTaxYearNotSupportedError)
      }
      "the taxYear has not ended" in {
        validator.validate(AmendOtherExpensesRawData(validNino, "2023-24", requestBodyJson)) shouldBe List(RuleTaxYearNotEndedError)
      }
      "all path parameters are invalid" in {
        validator.validate(AmendOtherExpensesRawData("Walrus", "2000", requestBodyJson)) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, emptyJson)) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }
  }
}