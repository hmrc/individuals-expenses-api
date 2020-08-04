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

package v1.controllers.requestParsers.validators.validations

import config.AppConfig
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import utils.{CurrentDateTime, CurrentTaxYear}
import v1.models.errors.{MtdError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError}
import v1.models.request.DesTaxYear

object MtdTaxYearValidation {

  // @param taxYear In format YYYY-YY
  def validate(taxYear: String, checkCurrentTaxYear: Boolean = false)
              (implicit dateTimeProvider: CurrentDateTime, appConfig: AppConfig,  currentTaxYear: CurrentTaxYear): List[MtdError] = {

    val desTaxYear = Integer.parseInt(DesTaxYear.fromMtd(taxYear).value)
    val currentDate: DateTime = dateTimeProvider.getDateTime

    desTaxYear match {
      case _ if desTaxYear < appConfig.minimumPermittedTaxYear => List(RuleTaxYearNotSupportedError)
      case _ if checkCurrentTaxYear && desTaxYear >= currentTaxYear.getCurrentTaxYear(currentDate) => List(RuleTaxYearNotEndedError)
      case _ => NoValidationErrors
    }
  }

}