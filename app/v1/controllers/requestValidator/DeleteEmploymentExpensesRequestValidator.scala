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

package v1.controllers.requestValidator

import api.controllers.requestParsers.validators.validations.{NinoValidation, TaxYearNotSupportedValidation, TaxYearValidation}
import api.controllers.requestValidators.RequestValidator
import api.models.domain.{Nino, TaxYear}
import api.models.errors.MtdError
import config.AppConfig
import v1.models.request.deleteEmploymentExpenses.{DeleteEmploymentExpensesRawData, DeleteEmploymentExpensesRequest}

import javax.inject.{Inject, Singleton}

@Singleton
class DeleteEmploymentExpensesRequestValidator @Inject() (appConfig: AppConfig)
    extends RequestValidator[DeleteEmploymentExpensesRawData, DeleteEmploymentExpensesRequest] {

  override protected def validationSet: List[DeleteEmploymentExpensesRawData => List[List[MtdError]]] =
    List(parameterFormatValidation, parameterRuleValidation)

  private def parameterFormatValidation: DeleteEmploymentExpensesRawData => List[List[MtdError]] =
    (data: DeleteEmploymentExpensesRawData) => {
      List(
        NinoValidation.validate(data.nino),
        TaxYearValidation.validate(data.taxYear)
      )
    }

  private def parameterRuleValidation: DeleteEmploymentExpensesRawData => List[List[MtdError]] =
    (data: DeleteEmploymentExpensesRawData) => {
      List(
        TaxYearNotSupportedValidation.validate(data.taxYear, appConfig.employmentExpensesMinimumTaxYear)
      )
    }

  override protected def requestFor(data: DeleteEmploymentExpensesRawData): DeleteEmploymentExpensesRequest =
    DeleteEmploymentExpensesRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear))

}
