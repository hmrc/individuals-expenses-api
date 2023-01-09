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

import v1.mocks.connectors.MockCreateAmendEmploymentExpensesConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.CreateAmendEmploymentExpenses.{AmendEmploymentExpensesBody, CreateAmendEmploymentExpensesRequest, Expenses}
import v1.models.request.TaxYear
import scala.concurrent.Future

class CreateAmendEmploymentExpensesServiceSpec extends ServiceSpec {

  val taxYear: TaxYear = TaxYear.fromMtd("2021-22")
  val nino: Nino       = Nino("AA123456A")

  val body: AmendEmploymentExpensesBody = AmendEmploymentExpensesBody(
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
  )

  private val requestData = CreateAmendEmploymentExpensesRequest(nino, taxYear, body)

  "service" should {

    "service call successful" when {

      "return mapped result" in new Test {

        MockAmendEmploymentExpensesConnector
          .amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAmendEmploymentExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {

    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockAmendEmploymentExpensesConnector
            .amend(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.createAmendEmploymentExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = Seq(
        "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
        "INVALID_TAX_YEAR"                -> TaxYearFormatError,
        "INVALID_CORRELATIONID"           -> StandardDownstreamError,
        "INVALID_PAYLOAD"                 -> StandardDownstreamError,
        "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
        "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
        "SERVER_ERROR"                    -> StandardDownstreamError,
        "SERVICE_UNAVAILABLE"             -> StandardDownstreamError
      )

      val extraTysErrors = Seq(
        "INVALID_CORRELATION_ID" -> StandardDownstreamError,
        "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
      )

      (errors ++ extraTysErrors).foreach(args => (serviceError _).tupled(args))
    }
  }

  trait Test extends MockCreateAmendEmploymentExpensesConnector {

    val service = new CreateAmendEmploymentExpensesService(
      connector = mockAmendEmploymentExpensesConnector
    )

  }

}
