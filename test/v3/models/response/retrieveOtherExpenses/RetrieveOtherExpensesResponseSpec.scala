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

package v3.models.response.retrieveOtherExpenses

import play.api.libs.json.Json
import shared.config.MockAppConfig
import shared.hateoas.Link
import shared.hateoas.Method.{DELETE, GET, PUT}
import shared.models.domain.Timestamp
import shared.utils.UnitSpec

class RetrieveOtherExpensesResponseSpec extends UnitSpec with MockAppConfig {

  val retrieveOtherExpensesBody = RetrieveOtherExpensesResponse(
    Timestamp("2019-04-04T01:01:01Z"),
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2314.32)),
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2314.32))
  )

  val retrieveOtherExpensesBodyWithoutPatents = RetrieveOtherExpensesResponse(
    Timestamp("2019-04-04T01:01:01Z"),
    Some(PaymentsToTradeUnionsForDeathBenefits(Some("TRADE UNION PAYMENTS"), 2314.32)),
    None
  )

  val retrieveOtherExpensesBodyWithoutPayments = RetrieveOtherExpensesResponse(
    Timestamp("2019-04-04T01:01:01Z"),
    None,
    Some(PatentRoyaltiesPayments(Some("ROYALTIES PAYMENTS"), 2314.32))
  )

  val json = Json.parse(
    """{
      |  "submittedOn": "2019-04-04T01:01:01.000Z",
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 2314.32
      |  },
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  val patentsMissingJson = Json.parse(
    """{
      |  "submittedOn": "2019-04-04T01:01:01.000Z",
      |  "paymentsToTradeUnionsForDeathBenefits": {
      |    "customerReference": "TRADE UNION PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  val paymentsMissingJson = Json.parse(
    """{
      |  "submittedOn": "2019-04-04T01:01:01.000Z",
      |  "patentRoyaltiesPayments":{
      |    "customerReference": "ROYALTIES PAYMENTS",
      |    "expenseAmount": 2314.32
      |  }
      |}""".stripMargin
  )

  "reads" when {
    "passed valid JSON" should {
      "return a valid model" in {
        retrieveOtherExpensesBody shouldBe json.as[RetrieveOtherExpensesResponse]
      }
    }
    "passed empty JSON with missing Patents" should {
      "convert JSON into an empty CreateAndAmendOtherExpensesBody object" in {
        retrieveOtherExpensesBodyWithoutPatents shouldBe patentsMissingJson.as[RetrieveOtherExpensesResponse]
      }
    }
    "passed empty JSON with missing Payments" should {
      "convert JSON into an empty CreateAndAmendOtherExpensesBody object" in {
        retrieveOtherExpensesBodyWithoutPayments shouldBe paymentsMissingJson.as[RetrieveOtherExpensesResponse]
      }
    }
  }

  "writes" when {
    "passed a response object" should {
      "return valid JSON" in {
        Json.toJson(retrieveOtherExpensesBody) shouldBe json
      }
    }
    "passed a response object missing patents" should {
      "return an empty JSON" in {
        Json.toJson(retrieveOtherExpensesBodyWithoutPatents) shouldBe patentsMissingJson
      }
    }
    "passed a response object missing payments" should {
      "return an empty JSON" in {
        Json.toJson(retrieveOtherExpensesBodyWithoutPayments) shouldBe paymentsMissingJson
      }
    }
  }

  "LinksFactory" should {
    "return the correct links" in {
      val nino    = "mynino"
      val taxYear = "mytaxyear"

      MockedAppConfig.apiGatewayContext.returns("my/context").anyNumberOfTimes()
      RetrieveOtherExpensesResponse.RetrieveOtherExpensesLinksFactory.links(mockAppConfig, RetrieveOtherExpensesHateoasData(nino, taxYear)) shouldBe
        Seq(
          Link(s"/my/context/other/$nino/$taxYear", PUT, "amend-expenses-other"),
          Link(s"/my/context/other/$nino/$taxYear", GET, "self"),
          Link(s"/my/context/other/$nino/$taxYear", DELETE, "delete-expenses-other")
        )
    }
  }

}
