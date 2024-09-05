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

package routing

import play.api.http.HeaderNames.ACCEPT
import play.api.libs.json._
import play.api.test.FakeRequest
import routing.Version.VersionReads
import support.UnitSpec

class VersionSpec extends UnitSpec {

  "serialized to Json" must {
    "return the expected Json output" in {
      val version: Version = Version2
      val expected         = Json.parse(""" "2.0" """)
      val result           = Json.toJson(version)
      result shouldBe expected
    }
  }

  "Versions" when {
    "retrieved from a request header" should {
      "return Version for valid header" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))) shouldBe Right(Version2)
      }

      "return InvalidHeader when the version header is missing" in {
        Versions.getFromRequest(FakeRequest().withHeaders()) shouldBe Left(InvalidHeader)
      }

      "return VersionNotFound for unrecognised version" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "application/vnd.hmrc.0.0+json"))) shouldBe Left(VersionNotFound)
      }

      "return InvalidHeader for a header format that doesn't match regex" in {
        Versions.getFromRequest(FakeRequest().withHeaders((ACCEPT, "invalidHeaderFormat"))) shouldBe Left(InvalidHeader)
      }
    }
  }

  "VersionReads" should {
    "successfully read Version2" in {
      val versionJson: JsValue      = JsString(Version2.name)
      val result: JsResult[Version] = VersionReads.reads(versionJson)

      result shouldEqual JsSuccess(Version2)
    }

    "return error for unrecognised version" in {
      val versionJson: JsValue      = JsString("UnknownVersion")
      val result: JsResult[Version] = VersionReads.reads(versionJson)

      result shouldBe a[JsError]
    }
  }

  "toString" should {
    "return the version name" in {
      val result = Version2.toString
      result shouldBe Version2.name
    }
  }

}
