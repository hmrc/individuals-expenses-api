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

package v3.connectors

import shared.config.AppConfig
import shared.connectors.DownstreamUri.IfsUri
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import v3.models.request.deleteOtherExpenses.DeleteOtherExpensesRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteOtherExpensesConnector @Inject() (
    val http: HttpClient,
    val appConfig: AppConfig
) extends BaseDownstreamConnector {

  def deleteOtherExpenses(request: DeleteOtherExpensesRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {

    import request._

    val uri = if (taxYear.useTaxYearSpecificApi) {
      IfsUri[Unit](s"income-tax/expenses/other/${taxYear.asTysDownstream}/$nino")
    } else {
      IfsUri[Unit](s"income-tax/expenses/other/$nino/${taxYear.asMtd}")
    }

    delete(uri)
  }

}
