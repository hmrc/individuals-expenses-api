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

package api.models.domain

import common.domain.{DownstreamSource, MtdSource}
import support.UnitSpec
import utils.enums.EnumJsonSpecSupport

class MtdSourceSpec extends UnitSpec with EnumJsonSpecSupport {

  val mtdLatest   = MtdSource.`latest`
  val mtdCustomer = MtdSource.`user`
  val mtdHmrc     = MtdSource.`hmrcHeld`

  val desLatest       = DownstreamSource.`LATEST`
  val desCustomer     = DownstreamSource.`CUSTOMER`
  val desOutgoingHmrc = DownstreamSource.`HMRC-HELD`

  testRoundTrip[MtdSource](
    ("latest", MtdSource.`latest`),
    ("user", MtdSource.`user`),
    ("hmrcHeld", MtdSource.`hmrcHeld`)
  )

  "mtdSources" when {
    "when using the toDownstream" should {
      "return the correct DesSource for latest" in {
        mtdLatest.toDownstream shouldBe desLatest
      }
      "return the correct DesSource for customer" in {
        mtdCustomer.toDownstream shouldBe desCustomer
      }
      "return the correct DesSource for hmrcHeld" in {
        mtdHmrc.toDownstream shouldBe desOutgoingHmrc
      }
    }
  }

}
