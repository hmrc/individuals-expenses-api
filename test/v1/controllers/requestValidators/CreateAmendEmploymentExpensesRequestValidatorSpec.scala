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
import api.models.errors._
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.controllers.requestValidators.CreateAmendEmploymentExpensesRequestValidatorSpec._
import v1.models.request.createAndAmendEmploymentExpenses._

class CreateAmendEmploymentExpensesRequestValidatorSpec extends UnitSpec {

  private val validNino                      = "AA123456A"
  private val validTaxYear                   = "2020-21"
  private val date                           = DateTime.parse("2020-08-05")
  implicit private val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val rawData = CreateAndAmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJson)

  "parseRequest" should {
    "return a request object" when {
      "valid request data is supplied" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), requestBody)

        requestValidator.parseRequest(rawData) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without decimal places in the JSON" in new Test {
        private val parsedRequest =
          CreateAndAmendEmploymentExpensesRequest(
            Nino(validNino),
            TaxYear.fromMtd(validTaxYear),
            CreateAndAmendEmploymentExpensesBody(expensesNoDecimals))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoDecimals)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without businessTravelCosts in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(businessTravelCosts = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoBusinessTravelCosts)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without jobExpenses in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(jobExpenses = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoJobExpenses)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without flatRateJobExpenses in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(flatRateJobExpenses = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoFlatRateJobExpenses)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without professionalSubscriptions in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(professionalSubscriptions = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoProfessionalSubscriptions)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without hotelAndMealExpenses in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(hotelAndMealExpenses = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoHotelAndMealExpenses)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without otherAndCapitalAllowances in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(otherAndCapitalAllowances = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoOtherAndCapitalAllowances)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without vehicleExpenses in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(vehicleExpenses = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoVehicleExpenses)) shouldBe Right(parsedRequest)
      }

      "a valid request is supplied without mileageAllowanceRelief in the JSON" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(
          Nino(validNino),
          TaxYear.fromMtd(validTaxYear),
          CreateAndAmendEmploymentExpensesBody(expensesNoDecimals.copy(mileageAllowanceRelief = None)))

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonNoMileageAllowanceRelief)) shouldBe Right(parsedRequest)
      }

      "a valid request data with a tax year that has not ended is supplied but temporal validation is disabled" in new Test {
        private val parsedRequest = CreateAndAmendEmploymentExpensesRequest(Nino(validNino), TaxYear.fromMtd("2022-23"), requestBody)

        requestValidator.parseRequest(rawData.copy(taxYear = "2022-23", temporalValidationEnabled = false)) shouldBe Right(parsedRequest)
      }

    }

    "return a request parameter error in an ErrorWrapper" when {
      "an invalid nino is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, NinoFormatError, None)

        requestValidator.parseRequest(rawData.copy(nino = "invalid nino")) shouldBe Left(expectedOutcome)
      }

      "an invalid tax year format is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, TaxYearFormatError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }

      "an invalid tax year range is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "2020-22")) shouldBe Left(expectedOutcome)
      }

      "a tax year below the minimum is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearNotSupportedError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "2012-13")) shouldBe Left(expectedOutcome)
      }

      "a tax year that has yet to end is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearNotEndedError, None)

        requestValidator.parseRequest(rawData.copy(taxYear = "2022-23")) shouldBe Left(expectedOutcome)
      }

      "multiple invalid parameters are supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError)))

        requestValidator.parseRequest(rawData.copy(nino = "invalid nino", taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }
    }

    "return a request body error in an ErrorWrapper" when {
      "an empty JSON body is submitted" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError, None)

        requestValidator.parseRequest(rawData.copy(body = emptyJson)) shouldBe Left(expectedOutcome)
      }

      "an empty expenses object is submitted" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError, None)

        requestValidator.parseRequest(rawData.copy(body = requestBodyJsonEmptyExpensesObject)) shouldBe Left(expectedOutcome)
      }

      "a value field is below 0" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, ValueFormatError.copy(paths = Some(Seq("/expenses/businessTravelCosts"))), None)

        requestValidator.parseRequest(rawData.copy(body = singleNegativeExpenseAmountJson)) shouldBe Left(expectedOutcome)
      }

      "multiple fields are below 0" in new Test {
        private val expectedOutcome = ErrorWrapper(
          correlationId,
          ValueFormatError.copy(paths = Some(Seq(
            "/expenses/businessTravelCosts",
            "/expenses/jobExpenses",
            "/expenses/flatRateJobExpenses",
            "/expenses/professionalSubscriptions",
            "/expenses/hotelAndMealExpenses",
            "/expenses/otherAndCapitalAllowances",
            "/expenses/vehicleExpenses",
            "/expenses/mileageAllowanceRelief"
          ))),
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

    val requestValidator = new CreateAmendEmploymentExpensesRequestValidator(mockAppConfig)

    MockAppConfig.employmentExpensesMinimumTaxYear
      .returns(2020)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-08-05", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear
      .getCurrentTaxYear(date)
      .returns(2023)

  }

}

object CreateAmendEmploymentExpensesRequestValidatorSpec {

