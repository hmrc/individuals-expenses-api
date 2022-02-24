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
import v1r7a.mocks.connectors.MockAmendEmploymentExpensesConnector
import v1r7a.models.errors._
import v1r7a.models.outcomes.ResponseWrapper
import v1r7a.models.request.amendEmploymentExpenses.{AmendEmploymentExpensesBody, AmendEmploymentExpensesRequest, Expenses}

import scala.concurrent.Future

class AmendEmploymentExpensesServiceSpec extends ServiceSpec {

  val taxYear = "2021-22"
  val nino: Nino = Nino("AA123456A")

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

  private val requestData = AmendEmploymentExpensesRequest(nino, taxYear, body)

  trait Test extends MockAmendEmploymentExpensesConnector {
    val service = new AmendEmploymentExpensesService(
      connector = mockAmendEmploymentExpensesConnector
    )
  }

  "service" should {
    "service call successful" when {
      "return mapped result" in new Test {
        MockAmendEmploymentExpensesConnector.amend(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        await(service.amend(requestData)) shouldBe Right(ResponseWrapper(correlationId, ()))
      }
    }
  }

  "unsuccessful" should {
    "map errors according to spec" when {

      def serviceError(desErrorCode: String, error: MtdError): Unit =
        s"a $desErrorCode error is returned from the service" in new Test {

          MockAmendEmploymentExpensesConnector.amend(requestData)
            .returns(Future.successful(Left(ResponseWrapper(correlationId, DesErrors.single(DesErrorCode(desErrorCode))))))

          await(service.amend(requestData)) shouldBe Left(ErrorWrapper(correlationId, error))
        }

      val input = Seq(
        "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
        "INVALID_TAX_YEAR" -> TaxYearFormatError,
        "INVALID_CORRELATIONID" -> DownstreamError,
        "INVALID_PAYLOAD" -> DownstreamError,
        "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
        "INCOME_SOURCE_NOT_FOUND" -> NotFoundError,
        "SERVER_ERROR" -> DownstreamError,
        "SERVICE_UNAVAILABLE" -> DownstreamError
      )

      input.foreach(args => (serviceError _).tupled(args))
    }
  }
}