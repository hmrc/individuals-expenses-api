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

package v1.controllers.requestParsers

import api.models.domain.{MtdSource, Nino, TaxYear}
import v1.controllers.requestParsers.validators.RetrieveEmploymentExpensesValidator
import v1.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}

import javax.inject.Inject

class RetrieveEmploymentsExpensesRequestParser @Inject() (val validator: RetrieveEmploymentExpensesValidator)
    extends RequestParser[RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest] {

  override protected def requestFor(data: RetrieveEmploymentsExpensesRawData): RetrieveEmploymentsExpensesRequest = {

    val source: MtdSource = MtdSource.parser(data.source)

    RetrieveEmploymentsExpensesRequest(Nino(data.nino), TaxYear.fromMtd(data.taxYear), source)
  }

}
