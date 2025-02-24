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

package v3.services

import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v3.connectors.MockDeleteOtherExpensesConnector
import v3.models.request.deleteOtherExpenses.DeleteOtherExpensesRequestData

import scala.concurrent.Future

class DeleteOtherExpensesServiceSpec extends ServiceSpec {

  private val taxYear    = TaxYear.fromMtd("2017-18")
  private val nino: Nino = Nino("AA123456A")

  private val requestData = DeleteOtherExpensesRequestData(nino, taxYear)

  trait Test extends MockDeleteOtherExpensesConnector {

    val service = new DeleteOtherExpensesService(
      deleteOtherExpensesConnector = mockDeleteOtherExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockDeleteOtherExpensesConnector
          .deleteOtherExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.deleteOtherExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockDeleteOtherExpensesConnector
            .deleteOtherExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.deleteOtherExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "NO_DATA_FOUND"             -> NotFoundError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      val extraTysErrors = Seq(
        "INVALID_CORRELATION_ID"   -> InternalError,
        "TAX_YEAR_NOT_SUPPORTED"   -> RuleTaxYearNotSupportedError,
        "OUTSIDE_AMENDMENT_WINDOW" -> RuleOutsideAmendmentWindow
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
