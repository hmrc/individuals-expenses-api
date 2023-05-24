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

package v1.controllers.requestValidators

import api.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, SourceValidation, TaxYearValidation}
import api.controllers.requestValidators.RequestValidator
import api.models.domain.{MtdSource, Nino, TaxYear}
import api.models.errors.MtdError
import config.AppConfig
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class RetrieveEmploymentsExpensesRequestValidator @Inject() (appConfig: AppConfig)(implicit
    currentDateTime: CurrentDateTime,
    currentTaxYear: CurrentTaxYear)
    extends RequestValidator[RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest] {

  override protected def requestFor(data: RetrieveEmploymentsExpensesRawData): RetrieveEmploymentsExpensesRequest =
    RetrieveEmploymentsExpensesRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear), MtdSource.parser(data.source))

  override protected def validationSet: List[RetrieveEmploymentsExpensesRawData => List[List[MtdError]]] =
    List(parameterFormatValidation, parameterRuleValidation)

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

}
