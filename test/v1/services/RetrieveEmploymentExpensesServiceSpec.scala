/*
 * Copyright 2020 HM Revenue & Customs
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

import support.UnitSpec
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HeaderCarrier
import v1.controllers.EndpointLogContext
import v1.mocks.connectors.MockRetrieveEmploymentExpensesConnector
import v1.models.des.DesSource
import v1.models.domain.MtdSource
import v1.models.errors._
import v1.models.outcomes.ResponseWrapper
import v1.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequest
import v1.models.response.retrieveEmploymentExpenses.{Expenses, RetrieveEmploymentExpensesResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveEmploymentExpensesServiceSpec extends UnitSpec {

  val taxYear = "2017-18"
  val nino = Nino("AA123456A")
  val correlationId = "X-123"
  val source = DesSource.`CUSTOMER`

  val body = RetrieveEmploymentExpensesResponse(
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


  trait Test extends MockRetrieveEmploymentExpensesConnector {
    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val logContext: EndpointLogContext = EndpointLogContext("c", "ep")

    val service = new RetrieveEmploymentExpensesService(
      retrieveEmploymentsExpensesConnector = mockRetrieveEmploymentExpensesConnector
    )
  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockRetrieveEmploymentExpensesConnector.retrieveEmploymentExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, body))))

        await(service.retrieveEmploymentExpenses(requestData)) shouldBe Right(ResponseWrapper(correlationId, body))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockRetrieveEmploymentExpensesConnector.retrieveEmploymentExpenses(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.retrieveEmploymentExpenses(requestData)) shouldBe Left(ErrorWrapper(Some(correlationId), error))
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
