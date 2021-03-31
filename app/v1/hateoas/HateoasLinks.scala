/*
 * Copyright 2021 HM Revenue & Customs
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

package v1.hateoas

import config.AppConfig
import v1.models.hateoas.Link
import v1.models.hateoas.Method._
import v1.models.hateoas.RelType._

trait HateoasLinks {

  //Domain URIs
  private def otherExpensesUri(appConfig: AppConfig, nino: String, taxYear: String): String =
    s"/${appConfig.apiGatewayContext}/other/$nino/$taxYear"

  private def employmentExpensesUri(appConfig: AppConfig, nino: String, taxYear: String): String =
    s"/${appConfig.apiGatewayContext}/employments/$nino/$taxYear"

  private def ignoreEmploymentExpensesUri(appConfig: AppConfig, nino: String, taxYear: String): String =
    s"/${appConfig.apiGatewayContext}/employments/$nino/$taxYear/ignore"

    //API resource links
  def retrieveOtherExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = otherExpensesUri(appConfig, nino, taxYear), method = GET, rel = SELF)

  def amendOtherExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = otherExpensesUri(appConfig, nino, taxYear), method = PUT, rel = AMEND_EXPENSES_OTHER)

  def deleteOtherExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = otherExpensesUri(appConfig, nino, taxYear), method = DELETE, rel = DELETE_EXPENSES_OTHER)

  def retrieveEmploymentExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = employmentExpensesUri(appConfig, nino, taxYear), method = GET, rel = SELF)

  def amendEmploymentExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = employmentExpensesUri(appConfig, nino, taxYear), method = PUT, rel = AMEND_EMPLOYMENT_EXPENSES)

  def deleteEmploymentExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = employmentExpensesUri(appConfig, nino, taxYear), method = DELETE, rel = DELETE_EMPLOYMENT_EXPENSES)

  def ignoreEmploymentExpenses(appConfig: AppConfig, nino: String, taxYear: String): Link =
    Link(href = ignoreEmploymentExpensesUri(appConfig, nino, taxYear), method = POST, rel = IGNORE_EMPLOYMENT_EXPENSES)

}
