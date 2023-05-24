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

import api.controllers.requestParsers.validators.validations.{MtdTaxYearValidation, NinoValidation, TaxYearValidation}
import api.controllers.requestValidators.RequestValidator
import api.models.domain.{Nino, TaxYear}
import api.models.errors.MtdError
import config.AppConfig
import utils.{CurrentDateTime, CurrentTaxYear}

import v1.models.request.ignoreEmploymentExpenses.{IgnoreEmploymentExpensesRawData, IgnoreEmploymentExpensesRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class IgnoreEmploymentExpensesRequestValidator @Inject() (appConfig: AppConfig)(implicit
    currentDateTime: CurrentDateTime,
    currentTaxYear: CurrentTaxYear)
    extends RequestValidator[IgnoreEmploymentExpensesRawData, IgnoreEmploymentExpensesRequest] {

  override protected def requestFor(data: IgnoreEmploymentExpensesRawData): IgnoreEmploymentExpensesRequest =
    IgnoreEmploymentExpensesRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear))

  override protected def validationSet: List[IgnoreEmploymentExpensesRawData => List[List[MtdError]]] =
    List(parameterFormatValidation, parameterRuleValidation)

  private def parameterFormatValidation: IgnoreEmploymentExpensesRawData => List[List[MtdError]] = (data: IgnoreEmploymentExpensesRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: IgnoreEmploymentExpensesRawData => List[List[MtdError]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, appConfig.employmentExpensesMinimumTaxYear, checkCurrentTaxYear = data.temporalValidationEnabled)
    )
  }

}
