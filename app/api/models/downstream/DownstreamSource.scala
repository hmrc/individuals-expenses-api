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

package api.models.downstream

import api.models.domain.MtdSource
import play.api.libs.json._
import utils.enums.Enums

sealed trait DownstreamSource {
  def toMtd: MtdSource
}

object DownstreamSource {

  case object `HMRC HELD` extends DownstreamSource {
    override def toMtd: MtdSource = MtdSource.`hmrcHeld`
  }

  case object `CUSTOMER` extends DownstreamSource {
    override def toMtd: MtdSource = MtdSource.`user`
  }

  case object `LATEST` extends DownstreamSource {
    override def toMtd: MtdSource = MtdSource.`latest`
  }

  case object `HMRC-HELD` extends DownstreamSource {
    override def toMtd: MtdSource = MtdSource.`hmrcHeld`
  }

  implicit val format: Format[DownstreamSource] = Enums.format[DownstreamSource]
  val parser: PartialFunction[String, DownstreamSource] = Enums.parser[DownstreamSource]
}
