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
import play.api.libs.json.Json
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.mocks.{MockCurrentDateTime, MockCurrentTaxYear}
import v1.models.errors._
import v1.models.request.amendOtherExpenses.AmendOtherExpensesRawData

class AmendOtherExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2021-22"
  private val date = DateTime.parse("2020-08-05")
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


  class Test extends MockCurrentDateTime with MockCurrentTaxYear with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig
    implicit val currentTaxYear: CurrentTaxYear = mockCurrentTaxYear

    val validator = new AmendOtherExpensesValidator()

    MockedAppConfig.otherExpensesMinimumTaxYear.returns(2022)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear.getCurrentTaxYear(date)
      .returns(2021)
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJson)) shouldBe Nil
      }
      "a valid request is supplied without decimal places in the JSON" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoDecimals)) shouldBe Nil
      }
      "a valid request is supplied without paymentsToTradeUnionsForDeathBenefits in the JSON" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoPaymentsToTradeUnionsForDeathBenefits)) shouldBe Nil
      }
      "a valid request is supplied without patentRoyaltiesPayments in the JSON" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJsonNoPatentRoyaltiesPayments)) shouldBe Nil
      }
    }

    "return a path parameter error" when {
      "the nino is invalid" in new Test {
        validator.validate(AmendOtherExpensesRawData("Walrus", validTaxYear, requestBodyJson)) shouldBe List(NinoFormatError)
      }
      "the taxYear format is invalid" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, "2000", requestBodyJson)) shouldBe List(TaxYearFormatError)
      }
      "the taxYear range is invalid" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, "2017-20", requestBodyJson)) shouldBe List(RuleTaxYearRangeInvalidError)
      }
      "the taxYear is below the minimum" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, "2018-19", requestBodyJson)) shouldBe List(RuleTaxYearNotSupportedError)
      }
      "all path parameters are invalid" in new Test {
        validator.validate(AmendOtherExpensesRawData("Walrus", "2000", requestBodyJson)) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }

    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in new Test {
        validator.validate(AmendOtherExpensesRawData(validNino, validTaxYear, emptyJson)) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
      "at least one mandatory field is missing" in new Test {
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
      "expenseAmount is below 0" in new Test {
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
      "multiple errors is below 0" in new Test {
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