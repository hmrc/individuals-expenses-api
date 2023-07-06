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

package v1.controllers.requestParsers

import api.models.domain.{Nino, TaxYear}
import api.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import play.api.libs.json.Json
import support.UnitSpec
import v1.mocks.validators.MockCreateAndAmendEmploymentExpensesValidator
import v1.models.request.createAndAmendEmploymentExpenses.{CreateAndAmendEmploymentExpensesBody, CreateAndAmendEmploymentExpensesRawData, CreateAndAmendEmploymentExpensesRequest, Expenses}

class CreateAndAmendEmploymentExpensesRequestParserSpec extends UnitSpec {

  val nino    = "AA123456B"
  val taxYear = "2017-18"

  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val requestBodyJson = Json.parse("""
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

  private val rawData: CreateAndAmendEmploymentExpensesRawData =
    CreateAndAmendEmploymentExpensesRawData(nino = nino, taxYear = taxYear, body = requestBodyJson)

  private val requestData = CreateAndAmendEmploymentExpensesRequest(
    nino = Nino(nino),
    taxYear = TaxYear.fromMtd(taxYear),
    body = CreateAndAmendEmploymentExpensesBody(
      Expenses(Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12), Some(123.12))
    )
  )

  trait Test extends MockCreateAndAmendEmploymentExpensesValidator {

    lazy val parser = new CreateAndAmendEmploymentExpensesRequestParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {

      "valid request data is supplied" in new Test {

        MockCreateAndAmendEmploymentExpensesValidator.validate(rawData).returns(Nil)

        val result: Either[ErrorWrapper, CreateAndAmendEmploymentExpensesRequest] = parser.parseRequest(rawData)

        result shouldBe Right(requestData)
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockCreateAndAmendEmploymentExpensesValidator
          .validate(rawData)
          .returns(List(NinoFormatError))

        val result = parser.parseRequest(rawData)

        result shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockCreateAndAmendEmploymentExpensesValidator
          .validate(rawData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        val result: Either[ErrorWrapper, CreateAndAmendEmploymentExpensesRequest] = parser.parseRequest(rawData)

        result shouldBe Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }

}
