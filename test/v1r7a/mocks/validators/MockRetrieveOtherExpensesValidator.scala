/*
 * Copyright 2022 HM Revenue & Customs
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

package v1r7a.mocks.validators

import org.scalamock.handlers.CallHandler1
import org.scalamock.scalatest.MockFactory
import v1r7a.controllers.requestParsers.validators.RetrieveOtherExpensesValidator
import v1r7a.models.errors.MtdError
import v1r7a.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRawData

class MockRetrieveOtherExpensesValidator extends MockFactory {

  val mockValidator: RetrieveOtherExpensesValidator = mock[RetrieveOtherExpensesValidator]

  object MockRetrieveOtherExpensesValidator {

    def validate(data: RetrieveOtherExpensesRawData): CallHandler1[RetrieveOtherExpensesRawData, List[MtdError]] = {
      (mockValidator
        .validate(_: RetrieveOtherExpensesRawData))
        .expects(data)
    }
  }
}
