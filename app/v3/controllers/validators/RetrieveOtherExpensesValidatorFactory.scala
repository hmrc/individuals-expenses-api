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
import cats.data.Validated._
import cats.implicits.catsSyntaxTuple2Semigroupal
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v3.controllers.validators.RetrieveOtherExpensesValidatorFactory.resolveTaxYear
import v3.models.request.retrieveOtherExpenses.RetrieveOtherExpensesRequestData

import javax.inject.Singleton

@Singleton
class RetrieveOtherExpensesValidatorFactory {

  def validator(nino: String, taxYear: String): Validator[RetrieveOtherExpensesRequestData] =
    new Validator[RetrieveOtherExpensesRequestData] {

      def validate: Validated[Seq[MtdError], RetrieveOtherExpensesRequestData] =
        (
          ResolveNino(nino),
          resolveTaxYear(taxYear)
        ).mapN(RetrieveOtherExpensesRequestData)

    }

}

object RetrieveOtherExpensesValidatorFactory {
  private val minimumTaxYear = TaxYear.ending(2022)
  private val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

}
