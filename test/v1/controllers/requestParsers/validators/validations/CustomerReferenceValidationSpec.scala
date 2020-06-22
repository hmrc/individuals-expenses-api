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

package v1.controllers.requestParsers.validators.validations

import support.UnitSpec
import v1.models.errors.CustomerReferenceFormatError

class CustomerReferenceValidationSpec extends UnitSpec {

  val validReference: Option[String] = Some("validReference")
  val smallestAllowedReference: Option[String] = Some("")
  val largestAllowedReference: Option[String] = Some("abcdefghijklmnopqrstuvwxy")
  val invalidReference: Option[String] = Some("abcdefghijklmnopqrstuvwxyz")

  "validate" should {
    "return no errors" when {
      "a valid reference is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(validReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
      "no reference is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(None, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
      "the smallest allowed reference (0) is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(smallestAllowedReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
      "the largest allowed reference (25) is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(largestAllowedReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
    }

    "return an error" when {
      "an invalid reference is supplied (26)" in {
        val validationResult = CustomerReferenceValidation.validateOptional(invalidReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe CustomerReferenceFormatError.copy(paths = Some(Seq("/vctSubscription/1/customerReference")))
      }
    }
  }
}