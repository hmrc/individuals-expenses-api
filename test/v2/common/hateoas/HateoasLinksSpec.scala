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

package v2.common.hateoas

import shared.config.MockAppConfig
import shared.hateoas.Method.{DELETE, GET, POST, PUT}
import shared.hateoas.{Link, Method}
import shared.models.domain.TaxYear
import shared.utils.UnitSpec
import v2.common.hateoas.RelType._

class HateoasLinksSpec extends UnitSpec with MockAppConfig {

  private val nino        = "AA111111A"
  private val taxYear2023 = TaxYear.fromMtd("2022-23")

  def assertCorrectLink(makeLink: TaxYear => Link, baseHref: String, method: Method, rel: String): Unit = {
    "return the correct link" in new Test {
      makeLink(taxYear2023) shouldBe Link(href = baseHref, method = method, rel = rel)
    }

  }

  class Test {
    MockedAppConfig.apiGatewayContext.returns("context").anyNumberOfTimes()
  }

  "HateoasLinks" when {
    "createAndAmendOtherExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/other/$nino/${taxYear2023.asMtd}", method = PUT, rel = AMEND_EXPENSES_OTHER)
        Target.createAndAmendOtherExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }
    "retrieveOtherExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/other/$nino/${taxYear2023.asMtd}", method = GET, rel = SELF)
        Target.retrieveOtherExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }
    "deleteOtherExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/other/$nino/${taxYear2023.asMtd}", method = DELETE, rel = DELETE_EXPENSES_OTHER)
        Target.deleteOtherExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }

    "createAndAmendEmploymentExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/employments/$nino/${taxYear2023.asMtd}", method = PUT, rel = AMEND_EMPLOYMENT_EXPENSES)
        Target.createAndAmendEmploymentExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }

    }
    "retrieveEmploymentExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/employments/$nino/${taxYear2023.asMtd}", method = GET, rel = SELF)
        Target.retrieveEmploymentExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }
    "deleteEmploymentExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/employments/$nino/${taxYear2023.asMtd}", method = DELETE, rel = DELETE_EMPLOYMENT_EXPENSES)
        Target.deleteEmploymentExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }

    "ignoreEmploymentExpenses" should {
      "return the correct link" in new Test {
        val link: Link = Link(href = s"/context/employments/$nino/${taxYear2023.asMtd}/ignore", method = POST, rel = IGNORE_EMPLOYMENT_EXPENSES)
        Target.ignoreEmploymentExpenses(mockAppConfig, nino, taxYear2023.asMtd) shouldBe link
      }
    }

  }

  object Target extends HateoasLinks
}
