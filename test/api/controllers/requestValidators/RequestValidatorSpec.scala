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

package api.controllers.requestValidators

import api.models.domain.Nino
import api.models.errors.{BadRequestError, ErrorWrapper, MtdError, NinoFormatError, RuleIncorrectOrEmptyBodyError}
import api.models.request.RawData
import org.scalamock.scalatest.MockFactory
import support.UnitSpec

class RequestValidatorSpec extends UnitSpec with MockFactory {

  private val nino                   = "AA123456A"
  implicit val correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"

  case class RawRequest(nino: String) extends RawData
  case class ParsedRequest(nino: Nino)

  trait Test {
    test =>

    val validationSet: List[RawRequest => List[List[MtdError]]]

    val requestValidator: RequestValidator[RawRequest, ParsedRequest] = new RequestValidator[RawRequest, ParsedRequest] {
      protected def validationSet: List[RawRequest => List[List[MtdError]]] = test.validationSet
      protected def requestFor(data: RawRequest): ParsedRequest             = ParsedRequest(Nino(data.nino))
    }

  }

  "parseRequest" should {
    "return the parsed request data" when {
      "the validator returns no errors" in new Test {
        val validationSet: List[RawRequest => List[List[MtdError]]] = List(_ => List(Nil))

        requestValidator.parseRequest(RawRequest(nino)) shouldBe Right(ParsedRequest(Nino(nino)))
      }
    }

    "return a single error" when {
      "the validator returns a single error" in new Test {
        val validationSet: List[RawRequest => List[List[MtdError]]] = List(_ => List(List(NinoFormatError)))

        requestValidator.parseRequest(RawRequest(nino)) shouldBe Left(ErrorWrapper(correlationId, NinoFormatError, None))
      }
    }

    "return multiple errors" when {
      "the validator returns multiple errors" in new Test {
        val validationSet: List[RawRequest => List[List[MtdError]]] =
          List(_ => List(List(NinoFormatError, RuleIncorrectOrEmptyBodyError)))

        requestValidator.parseRequest(RawRequest(nino)) shouldBe Left(
          ErrorWrapper(correlationId, BadRequestError, Some(Seq(NinoFormatError, RuleIncorrectOrEmptyBodyError))))
      }
    }
  }

}
