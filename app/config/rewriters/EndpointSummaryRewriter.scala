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
import config.rewriters.DocumentationRewriters.CheckAndRewrite

import javax.inject.{Inject, Singleton}

@Singleton class EndpointSummaryRewriter @Inject() (appConfig: AppConfig) {

  private val rewriteSummaryRegex = ".*(summary: [\"]?)(.*)".r

  val rewriteEndpointSummary: CheckAndRewrite = CheckAndRewrite(
    check = (version, filename) => {
      // Checks if an endpoint switch exists with
      // the same name as the endpoint OAS file, and is disabled.

      filename.endsWith(".yaml") && filename != "application.yaml" && {
        val key =
          filename
            .dropRight(5)
            .replace("_", "-")

        !appConfig.endpointReleasedInProduction(version, key)
      }
    },
    rewrite = (_, _, yaml) => {
      val maybeLine = rewriteSummaryRegex.findFirstIn(yaml)
      maybeLine
        .collect {
          case line if !(line.toLowerCase.contains("[test only]")) =>
            val summary = line
              .split("summary: ")(1)
              .replace("\"", "")

            val replacement = s"""summary: "$summary [test only]""""
            rewriteSummaryRegex.replaceFirstIn(yaml, replacement)
        }
        .getOrElse(yaml)
    }
  )

}
