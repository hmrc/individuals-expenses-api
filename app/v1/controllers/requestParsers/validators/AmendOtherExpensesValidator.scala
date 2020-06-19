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

import v1.controllers.requestParsers.validators.validations._
import v1.models.errors._

class AmendOtherExpensesValidator extends Validator[AmendOtherExpensesRawData] {
  private val validationSet = List(parameterFormatValidation, parameterRuleValidation)

  private def parameterFormatValidation: AmendOtherExpensesRawData => List[List[MtdError]] = (data: AmendOtherExpensesRawData) => {
  List(
  NinoValidation.validate(data.nino),
  TaxYearValidation.validate(data.taxYear),
  JsonFormatValidation.validate[AmendOtherExpensesBody](data.body, RuleIncorrectOrEmptyBodyError)
  )
}

  private def parameterRuleValidation: AmendOtherExpensesRawData => List[List[MtdError]] = { data =>
  List(
  MtdTaxYearValidation.validate(data.taxYear, RuleTaxYearNotSupportedError)
  )
}

  private def validateCustomerReference()

  override def validate(data: AmendOtherExpensesRawData): List[MtdError] = {
  run(validationSet, data).distinct
}
}
