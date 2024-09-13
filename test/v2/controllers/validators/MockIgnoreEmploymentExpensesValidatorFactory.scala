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

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import v2.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRequestData

import java.time.Clock

trait MockIgnoreEmploymentExpensesValidatorFactory extends MockFactory {

  val mockIgnoreEmploymentExpensesValidatorFactory: IgnoreEmploymentExpensesValidatorFactory =
    mock[IgnoreEmploymentExpensesValidatorFactory]

  object MockedIgnoreEmploymentExpensesValidatorFactory {

    def validator(): CallHandler[Validator[IgnoreEmploymentExpensesRequestData]] =
      (mockIgnoreEmploymentExpensesValidatorFactory.validator(_: String, _: String, _: Boolean)(_: Clock)).expects(*, *, *, *)

  }

  def willUseValidator(use: Validator[IgnoreEmploymentExpensesRequestData]): CallHandler[Validator[IgnoreEmploymentExpensesRequestData]] = {
    MockedIgnoreEmploymentExpensesValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: IgnoreEmploymentExpensesRequestData): Validator[IgnoreEmploymentExpensesRequestData] =
    new Validator[IgnoreEmploymentExpensesRequestData] {
      def validate: Validated[Seq[MtdError], IgnoreEmploymentExpensesRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[IgnoreEmploymentExpensesRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[IgnoreEmploymentExpensesRequestData] =
    new Validator[IgnoreEmploymentExpensesRequestData] {
      def validate: Validated[Seq[MtdError], IgnoreEmploymentExpensesRequestData] = Invalid(result)
    }

}
