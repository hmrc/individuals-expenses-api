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

package v1.models.des

import play.api.libs.json._
import utils.enums.Enums
import v1.models.domain.MtdSource

sealed trait DesSource {
  def toMtd: MtdSource
}

object DesSource {

  case object `HMRC HELD` extends DesSource {
    override def toMtd: MtdSource = MtdSource.`hmrcHeld`
  }

  case object `CUSTOMER` extends DesSource {
    override def toMtd: MtdSource = MtdSource.`user`
  }

  case object `LATEST` extends DesSource {
    override def toMtd: MtdSource = MtdSource.`latest`
  }
  
  case object `HMRC-HELD` extends DesSource {
    override def toMtd: MtdSource = MtdSource.`hmrcHeld`
  }

  implicit val format: Format[DesSource] = Enums.format[DesSource]
  val parser: PartialFunction[String, DesSource] = Enums.parser[DesSource]
}

