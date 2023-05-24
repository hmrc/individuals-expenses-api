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

import api.controllers.requestParsers.validators.validations._
import api.controllers.requestValidators.RequestValidator
import api.models.domain.{Nino, TaxYear}
import api.models.errors.{MtdError, RuleIncorrectOrEmptyBodyError}
import config.AppConfig
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.request.createAndAmendEmploymentExpenses._

import javax.inject.{Inject, Singleton}

@Singleton
class CreateAmendEmploymentExpensesRequestValidator @Inject() (appConfig: AppConfig)(implicit
    currentDateTime: CurrentDateTime,
    currentTaxYear: CurrentTaxYear)
    extends RequestValidator[CreateAndAmendEmploymentExpensesRawData, CreateAndAmendEmploymentExpensesRequest] {

  override protected def requestFor(data: CreateAndAmendEmploymentExpensesRawData): CreateAndAmendEmploymentExpensesRequest =
    CreateAndAmendEmploymentExpensesRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear), data.body.as[CreateAndAmendEmploymentExpensesBody])

  override protected def validationSet: List[CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]]] =
    List(parameterFormatValidation, bodyFormatValidation, parameterRuleValidation, incorrectOrEmptyBodySubmittedValidation, bodyFieldValidation)

  private def parameterFormatValidation: CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]] =
    (data: CreateAndAmendEmploymentExpensesRawData) => {
      List(
        NinoValidation.validate(data.nino),
        TaxYearValidation.validate(data.taxYear)
      )
    }

  private def bodyFormatValidation: CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[CreateAndAmendEmploymentExpensesBody](data.body, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def parameterRuleValidation: CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]] = { data =>
    List(
      MtdTaxYearValidation.validate(data.taxYear, appConfig.employmentExpensesMinimumTaxYear, data.temporalValidationEnabled)
    )
  }

  private def incorrectOrEmptyBodySubmittedValidation: CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]] = { data =>
    val body = data.body.as[CreateAndAmendEmploymentExpensesBody]
    if (body.isIncorrectOrEmptyBody) List(List(RuleIncorrectOrEmptyBodyError)) else NoValidationErrors
  }

  private def bodyFieldValidation: CreateAndAmendEmploymentExpensesRawData => List[List[MtdError]] = { data =>
    val body = data.body.as[CreateAndAmendEmploymentExpensesBody]

    List(
      RequestValidator.flattenErrors(
        List(
          validateExpenses(body.expenses)
        )
      ))
  }

  private def validateExpenses(expenses: Expenses): List[MtdError] = {
    List(
      NumberValidation.validateOptional(
        field = expenses.businessTravelCosts,
        path = s"/expenses/businessTravelCosts"
      ),
      NumberValidation.validateOptional(
        field = expenses.jobExpenses,
        path = s"/expenses/jobExpenses"
      ),
      NumberValidation.validateOptional(
        field = expenses.flatRateJobExpenses,
        path = s"/expenses/flatRateJobExpenses"
      ),
      NumberValidation.validateOptional(
        field = expenses.professionalSubscriptions,
        path = s"/expenses/professionalSubscriptions"
      ),
      NumberValidation.validateOptional(
        field = expenses.hotelAndMealExpenses,
        path = s"/expenses/hotelAndMealExpenses"
      ),
      NumberValidation.validateOptional(
        field = expenses.otherAndCapitalAllowances,
        path = s"/expenses/otherAndCapitalAllowances"
      ),
      NumberValidation.validateOptional(
        field = expenses.vehicleExpenses,
        path = s"/expenses/vehicleExpenses"
      ),
      NumberValidation.validateOptional(
        field = expenses.mileageAllowanceRelief,
        path = s"/expenses/mileageAllowanceRelief"
      )
    ).flatten
  }

}
