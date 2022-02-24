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

package v1r7a.mocks.connectors

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.http.HeaderCarrier
import v1r7a.connectors.{AmendOtherExpensesConnector, DownstreamOutcome}
import v1r7a.models.request.amendOtherExpenses.AmendOtherExpensesRequest

import scala.concurrent.{ExecutionContext, Future}

trait MockAmendOtherExpensesConnector extends MockFactory {

  val mockAmendOtherExpensesConnector: AmendOtherExpensesConnector = mock[AmendOtherExpensesConnector]

  object MockAmendOtherExpensesConnector {

    def amend(requestData: AmendOtherExpensesRequest): CallHandler[Future[DownstreamOutcome[Unit]]] = {
      (mockAmendOtherExpensesConnector
        .amend(_: AmendOtherExpensesRequest)(_: HeaderCarrier, _: ExecutionContext, _: String))
        .expects(requestData, *, *, *)
    }
  }

}
