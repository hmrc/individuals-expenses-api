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

import api.controllers.RequestContext
import api.models.errors._
import api.services.{BaseService, ServiceOutcome}
import cats.implicits.toBifunctorOps
import v2.connectors.CreateAndAmendEmploymentExpensesConnector
import v2.models.request.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CreateAndAmendEmploymentExpensesService @Inject() (connector: CreateAndAmendEmploymentExpensesConnector) extends BaseService {

  def createAndAmendEmploymentExpenses(
      request: CreateAndAmendEmploymentExpensesRequest)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    connector.createAmendEmploymentExpenses(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))

  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val errors = Map(
      "INVALID_TAXABLE_ENTITY_ID"       -> NinoFormatError,
      "INVALID_TAX_YEAR"                -> TaxYearFormatError,
      "INVALID_CORRELATIONID"           -> StandardDownstreamError,
      "INVALID_PAYLOAD"                 -> StandardDownstreamError,
      "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
      "INCOME_SOURCE_NOT_FOUND"         -> NotFoundError,
      "SERVER_ERROR"                    -> StandardDownstreamError,
      "SERVICE_UNAVAILABLE"             -> StandardDownstreamError
    )
    val extraTysErrors = Map(
      "INVALID_CORRELATION_ID" -> StandardDownstreamError,
      "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
    )

    errors ++ extraTysErrors
  }

}
