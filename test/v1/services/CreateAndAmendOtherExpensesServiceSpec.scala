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

package v1.services

import v1.mocks.connectors.MockCreateAndAmendOtherExpensesConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.createAndAmendOtherExpenses.{
  CreateAndAmendOtherExpensesBody,
  CreateAndAmendOtherExpensesRequest,
  PatentRoyaltiesPayments,
  PaymentsToTradeUnionsForDeathBenefits
}

import scala.concurrent.Future

class CreateAndAmendOtherExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2017-18"
  val nino: Nino = Nino("AA123456A")

  val body: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2000.99)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.99))
  )

  private val requestData = CreateAndAmendOtherExpensesRequest(nino, taxYear, body)

  trait Test extends MockCreateAndAmendOtherExpensesConnector {

    val service = new CreateAndAmendOtherExpensesService(
      connector = mockCreateAndAmendOtherExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockCreateAndAmendOtherExpensesConnector
          .createAndAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAndAmend(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockCreateAndAmendOtherExpensesConnector
            .createAndAmend(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(desErrorCode))))))

          await(service.createAndAmend(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "FORMAT_TAX_YEAR"           -> TaxYearFormatError,
        "SERVER_ERROR"              -> StandardDownstreamError,
        "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }

}