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

package v2.connectors

import api.connectors.DownstreamUri._
import api.connectors.httpparsers.StandardDownstreamHttpParser._
import api.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import config.AppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v2.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequestData
import v2.models.response.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesResponse

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveEmploymentsExpensesConnector @Inject() (val http: HttpClient, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def retrieveEmploymentExpenses(request: RetrieveEmploymentsExpensesRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[RetrieveEmploymentsExpensesResponse]] = {

    val source  = request.source.toDownstream
    val nino    = request.nino.value
    val taxYear = request.taxYear

    val url = if (taxYear.useTaxYearSpecificApi) {
      TaxYearSpecificIfsUri[RetrieveEmploymentsExpensesResponse](s"income-tax/expenses/employments/${taxYear.asTysDownstream}/$nino?view=$source")
    } else {
      IfsR6Uri[RetrieveEmploymentsExpensesResponse](s"income-tax/expenses/employments/$nino/${taxYear.asMtd}?view=$source")
    }

    get(url)
  }

}
