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

package auth

import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.{WSRequest, WSResponse}
import shared.auth.AuthMainAgentsOnlyISpec
import shared.models.domain.TaxYear
import shared.services.DownstreamStub
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

class IndividualsExpensesAuthMainAgentsOnlyISpec extends AuthMainAgentsOnlyISpec {

  private val taxYear = TaxYear.fromMtd("2019-20")

  override protected val callingApiVersion = "3.0"

  override protected val supportingAgentsNotAllowedEndpoint = "ignore-employment-expenses"

  override protected val mtdUrl = s"/employments/$nino/${taxYear.asMtd}/ignore"

  override protected def sendMtdRequest(request: WSRequest): WSResponse = await(request.post(JsObject.empty))

  override protected val downstreamUri: String = s"/income-tax/expenses/employments/$nino/${taxYear.asMtd}"

  override protected val maybeDownstreamResponseJson: Option[JsValue] = Some(JsObject.empty)

  override protected val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  override protected val downstreamSuccessStatus: Int = NO_CONTENT
}
