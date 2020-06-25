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

class AmendOtherExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2018-19"
  private val requestBodyJson = Json.parse(
    """
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223.22
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}""".stripMargin)

  private val requestBodyJsonNoDecimals = Json.parse(
    """
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223
      |  }
      |}""".stripMargin)

  private val requestBodyJsonNoPaymentsToTradeUnionsForDeathBenefits = Json.parse(
    """
      |{
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}""".stripMargin)

  private val requestBodyJsonNoPatentRoyaltiesPayments = Json.parse(
    """
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}""".stripMargin)

  private val emptyJson = Json.parse(
    """
      |{}
      |""".stripMargin
  )

  val validator = new AmendOtherExpensesValidator


  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJson)) shouldBe Nil
      }
      "a valid request is supplied without decimal places in the JSON" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoDecimals)) shouldBe Nil
      }
      "a valid request is supplied without paymentsToTradeUnionsForDeathBenefits in the JSON" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoPaymentsToTradeUnionsForDeathBenefits)) shouldBe Nil
      }
      "a valid request is supplied without patentRoyaltiesPayments in the JSON" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoPatentRoyaltiesPayments)) shouldBe Nil
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
      "all path parameters are invalid" in {
        validator.validate(AmendOtherExpensesRawData("Walrus", "2000", requestBodyJson)) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, emptyJson)) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
      "at least one mandatory field is missing" in {
        val json =  Json.parse(
          """
            |{
            |  "paymentsToTradeUnionsForDeathBenefits": {},
            |  "patentRoyaltiesPayments":{
            |    "customerReference": "ROYALTIES PAYMENTS",
            |    "expenseAmount": 1223
            |  }
            |}""".stripMargin)
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, json)) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }
    "return a FORMAT_VALUE error" when {
      "expenseAmount is below 0" in {
        val badJson = Json.parse(
          """
            |{
            |  "paymentsToTradeUnionsForDeathBenefits": {
            |    "customerReference": "TRADE UNION PAYMENTS",
            |    "expenseAmount": 1223
            |  },
            |  "patentRoyaltiesPayments":{
            |    "customerReference": "ROYALTIES PAYMENTS",
            |    "expenseAmount": -1223
            |  }
            |}""".stripMargin)
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, badJson)) shouldBe List(
          ValueFormatError.copy(paths = Some(Seq("/patentRoyaltiesPayments/expenseAmount")))
        )
      }
      "multiple errors is below 0" in {
        val badJson = Json.parse(
          """
            |{
            |  "paymentsToTradeUnionsForDeathBenefits": {
            |    "customerReference": "TRADE UNION PAYMENTS",
            |    "expenseAmount": -1223
            |  },
            |  "patentRoyaltiesPayments":{
            |    "customerReference": "ROYALTIES PAYMENTS",
            |    "expenseAmount": -1223
            |  }
            |}""".stripMargin)
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, badJson)) shouldBe List(
          ValueFormatError.copy(paths = Some(Seq("/paymentsToTradeUnionsForDeathBenefits/expenseAmount",
            "/patentRoyaltiesPayments/expenseAmount")))
        )
      }
    }

  }
}