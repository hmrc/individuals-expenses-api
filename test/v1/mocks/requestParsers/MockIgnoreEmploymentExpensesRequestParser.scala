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

package v1.mocks.requestParsers

import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v1.controllers.requestParsers.IgnoreEmploymentExpensesRequestParser
import v1.models.errors.ErrorWrapper
import v1.models.request.ignoreEmploymentExpenses.{IgnoreEmploymentExpensesRawData, IgnoreEmploymentExpensesRequest}

trait MockIgnoreEmploymentExpensesRequestParser extends MockFactory {

  val mockRequestParser: IgnoreEmploymentExpensesRequestParser = mock[IgnoreEmploymentExpensesRequestParser]

  object MockIgnoreEmploymentExpensesRequestParser {

    def parseRequest(data: IgnoreEmploymentExpensesRawData): CallHandler[Either[ErrorWrapper, IgnoreEmploymentExpensesRequest]] = {
      (mockRequestParser.parseRequest(_: IgnoreEmploymentExpensesRawData)(_: String)).expects(data, *)
    }
  }

}
