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

import config.AppConfig
import javax.inject.Inject
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, SourceValidation, TaxYearValidation}
import v1.models.errors.MtdError
import v1.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRawData

class RetrieveEmploymentExpensesValidator @Inject()(implicit currentDateTime: CurrentDateTime, appConfig: AppConfig, currentTaxYear: CurrentTaxYear)
  extends Validator[RetrieveEmploymentsExpensesRawData] {

  private val validationSet = List(parameterFormatValidation, parameterRuleValidation)

  private def parameterFormatValidation: RetrieveEmploymentsExpensesRawData => List[List[MtdError]] = (data: RetrieveEmploymentsExpensesRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear),
      SourceValidation.validate(data.source)
    )
  }

  private def parameterRuleValidation: RetrieveEmploymentsExpensesRawData => List[List[MtdError]] = (data: RetrieveEmploymentsExpensesRawData) => {
    List(
      MtdTaxYearValidation.validate(data.taxYear, appConfig.employmentExpensesMinimumTaxYear)
    )
  }

  override def validate(data: RetrieveEmploymentsExpensesRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }
}