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

import v1.mocks.connectors.MockRetrieveOtherExpensesConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequest
import v1.models.response.retrieveOtherExpenses._

import scala.concurrent.Future

class RetrieveOtherExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2017-18"
  val nino: Nino = Nino("AA123456A")

  val body: RetrieveOtherExpensesResponse = RetrieveOtherExpensesResponse(
    "2019-04-04T01:01:01Z",
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 76543.32)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 5423.65))
  )

  private val requestData = RetrieveOtherExpensesRequest(nino, taxYear)

  trait Test extends MockRetrieveOtherExpensesConnector {

    val service = new RetrieveOtherExpensesService(
      retrieveOtherExpensesConnector = mockRetrieveOtherExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockRetrieveOtherExpensesConnector
          .retrieveOtherExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, body))))

        await(service.retrieveOtherExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, body))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockRetrieveOtherExpensesConnector
            .retrieveOtherExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DesErrorCode(desErrorCode))))))

          await(service.retrieveOtherExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "FORMAT_TAX_YEAR"           -> TaxYearFormatError,
        "NO_DATA_FOUND"             -> NotFoundError,
        "SERVER_ERROR"              -> StandardDownstreamError,
        "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }

}
