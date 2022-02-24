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

package v1r7a.services

import v1r7a.models.domain.Nino
import v1r7a.mocks.connectors.MockRetrieveEmploymentsExpensesConnector
import v1r7a.models.domain.MtdSource
import v1r7a.models.errors._
import v1r7a.models.outcomes.ResponseWrapper
import v1r7a.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequest
import v1r7a.models.response.retrieveEmploymentExpenses.{Expenses, RetrieveEmploymentsExpensesResponse}

import scala.concurrent.Future

class RetrieveEmploymentsExpensesServiceSpec extends ServiceSpec {

  val taxYear = "2017-18"
  val nino: Nino = Nino("AA123456A")
  val source: MtdSource.`user`.type = MtdSource.`user`

  val body: RetrieveEmploymentsExpensesResponse = RetrieveEmploymentsExpensesResponse(
    Some("2019-04-06"),
    Some(2000.99),
    Some(MtdSource.`user`),
    Some("2019-04-06"),
    Some(Expenses(
      Some(2000.99),
      Some(2000.99),
      Some(2000.99),
      Some(2000.99),
      Some(2000.99),
      Some(2000.99),
      Some(2000.99),
      Some(2000.99)
    ))
  )

  private val requestData = RetrieveEmploymentsExpensesRequest(nino, taxYear, source)

  trait Test extends MockRetrieveEmploymentsExpensesConnector {
    val service = new RetrieveEmploymentsExpensesService(
      retrieveEmploymentsExpensesConnector = mockRetrieveEmploymentsExpensesConnector
    )
  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockRetrieveEmploymentsExpensesConnector.retrieveEmploymentsExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, body))))

        await(service.retrieveEmploymentsExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, body))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockRetrieveEmploymentsExpensesConnector.retrieveEmploymentsExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.retrieveEmploymentsExpenses(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR" -> TaxYearFormatError,
        "INVALID_VIEW" -> SourceFormatError,
        "INVALID_CORRELATIONID" -> DownstreamError,
        "NO_DATA_FOUND" -> NotFoundError,
        "INVALID_DATE_RANGE" -> RuleTaxYearNotSupportedError,
        "SERVER_ERROR" -> DownstreamError,
        "SERVICE_UNAVAILABLE" -> DownstreamError
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }

}