  private val requestBodyJson: JsValue = Json.parse("""
       |{
       |    "expenses": {
       |        "businessTravelCosts": 123.12,
       |        "jobExpenses": 123.12,
       |        "flatRateJobExpenses": 123.12,
       |        "professionalSubscriptions": 123.12,
       |        "hotelAndMealExpenses": 123.12,
       |        "otherAndCapitalAllowances": 123.12,
       |        "vehicleExpenses": 123.12,
       |        "mileageAllowanceRelief": 123.12
       |    }
       |}""".stripMargin)

  private val requestBodyJsonNoDecimals: JsValue = Json.parse("""
     |{
     |    "expenses": {
     |        "businessTravelCosts": 123,
     |        "jobExpenses": 123,
     |        "flatRateJobExpenses": 123,
     |        "professionalSubscriptions": 123,
     |        "hotelAndMealExpenses": 123,
     |        "otherAndCapitalAllowances": 123,
     |        "vehicleExpenses": 123,
     |        "mileageAllowanceRelief": 123
     |    }
     |}""".stripMargin)

  private val requestBodyJsonNoBusinessTravelCosts: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "jobExpenses": 123,
    |        "flatRateJobExpenses": 123,
    |        "professionalSubscriptions": 123,
    |        "hotelAndMealExpenses": 123,
    |        "otherAndCapitalAllowances": 123,
    |        "vehicleExpenses": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoJobExpenses: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": 123,
    |        "flatRateJobExpenses": 123,
    |        "professionalSubscriptions": 123,
    |        "hotelAndMealExpenses": 123,
    |        "otherAndCapitalAllowances": 123,
    |        "vehicleExpenses": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoFlatRateJobExpenses: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": 123,
    |        "jobExpenses": 123,
    |        "professionalSubscriptions": 123,
    |        "hotelAndMealExpenses": 123,
    |        "otherAndCapitalAllowances": 123,
    |        "vehicleExpenses": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoProfessionalSubscriptions: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": 123,
    |        "jobExpenses": 123,
    |        "flatRateJobExpenses": 123,
    |        "hotelAndMealExpenses": 123,
    |        "otherAndCapitalAllowances": 123,
    |        "vehicleExpenses": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoHotelAndMealExpenses: JsValue = Json.parse("""
   |{
   |    "expenses": {
   |        "businessTravelCosts": 123,
   |        "jobExpenses": 123,
   |        "flatRateJobExpenses": 123,
   |        "professionalSubscriptions": 123,
   |        "otherAndCapitalAllowances": 123,
   |        "vehicleExpenses": 123,
   |        "mileageAllowanceRelief": 123
   |    }
   |}""".stripMargin)

  private val requestBodyJsonNoOtherAndCapitalAllowances: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": 123,
    |        "jobExpenses": 123,
    |        "flatRateJobExpenses": 123,
    |        "professionalSubscriptions": 123,
    |        "hotelAndMealExpenses": 123,
    |        "vehicleExpenses": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoVehicleExpenses: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": 123,
    |        "jobExpenses": 123,
    |        "flatRateJobExpenses": 123,
    |        "professionalSubscriptions": 123,
    |        "hotelAndMealExpenses": 123,
    |        "otherAndCapitalAllowances": 123,
    |        "mileageAllowanceRelief": 123
    |    }
    |}""".stripMargin)

  private val requestBodyJsonNoMileageAllowanceRelief: JsValue = Json.parse("""
     |{
     |    "expenses": {
     |        "businessTravelCosts": 123,
     |        "jobExpenses": 123,
     |        "flatRateJobExpenses": 123,
     |        "professionalSubscriptions": 123,
     |        "hotelAndMealExpenses": 123,
     |        "otherAndCapitalAllowances": 123,
     |        "vehicleExpenses": 123
     |    }
     |}""".stripMargin)

  private val emptyJson = Json.parse(
    """
      |{}
      |""".stripMargin
  )

  private val requestBodyJsonEmptyExpensesObject: JsValue = Json.parse("""
    |{
    |    "expenses": {}
    |}""".stripMargin)

  private val singleNegativeExpenseAmountJson: JsValue = Json.parse("""
    |{
    |    "expenses": {
    |        "businessTravelCosts": -123.12,
    |        "jobExpenses": 123.12,
    |        "flatRateJobExpenses": 123.12,
    |        "professionalSubscriptions": 123.12,
    |        "hotelAndMealExpenses": 123.12,
    |        "otherAndCapitalAllowances": 123.12,
    |        "vehicleExpenses": 123.12,
    |        "mileageAllowanceRelief": 123.12
    |    }
    |}""".stripMargin)

  private val multipleNegativeExpenseAmountsJson: JsValue = Json.parse("""
     |{
     |    "expenses": {
     |        "businessTravelCosts": -123.12,
     |        "jobExpenses": -123.12,
     |        "flatRateJobExpenses": -123.12,
     |        "professionalSubscriptions": -123.12,
     |        "hotelAndMealExpenses": -123.12,
     |        "otherAndCapitalAllowances": -123.12,
     |        "vehicleExpenses": -123.12,
     |        "mileageAllowanceRelief": -123.12
     |    }
     |}""".stripMargin)

  val expenses: Expenses = Expenses(Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12))

  val expensesNoDecimals: Expenses = Expenses(Some(123), Some(123), Some(123), Some(123), Some(123), Some(123), Some(123), Some(123))

  val requestBody: CreateAndAmendEmploymentExpensesBody = CreateAndAmendEmploymentExpensesBody(expenses)

}
