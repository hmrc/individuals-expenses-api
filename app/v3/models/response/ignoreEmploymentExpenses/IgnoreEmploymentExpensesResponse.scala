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

package v3.models.response.ignoreEmploymentExpenses

import shared.config.AppConfig
import shared.hateoas.{HateoasData, HateoasLinksFactory, Link}
import v3.common.hateoas.HateoasLinks

object IgnoreEmploymentExpensesResponse extends HateoasLinks {

  implicit object IgnoreEmploymentExpensesLinksFactory extends HateoasLinksFactory[Unit, IgnoreEmploymentExpensesHateoasData] {

    override def links(appConfig: AppConfig, data: IgnoreEmploymentExpensesHateoasData): Seq[Link] = {
      import data._
      Seq(
        retrieveEmploymentExpenses(appConfig, nino, taxYear),
        deleteEmploymentExpenses(appConfig, nino, taxYear)
      )
    }

  }

}

case class IgnoreEmploymentExpensesHateoasData(nino: String, taxYear: String) extends HateoasData
