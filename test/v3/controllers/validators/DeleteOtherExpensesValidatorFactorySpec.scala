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

package v3.controllers.validators

import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.utils.UnitSpec
import v3.models.request.deleteOtherExpenses.DeleteOtherExpensesRequestData

class DeleteOtherExpensesValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2023-24"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)

  private val validatorFactory = new DeleteOtherExpensesValidatorFactory

  private def validator(nino: String, taxYear: String) = validatorFactory.validator(nino, taxYear)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in {
        val result = validator(validNino, validTaxYear).validateAndWrapResult()

        result shouldBe Right(
          DeleteOtherExpensesRequestData(parsedNino, parsedTaxYear)
        )
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result = validator("A12344A", validTaxYear).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }

      "passed an invalid tax year" in {
        val result = validator(validNino, "202223").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }

      "passed a tax year with an invalid range" in {
        val result = validator(validNino, "2022-24").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }

      "passed a tax year that precedes the minimum" in {
        val result = validator(validNino, "2018-19").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in {
        val result = validator("not-a-nino", "not-a-tax-year").validateAndWrapResult()

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
