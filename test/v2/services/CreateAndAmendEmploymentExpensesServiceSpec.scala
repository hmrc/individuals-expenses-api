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

import common.error.{RuleInvalidSubmissionPensionScheme, TaxYearNotEndedError}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import shared.services.ServiceSpec
import v2.connectors.MockCreateAndAmendEmploymentExpensesConnector
import v2.models.request.createAndAmendEmploymentExpenses.{
  CreateAndAmendEmploymentExpensesBody,
  CreateAndAmendEmploymentExpensesRequestData,
  Expenses
}

import scala.concurrent.Future

class CreateAndAmendEmploymentExpensesServiceSpec extends ServiceSpec {

  val taxYear: TaxYear = TaxYear.fromMtd("2021-22")
  val nino: Nino       = Nino("AA123456A")

  val body: CreateAndAmendEmploymentExpensesBody = CreateAndAmendEmploymentExpensesBody(
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

  private val requestData = CreateAndAmendEmploymentExpensesRequestData(nino, taxYear, body)

  "service" should {

    "service call successful" when {

      "return mapped result" in new Test {

        MockCreateAndAmendEmploymentExpensesConnector
          .amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.createAndAmendEmploymentExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {

    "map errors according to spec" when {

      def serviceError(downstreamErrorCode: String, error: MtdError): Unit =
        s"a $downstreamErrorCode error is returned from the service" in new Test {

          MockCreateAndAmendEmploymentExpensesConnector
            .amend(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(downstreamErrorCode))))))

          await(service.createAndAmendEmploymentExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val errors = Seq(
        "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
        "INVALID_TAX_YEAR"                -> TaxYearFormatError,
        "INVALID_CORRELATIONID"           -> InternalError,
        "INVALID_PAYLOAD"                 -> InternalError,
        "INVALID_REQUEST_BEFORE_TAX_YEAR" -> TaxYearNotEndedError,
        "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
        "SERVER_ERROR"                    -> InternalError,
        "SERVICE_UNAVAILABLE"             -> InternalError
      )

      val extraTysErrors = Seq(
        "INVALID_CORRELATION_ID"            -> InternalError,
        "TAX_YEAR_NOT_SUPPORTED"            -> RuleTaxYearNotSupportedError,
        "INVALID_SUBMISSION_PENSION_SCHEME" -> RuleInvalidSubmissionPensionScheme
      )

      (errors ++ extraTysErrors).foreach(args => serviceError.tupled(args))
    }
  }

  trait Test extends MockCreateAndAmendEmploymentExpensesConnector {

    val service = new CreateAndAmendEmploymentExpensesService(
      connector = mockCreateAndAmendEmploymentExpensesConnector
    )

  }

}
