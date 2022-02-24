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
import uk.gov.hmrc.http.HeaderCarrier
import cats.implicits._
import utils.Logging
import v1r7a.connectors.RetrieveEmploymentsExpensesConnector
import v1r7a.controllers.EndpointLogContext
import v1r7a.models.errors._
import v1r7a.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequest
import v1r7a.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveEmploymentsExpensesService @Inject()(retrieveEmploymentsExpensesConnector: RetrieveEmploymentsExpensesConnector)
  extends DesResponseMappingSupport with Logging {

  def retrieveEmploymentsExpenses(request: RetrieveEmploymentsExpensesRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext,
    correlationId: String): Future[RetrieveEmploymentExpensesServiceOutcome] = {

    val result = for {
      desResponseWrapper <- EitherT(retrieveEmploymentsExpensesConnector.retrieveEmploymentExpenses(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper
    result.value
  }
  private def desErrorMap: Map[String, MtdError] =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR" -> TaxYearFormatError,
      "INVALID_VIEW" -> SourceFormatError,
      "INVALID_CORRELATIONID" -> DownstreamError,
      "NO_DATA_FOUND" -> NotFoundError,
      "INVALID_DATE_RANGE" -> RuleTaxYearNotSupportedError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )

}
