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

package v2.services

import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v2.connectors.MockCreateAndAmendOtherExpensesConnector
import v2.models.request.createAndAmendOtherExpenses._

import scala.concurrent.Future

class CreateAndAmendOtherExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2021-22"
  val nino: Nino = Nino("AA123456A")

  val body: CreateAndAmendOtherExpensesBody = CreateAndAmendOtherExpensesBody(
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2000.99)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2000.99))
  )

  private val requestData = CreateAndAmendOtherExpensesRequestData(nino, TaxYear.fromMtd(taxYear), body)

  trait Test extends MockCreateAndAmendOtherExpensesConnector {

    val service = new CreateAndAmendOtherExpensesService(
      connector = mockCreateAndAmendOtherExpensesConnector
    )

  }

  "CreateAndAmendOtherExpensesService" should {
    "CreateAndAmendOtherExpenses" must {
      "return correct result for a success" in new Test {
        MockCreateAndAmendOtherExpensesConnector
          .createAndAmend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAndAmend(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "map errors according to spec" when {

    def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
      s"return ${error.code} error when $downstreamErrorCode error is returned from the service" in new Test {

        MockCreateAndAmendOtherExpensesConnector
          .createAndAmend(requestData)
          .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

        await(service.createAndAmend(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
      }

    val errors = Seq(
      ("INVALID_TAXABLE_ENTITY_ID", NinoFormatError),
      ("INVALID_TAX_YEAR", TaxYearFormatError),
      ("INVALID_CORRELATIONID", InternalError),
      ("INVALID_PAYLOAD", InternalError),
      ("UNPROCESSABLE_ENTITY", InternalError),
      ("SERVER_ERROR", InternalError),
      ("SERVICE_UNAVAILABLE", InternalError)
    )

    val extraTysErrors = Seq(
      ("INVALID_CORRELATION_ID", InternalError),
      ("TAX_YEAR_NOT_SUPPORTED", RuleTaxYearNotSupportedError)
    )

    (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
  }

}
