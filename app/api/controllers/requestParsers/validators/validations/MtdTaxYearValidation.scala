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

package api.controllers.requestParsers.validators.validations

import api.models.domain.TaxYear
import api.models.errors.{MtdError, RuleTaxYearNotEndedError, RuleTaxYearNotSupportedError}
import org.joda.time.DateTime
import utils.{CurrentDateTime, CurrentTaxYear}

object MtdTaxYearValidation {

  /** @param taxYear
    * taxYear in MTD format YYYY-YY
    */
  def validate(taxYear: String, minimumTaxYear: Int, checkCurrentTaxYear: Boolean = false)(implicit
                                                                                           dateTimeProvider: CurrentDateTime,
                                                                                           currentTaxYear: CurrentTaxYear): List[MtdError] = {

    val year = TaxYear.fromMtd(taxYear).year
    val currentDate: DateTime = dateTimeProvider.getDateTime

    year match {
      case _ if year < minimumTaxYear => List(RuleTaxYearNotSupportedError)
      case _ if checkCurrentTaxYear && year >= currentTaxYear.getCurrentTaxYear(currentDate) => List(RuleTaxYearNotEndedError)
      case _ => NoValidationErrors
    }
  }

}