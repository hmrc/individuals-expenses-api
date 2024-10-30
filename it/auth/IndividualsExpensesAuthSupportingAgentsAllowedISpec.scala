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
import shared.auth.AuthSupportingAgentsAllowedISpec
import shared.models.domain.TaxYear
import shared.services.DownstreamStub

class IndividualsExpensesAuthSupportingAgentsAllowedISpec extends AuthSupportingAgentsAllowedISpec {

  private val taxYear = TaxYear.fromMtd("2019-20")

  override val callingApiVersion = "2.0"

  override val supportingAgentsAllowedEndpoint = "ignore-employment-expenses"

  override val mtdUrl = s"/employments/$nino/${taxYear.asMtd}/ignore"

  override def sendMtdRequest(request: WSRequest): WSResponse = await(request.post(JsObject.empty))

  override val downstreamUri: String = s"/income-tax/expenses/employments/$nino/${taxYear.asMtd}"

  override val downstreamHttpMethod: DownstreamStub.HTTPMethod = DownstreamStub.PUT

  override val downstreamSuccessStatus: Int = NO_CONTENT

  override val maybeDownstreamResponseJson: Option[JsValue] = Some(JsObject.empty)

}
