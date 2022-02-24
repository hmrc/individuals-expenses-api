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

package v1r7a.connectors

import config.AppConfig
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpClient
import v1r7a.connectors.httpparsers.StandardDesHttpParser._
import v1r7a.models.request.deleteOtherExpenses.DeleteOtherExpensesRequest

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteOtherExpensesConnector @Inject()(val http: HttpClient,
                                             val appConfig: AppConfig) extends BaseDownstreamConnector {

  def deleteOtherExpenses(request: DeleteOtherExpensesRequest)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext,
    correlationId: String): Future[DownstreamOutcome[Unit]] = {

    delete(
      uri = BackendUri.IfsUri[Unit](s"income-tax/expenses/other/${request.nino.nino}/${request.taxYear}")
    )
  }
}
