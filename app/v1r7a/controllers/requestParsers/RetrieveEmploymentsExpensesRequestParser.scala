/*
 * Copyright 2022 HM Revenue & Customs
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

package v1r7a.controllers.requestParsers

import javax.inject.Inject
import v1r7a.models.domain.Nino
import v1r7a.controllers.requestParsers.validators.RetrieveEmploymentExpensesValidator
import v1r7a.models.domain.MtdSource
import v1r7a.models.request.retrieveEmploymentExpenses.{RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest}

class RetrieveEmploymentsExpensesRequestParser @Inject()(val validator: RetrieveEmploymentExpensesValidator)
  extends RequestParser[RetrieveEmploymentsExpensesRawData, RetrieveEmploymentsExpensesRequest] {

  override protected def requestFor(data: RetrieveEmploymentsExpensesRawData): RetrieveEmploymentsExpensesRequest = {

    val source: MtdSource = MtdSource.parser(data.source)

    RetrieveEmploymentsExpensesRequest(Nino(data.nino), data.taxYear, source)
  }

}
