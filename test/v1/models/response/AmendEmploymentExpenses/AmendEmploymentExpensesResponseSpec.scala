/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.models.response.AmendEmploymentExpenses

import mocks.MockAppConfig
import support.UnitSpec
import v1.models.hateoas.Link
import v1.models.hateoas.Method.{DELETE, GET, PUT}
import v1.models.response.amendEmploymentExpenses.{AmendEmploymentExpensesHateoasData, AmendEmploymentExpensesResponse}

class AmendEmploymentExpensesResponseSpec extends UnitSpec with MockAppConfig {

  "LinksFactory" should {
    "return the correct links" in {
      val nino = "mynino"
      val taxYear = "mytaxyear"

      MockAppConfig.apiGatewayContext.returns("my/context").anyNumberOfTimes
      AmendEmploymentExpensesResponse.AmendOrderLinksFactory.links(mockAppConfig, AmendEmploymentExpensesHateoasData(nino, taxYear)) shouldBe
        Seq(
          Link(s"/my/context/employments/$nino/$taxYear", GET, "self"),
          Link(s"/my/context/employments/$nino/$taxYear", PUT, "amend-employment-expenses"),
          Link(s"/my/context/employments/$nino/$taxYear", DELETE, "delete-employment-expenses")
        )
    }
  }
}