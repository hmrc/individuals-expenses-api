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

package v1.models.hateoas

object RelType {
  val AMEND_EXPENSES_OTHER = "amend-expenses-other"
  val DELETE_EXPENSES_OTHER = "delete-expenses-other"

  val IGNORE_EMPLOYMENT_EXPENSES = "ignore-employment-expenses"
  val RETRIEVE_EMPLOYMENT_EXPENSES = "retrieve-employment-expenses"
  val AMEND_EMPLOYMENT_EXPENSES = "amend-employment-expenses"
  val DELETE_EMPLOYMENT_EXPENSES = "delete-employment-expenses"

  val SELF = "self"
}
