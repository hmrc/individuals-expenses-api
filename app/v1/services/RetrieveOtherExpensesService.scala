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

import cats.data.EitherT
import javax.inject.Inject
import cats.implicits._
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1.connectors.RetrieveOtherExpensesConnector
import v1.controllers.EndpointLogContext
import v1.models.errors._
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequest
import v1.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

class RetrieveOtherExpensesService @Inject()(retrieveOtherExpensesConnector: RetrieveOtherExpensesConnector)
  extends DesResponseMappingSupport with Logging {

  def retrieveOtherExpenses(request: RetrieveOtherExpensesRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext): Future[RetrieveOtherExpensesServiceOutcome] = {

    val result = for {
      desResponseWrapper <- EitherT(retrieveOtherExpensesConnector.retrieveOtherExpenses(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper
    result.value
  }
  private def desErrorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "FORMAT_TAX_YEAR" -> TaxYearFormatError,
      "NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}
