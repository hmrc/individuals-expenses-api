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

package v1.controllers.requestParsers.validators

import api.controllers.requestParsers.validators.Validator
import api.controllers.requestParsers.validators.validations._
import api.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}
import config.AppConfig
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.request.createAndAmendOtherExpenses._

import javax.inject.Inject

class CreateAndAmendOtherExpensesValidator @Inject() (implicit currentDateTime: CurrentDateTime, appConfig: AppConfig, currentTaxYear: CurrentTaxYear)
    extends Validator[CreateAndAmendOtherExpensesRawData] {
  private val validationSet = List(parameterFormatValidation, bodyFormatValidation, parameterRuleValidation, bodyFieldValidation)

  private def parameterFormatValidation: CreateAndAmendOtherExpensesRawData => List[List[MtdError]] = (data: CreateAndAmendOtherExpensesRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def bodyFormatValidation: CreateAndAmendOtherExpensesRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[CreateAndAmendOtherExpensesBody](data.body, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def parameterRuleValidation: CreateAndAmendOtherExpensesRawData => List[List[MtdError]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, appConfig.otherExpensesMinimumTaxYear)
    )
  }

  private def bodyFieldValidation: CreateAndAmendOtherExpensesRawData => List[List[MtdError]] = { data =>
    val body = data.body.as[CreateAndAmendOtherExpensesBody]

    List(
      flattenErrors(
        List(
          body.paymentsToTradeUnionsForDeathBenefits.map(validatePaymentsToTradeUnionsForDeathBenefits).getOrElse(NoValidationErrors),
          body.patentRoyaltiesPayments.map(validatePatentRoyaltiesPayments).getOrElse(NoValidationErrors)
        )
      ))
  }

  private def validatePaymentsToTradeUnionsForDeathBenefits(
      paymentsToTradeUnionsForDeathBenefits: PaymentsToTradeUnionsForDeathBenefits): List[MtdError] = {
    List(
      CustomerReferenceValidation.validateOptional(
        field = paymentsToTradeUnionsForDeathBenefits.customerReference,
        path = s"/paymentsToTradeUnionsForDeathBenefits/customerReference"
      ),
      NumberValidation.validateOptional(
        field = Some(paymentsToTradeUnionsForDeathBenefits.expenseAmount),
        path = s"/paymentsToTradeUnionsForDeathBenefits/expenseAmount"
      )
    ).flatten
  }

  private def validatePatentRoyaltiesPayments(patentRoyaltiesPayments: PatentRoyaltiesPayments): List[MtdError] = {
    List(
      CustomerReferenceValidation.validateOptional(
        field = patentRoyaltiesPayments.customerReference,
        path = s"/patentRoyaltiesPayments/customerReference"
      ),
      NumberValidation.validateOptional(
        field = Some(patentRoyaltiesPayments.expenseAmount),
        path = s"/patentRoyaltiesPayments/expenseAmount"
      )
    ).flatten
  }

  override def validate(data: CreateAndAmendOtherExpensesRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }

}
