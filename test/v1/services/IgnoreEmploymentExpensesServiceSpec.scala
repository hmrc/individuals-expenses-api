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

package v1.services

import api.models.domain.{Nino, TaxYear}
import api.models.errors.{DownstreamErrorCode, DownstreamErrors, ErrorWrapper, MtdError, NinoFormatError, NotFoundError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError, StandardDownstreamError, TaxYearFormatError}
import api.models.outcomes.ResponseWrapper
import v1.mocks.connectors.MockIgnoreEmploymentExpensesConnector
import v1.models.errors._
import v1.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRequest

import scala.concurrent.Future

class IgnoreEmploymentExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2021-22"
  val nino: Nino = Nino("AA123456A")

  private val requestData = IgnoreEmploymentExpensesRequest(nino, TaxYear.fromMtd(taxYear))

  trait Test extends MockIgnoreEmploymentExpensesConnector {

    val service = new IgnoreEmploymentExpensesService(
      connector = mockIgnoreEmploymentExpensesConnector
    )

  }

  "service" should {
    "return mapped result" when {
      "connector call successful" in new Test {
        MockIgnoreEmploymentExpensesConnector
          .ignore(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.ignore(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "map errors according to spec" should {
    "connector call unsuccessful" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockIgnoreEmploymentExpensesConnector
            .ignore(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.ignore(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = List(
        "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
        "INVALID_TAX_YEAR"                -> TaxYearFormatError,
        "INVALID_CORRELATIONID"           -> StandardDownstreamError,
        "INVALID_PAYLOAD"                 -> StandardDownstreamError,
        "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
        "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
        "SERVER_ERROR"                    -> StandardDownstreamError,
        "SERVICE_UNAVAILABLE"             -> StandardDownstreamError
      )

      val extraTysErrors = List(
        "INVALID_CORRELATION_ID" -> StandardDownstreamError,
        "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

}
