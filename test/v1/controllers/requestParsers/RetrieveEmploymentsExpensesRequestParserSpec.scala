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

package v1.controllers.requestParsers

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockRetrieveEmploymentExpensesValidator
import v1.models.domain.MtdSource
import v1.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}

class RetrieveEmploymentsExpensesRequestParserSpec extends UnitSpec {
  val nino = "AA123456B"
  val taxYear = "2021-22"

  val inputDataLatest = RetrieveEmploymentsExpensesRawData(nino, taxYear, "latest")
  val inputDataHmrcHeld = RetrieveEmploymentsExpensesRawData(nino, taxYear, "hmrcHeld")
  val inputDataUser = RetrieveEmploymentsExpensesRawData(nino, taxYear, "user")

  trait Test extends MockRetrieveEmploymentExpensesValidator {
    lazy val parser = new RetrieveEmploymentsExpensesRequestParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied with the latest query parameter" in new Test {
        MockRetrieveEmploymentExpensesValidator.validate(inputDataLatest).returns(Nil)

        parser.parseRequest(inputDataLatest) shouldBe
          Right(RetrieveEmploymentsExpensesRequest(Nino(nino), "2021-22", MtdSource.`latest`))
      }
      "valid request data is supplied with the HMRC Held query parameter" in new Test {
        MockRetrieveEmploymentExpensesValidator.validate(inputDataHmrcHeld).returns(Nil)

        parser.parseRequest(inputDataHmrcHeld) shouldBe
          Right(RetrieveEmploymentsExpensesRequest(Nino(nino), "2021-22", MtdSource.`hmrcHeld`))
      }
      "valid request data is supplied with the user query parameter" in new Test {
        MockRetrieveEmploymentExpensesValidator.validate(inputDataUser).returns(Nil)

        parser.parseRequest(inputDataUser) shouldBe
          Right(RetrieveEmploymentsExpensesRequest(Nino(nino), "2021-22", MtdSource.`user`))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockRetrieveEmploymentExpensesValidator.validate(inputDataLatest)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputDataLatest) shouldBe
          Left(ErrorWrapper(None, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockRetrieveEmploymentExpensesValidator.validate(inputDataLatest)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(inputDataLatest) shouldBe
          Left(ErrorWrapper(None, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}

