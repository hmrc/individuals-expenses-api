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

package v3.controllers

import common.domain.MtdSource
import play.api.Configuration
import play.api.mvc.Result
import shared.config.MockAppConfig
import shared.controllers.{ControllerBaseSpec, ControllerTestRunner}
import shared.models.domain.{Nino, TaxYear}
import shared.models.errors._
import shared.models.outcomes.ResponseWrapper
import v3.controllers.validators.MockRetrieveEmploymentExpensesValidatorFactory
import v3.fixtures.RetrieveEmploymentsExpensesFixtures._
import v3.models.request.retrieveEmploymentExpenses.RetrieveEmploymentsExpensesRequestData
import v3.services.MockRetrieveEmploymentsExpensesService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveEmploymentsExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveEmploymentsExpensesService
    with MockRetrieveEmploymentExpensesValidatorFactory
    with MockAppConfig {

  private val taxYear = "2019-20"
  private val source  = MtdSource.`latest`

  private val requestData = RetrieveEmploymentsExpensesRequestData(Nino(validNino), TaxYear.fromMtd(taxYear), source)

  "handleRequest" should {
    "return a successful response with status 200 (OK)" when {
      "given a valid request" in new Test {
        willUseValidator(returningSuccess(requestData))

        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

        MockRetrieveEmploymentsExpensesService
          .retrieveEmploymentsExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseModelLatest))))

        runOkTest(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(retrieveEmploymentsExpensesMtdResponseLatest)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {
        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

        willUseValidator(returning(NinoFormatError))

        runErrorTest(NinoFormatError)

      }

      "the service returns an error" in new Test {
        MockedAppConfig.featureSwitchConfig.anyNumberOfTimes() returns Configuration(
          "supporting-agents-access-control.enabled" -> true
        )

        MockedAppConfig.endpointAllowsSupportingAgents(controller.endpointName).anyNumberOfTimes() returns false

        willUseValidator(returningSuccess(requestData))

        MockRetrieveEmploymentsExpensesService
          .retrieveEmploymentsExpenses(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest {

    val controller: RetrieveEmploymentsExpensesController = new RetrieveEmploymentsExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      validatorFactory = mockRetrieveEmploymentExpensesValidatorFactory,
      service = mockRetrieveEmploymentsExpensesService,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(validNino, taxYear, source.toString)(fakeGetRequest)
  }

}
