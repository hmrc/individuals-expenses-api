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

package common.domain

import api.utils.enums.Enums
import play.api.libs.json.*

enum MtdSource {
  case hmrcHeld, user, latest

  def toDownstream: String = this match
    case MtdSource.hmrcHeld => "HMRC-HELD"
    case MtdSource.user     => "CUSTOMER"
    case MtdSource.latest   => "LATEST"

}

object MtdSource {
  val parser: PartialFunction[String, MtdSource] = Enums.parser(values)

  given Format[MtdSource] = Enums.format(values)
}
