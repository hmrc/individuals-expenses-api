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

import common.domain.MtdSource
import common.error.SourceFormatError
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v3.connectors.MockRetrieveEmploymentsExpensesConnector
import v3.fixtures.RetrieveEmploymentsExpensesFixtures._
import v3.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequestData

import scala.concurrent.Future

class RetrieveEmploymentsExpensesServiceSpec extends ServiceSpec {

  val taxYear                       = "2017-18"
  val nino: Nino                    = Nino("AA123456A")
  val source: MtdSource.`user`.type = MtdSource.`user`

  private val requestData = RetrieveEmploymentsExpensesRequestData(nino, TaxYear.fromMtd(taxYear), source)

  trait Test extends MockRetrieveEmploymentsExpensesConnector {

    val service = new RetrieveEmploymentsExpensesService(
      connector = mockRetrieveEmploymentsExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockRetrieveEmploymentsExpensesConnector
          .retrieveEmploymentsExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseModelLatest))))

        await(service.retrieveEmploymentsExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, responseModelLatest))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockRetrieveEmploymentsExpensesConnector
            .retrieveEmploymentsExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.retrieveEmploymentsExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR"          -> TaxYearFormatError,
        "INVALID_VIEW"              -> SourceFormatError,
        "INVALID_CORRELATIONID"     -> InternalError,
        "NO_DATA_FOUND"             -> NotFoundError,
        "INVALID_DATE_RANGE"        -> RuleTaxYearNotSupportedError,
        "SERVER_ERROR"              -> InternalError,
        "SERVICE_UNAVAILABLE"       -> InternalError
      )

      val extraTysErrors = List(
        "INVALID_CORRELATION_ID" -> InternalError,
        "NOT_FOUND"              -> NotFoundError,
        "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
