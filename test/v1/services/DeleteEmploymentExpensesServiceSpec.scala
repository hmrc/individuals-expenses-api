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

import v1.mocks.connectors.MockDeleteEmploymentExpensesConnector
import v1.models.domain.Nino
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.TaxYear
import v1.models.request.deleteEmploymentExpenses.DeleteEmploymentExpensesRequest

import scala.concurrent.Future

class DeleteEmploymentExpensesServiceSpec extends ServiceSpec {

  val taxYear    = "2021-22"
  val nino: Nino = Nino("AA123456A")

  private val requestData = DeleteEmploymentExpensesRequest(nino, TaxYear.fromMtd(taxYear))

  trait Test extends MockDeleteEmploymentExpensesConnector {

    val service = new DeleteEmploymentExpensesService(
      deleteEmploymentExpensesConnector = mockDeleteEmploymentExpensesConnector
    )

  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockDeleteEmploymentExpensesConnector
          .deleteEmploymentExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.deleteEmploymentExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(errorCode: String, error: MtdError): Unit =
        s"a $errorCode error is returned from the service" in new Test {

          MockDeleteEmploymentExpensesConnector
            .deleteEmploymentExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DownstreamErrors.single(DownstreamErrorCode(errorCode))))))

          await(service.deleteEmploymentExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

        def input: Map[String, MtdError] = {
          val errorMap = Map(
            "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
            "INVALID_TAX_YEAR"          -> TaxYearFormatError,
            "INVALID_CORRELATIONID"     -> StandardDownstreamError,
            "NO_DATA_FOUND"             -> NotFoundError,
            "SERVER_ERROR"              -> StandardDownstreamError,
            "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
          )

          val extraTysErrors = Map(
            "INVALID_CORRELATION_ID" -> StandardDownstreamError,
            "NOT_FOUND"              -> NotFoundError,
            "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
          )

          errorMap ++ extraTysErrors
        }

      input.foreach(args => (serviceError _).tupled(args))
    }
  }

}
