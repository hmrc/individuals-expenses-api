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

package common.connectors

import config.ExpensesConfig
import shared.config.DownstreamConfig
import shared.connectors.{DownstreamStrategy, DownstreamUri}

object ExpensesDownstreamUri {

  private def withStandardStrategy[Resp](path: String, config: DownstreamConfig): DownstreamUri[Resp] =
    DownstreamUri(path, DownstreamStrategy.standardStrategy(config))

  def ifsR5Uri[Resp](value: String)(implicit appConfig: ExpensesConfig): DownstreamUri[Resp] =
    withStandardStrategy(value, appConfig.ifsR5DownstreamConfig)

  def ifsR6Uri[Resp](value: String)(implicit appConfig: ExpensesConfig): DownstreamUri[Resp] =
    withStandardStrategy(value, appConfig.ifsR6DownstreamConfig)

}
