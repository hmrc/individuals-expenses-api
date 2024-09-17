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

import common.error.CustomerReferenceFormatError
import play.api.libs.json._
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.utils.JsonErrorValidators
import shared.utils.UnitSpec
import v2.models.request.createAndAmendOtherExpenses.{
  CreateAndAmendOtherExpensesBody,
  CreateAndAmendOtherExpensesRequestData,
  PatentRoyaltiesPayments,
  PaymentsToTradeUnionsForDeathBenefits
}

class CreateAndAmendOtherExpensesValidatorFactorySpec extends UnitSpec with JsonErrorValidators {

  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2023-24"

  private def getNumber(withDecimals: Boolean)(base: Int) = if (withDecimals) base else base + 0.12

  private def validBody(withDecimals: Boolean = true): JsValue = {
    val number = getNumber(withDecimals)(_)

    Json.parse(s"""
        |{
        |  "paymentsToTradeUnionsForDeathBenefits": {
        |    "customerReference": "TRADE UNION PAYMENTS",
        |    "expenseAmount": ${number(0)}
        |  },
        |  "patentRoyaltiesPayments":{
        |    "customerReference": "ROYALTIES PAYMENTS",
        |    "expenseAmount": ${number(1)}
        |  }
        |}""".stripMargin)
  }

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private def parsedBody(withDecimals: Boolean = true) = {
    val number = getNumber(withDecimals)(_)

    CreateAndAmendOtherExpensesBody(
      Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), number(0))),
      Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), number(1)))
    )
  }

  private val validatorFactory = new CreateAndAmendOtherExpensesValidatorFactory

  private def validator(nino: String, taxYear: String, body: JsValue) = validatorFactory.validator(nino, taxYear, body)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request with decimals" in {
        val result = validator(validNino, validTaxYear, validBody()).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendOtherExpensesRequestData(parsedNino, parsedTaxYear, parsedBody())
        )
      }

      "passed a valid request without decimals" in {
        val result = validator(validNino, validTaxYear, validBody(withDecimals = false)).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendOtherExpensesRequestData(parsedNino, parsedTaxYear, parsedBody(withDecimals = false))
        )
      }

      "passed a valid request without paymentsToTradeUnionsForDeathBenefits" in {
        val result = validator(validNino, validTaxYear, validBody().removeProperty("/paymentsToTradeUnionsForDeathBenefits")).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendOtherExpensesRequestData(parsedNino, parsedTaxYear, parsedBody().copy(paymentsToTradeUnionsForDeathBenefits = None))
        )
      }

      "passed a valid request without patentRoyaltiesPayments" in {
        val result = validator(validNino, validTaxYear, validBody().removeProperty("/patentRoyaltiesPayments")).validateAndWrapResult()

        result shouldBe Right(
          CreateAndAmendOtherExpensesRequestData(parsedNino, parsedTaxYear, parsedBody().copy(patentRoyaltiesPayments = None))
        )
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result = validator("A12344A", validTaxYear, validBody()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }

      "passed an invalid tax year" in {
        val result = validator(validNino, "202223", validBody()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }

      "passed a tax year with an invalid range" in {
        val result = validator(validNino, "2022-24", validBody()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }

      "passed a tax year that precedes the minimum" in {
        val result = validator(validNino, "2018-19", validBody()).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }

      "passed an empty JSON body" in {
        val invalidBody = JsObject.empty
        val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError))
      }

      val expenseAmountPaths = List(
        "/paymentsToTradeUnionsForDeathBenefits/expenseAmount",
        "/patentRoyaltiesPayments/expenseAmount"
      )

      expenseAmountPaths.foreach(path =>
        s"passed a body missing a mandatory $path field" in {
          val invalidBody = validBody().replaceWithEmptyObject(path)
          val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
          result shouldBe Left(ErrorWrapper(correlationId, RuleIncorrectOrEmptyBodyError.withPath(path)))
        })

      expenseAmountPaths.foreach(path =>
        s"passed a body with negative values for $path" in {
          val invalidBody = validBody().update(path, JsNumber(-1))
          val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
          result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPath(path)))
        })

      "passed a body with numeric fields containing negative values" in {
        val invalidBody = expenseAmountPaths.foldLeft(validBody())((body, path) => body.update(path, JsNumber(-1)))
        val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, ValueFormatError.withPaths(expenseAmountPaths)))
      }

      val customerReferencePaths = List(
        "/paymentsToTradeUnionsForDeathBenefits/customerReference",
        "/patentRoyaltiesPayments/customerReference"
      )

      customerReferencePaths.foreach(path =>
        s"passed a body with an invalid customer reference for $path" in {
          val invalidBody = validBody().update(path, JsString("this!&ßisinvalid"))
          val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
          result shouldBe Left(ErrorWrapper(correlationId, CustomerReferenceFormatError.withPath(path)))
        })

      "passed a body with multiple customer reference fields containing invalid values" in {
        val invalidBody = customerReferencePaths.foldLeft(validBody())((body, path) => body.update(path, JsString("this!&ßisinvalid")))
        val result      = validator(validNino, validTaxYear, invalidBody).validateAndWrapResult()
        result shouldBe Left(ErrorWrapper(correlationId, CustomerReferenceFormatError.withPaths(customerReferencePaths)))
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
