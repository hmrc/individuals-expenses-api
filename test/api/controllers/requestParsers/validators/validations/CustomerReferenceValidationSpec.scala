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

package api.controllers.requestParsers.validators.validations

import api.models.errors.CustomerReferenceFormatError
import support.UnitSpec

class CustomerReferenceValidationSpec extends UnitSpec {

  val validReference: Option[String]           = Some("validReference")
  val smallestAllowedReference: Option[String] = Some("a")
  val largestAllowedReference: Option[String]  = Some("abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijknine")

  val invalidReferenceSmallestLength: Option[String] = Some("")

  val invalidReferenceLength: Option[String] = Some(
    "abcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijklmnopqrstuvwxyabcdefghijknineandthensome")

  val invalidReferenceCharacter: Option[String] = Some("REFERENCE\\EXAMPLE")

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
      "the smallest allowed reference (1) is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(smallestAllowedReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
      "the largest allowed reference (90) is supplied" in {
        val validationResult = CustomerReferenceValidation.validateOptional(largestAllowedReference, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe true
      }
    }

    "return an error" when {
      "an invalid reference is supplied (too short)" in {
        val validationResult = CustomerReferenceValidation.validateOptional(invalidReferenceSmallestLength, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe CustomerReferenceFormatError.copy(paths = Some(Seq("/vctSubscription/1/customerReference")))
      }
      "an invalid reference is supplied (too long)" in {
        val validationResult = CustomerReferenceValidation.validateOptional(invalidReferenceLength, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe CustomerReferenceFormatError.copy(paths = Some(Seq("/vctSubscription/1/customerReference")))
      }
      "an invalid reference is supplied (\\ character)" in {
        val validationResult = CustomerReferenceValidation.validateOptional(invalidReferenceCharacter, "/vctSubscription/1/customerReference")
        validationResult.isEmpty shouldBe false
        validationResult.length shouldBe 1
        validationResult.head shouldBe CustomerReferenceFormatError.copy(paths = Some(Seq("/vctSubscription/1/customerReference")))
      }
    }
  }

}
