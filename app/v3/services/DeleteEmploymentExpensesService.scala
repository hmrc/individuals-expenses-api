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
import shared.controllers.RequestContext
import shared.models.errors._
import shared.services.{BaseService, ServiceOutcome}
import v3.connectors.DeleteEmploymentExpensesConnector
import v3.models.request.deleteEmploymentExpenses.DeleteEmploymentExpensesRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteEmploymentExpensesService @Inject() (deleteEmploymentExpensesConnector: DeleteEmploymentExpensesConnector) extends BaseService {

  def deleteEmploymentExpenses(
      request: DeleteEmploymentExpensesRequestData)(implicit ctx: RequestContext, ec: ExecutionContext): Future[ServiceOutcome[Unit]] = {

    deleteEmploymentExpensesConnector.deleteEmploymentExpenses(request).map(_.leftMap(mapDownstreamErrors(downstreamErrorMap)))

  }

  private val downstreamErrorMap: Map[String, MtdError] = {
    val errorMap = Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR"          -> TaxYearFormatError,
      "INVALID_CORRELATIONID"     -> InternalError,
      "NO_DATA_FOUND"             -> NotFoundError,
      "SERVER_ERROR"              -> InternalError,
      "SERVICE_UNAVAILABLE"       -> InternalError
    )

    val extraTysErrors = Map(
      "INVALID_CORRELATION_ID" -> InternalError,
      "NOT_FOUND"              -> NotFoundError,
      "TAX_YEAR_NOT_SUPPORTED" -> RuleTaxYearNotSupportedError
    )

    errorMap ++ extraTysErrors
  }

}
