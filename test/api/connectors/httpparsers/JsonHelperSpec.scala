/*
 * Copyright 2026 HM Revenue & Customs
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

package api.connectors.httpparsers

import api.utils.UnitSpec
import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

class JsonHelperSpec extends UnitSpec with HttpParser with LogCapturing {

  private case class TestModel(value: String)
  private implicit val testReads: Reads[TestModel] = Json.reads[TestModel]

  private def httpResponseWithBody(body: String): HttpResponse =
    HttpResponse(status = 200, body = body)

  "validateJson" when {
    "response JSON is valid and matches the expected model" should {
      "return the parsed model" in {
        val response = httpResponseWithBody("""{"value":"hello"}""")
        response.validateJson[TestModel] shouldBe Some(TestModel("hello"))
      }
    }

    "response JSON is valid but does not match the expected model" should {
      "return None" in {
        val response = httpResponseWithBody("""{"unexpected":"field"}""")
        response.validateJson[TestModel] shouldBe None
      }
    }

    "response body is not valid JSON" should {
      "return None" in {
        val response = httpResponseWithBody("not-json")
        response.validateJson[TestModel] shouldBe None
      }
    }
  }

  "validateJsonWithLogging" when {
    "response JSON is valid and matches the expected model" should {
      "return the parsed model" in {
        val response = httpResponseWithBody("""{"value":"hello"}""")
        response.validateJsonWithLogging[TestModel] shouldBe Some(TestModel("hello"))
      }
    }

    "JSON validation fails due to type mismatch" should {
      "return None and log a warning with validation errors" in {
        val response = httpResponseWithBody("""{"unexpected":"field"}""")
        withCaptureOfLoggingFrom(logger) { logs =>
          response.validateJsonWithLogging[TestModel] shouldBe None

          logs.head.getMessage should startWith("[JsonResponseHelper][validateJsonWithLogging] JSON validation failed:")
        }
      }
    }

    "response body is not valid JSON" should {
      "return None and log a warning" in {
        val response = httpResponseWithBody("not-json")
        withCaptureOfLoggingFrom(logger) { logs =>
          response.validateJsonWithLogging[TestModel] shouldBe None

          logs.head.getMessage shouldBe "[JsonResponseHelper][validateJsonWithLogging] Response body is not valid JSON"
        }
      }
    }
  }

}
