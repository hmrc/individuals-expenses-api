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

import play.api.http.Status.NO_CONTENT
import shared.config.AppConfig
import shared.connectors.DownstreamUri.{DesUri, IfsUri}
import shared.connectors.httpparsers.StandardDownstreamHttpParser._
import shared.connectors.{BaseDownstreamConnector, DownstreamOutcome}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import v2.models.request.deleteEmploymentExpenses.DeleteEmploymentExpensesRequestData

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeleteEmploymentExpensesConnector @Inject() (val http: HttpClientV2, val appConfig: AppConfig) extends BaseDownstreamConnector {

  def deleteEmploymentExpenses(request: DeleteEmploymentExpensesRequestData)(implicit
      hc: HeaderCarrier,
      ec: ExecutionContext,
      correlationId: String): Future[DownstreamOutcome[Unit]] = {

    implicit val successCode: SuccessCode = SuccessCode(NO_CONTENT)

    import request._

    val downstreamUri =
      if (request.taxYear.useTaxYearSpecificApi) {
        IfsUri[Unit](s"income-tax/expenses/employments/${taxYear.asTysDownstream}/$nino")
      } else {
        // The endpoint uses the MTD tax year format:
        DesUri[Unit](s"income-tax/expenses/employments/$nino/${taxYear.asMtd}")
      }

    delete(
      uri = downstreamUri
    )
  }

}
