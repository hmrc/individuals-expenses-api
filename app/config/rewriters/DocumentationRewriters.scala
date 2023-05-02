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

package config.rewriters

import config.AppConfig
import config.rewriters.ApiVersionTitleRewriter.rewriteApiVersionTitle
import config.rewriters.EndpointSummaryGroupRewriter.rewriteGroupedEndpointSummaries
import config.rewriters.EndpointSummaryRewriter.rewriteEndpointSummary

object DocumentationRewriters {

  val rewriters =
    List(
      rewriteApiVersionTitle,
      rewriteEndpointSummary,
      rewriteGroupedEndpointSummaries
    )

  trait CheckRewrite {
    def apply(version: String, filename: String, appConfig: AppConfig): Boolean
  }

}
