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
import cats.implicits._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import utils.Logging
import v1r7a.connectors.IgnoreEmploymentExpensesConnector
import v1r7a.controllers.EndpointLogContext
import v1r7a.models.errors._
import v1r7a.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRequest
import v1r7a.support.DesResponseMappingSupport

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IgnoreEmploymentExpensesService @Inject()(connector: IgnoreEmploymentExpensesConnector) extends DesResponseMappingSupport with Logging {

  def ignore(request: IgnoreEmploymentExpensesRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    logContext: EndpointLogContext,
    correlationId: String): Future[IgnoreEmploymentExpensesServiceOutcome] = {

    val result = for {
      desResponseWrapper <- EitherT(connector.ignore(request)).leftMap(mapDesErrors(desErrorMap))
    } yield desResponseWrapper

    result.value
  }

  private def desErrorMap =
    Map(
      "INVALID_TAXABLE_ENTITY_ID" -> NinoFormatError,
      "INVALID_TAX_YEAR" -> TaxYearFormatError,
      "INVALID_CORRELATIONID" -> DownstreamError,
      "INVALID_PAYLOAD" -> DownstreamError,
      "INVALID_REQUEST_BEFORE_TAX_YEAR" -> RuleTaxYearNotEndedError,
      "INCOME_SOURCE_NOT_FOUND" -> NotFoundError,
      "SERVER_ERROR" -> DownstreamError,
      "SERVICE_UNAVAILABLE" -> DownstreamError
    )
}
