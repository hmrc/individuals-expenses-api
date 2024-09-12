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
import cats.data.Validated._
import cats.implicits.catsSyntaxTuple2Semigroupal
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveIncompleteTaxYear, ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v2.models.request.ignoreEmploymentExpenses.IgnoreEmploymentExpensesRequestData

import java.time.Clock
import javax.inject.{Inject, Singleton}

@Singleton
class IgnoreEmploymentExpensesValidatorFactory @Inject() (implicit clock: Clock = Clock.systemUTC) {

  private val minimumTaxYear = TaxYear.starting(2020)

  def validator(nino: String, taxYear: String, temporalValidationEnabled: Boolean): Validator[IgnoreEmploymentExpensesRequestData] =
    new Validator[IgnoreEmploymentExpensesRequestData] {

      private lazy val resolvedTaxYear = {
        ResolveTaxYearMinimum(minimumTaxYear)(taxYear) andThen { parsedTaxYear =>
          if (temporalValidationEnabled) ResolveIncompleteTaxYear().resolver(taxYear) else Valid(parsedTaxYear)
        }
      }

      def validate: Validated[Seq[MtdError], IgnoreEmploymentExpensesRequestData] = {
        (
          ResolveNino(nino),
          resolvedTaxYear
        ).mapN(IgnoreEmploymentExpensesRequestData)
      }

    }

}
