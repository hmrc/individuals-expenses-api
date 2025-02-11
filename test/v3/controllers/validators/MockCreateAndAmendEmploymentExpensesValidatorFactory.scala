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

package v3.controllers.validators

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import org.scalamock.handlers.CallHandler
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.models.errors.MtdError
import v3.models.request.createAndAmendEmploymentExpenses.CreateAndAmendEmploymentExpensesRequestData

import java.time.Clock

trait MockCreateAndAmendEmploymentExpensesValidatorFactory extends MockFactory {

  val mockCreateAndAmendEmploymentExpensesValidatorFactory: CreateAndAmendEmploymentExpensesValidatorFactory =
    mock[CreateAndAmendEmploymentExpensesValidatorFactory]

  object MockedCreateAndAmendEmploymentExpensesValidatorFactory {

    def validator(): CallHandler[Validator[CreateAndAmendEmploymentExpensesRequestData]] =
      (mockCreateAndAmendEmploymentExpensesValidatorFactory.validator(_: String, _: String, _: JsValue, _: Boolean)(_: Clock)).expects(*, *, *, *, *)

  }

  def willUseValidator(
      use: Validator[CreateAndAmendEmploymentExpensesRequestData]): CallHandler[Validator[CreateAndAmendEmploymentExpensesRequestData]] = {
    MockedCreateAndAmendEmploymentExpensesValidatorFactory
      .validator()
      .anyNumberOfTimes()
      .returns(use)
  }

  def returningSuccess(result: CreateAndAmendEmploymentExpensesRequestData): Validator[CreateAndAmendEmploymentExpensesRequestData] =
    new Validator[CreateAndAmendEmploymentExpensesRequestData] {
      def validate: Validated[Seq[MtdError], CreateAndAmendEmploymentExpensesRequestData] = Valid(result)
    }

  def returning(result: MtdError*): Validator[CreateAndAmendEmploymentExpensesRequestData] = returningErrors(result)

  def returningErrors(result: Seq[MtdError]): Validator[CreateAndAmendEmploymentExpensesRequestData] =
    new Validator[CreateAndAmendEmploymentExpensesRequestData] {
      def validate: Validated[Seq[MtdError], CreateAndAmendEmploymentExpensesRequestData] = Invalid(result)
    }

}
