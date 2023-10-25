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

package v1.models.response.retrieveOtherExpenses

import api.hateoas.{HateoasData, HateoasLinks, HateoasLinksFactory, Link}
import api.models.domain.Timestamp
import config.AppConfig
import play.api.libs.json.{Json, OFormat}

case class RetrieveOtherExpensesResponse(submittedOn: Timestamp,
                                         paymentsToTradeUnionsForDeathBenefits: Option[PaymentsToTradeUnionsForDeathBenefits],
                                         patentRoyaltiesPayments: Option[PatentRoyaltiesPayments])

object RetrieveOtherExpensesResponse extends HateoasLinks {
  implicit val format: OFormat[RetrieveOtherExpensesResponse] = Json.format[RetrieveOtherExpensesResponse]

  implicit object RetrieveOtherExpensesLinksFactory extends HateoasLinksFactory[RetrieveOtherExpensesResponse, RetrieveOtherExpensesHateoasData] {

    override def links(appConfig: AppConfig, data: RetrieveOtherExpensesHateoasData): Seq[Link] = {
      import data._
      Seq(
        createAndAmendOtherExpenses(appConfig, nino, taxYear),
        retrieveOtherExpenses(appConfig, nino, taxYear),
        deleteOtherExpenses(appConfig, nino, taxYear)
      )
    }

  }

}

case class RetrieveOtherExpensesHateoasData(nino: String, taxYear: String) extends HateoasData
