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
import api.models.domain.{Nino, TaxYear}
import api.models.errors._
import api.models.hateoas.Method.{DELETE, GET, PUT}
import api.models.hateoas.{HateoasWrapper, Link}
import api.models.outcomes.ResponseWrapper
import play.api.mvc.Result
import v1.fixtures.RetrieveOtherExpensesFixtures._
import v1.mocks.requestParsers.MockRetrieveOtherExpensesRequestParser
import v1.mocks.services.MockRetrieveOtherExpensesService
import v1.models.request.retrieveOtherExpenses.{RetrieveOtherExpensesRawData, RetrieveOtherExpensesRequest}
import v1.models.response.retrieveOtherExpenses._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RetrieveOtherExpensesControllerSpec
    extends ControllerBaseSpec
    with ControllerTestRunner
    with MockRetrieveOtherExpensesService
    with MockRetrieveOtherExpensesRequestParser
    with MockHateoasFactory {

  private val taxYear = "2019-20"

  private val rawData     = RetrieveOtherExpensesRawData(nino, taxYear)
  private val requestData = RetrieveOtherExpensesRequest(Nino(nino), TaxYear.fromMtd(taxYear))

  private val testHateoasLink = Seq(
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = PUT, rel = "amend-expenses-other"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = GET, rel = "self"),
    Link(href = s"/individuals/expenses/other/$nino/$taxYear", method = DELETE, rel = "delete-expenses-other")
  )

  private val responseBodyJson = mtdResponseWithHateoasLinks(taxYear)

  "handleRequest" should {
    "return a successful response with status 200 (OK)" when {
      "given a valid request" in new Test {

        MockRetrieveOtherExpensesRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveOtherExpensesService
          .retrieveOtherExpenses(requestData)
          .returns(Future.successful(Right(ResponseWrapper(correlationId, responseModel))))

        MockHateoasFactory
          .wrap(responseModel, RetrieveOtherExpensesHateoasData(nino, taxYear))
          .returns(HateoasWrapper(responseModel, testHateoasLink))

        runOkTest(
          expectedStatus = OK,
          maybeExpectedResponseBody = Some(responseBodyJson)
        )
      }
    }

    "return the error as per spec" when {
      "the parser validation fails" in new Test {

        MockRetrieveOtherExpensesRequestParser
          .parse(rawData)
          .returns(Left(ErrorWrapper(correlationId, NinoFormatError)))

        runErrorTest(NinoFormatError)

      }

      "the service returns an error" in new Test {

        MockRetrieveOtherExpensesRequestParser
          .parse(rawData)
          .returns(Right(requestData))

        MockRetrieveOtherExpensesService
          .retrieveOtherExpenses(requestData)
          .returns(Future.successful(Left(ErrorWrapper(correlationId, RuleTaxYearNotSupportedError))))

        runErrorTest(RuleTaxYearNotSupportedError)
      }
    }
  }

  trait Test extends ControllerTest {

    val controller = new RetrieveOtherExpensesController(
      authService = mockEnrolmentsAuthService,
      lookupService = mockMtdIdLookupService,
      parser = mockRetrieveOtherExpensesRequestParser,
      service = mockRetrieveOtherExpensesService,
      hateoasFactory = mockHateoasFactory,
      cc = cc,
      idGenerator = mockIdGenerator
    )

    protected def callController(): Future[Result] = controller.handleRequest(nino, taxYear)(fakeGetRequest)
  }

}
