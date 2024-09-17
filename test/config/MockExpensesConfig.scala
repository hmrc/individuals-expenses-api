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

package config

import org.scalamock.handlers.{CallHandler, CallHandler0}
import org.scalamock.scalatest.MockFactory
import shared.config.DownstreamConfig

trait MockExpensesConfig extends MockFactory {

  implicit val mockExpensesConfig: ExpensesConfig = mock[ExpensesConfig]

  object MockedExpensesConfig {
    def ifsR5DownstreamConfig: CallHandler0[DownstreamConfig] = (() => mockExpensesConfig.ifsR5DownstreamConfig: DownstreamConfig).expects()
    def ifsR6DownstreamConfig: CallHandler0[DownstreamConfig] = (() => mockExpensesConfig.ifsR6DownstreamConfig: DownstreamConfig).expects()

    def otherExpensesMinimumTaxYear: CallHandler[Int] = (() => mockExpensesConfig.otherExpensesMinimumTaxYear).expects()

    def employmentExpensesMinimumTaxYear: CallHandler[Int] = (() => mockExpensesConfig.employmentExpensesMinimumTaxYear).expects()

  }

}
