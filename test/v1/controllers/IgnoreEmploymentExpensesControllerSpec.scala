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

package v1.controllers

import api.controllers.{ControllerBaseSpec, ControllerTestRunner}
import api.mocks.hateoas.MockHateoasFactory
import api.models.audit.{AuditEvent, AuditResponse, GenericAuditDetail}
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.hateoas.Method.{DELETE, GET}
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import mocks.MockAppConfig
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import v1.mocks.requestParsers.MockIgnoreEmploymentExpensesRequestParser
import v1.mocks.services.MockIgnoreEmploymentExpensesService
import v1.models.request.ignoreEmploymentExpenses._
import v1.models.response.ignoreEmploymentExpenses.IgnoreEmploymentExpensesHateoasData

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IgnoreEmploymentExpensesControllerSpec
  extends ControllerBaseSpec
    with ControllerTestRunner
    with MockIgnoreEmploymentExpensesService
    with MockIgnoreEmploymentExpensesRequestParser
    with MockHateoasFactory
    with MockAppConfig {

  private val taxYear = "2019-20"

  private val testHateoasLinks = Seq(
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/employments/$nino/$taxYear", method = DELETE, rel = "delete-employment-expenses")
  )

  val responseBodyJson: JsValue = Json.parse(
    s"""
       |{
       |  "links": [
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "GET",
       |      "rel": "self"
       |    },
       |    {
       |      "href": "/individuals/expenses/employments/$nino/$taxYear",
       |      "method": "DELETE",
       |      "rel": "delete-employment-expenses"
       |    }
       |  ]
       |}
       |""".stripMargin)

  private val rawData = IgnoreEmploymentExpensesRawData(nino, taxYear)
  private val requestData = IgnoreEmploymentExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear))

  "handleRequest" should {
    "return Ok" when {
      "the request received is valid" in new Test {

        MockIgnoreEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockIgnoreEmploymentExpensesService
          .ignore(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, ()))))

        MockHateoasFactory
          .wrap((), IgnoreEmploymentExpensesHateoasData(nino, taxYear))
          .returns(HateoasWrapper((), testHateoasLinks))

        runOkTestWithAudit(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(responseBodyJson),
          maybeAuditResponseBody = Some(responseBodyJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {

        MockIgnoreEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTestWithAudit(NinoFormatError)
      }

      "service errors occur" in new Test {

        MockIgnoreEmploymentExpensesRequestParser
          .parseRequest(rawData)
          .returns(Right(requestData))

        MockIgnoreEmploymentExpensesService
          .ignore(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTestWithAudit(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest with AuditEventChecking {

    val controller = new IgnoreEmploymentExpensesController(
      appConfig = mockAppConfig,
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRequestParser,
      service = mockService,
      auditService = mockAuditService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    MockAppConfig.featureSwitches.returns(Configuration("allowTemporalValidationSuspension.enabled" -> true)).anyNumberOfTimes()

    protected def callController(): Future[Result] = controller.handleRequest(nino, taxYear)(fakeRequest)

    def event(auditResponse: AuditResponse, requestBody: Option[JsValue]): AuditEvent[GenericAuditDetail] =
      AuditEvent(
        auditType = "IgnoreEmploymentExpenses",
        transactionName = "ignore-employment-expenses",
        detail = GenericAuditDetail(
          userType = "Individual",
          agentReferenceNumber = None,
          params = Map("nino" -> nino, "taxYear" -> taxYear),
          request = None,
          `X-CorrelationId` = correlationId,
          response = auditResponse
        )
      )

  }

}
