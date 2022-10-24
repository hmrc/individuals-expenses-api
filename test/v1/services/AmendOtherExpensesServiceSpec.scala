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

import v1.mocks.connectors.MockAmendOtherExpensesConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendOtherExpenses.{AmendOtherExpensesBody, AmendOtherExpensesRequest, PatentRoyaltiesPayments, PaymentsToTradeUnionsForDeathBenefits}

import scala.concurrent.Future

class AmendOtherExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2017-18"
  val nino: Nino = Nino("AA123456A")

  val body: AmendOtherExpensesBody = AmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2000.99)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.99))
  )

  private val requestData = AmendOtherExpensesRequest(nino, taxYear, body)

  trait Test extends MockAmendOtherExpensesConnector {

    val service = new AmendOtherExpensesService(
      connector = mockAmendOtherExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockAmendOtherExpensesConnector
          .amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.amend(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockAmendOtherExpensesConnector
            .amend(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.amend(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "FORMAT_TAX_YEAR"           -> TaxYearFormatError,
        "SERVER_ERROR"              -> DownstreamError,
        "SERVICE_UNAVAILABLE"       -> DownstreamError
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }

}
