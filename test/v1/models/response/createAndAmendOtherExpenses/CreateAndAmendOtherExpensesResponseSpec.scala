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

package v1.models.response.createAndAmendOtherExpenses

import api.hateoas.Link
import api.hateoas.Method.{DELETE, GET, PUT}
import config.MockAppConfig
import support.UnitSpec

class CreateAndAmendOtherExpensesResponseSpec extends UnitSpec with MockAppConfig {

  "LinksFactory" should {
    "return the correct links" in {
      val nino    = "mynino"
      val taxYear = "mytaxyear"

      MockedAppConfig.apiGatewayContext.returns("my/context").anyNumberOfTimes()
      CreateAndAmendOtherExpensesResponse.CreateAndAmendOtherExpensesLinksFactory.links(
        mockAppConfig,
        CreateAndAmendOtherExpensesHateoasData(nino, taxYear)) shouldBe
        Seq(
          Link(s"/my/context/other/$nino/$taxYear", GET, "self"),
          Link(s"/my/context/other/$nino/$taxYear", PUT, "amend-expenses-other"),
          Link(s"/my/context/other/$nino/$taxYear", DELETE, "delete-expenses-other")
        )
    }
  }

}
