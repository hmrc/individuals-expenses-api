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
import api.controllers.validators.resolvers.{DetailedResolveTaxYear, ResolveNino, ResolveNonEmptyJsonObject, ResolveParsedNumber}
import api.models.domain.{TaxYear, TodaySupplier}
import api.models.errors.MtdError
import cats.data.Validated
import cats.data.Validated._
import cats.implicits._
import play.api.libs.json.JsValue
import v2.models.request.createAndAmendEmploymentExpenses.{CreateAndAmendEmploymentExpensesBody, CreateAndAmendEmploymentExpensesRequestData}

import javax.inject.{Inject, Singleton}

@Singleton
class CreateAndAmendEmploymentExpensesValidatorFactory @Inject() (implicit todaySupplier: TodaySupplier = new TodaySupplier) {

  private val resolveJson = new ResolveNonEmptyJsonObject[CreateAndAmendEmploymentExpensesBody]()

  private val resolveParsedNumber = ResolveParsedNumber()

  def validator(nino: String,
                taxYear: String,
                body: JsValue,
                temporalValidationEnabled: Boolean): Validator[CreateAndAmendEmploymentExpensesRequestData] =
    new Validator[CreateAndAmendEmploymentExpensesRequestData] {

      private val resolveTaxYear = DetailedResolveTaxYear(
        allowIncompleteTaxYear = !temporalValidationEnabled,
        maybeMinimumTaxYear = Some(TaxYear.employmentExpensesMinimumTaxYear))

      def validate: Validated[Seq[MtdError], CreateAndAmendEmploymentExpensesRequestData] =
        (
          ResolveNino(nino),
          resolveTaxYear(taxYear),
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
          resolveParsedNumber(value, path = Some(path))
        }.map(_ => parsed)
      }

    }

}
