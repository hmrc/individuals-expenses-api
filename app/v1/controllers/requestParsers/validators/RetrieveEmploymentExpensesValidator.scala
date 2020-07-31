/*
 * Copyright 2020 HM Revenue & Customs
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

import v1.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, TaxYearValidation}
import v1.models.errors.{MtdError, RuleTaxYearNotSupportedError}

class RetrieveEmploymentExpensesValidator extends Validator[RetrieveEmploymentExpensesRawData] {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation)

  private def parameterFormatValidation: RetrieveEmploymentExpensesRawData => List[List[MtdError]] = (data: RetrieveEmploymentExpensesRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: RetrieveEmploymentExpensesRawData => List[List[MtdError]] = (data: RetrieveEmploymentExpensesRawData) => {
    List(
      MtdTaxYearValidation.validate(data.taxYear, RuleTaxYearNotSupportedError)
    )
  }

  override def validate(data: RetrieveEmploymentExpensesRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }
}