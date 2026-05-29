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

package v3.controllers.validators.resolvers

import cats.data.Validated.{Invalid, Valid}
import common.domain.MtdSource.{`hmrcHeld`, `latest`, `user`}
import common.error.SourceFormatError
import shared.utils.UnitSpec

class ResolveMtdSourceSpec extends UnitSpec {

  "ResolveMtdSource" should {
    List(("latest", `latest`), ("user", `user`), ("hmrcHeld", `hmrcHeld`))
      .foreach { case (code, source) =>
        s"return an empty list for valid country code $code" in {
          val result = ResolveMtdSource(code)
          result shouldBe Valid(source)
        }
      }

    "return a SourceFormatError for an invalid source" in {
      val result = ResolveMtdSource("notASource")
      result shouldBe Invalid(List(SourceFormatError))
    }
  }

}
