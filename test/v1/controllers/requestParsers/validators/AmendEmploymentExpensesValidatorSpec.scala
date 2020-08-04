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

import config.AppConfig
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.libs.json.Json
import support.UnitSpec
import utils.CurrentDateTime
import v1.mocks.MockCurrentDateTime
import v1.models.errors._
import v1.models.request.amendEmploymentExpenses.AmendEmploymentExpensesRawData

class AmendEmploymentExpensesValidatorSpec extends UnitSpec {

  private val validNino = "AA123456A"
  private val validTaxYear = "2019-20"
  private val requestBodyJson = Json.parse(
    """
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

  private val requestBodyJsonNoDecimals = Json.parse(
    """
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

  private val requestBodyJsonNoBusinessTravelCosts = Json.parse(
    """
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

  private val requestBodyJsonNoJobExpenses = Json.parse(
    """
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

  private val requestBodyJsonNoFlatRateJobExpenses = Json.parse(
    """
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

  private val requestBodyJsonNoProfessionalSubscriptions = Json.parse(
    """
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

  private val requestBodyJsonNoHotelAndMealExpenses = Json.parse(
    """
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

  private val requestBodyJsonNoOtherAndCapitalAllowances = Json.parse(
    """
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

  private val requestBodyJsonNoVehicleExpenses = Json.parse(
    """
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

  private val requestBodyJsonNoMileageAllowanceRelief = Json.parse(
    """
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

  private val requestBodyJsonEmptyExpensesObject = Json.parse(
    """
      |{
      |    "expenses": {}
      |}""".stripMargin)

  private val emptyJson = Json.parse(
    """
      |{}
      |""".stripMargin
  )

  class Test extends MockCurrentDateTime with MockAppConfig {

    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val appConfig: AppConfig = mockAppConfig

    val validator = new AmendEmploymentExpensesValidator()

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-07-11", dateTimeFormatter))
      .anyNumberOfTimes()

    MockedAppConfig.minimumPermittedTaxYear
      .returns(2019)
  }

  "running a validation" should {
    "return no errors" when {
      "a valid request is supplied" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJson)) shouldBe Nil
      }
      "a valid request is supplied without decimal places in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoDecimals)) shouldBe Nil
      }
      "a valid request is supplied without businessTravelCosts in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoBusinessTravelCosts)) shouldBe Nil
      }
      "a valid request is supplied without jobExpenses in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoJobExpenses)) shouldBe Nil
      }
      "a valid request is supplied without flatRateJobExpenses in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoFlatRateJobExpenses)) shouldBe Nil
      }
      "a valid request is supplied without professionalSubscriptions in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoProfessionalSubscriptions)) shouldBe Nil
      }
      "a valid request is supplied without hotelAndMealExpenses in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoHotelAndMealExpenses)) shouldBe Nil
      }
      "a valid request is supplied without otherAndCapitalAllowances in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoOtherAndCapitalAllowances)) shouldBe Nil
      }
      "a valid request is supplied without vehicleExpenses in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoVehicleExpenses)) shouldBe Nil
      }
      "a valid request is supplied without mileageAllowanceRelief in the JSON" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, requestBodyJsonNoMileageAllowanceRelief)) shouldBe Nil
      }
      "an empty expenses object is submitted" in new Test {
        validator.validate(AmendEmploymentExpensesRawData
        (validNino, validTaxYear, requestBodyJsonEmptyExpensesObject))shouldBe Nil
      }
    }

    "return a path parameter error" when {
      "the nino is invalid" in new Test {
        validator.validate(AmendEmploymentExpensesRawData("A12344A", validTaxYear, requestBodyJson)) shouldBe List(NinoFormatError)
      }
      "the taxYear format is invalid" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, "2000", requestBodyJson)) shouldBe List(TaxYearFormatError)
      }
      "the taxYear range is invalid" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, "2021-24", requestBodyJson)) shouldBe List(RuleTaxYearRangeInvalidError)
      }
      "the taxYear is below the minimum tax year" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, "2016-17", requestBodyJson)) shouldBe List(RuleTaxYearNotSupportedError)
      }
      "the taxYear has not yet ended" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, "2022-23", requestBodyJson)) shouldBe List(RuleTaxYearNotEndedError)
      }
      "all path parameters are invalid" in new Test {
        validator.validate(AmendEmploymentExpensesRawData("A12344A", "2000", requestBodyJson)) shouldBe List(NinoFormatError, TaxYearFormatError)
      }
    }
    "return RuleIncorrectOrEmptyBodyError error" when {
      "an empty JSON body is submitted" in new Test {
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, emptyJson)) shouldBe List(RuleIncorrectOrEmptyBodyError)
      }
    }
    "return a FORMAT_VALUE error" when {
      "a value field is below 0" in new Test {
        val badJson = Json.parse(
          """
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
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, badJson)) shouldBe List(
          ValueFormatError.copy(paths = Some(Seq("/expenses/businessTravelCosts")))
        )
      }
      "multiple fields are below 0" in new Test {
        val badjson = Json.parse(
          """
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
        validator.validate(AmendEmploymentExpensesRawData(validNino, validTaxYear, badjson)) shouldBe List(
          ValueFormatError.copy(paths = Some(Seq(
            "/expenses/businessTravelCosts",
            "/expenses/jobExpenses",
            "/expenses/flatRateJobExpenses",
            "/expenses/professionalSubscriptions",
            "/expenses/hotelAndMealExpenses",
            "/expenses/otherAndCapitalAllowances",
            "/expenses/vehicleExpenses",
            "/expenses/mileageAllowanceRelief"
          )))
        )
      }
    }
  }

}
