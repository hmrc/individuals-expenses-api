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

package v3.fixtures

import common.domain.MtdSource
import play.api.libs.json.{JsObject, JsValue, Json}
import shared.models.domain.Timestamp
import v3.models.response.retrieveEmploymentExpenses.{Expenses, RetrieveEmploymentsExpensesResponse}

object RetrieveEmploymentsExpensesFixtures {

  val expensesModel: Expenses = Expenses(
    businessTravelCosts = Some(1000.99),
    jobExpenses = Some(2000.99),
    flatRateJobExpenses = Some(3000.99),
    professionalSubscriptions = Some(4000.99),
    hotelAndMealExpenses = Some(5000.99),
    otherAndCapitalAllowances = Some(6000.99),
    vehicleExpenses = Some(7000.99),
    mileageAllowanceRelief = Some(8000.99)
  )

  private def responseModel(source: Option[MtdSource]) = RetrieveEmploymentsExpensesResponse(
    submittedOn = Some(Timestamp("2020-12-12T12:12:12Z")),
    totalExpenses = Some(1000.99),
    source = source,
    dateIgnored = Some(Timestamp("2020-07-13T20:37:27Z")),
    expenses = Some(expensesModel)
  )

  val responseModelLatest: RetrieveEmploymentsExpensesResponse   = responseModel(Some(MtdSource.`latest`))
  val responseModelUser: RetrieveEmploymentsExpensesResponse     = responseModel(Some(MtdSource.`user`))
  val responseModelHmrcHeld: RetrieveEmploymentsExpensesResponse = responseModel(Some(MtdSource.`hmrcHeld`))

  val expensesJson: JsValue = Json.parse(
    s"""
       |{
       |	"businessTravelCosts": 1000.99,
       |	"jobExpenses": 2000.99,
       |	"flatRateJobExpenses": 3000.99,
       |	"professionalSubscriptions": 4000.99,
       |	"hotelAndMealExpenses": 5000.99,
       |	"otherAndCapitalAllowances": 6000.99,
       |	"vehicleExpenses": 7000.99,
       |	"mileageAllowanceRelief": 8000.99
       |}
       |""".stripMargin
  )

  private def responseJson(source: String) = Json.parse(
    s"""
       |{
       |	"submittedOn": "2020-12-12T12:12:12.000Z",
       |	"totalExpenses": 1000.99,
       |  	"source": "$source",
       |    "dateIgnored": "2020-07-13T20:37:27.000Z",
       |	"expenses": $expensesJson
       |}
       |""".stripMargin
  )

  val mtdResponseJsonLatest: JsValue   = responseJson("latest")
  val mtdResponseJsonUser: JsValue     = responseJson("user")
  val mtdResponseJsonHmrcHeld: JsValue = responseJson("hmrcHeld")

  val downstreamResponseJsonLatest: JsValue   = responseJson("LATEST")
  val downstreamResponseJsonCustomer: JsValue = responseJson("CUSTOMER")
  val downstreamResponseJsonHmrcHeld: JsValue = responseJson("HMRC HELD")

  private def linksJsonLatest(taxYear: String) = Json.parse(
    s"""
       |{
       |    "links": [
       |        {
       |			"href": "/individuals/expenses/employments/AA123456A/$taxYear",
       |			"method": "PUT",
       |			"rel": "amend-employment-expenses"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/$taxYear",
       |			"method": "GET",
       |			"rel": "self"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/$taxYear",
       |			"method": "DELETE",
       |			"rel": "delete-employment-expenses"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/$taxYear/ignore",
       |			"method": "POST",
       |			"rel": "ignore-employment-expenses"
       |	    }
       |	]
       | }
       |""".stripMargin
  )

  private val linksJsonUser = Json.parse(
    s"""
       |{
       |    "links": [
       |        {
       |			"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |			"method": "PUT",
       |			"rel": "amend-employment-expenses"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |			"method": "GET",
       |			"rel": "self"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |			"method": "DELETE",
       |			"rel": "delete-employment-expenses"
       |		}
       |	]
       | }
       |""".stripMargin
  )

  private val linksJsonHmrcHeld = Json.parse(
    s"""
       |{
       |    "links": [
       |        {
       |			"href": "/individuals/expenses/employments/AA123456A/2019-20",
       |			"method": "GET",
       |			"rel": "self"
       |		},
       |		{
       |			"href": "/individuals/expenses/employments/AA123456A/2019-20/ignore",
       |			"method": "POST",
       |			"rel": "ignore-employment-expenses"
       |		}
       |	]
       | }
       |""".stripMargin
  )

  def mtdResponseWithHateoasLinksLatest(taxYear: String = "2019-20"): JsValue =
    mtdResponseJsonLatest.as[JsObject] ++ linksJsonLatest(taxYear).as[JsObject]

  val mtdResponseWithHateoasLinksUser: JsValue     = mtdResponseJsonUser.as[JsObject] ++ linksJsonUser.as[JsObject]
  val mtdResponseWithHateoasLinksHmrcHeld: JsValue = mtdResponseJsonHmrcHeld.as[JsObject] ++ linksJsonHmrcHeld.as[JsObject]

}
