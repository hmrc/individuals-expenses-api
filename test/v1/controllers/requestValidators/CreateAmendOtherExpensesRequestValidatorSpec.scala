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

package v1.controllers.requestValidators

import api.mocks.{MockCurrentDateTime, MockCurrentTaxYear}
import api.models.domain.{Nino, TaxYear}
import play.api.libs.json.JsValue
import v1.controllers.requestValidators.CreateAmendOtherExpensesRequestValidatorSpec._
import v1.models.request.createAndAmendOtherExpenses._
import api.models.errors._
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.Json
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.request.createAndAmendOtherExpenses.CreateAndAmendOtherExpensesRawData

class CreateAmendOtherExpensesRequestValidatorSpec extends UnitSpec {

  private val validNino                      = "AA123456A"
  private val validTaxYear                   = "2020-21"
  private val date                           = DateTime.parse("2020-08-05")
  implicit private val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val rawData = CreateAndAmendOtherExpensesRawData(validNino, validTaxYear, requestBodyJson)

  "parseRequest" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        private val parsedRequest = CreateAndAmendOtherExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), requestBody)

        requestValidator.parseRequest(rawData) shouldBe Right(parsedRequest)
      }

      "valid request data is supplied without decimal places in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendOtherExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), requestBodyNoDecimals)

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoDecimals)) shouldBe Right(parsedRequest)
      }

      "valid request data is supplied without paymentsToTradeUnionsForDeathBenefits in the JSON" in new Test {
        private val parsedRequest =
          CreateAndAmendOtherExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), requestBodyNoPaymentsToTradeUnionsForDeathBenefits)

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoPaymentsToTradeUnionsForDeathBenefits)) shouldBe Right(parsedRequest)
      }

      "valid request data is supplied without patentRoyaltiesPayments in the JSON" in new Test {
        private val parsedRequest =
          CreateAndAmendOtherExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), requestBodyNoPatentRoyaltiesPayments)

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoPatentRoyaltiesPayments)) shouldBe Right(parsedRequest)
      }
    }

    "return a request parameter error in an ErrorWrapper" when {
      "an invalid nino is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, NinoFormatError, None)

        requestValidator.parseRequest(rawData.copy(nino = "invalid nino")) shouldBe Left(expectedOutcome)
      }

      "an invalid tax year is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, TaxYearFormatError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }

      "a tax year with an invalid range is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "2020-22")) shouldBe Left(expectedOutcome)
      }

      "a taxYear below the minimum is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearNotSupportedError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "2018-19")) shouldBe Left(expectedOutcome)
      }

      "request supplied has multiple errors" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError)))

        requestValidator.parseRequest(rawData.copy(nino = "invalid nino", taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }
    }

    "return a request body error in an ErrorWrapper" when {
      "an empty JSON body is submitted" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError, None)

        requestValidator.parseRequest(rawData.copy(body = emptyJson)) shouldBe Left(expectedOutcome)
      }

      "a JSON body with a mandatory field missing is submitted" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError, None)

        requestValidator.parseRequest(rawData.copy(body = emptyRequiredFieldJson)) shouldBe Left(expectedOutcome)
      }

      "an expenseAmount below 0 is submitted" in new Test {
        private val expectedOutcome =
          ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(Seq("/patentRoyaltiesPayments/expenseAmount"))), None)

        requestValidator.parseRequest(rawData.copy(body = singleNegativeExpenseAmountJson)) shouldBe Left(expectedOutcome)
      }

      "both expenseAmounts below 0 are submitted" in new Test {
        private val expectedOutcome = ErrorWrapper(
          correlationId,
          ValueFormatError.copy(paths = Some(Seq("/paymentsToTradeUnionsForDeathBenefits/expenseAmount", "/patentRoyaltiesPayments/expenseAmount"))),
          None
        )

        requestValidator.parseRequest(rawData.copy(body = multipleNegativeExpenseAmountsJson)) shouldBe Left(expectedOutcome)
      }

    }
  }

  private trait Test extends MockCurrentDateTime with MockCurrentTaxYear with MockAppConfig {
    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter       = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val currentTaxYear: CurrentTaxYear = mockCurrentTaxYear

    val requestValidator = new CreateAmendOtherExpensesRequestValidator(mockAppConfig)

    MockAppConfig.otherExpensesMinimumTaxYear
      .returns(2020)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-08-05", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear
      .getCurrentTaxYear(date)
      .returns(2023)

  }

}

object CreateAmendOtherExpensesRequestValidatorSpec {

  private val requestBodyJson: JsValue = Json.parse("""
     |{
     |  "paymentsToTradeUnionsForDeathBenefits": {
     |    "customerReference": "TRADE UNION PAYMENTS",
     |    "expenseAmount": 1223.22
     |  },
     |  "patentRoyaltiesPayments":{
     |    "customerReference": "ROYALTIES PAYMENTS",
     |    "expenseAmount": 1223.22
     |  }
     |}
     |""".stripMargin)

  private val requestBodyJsonNoDecimals: JsValue = Json.parse("""
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

  private val requestBodyJsonNoPaymentsToTradeUnionsForDeathBenefits: JsValue = Json.parse("""
      |{
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}""".stripMargin)

  private val requestBodyJsonNoPatentRoyaltiesPayments: JsValue = Json.parse("""
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}""".stripMargin)

  private val emptyJson: JsValue = Json.parse("""
      |{}
      |""".stripMargin)

  private val emptyRequiredFieldJson: JsValue = Json.parse("""
      |{
      |  "paymentsToTradeUnionsForDeathBenefits": {},
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223
      |  }
      |}""".stripMargin)

  private val singleNegativeExpenseAmountJson: JsValue = Json.parse("""
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

  private val multipleNegativeExpenseAmountsJson: JsValue = Json.parse("""
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

  private val requestBody: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 1223.22)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 1223.22))
  )

  private val requestBodyNoDecimals: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 1223)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 1223))
  )

  private val requestBodyNoPaymentsToTradeUnionsForDeathBenefits: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    None,
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 1223.22))
  )

  private val requestBodyNoPatentRoyaltiesPayments: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 1223.22)),
    None
  )

}
