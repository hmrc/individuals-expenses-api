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

import api.controllers.RequestContext
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits._
import v1.connectors.RetrieveOtherExpensesConnector
import v1.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequest
import v1.models.response.retrieveOtherExpenses.RetrieveOtherExpensesResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveOtherExpensesService @Inject() (connector: RetrieveOtherExpensesConnector) extends BaseService {

  def retrieveOtherExpenses(request: RetrieveOtherExpensesRequest)(implicit
      ctx: RequestContext,
      ec: ExecutionContext): Future[ServiceOutcome[RetrieveOtherExpensesResponse]] = {

    connector.retrieveOtherExpenses(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private def downstreamErrorMap: Map[String, MtdError] = {
    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "FORMAT_TAX_YEAR"           -> TaxYearFormatError,
      "NO_DATA_FOUND"             -> NotFoundError,
      "INVALID_CORRELATIONID"     -> StandardDownstreamError,
      "SERVER_ERROR"              -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"       -> StandardDownstreamError
    )

    val extraTysErrors = Map(
      "INVALID_CORRELATION_ID" -> StandardDownstreamError,
      "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
    )

    errors ++ extraTysErrors
  }

}
