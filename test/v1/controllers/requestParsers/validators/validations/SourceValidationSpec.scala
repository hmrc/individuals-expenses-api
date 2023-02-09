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

package v1.controllers.requestParsers.validators.validations

import api.models.errors.SourceFormatError
import support.UnitSpec

class SourceValidationSpec extends UnitSpec {

  "validate" should {
    "return no errors" when {
      "latest is supplied" in {

        val validSource      = "latest"
        val validationResult = SourceValidation.validate(validSource)
        validationResult.isEmpty shouldBe true

      }
      "hmrcHeld is supplied" in {

        val validSource      = "hmrcHeld"
        val validationResult = SourceValidation.validate(validSource)
        validationResult.isEmpty shouldBe true

      }
      "user is supplied" in {

        val validSource      = "user"
        val validationResult = SourceValidation.validate(validSource)
        validationResult.isEmpty shouldBe true

      }
    }

    "return an error" when {
      "an invalid source is supplied" in {

        val invalidSource    = "Walrus"
        val validationResult = SourceValidation.validate(invalidSource)
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe SourceFormatError

      }
    }
  }

}
