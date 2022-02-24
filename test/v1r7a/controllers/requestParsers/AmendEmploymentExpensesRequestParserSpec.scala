/*
 * Copyright 2022 HM Revenue & Customs
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

package v1r7a.controllers.requestParsers

import play.api.libs.json.Json
import support.UnitSpec
import v1r7a.models.domain.Nino
import v1r7a.mocks.validators.MockAmendEmploymentExpensesValidator
import v1r7a.models.errors.{BadRequestError, ErrorWrapper, NinoFormatError, TaxYearFormatError}
import v1r7a.models.request.amendEmploymentExpenses.{AmendEmploymentExpensesBody, AmendEmploymentExpensesRawData, AmendEmploymentExpensesRequest, Expenses}

class AmendEmploymentExpensesRequestParserSpec extends UnitSpec {

  val nino = "AA123456B"
  val taxYear = "2017-18"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

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

  val inputData =
    AmendEmploymentExpensesRawData(nino, taxYear, requestBodyJson)

  trait Test extends MockAmendEmploymentExpensesValidator {
    lazy val parser = new AmendEmploymentExpensesRequestParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockAmendEmploymentExpensesValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(AmendEmploymentExpensesRequest(Nino(nino), taxYear, AmendEmploymentExpensesBody(
            Expenses(
              Some(123.12),
              Some(123.12),
              Some(123.12),
              Some(123.12),
              Some(123.12),
              Some(123.12),
              Some(123.12),
              Some(123.12)
            )
          )))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockAmendEmploymentExpensesValidator.validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockAmendEmploymentExpensesValidator.validate(inputData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}
