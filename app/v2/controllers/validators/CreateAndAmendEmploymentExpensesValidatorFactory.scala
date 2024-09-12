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
import cats.implicits._
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers._
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v2.models.request.createAndAmendEmploymentExpenses.{CreateAndAmendEmploymentExpensesBody, CreateAndAmendEmploymentExpensesRequestData}

import java.time.Clock
import javax.inject.{Inject, Singleton}

@Singleton
class CreateAndAmendEmploymentExpensesValidatorFactory @Inject() (implicit clock: Clock = Clock.systemUTC) {

  private val resolveJson    = new ResolveNonEmptyJsonObject[CreateAndAmendEmploymentExpensesBody]()
  private val minimumTaxYear = TaxYear.starting(2020)

  private val resolveParsedNumber = ResolveParsedNumber()

  def validator(nino: String,
                taxYear: String,
                body: JsValue,
                temporalValidationEnabled: Boolean): Validator[CreateAndAmendEmploymentExpensesRequestData] =
    new Validator[CreateAndAmendEmploymentExpensesRequestData] {

      private lazy val resolvedTaxYear = {
        ResolveTaxYearMinimum(minimumTaxYear)(taxYear) andThen { parsedTaxYear =>
          if (temporalValidationEnabled) ResolveIncompleteTaxYear().resolver(taxYear) else Valid(parsedTaxYear)
        }
      }

      def validate: Validated[Seq[MtdError], CreateAndAmendEmploymentExpensesRequestData] =
        (
          ResolveNino(nino),
          resolvedTaxYear,
          resolveJson(body)
        ).mapN(CreateAndAmendEmploymentExpensesRequestData) andThen validateBusinessRules

      private def validateBusinessRules(
          parsed: CreateAndAmendEmploymentExpensesRequestData): Validated[Seq[MtdError], CreateAndAmendEmploymentExpensesRequestData] = {
        import parsed.body.expenses._

        List(
          (businessTravelCosts, "/expenses/businessTravelCosts"),
          (jobExpenses, "/expenses/jobExpenses"),
          (flatRateJobExpenses, "/expenses/flatRateJobExpenses"),
          (professionalSubscriptions, "/expenses/professionalSubscriptions"),
          (hotelAndMealExpenses, "/expenses/hotelAndMealExpenses"),
          (otherAndCapitalAllowances, "/expenses/otherAndCapitalAllowances"),
          (vehicleExpenses, "/expenses/vehicleExpenses"),
          (mileageAllowanceRelief, "/expenses/mileageAllowanceRelief")
        ).traverse_ { case (value, path) =>
          resolveParsedNumber(value, path = path)
        }.map(_ => parsed)
      }

    }

}
