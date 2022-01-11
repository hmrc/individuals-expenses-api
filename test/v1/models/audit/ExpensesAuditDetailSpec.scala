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

package v1.models.audit

import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import support.UnitSpec

class ExpensesAuditDetailSpec extends UnitSpec {

  val auditErrors: Seq[AuditError] = Seq(AuditError(errorCode = "FORMAT_NINO"), AuditError(errorCode = "FORMAT_TAX_YEAR"))
  val body: JsValue = Json.parse("""{ "aField" : "aValue" }""")

  val nino: String = "ZG903729C"
  val taxYear: String = "2019-20"
  val userType: String = "Agent"
  val agentReferenceNumber: Option[String] = Some("012345678")
  val pathParams: Map[String, String] = Map("nino" -> nino, "taxYear" -> taxYear)
  val requestBody: Option[JsValue] = None
  val xCorrId = "a1e8057e-fbbc-47a8-a8b478d9f015c253"

  val auditResponseModelWithBody: AuditResponse =
    AuditResponse(
      httpStatus = OK,
      response = Right(Some(body))
    )

  val employmentExpensesAuditDetailModelSuccess: ExpensesAuditDetail =
    ExpensesAuditDetail(
      userType = userType,
      agentReferenceNumber = agentReferenceNumber,
      params = pathParams,
      requestBody = requestBody,
      `X-CorrelationId` = xCorrId,
      auditResponse = auditResponseModelWithBody
    )

  val auditResponseModelWithErrors: AuditResponse =
    AuditResponse(
      httpStatus = BAD_REQUEST,
      response = Left(auditErrors)
    )

  val employmentExpensesAuditDetailModelError: ExpensesAuditDetail =
    employmentExpensesAuditDetailModelSuccess.copy(
      auditResponse = auditResponseModelWithErrors
    )

  val employmentExpensesAuditDetailJsonSuccess: JsValue = Json.parse(
    s"""
       |{
       |   "userType" : "$userType",
       |   "agentReferenceNumber" : "${agentReferenceNumber.get}",
       |   "nino" : "$nino",
       |   "taxYear" : "$taxYear",
       |   "response":{
       |     "httpStatus": ${auditResponseModelWithBody.httpStatus},
       |     "body": ${auditResponseModelWithBody.body.get}
       |   },
       |   "X-CorrelationId": "$xCorrId"
       |}
    """.stripMargin
  )

  val auditResponseJsonWithErrors: JsValue = Json.parse(
    s"""
       |{
       |  "httpStatus": $BAD_REQUEST,
       |  "errors" : [
       |    {
       |      "errorCode" : "FORMAT_NINO"
       |    },
       |    {
       |      "errorCode" : "FORMAT_TAX_YEAR"
       |    }
       |  ]
       |}
    """.stripMargin
  )

  val employmentExpensesAuditDetailJsonError: JsValue = Json.parse(
    s"""
       |{
       |   "userType" : "$userType",
       |   "agentReferenceNumber" : "${agentReferenceNumber.get}",
       |   "nino": "$nino",
       |   "taxYear" : "$taxYear",
       |   "response": $auditResponseJsonWithErrors,
       |   "X-CorrelationId": "$xCorrId"
       |}
     """.stripMargin
  )

  "EmploymentExpensesAuditDetail" when {
    "written to JSON (success)" should {
      "produce the expected JsObject" in {
        Json.toJson(employmentExpensesAuditDetailModelSuccess) shouldBe employmentExpensesAuditDetailJsonSuccess
      }
    }

    "written to JSON (error)" should {
      "produce the expected JsObject" in {
        Json.toJson(employmentExpensesAuditDetailModelError) shouldBe employmentExpensesAuditDetailJsonError
      }
    }
  }
}
