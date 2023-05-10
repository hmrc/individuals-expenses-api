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
import api.models.domain.{MtdSource, Nino, TaxYear}
import api.models.errors._
import mocks.MockAppConfig
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import support.UnitSpec
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}

class RetrieveEmploymentsExpensesRequestValidatorSpec extends UnitSpec {

  private val validNino              = "AA123456A"
  private val validTaxYear           = "2021-22"
  private val date                   = DateTime.parse("2020-08-05")
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val rawData = source => RetrieveEmploymentsExpensesRawData(validNino, validTaxYear, source)

  "parseRequest" should {
    "return a request object" when {
      "valid request data is supplied with the latest query parameter" in new Test {
        private val parsedRequest = RetrieveEmploymentsExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), MtdSource.`latest`)

        requestValidator.parseRequest(rawData("latest")) shouldBe Right(parsedRequest)
      }

      "valid request data is supplied with the HMRC Held query parameter" in new Test {
        private val parsedRequest = RetrieveEmploymentsExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), MtdSource.`hmrcHeld`)

        requestValidator.parseRequest(rawData("hmrcHeld")) shouldBe Right(parsedRequest)
      }

      "valid request data is supplied with the user query parameter" in new Test {
        private val parsedRequest = RetrieveEmploymentsExpensesRequest(Nino(validNino), TaxYear.fromMtd(validTaxYear), MtdSource.`user`)

        requestValidator.parseRequest(rawData("user")) shouldBe Right(parsedRequest)
      }
    }

    "return a NinoFormatError in an ErrorWrapper" when {
      "an invalid nino is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, NinoFormatError, None)

        requestValidator.parseRequest(rawData("hmrcHeld").copy(nino = "invalid nino")) shouldBe Left(expectedOutcome)
      }
    }

    "return a TaxYearFormatError in an ErrorWrapper" when {
      "an invalid tax year is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, TaxYearFormatError, None)

        requestValidator.parseRequest(rawData("hmrcHeld").copy(taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }
    }

    "return a SourceFormatError in an ErrorWrapper" when {
      "an invalid source is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, SourceFormatError, None)

        requestValidator.parseRequest(rawData("invalid source")) shouldBe Left(expectedOutcome)
      }
    }

    "return a RuleTaxYearRangeInvalidError in an ErrorWrapper" when {
      "an invalid tax year range is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearRangeInvalidError, None)

        requestValidator.parseRequest(rawData("hmrcHeld").copy(taxYear = "2019-21")) shouldBe Left(expectedOutcome)
      }
    }

    "return a RuleTaxYearNotSupportedError in an ErrorWrapper" when {
      "a taxYear below the minimum required is supplied" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, RuleTaxYearNotSupportedError, None)

        requestValidator.parseRequest(rawData("hmrcHeld").copy(taxYear = "2018-19")) shouldBe Left(expectedOutcome)
      }
    }

    "return multiple errors in an ErrorWrapper" when {
      "request supplied has multiple errors" in new Test {
        private val expectedOutcome = ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError, SourceFormatError)))

        requestValidator
          .parseRequest(
            rawData("invalid source")
              .copy(nino = "invalid nino", taxYear = "invalid tax year")) shouldBe Left(expectedOutcome)
      }
    }
  }

  private trait Test extends MockCurrentDateTime with MockCurrentTaxYear with MockAppConfig {
    implicit val dateTimeProvider: CurrentDateTime = mockCurrentDateTime
    val dateTimeFormatter: DateTimeFormatter       = DateTimeFormat.forPattern("yyyy-MM-dd")

    implicit val currentTaxYear: CurrentTaxYear = mockCurrentTaxYear

    val requestValidator = new RetrieveEmploymentsExpensesRequestValidator(mockAppConfig)

    MockAppConfig.employmentExpensesMinimumTaxYear
      .returns(2020)

    MockCurrentDateTime.getCurrentDate
      .returns(DateTime.parse("2020-08-05", dateTimeFormatter))
      .anyNumberOfTimes()

    MockCurrentTaxYear
      .getCurrentTaxYear(date)
      .returns(2021)

  }

}
