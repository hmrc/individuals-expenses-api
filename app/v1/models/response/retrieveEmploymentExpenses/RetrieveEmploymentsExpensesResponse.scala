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

package v1.models.response.retrieveEmploymentExpenses

import api.hateoas.{HateoasData, HateoasLinks, HateoasLinksFactory, Link}
import api.models.domain.{MtdSource, Timestamp}
import api.models.downstream.DownstreamSource
import config.AppConfig
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, OWrites, Reads}

case class RetrieveEmploymentsExpensesResponse(submittedOn: Option[Timestamp],
                                               totalExpenses: Option[BigDecimal],
                                               source: Option[MtdSource],
                                               dateIgnored: Option[Timestamp],
                                               expenses: Option[Expenses])

object RetrieveEmploymentsExpensesResponse extends HateoasLinks {

  implicit val reads: Reads[RetrieveEmploymentsExpensesResponse] = (
    (JsPath \ "submittedOn").readNullable[Timestamp] and
      (JsPath \ "totalExpenses").readNullable[BigDecimal] and
      (JsPath \ "source").readNullable[DownstreamSource].map(_.map(_.toMtd)) and
      (JsPath \ "dateIgnored").readNullable[Timestamp] and
      (JsPath \ "expenses").readNullable[Expenses]
  )(RetrieveEmploymentsExpensesResponse.apply _)

  implicit val writes: OWrites[RetrieveEmploymentsExpensesResponse] = Json.writes[RetrieveEmploymentsExpensesResponse]

  implicit object RetrieveEmploymentsExpensesLinksFactory
      extends HateoasLinksFactory[RetrieveEmploymentsExpensesResponse, RetrieveEmploymentsExpensesHateoasData] {

    override def links(appConfig: AppConfig, data: RetrieveEmploymentsExpensesHateoasData): Seq[Link] = {
      import data._
      data.source match {
        case "latest" =>
          Seq(
            createAndAmendEmploymentExpenses(appConfig, nino, taxYear),
            retrieveEmploymentExpenses(appConfig, nino, taxYear),
            deleteEmploymentExpenses(appConfig, nino, taxYear),
            ignoreEmploymentExpenses(appConfig, nino, taxYear)
          )
        case "hmrcHeld" => Seq(retrieveEmploymentExpenses(appConfig, nino, taxYear), ignoreEmploymentExpenses(appConfig, nino, taxYear))
        case "user" =>
          Seq(
            createAndAmendEmploymentExpenses(appConfig, nino, taxYear),
            retrieveEmploymentExpenses(appConfig, nino, taxYear),
            deleteEmploymentExpenses(appConfig, nino, taxYear)
          )
      }
    }

  }

}

case class RetrieveEmploymentsExpensesHateoasData(nino: String, taxYear: String, source: String) extends HateoasData
