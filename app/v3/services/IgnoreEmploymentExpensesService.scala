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

package v3.services

import cats.implicits._
import common.error.TaxYearNotEndedError
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v3.connectors.IgnoreEmploymentExpensesConnector
import v3.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IgnoreEmploymentExpensesService @Inject() (connector: IgnoreEmploymentExpensesConnector) extends BaseService {

  def ignore(request: IgnoreEmploymentExpensesRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector.ignore(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))
  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
      "INVALID_TAX_YEAR"                -> TaxYearFormatError,
      "INVALID_CORRELATIONID"           -> InternalError,
      "INVALID_PAYLOAD"                 -> InternalError,
      "INVALID_REQUEST_BEFORE_TAX_YEAR" -> TaxYearNotEndedError,
      "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
      "SERVER_ERROR"                    -> InternalError,
      "SERVICE_UNAVAILABLE"             -> InternalError
    )

    val extraTysErrors = Map(
      "INVALID_CORRELATION_ID"   -> InternalError,
      "TAX_YEAR_NOT_SUPPORTED"   -> RuleTaxYearNotSupportedError,
      "OUTSIDE_AMENDMENT_WINDOW" -> RuleOutsideAmendmentWindow
    )

    errors ++ extraTysErrors
  }

}
