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
import cats.implicits._
import common.error.CustomerReferenceFormatError
import play.api.libs.json.JsValue
import shared.controllers.validators.Validator
import shared.controllers.validators.resolvers.{ResolveNino, ResolveNonEmptyJsonObject, ResolveParsedNumber, ResolveTaxYearMinimum}
import shared.models.domain.TaxYear
import shared.models.errors.MtdError
import v3.models.request.createAndAmendOtherExpenses.{CreateAndAmendOtherExpensesBody, CreateAndAmendOtherExpensesRequestData}

import javax.inject.Singleton

@Singleton
class CreateAndAmendOtherExpensesValidatorFactory {

  private val minimumTaxYear = TaxYear.ending(2022)

  private val resolveJson = new ResolveNonEmptyJsonObject[CreateAndAmendOtherExpensesBody]()

  private val resolveParsedNumber = ResolveParsedNumber()

  def validator(nino: String, taxYear: String, body: JsValue): Validator[CreateAndAmendOtherExpensesRequestData] =
    new Validator[CreateAndAmendOtherExpensesRequestData] {

      private val resolveTaxYear = ResolveTaxYearMinimum(minimumTaxYear)

      def validate: Validated[Seq[MtdError], CreateAndAmendOtherExpensesRequestData] =
        (
          ResolveNino(nino),
          resolveTaxYear(taxYear),
          resolveJson(body)
        ).mapN(CreateAndAmendOtherExpensesRequestData.apply) andThen validateBusinessRules

      private val customerRefRegex = "^[0-9a-zA-Z{À-˿’}\\- _&`():.'^]{1,90}$".r

      private def validateBusinessRules(
          parsed: CreateAndAmendOtherExpensesRequestData): Validated[Seq[MtdError], CreateAndAmendOtherExpensesRequestData] = {
        import parsed.body._

        val validatedExpensesAmounts = List(
          (paymentsToTradeUnionsForDeathBenefits.map(_.expenseAmount), "/paymentsToTradeUnionsForDeathBenefits/expenseAmount"),
          (patentRoyaltiesPayments.map(_.expenseAmount), "/patentRoyaltiesPayments/expenseAmount")
        ).traverse_ { case (value, path) => resolveParsedNumber(value, path = path) }

        val validatedCustomerReferences = List(
          (paymentsToTradeUnionsForDeathBenefits.map(_.customerReference), "/paymentsToTradeUnionsForDeathBenefits/customerReference"),
          (patentRoyaltiesPayments.map(_.customerReference), "/patentRoyaltiesPayments/customerReference")
        ).traverse_ { case (maybeValue, path) => maybeValue.map(validate(_, path)).getOrElse(Valid(())) }

        List(validatedExpensesAmounts, validatedCustomerReferences).traverse_(identity).map(_ => parsed)
      }

      private def validate(maybeCustomerRef: Option[String], path: String): Validated[Seq[MtdError], Unit] =
        if (maybeCustomerRef.forall(customerRefRegex.matches)) {
          Valid(())
        } else {
          Invalid(List(CustomerReferenceFormatError.withPath(path)))
        }

    }

}
