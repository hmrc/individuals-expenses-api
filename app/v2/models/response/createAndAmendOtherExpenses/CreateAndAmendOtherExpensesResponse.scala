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

package v2.models.response.createAndAmendOtherExpenses

import api.hateoas.{HateoasData, HateoasLinks, HateoasLinksFactory, Link}
import config.AppConfig

object CreateAndAmendOtherExpensesResponse extends HateoasLinks {

  implicit object CreateAndAmendOtherExpensesLinksFactory extends HateoasLinksFactory[Unit, CreateAndAmendOtherExpensesHateoasData] {

    override def links(appConfig: AppConfig, data: CreateAndAmendOtherExpensesHateoasData): Seq[Link] = {
      import data._
      Seq(
        retrieveOtherExpenses(appConfig, nino, taxYear),
        createAndAmendOtherExpenses(appConfig, nino, taxYear),
        deleteOtherExpenses(appConfig, nino, taxYear)
      )
    }

  }

}

case class CreateAndAmendOtherExpensesHateoasData(nino: String, taxYear: String) extends HateoasData
