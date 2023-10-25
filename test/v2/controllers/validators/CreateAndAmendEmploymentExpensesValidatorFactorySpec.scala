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

package v2.controllers.validators

import api.models.domain.{Nino, TaxYear, TodaySupplier}
import api.models.errors._
import api.models.utils.JsonErrorValidators
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import support.UnitSpec
import v2.models.request.createAndAmendEmploymentExpenses.{
  CreateAndAmendEmploymentExpensesBody,
  CreateAndAmendEmploymentExpensesRequestData,
  Expenses
}

import java.time.LocalDate

class CreateAndAmendEmploymentExpensesValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2021-22"

  private def getNumber(withDecimals: Boolean)(base: Int) = if (withDecimals) base else base + 0.12

  private def validBody(withDecimals: Boolean = true): JsValue = {
    val number = getNumber(withDecimals)(_)

    Json.parse(s"""
                  |{
                  |    "expenses": {
                  |        "businessTravelCosts": ${number(0)},
                  |        "jobExpenses": ${number(1)},
                  |        "flatRateJobExpenses": ${number(2)},
                  |        "professionalSubscriptions": ${number(3)},
                  |        "hotelAndMealExpenses": ${number(4)},
                  |        "otherAndCapitalAllowances": ${number(5)},
                  |        "vehicleExpenses": ${number(6)},
                  |        "mileageAllowanceRelief": ${number(7)}
                  |    }
                  |}""".stripMargin)
  }

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private val parsedExpenses             = Expenses(Some(0), Some(1), Some(2), Some(3), Some(4), Some(5), Some(6), Some(7))
  private val parsedExpensesWithDecimals = Expenses(Some(0.12), Some(1.12), Some(2.12), Some(3.12), Some(4.12), Some(5.12), Some(6.12), Some(7.12))

  private def parsedBody(expenses: Expenses = parsedExpenses) = CreateAndAmendEmploymentExpensesBody(expenses)

  implicit val todaySupplier: TodaySupplier = new TodaySupplier {
    override def today(): LocalDate = LocalDate.parse("2022-07-11")
  }

  private val validatorFactory = new CreateAndAmendEmploymentExpensesValidatorFactory

  private def validator(nino: String, taxYear: String, body: JsValue, temporalValidationEnabled: Boolean = true) =
    validatorFactory.validator(nino, taxYear, body, temporalValidationEnabled)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request with decimals" in {
        val result = validator(validNino, validTaxYear, validBody()).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendEmploymentExpensesRequestData(parsedNino, parsedTaxYear, parsedBody())
        )
      }

      "passed a valid request without decimals" in {
        val result = validator(validNino, validTaxYear, validBody(withDecimals = false)).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendEmploymentExpensesRequestData(parsedNino, parsedTaxYear, parsedBody(parsedExpensesWithDecimals))
        )
      }

      List(
        ("/expenses/businessTravelCosts", parsedExpenses.copy(businessTravelCosts = None)),
        ("/expenses/jobExpenses", parsedExpenses.copy(jobExpenses = None)),
        ("/expenses/flatRateJobExpenses", parsedExpenses.copy(flatRateJobExpenses = None)),
        ("/expenses/professionalSubscriptions", parsedExpenses.copy(professionalSubscriptions = None)),
        ("/expenses/hotelAndMealExpenses", parsedExpenses.copy(hotelAndMealExpenses = None)),
        ("/expenses/otherAndCapitalAllowances", parsedExpenses.copy(otherAndCapitalAllowances = None)),
        ("/expenses/vehicleExpenses", parsedExpenses.copy(vehicleExpenses = None)),
        ("/expenses/mileageAllowanceRelief", parsedExpenses.copy(mileageAllowanceRelief = None))
      ).foreach { case (path, expenses) =>
        s"passed a valid request without $path" in {
          val result = validator(validNino, validTaxYear, validBody().removeProperty(path)).validateAndWrapResult()
          result shouldBe Right(CreateAndAmendEmploymentExpensesRequestData(parsedNino, parsedTaxYear, parsedBody(expenses)))
        }
      }

      "the taxYear has not ended but temporal validation is not enabled" in {
        val result = validator(validNino, "2023-24", validBody(), temporalValidationEnabled = false).validateAndWrapResult()
        result shouldBe Right(CreateAndAmendEmploymentExpensesRequestData(parsedNino, TaxYear.fromMtd("2023-24"), parsedBody()))
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result = validator("A12344A", validTaxYear, validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError))
      }

      "passed an invalid tax year" in {
        val result = validator(validNino, "202223", validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, TaxYearFormatError))
      }

      "passed a tax year with an invalid range" in {
        val result = validator(validNino, "2022-24", validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError))
      }

      "passed a tax year that precedes the minimum" in {
        val result = validator(validNino, "2018-19", validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))
      }

      "the taxYear has not ended and temporal validation is enabled" in {
        val result = validator(validNino, "2023-24", validBody()).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleTaxYearNotEndedError))
      }

      "passed an empty JSON body" in {
        val invalidBody = JsObject.empty
        val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      "passed a JSON body with an empty expenses object" in {
        val invalidBody = Json.obj("expenses" -> JsObject.empty)
        val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath("/expenses")))
      }

      "passed a body with a field below 0" when {
        val paths = List(
          "/expenses/businessTravelCosts",
          "/expenses/jobExpenses",
          "/expenses/flatRateJobExpenses",
          "/expenses/professionalSubscriptions",
          "/expenses/hotelAndMealExpenses",
          "/expenses/otherAndCapitalAllowances",
          "/expenses/vehicleExpenses",
          "/expenses/mileageAllowanceRelief"
        )

        paths.foreach(path =>
          s"for a single path $path" in {
            val invalidBody = validBody().update(path, JsNumber(-1))
            val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
            result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath(path)))
          })

        "for multiple paths" in {
          val invalidBody = paths.foldLeft(validBody())((body, path) => body.update(path, JsNumber(-1)))
          val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
          result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPaths(paths)))
        }
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in {
        val result = validator("not-a-nino", "not-a-tax-year", validBody()).validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(NinoFormatError, TaxYearFormatError))
          )
        )
      }
    }
  }

}
