/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.models.des

import support.UnitSpec
import v1.models.domain.MtdSource

class DesSourceSpec extends UnitSpec{

  val mtdLatest = MtdSource.`latest`
  val mtdCustomer = MtdSource.`user`
  val mtdHmrc = MtdSource.`hmrcHeld`

  val desLatest = DesSource.`LATEST`
  val desCustomer = DesSource.`CUSTOMER`
  val desHmrc = DesSource.`HMRC HELD`

  "mtdSources" when {
    "when using the toDes" should {
      "return the correct DesSource for latest" in {
        desLatest.toMtd shouldBe mtdLatest
      }
      "return the correct DesSource for customer" in {
        desCustomer.toMtd shouldBe mtdCustomer
      }
      "return the correct DesSource for hmrcHeld" in {
        desHmrc.toMtd shouldBe mtdHmrc
      }
    }
  }
}
