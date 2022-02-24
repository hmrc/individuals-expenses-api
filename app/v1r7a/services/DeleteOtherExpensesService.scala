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

import cats.data.EitherT
import javax.inject.{Inject, Singleton}
import cats.implicits._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1r7a.connectors.DeleteOtherExpensesConnector
import v1r7a.controllers.EndpointLogContext
import v1r7a.models.errors._
import v1r7a.models.request.deleteOtherExpenses.DeleteOtherExpensesRequest
import v1r7a.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteOtherExpensesService @Inject()(deleteOtherExpensesConnector: DeleteOtherExpensesConnector)
  extends DesResponseMappingSupport with Logging {

  def deleteOtherExpenses(request: DeleteOtherExpensesRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext,
    correlationId: String): Future[DeleteOtherExpensesServiceOutcome] = {

    val result = for {
      desResponseWrapper <- EitherT(deleteOtherExpensesConnector.deleteOtherExpenses(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper
    result.value
  }
  private def desErrorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "FORMAT_TAX_YEAR" -> TaxYearFormatError,
      "NO_DATA_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}