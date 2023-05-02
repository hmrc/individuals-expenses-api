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

import config.rewriters.DocumentationRewriters.CheckRewrite
import controllers.Rewriter

/** For the OAS workaround where the "grouped endpoints" yaml file (e.g. employment_expenses.yaml) must include the matching summary text for each
  * endpoint. This rewriter checks and rewrites each endpoint summary in the group file.
  */
object EndpointSummaryGroupRewriter {

  private val pathVersionRegex = "^/public/api/conf/([0-9]\\.0)$".r

  private def keyFrom(filename: String) = filename.dropRight(5).replace("_", "-")

  val rewriteGroupedEndpointSummaries: (CheckRewrite, Rewriter) =
    (
      (version, filename, appConfig) => {
        // Checks if any endpoint switches exist (and are disabled)
        // with the key name starting with this filename.

        filename.endsWith(".yaml") && filename != "application.yaml" && {
          val key = keyFrom(filename)

          // e.g:
          //   key:         "employment-expenses"
          //   endpointKey: "employment-expenses-create-and-amend"
          val result = appConfig
            .endpointSwitches(version)
            .exists { case (endpointKey, enabled) => !enabled && endpointKey.startsWith(key) }

          result
        }
      },
      (path, filename, appConfig, yaml) =>
        {
          pathVersionRegex.findFirstMatchIn(path).map(_.group(1)).map { version =>
            val key = keyFrom(filename)

            val disabledEndpointNames =
              appConfig
                .endpointSwitches(version)
                .collect { case (endpointKey, enabled) if !enabled && endpointKey.startsWith(key) => endpointKey }

            disabledEndpointNames.foldLeft(yaml)((acc, endpointName) => {
              val endpointFilename = endpointName.replace("-", "_")
              val regex            = (".*(\\$ref: \"\\./" + endpointFilename + "\\.yaml\"\\n  summary: [\"]?)(.*)").r

              val maybeLine = regex.findFirstIn(acc)
              maybeLine
                .collect {
                  case line if !(line.toLowerCase.contains("[test only]")) =>
                    val summary = line
                      .split("summary: ")(1)

                    val summaryNoQuotes    = summary.replace("\"", "")
                    val summaryReplacement = s"""summary: "$summaryNoQuotes [test only]""""

                    acc.replaceFirst(s"summary: $summary", summaryReplacement)
                }
                .getOrElse(acc)
            })
          }
        }.getOrElse(yaml)
    )

}
