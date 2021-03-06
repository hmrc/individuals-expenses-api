/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Json
import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import v1.mocks.validators.MockAmendOtherExpensesValidator
import v1.models.errors._
import v1.models.request.amendOtherExpenses._

class AmendOtherExpensesRequestParserSpec extends UnitSpec {

  val nino = "AA123456B"
  val taxYear = "2017-18"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  private val requestBodyJson = Json.parse(
    """{
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 1223.22
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 1223.22
      |  }
      |}
      |""".stripMargin)

  val inputData =
    AmendOtherExpensesRawData(nino, taxYear, requestBodyJson)

  trait Test extends MockAmendOtherExpensesValidator {
    lazy val parser = new AmendOtherExpensesRequestParser(mockValidator)
  }

  "parse" should {

    "return a request object" when {
      "valid request data is supplied" in new Test {
        MockAmendOtherExpensesValidator.validate(inputData).returns(Nil)

        parser.parseRequest(inputData) shouldBe
          Right(AmendOtherExpensesRequest(Nino(nino), taxYear, AmendOtherExpensesBody(
            Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 1223.22)),
            Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 1223.22))
          )))
      }
    }

    "return an ErrorWrapper" when {

      "a single validation error occurs" in new Test {
        MockAmendOtherExpensesValidator.validate(inputData)
          .returns(List(NinoFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }

      "multiple validation errors occur" in new Test {
        MockAmendOtherExpensesValidator.validate(inputData)
          .returns(List(NinoFormatError, TaxYearFormatError))

        parser.parseRequest(inputData) shouldBe
          Left(ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, TaxYearFormatError))))
      }
    }
  }
}