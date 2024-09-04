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

package v2.controllers.validators

import api.controllers.validators.Validator
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import v2.models.request.deleteOtherExpenses.DeleteOtherExpensesRequestData

trait MockDeleteOtherExpensesValidatorFactory extends MockFactory {

  val mockDeleteOtherExpensesValidatorFactory: DeleteOtherExpensesValidatorFactory =
    mock[DeleteOtherExpensesValidatorFactory]

  object MockedDeleteOtherExpensesValidatorFactory {

    def validator(): CallHandler[Validator[DeleteOtherExpensesRequestData]] =
      (mockDeleteOtherExpensesValidatorFactory.validator(_: String, _: String)).expects(*, *)

  }

  def willUseValidator(use: Validator[DeleteOtherExpensesRequestData]): CallHandler[Validator[DeleteOtherExpensesRequestData]] = {
    MockedDeleteOtherExpensesValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: DeleteOtherExpensesRequestData): Validator[DeleteOtherExpensesRequestData] =
    new Validator[DeleteOtherExpensesRequestData] {
      def validate: Validated[Seq[MtdError], DeleteOtherExpensesRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[DeleteOtherExpensesRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[DeleteOtherExpensesRequestData] =
    new Validator[DeleteOtherExpensesRequestData] {
      def validate: Validated[Seq[MtdError], DeleteOtherExpensesRequestData] = Invalid(result)
    }

}
