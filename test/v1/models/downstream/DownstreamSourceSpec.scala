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

package v1.models.downstream

import support.UnitSpec
import utils.enums.EnumJsonSpecSupport
import v1.models.domain.MtdSource

class DownstreamSourceSpec extends UnitSpec with EnumJsonSpecSupport {

  val mtdLatest   = MtdSource.`latest`
  val mtdCustomer = MtdSource.`user`
  val mtdHmrc     = MtdSource.`hmrcHeld`

  val desLatest       = DownstreamSource.`LATEST`
  val desCustomer     = DownstreamSource.`CUSTOMER`
  val desHmrc         = DownstreamSource.`HMRC HELD`
  val desOutgoingHmrc = DownstreamSource.`HMRC-HELD`

  testRoundTrip[DownstreamSource](
    ("LATEST", DownstreamSource.`LATEST`),
    ("CUSTOMER", DownstreamSource.`CUSTOMER`),
    ("HMRC HELD", DownstreamSource.`HMRC HELD`),
    ("HMRC-HELD", DownstreamSource.`HMRC-HELD`)
  )

  "mtdSources" when {
    "when using the toDownstream" should {
      "return the correct DesSource for latest" in {
        desLatest.toMtd shouldBe mtdLatest
      }
      "return the correct DesSource for customer" in {
        desCustomer.toMtd shouldBe mtdCustomer
      }
      "return the correct DesSource for hmrcHeld" in {
        desHmrc.toMtd shouldBe mtdHmrc
      }
      "return the correct DesSource for outgoingHmrcHeld" in {
        desOutgoingHmrc.toMtd shouldBe mtdHmrc
      }
    }
  }

}
