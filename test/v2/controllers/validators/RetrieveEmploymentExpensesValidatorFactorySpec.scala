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

import api.models.domain.{MtdSource, Nino, TaxYear}
import api.models.errors.{
  BadRequestError,
  ErrorWrapper,
  NinoFormatError,
  RuleTaxYearNotSupportedError,
  RuleTaxYearRangeInvalidError,
  SourceFormatError,
  TaxYearFormatError
}
import support.UnitSpec
import v2.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequestData

class RetrieveEmploymentExpensesValidatorFactorySpec extends UnitSpec {

  private implicit val correlationId: String = "1234"

  private val validNino    = "AA123456A"
  private val validTaxYear = "2023-24"
  private val validSource  = "latest"

  private val parsedNino    = Nino(validNino)
  private val parsedTaxYear = TaxYear.fromMtd(validTaxYear)
  private val parsedSource  = MtdSource.`latest`

  private val validatorFactory = new RetrieveEmploymentExpensesValidatorFactory

  private def validator(nino: String, taxYear: String, source: String) = validatorFactory.validator(nino, taxYear, source)

  "validator" should {
    "return the parsed domain object" when {
      "passed a valid request" in {
        val result = validator(validNino, validTaxYear, validSource).validateAndWrapResult()

        result shouldBe Right(
          RetrieveEmploymentsExpensesRequestData(parsedNino, parsedTaxYear, parsedSource)
        )
      }
    }

    "return a single error" when {
      "passed an invalid nino" in {
        val result = validator("A12344A", validTaxYear, validSource).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, NinoFormatError)
        )
      }

      "passed an invalid tax year" in {
        val result = validator(validNino, "202223", validSource).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, TaxYearFormatError)
        )
      }

      "passed a tax year with an invalid range" in {
        val result = validator(validNino, "2022-24", validSource).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError)
        )
      }

      "passed a tax year that precedes the minimum" in {
        val result = validator(validNino, "2018-19", validSource).validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, RuleTaxYearNotSupportedError)
        )
      }

      "passed an invalid source" in {
        val result = validator(validNino, validTaxYear, "invalid").validateAndWrapResult()
        result shouldBe Left(
          ErrorWrapper(correlationId, SourceFormatError)
        )
      }
    }

    "return multiple errors" when {
      "passed multiple invalid fields" in {
        val result = validator("not-a-nino", "not-a-tax-year", "not-a-source").validateAndWrapResult()

        result shouldBe Left(
          ErrorWrapper(
            correlationId,
            BadRequestError,
            Some(List(NinoFormatError, SourceFormatError, TaxYearFormatError))
          )
        )
      }
    }
  }

}
