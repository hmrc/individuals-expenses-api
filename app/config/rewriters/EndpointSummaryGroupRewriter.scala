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

import com.github.jknack.handlebars.Options
import config.AppConfig
import config.rewriters.DocumentationRewriters.CheckAndRewrite

import javax.inject.{Inject, Singleton}

// TODO repurpose this rewriter to use "maybeTestOnly" Handlebars helper etc

/** For the OAS workaround where the "grouped endpoints" yaml file (e.g. employment_expenses.yaml) must include the matching summary text for each
  * endpoint. This rewriter checks and rewrites each endpoint summary in the group file.
  */
@Singleton class EndpointSummaryGroupRewriter @Inject() (val appConfig: AppConfig) extends HandlebarsRewriter {

  hb.registerHelper(
    "maybeTestOnly",
    (endpointName: String, _: Options) => {
      val parts           = endpointName.split(' ')
      val (version, name) = (parts(0), parts(1)) // TODO better!

      if (appConfig.endpointReleasedInProduction(version, name)) "" else " [test only]"
    }
  )

  val rewriteGroupedEndpointSummaries: CheckAndRewrite = CheckAndRewrite(
    check = (version, filename) => {
      filename.endsWith(".yaml") && filename != "application.yaml" // && ...  TODO maybe other checks?
    },
    rewrite = (_, _, yaml) => {
      if (yaml.contains("#maybeTestOnly"))
        rewrite(yaml, Nil)
      else
        yaml
    }
  )

//////////////////////////////////////

//  private val pathVersionRegex = "^/public/api/conf/([0-9]\\.0)$".r
//
//  private def keyFrom(filename: String): String = filename.dropRight(5).replace("_", "-")
//
//  val rewriteGroupedEndpointSummariesOLD: (CheckRewrite, Rewriter) =
//    (
//      (version, filename) => {
//        // Checks if any endpoint switches exist (and are disabled)
//        // with the key name starting with this filename.
//
//        filename.endsWith(".yaml") && filename != "application.yaml" && {
//          val key = keyFrom(filename) + "-"
//
//          // e.g:
//          //   key:         "employment-expenses-"
//          //   endpointKey: "employment-expenses-create-and-amend"
//          val result = appConfig.endpointReleasedInProduction(version, endpointKey)
//            .endpointSwitches(version)
//            .exists { case (endpointKey, enabled) => !enabled && endpointKey.startsWith(key) } // check, might need default if endpoint name not in the config
//
//          result
//        }
//      },
//      (path, filename, yaml) =>
//        {
//          pathVersionRegex.findFirstMatchIn(path).map(_.group(1)).map { version =>
//            val key = keyFrom(filename)
//
////            val disabledEndpointNames =
////              appConfig
////                .endpointSwitches(version)
////                .collect { case (endpointKey, enabled) if !enabled && endpointKey.startsWith(key) => endpointKey }
//
//            val allEndpointRefsRegex            = (".*(\\$ref: \"\\./(.*)\\.yaml\"\\n  summary: [\"]?)(.*)").r
//            allEndpointRefsRegex.findAllMatchIn(yaml).foldLeft(yaml)((acc, endpointName))
//
//            disabledEndpointNames.foldLeft(yaml)((acc, endpointName) => {
//              val endpointFilename = endpointName.replace("-", "_")
//              val regex            = (".*(\\$ref: \"\\./" + endpointFilename + "\\.yaml\"\\n  summary: [\"]?)(.*)").r
//
//              val maybeLine = regex.findFirstIn(acc)
//              maybeLine
//                .collect {
//                  case line if !(line.toLowerCase.contains("[test only]")) =>
//                    val summary = line
//                      .split("summary: ")(1)
//
//                    val summaryNoQuotes    = summary.replace("\"", "")
//                    val summaryReplacement = s"""summary: "$summaryNoQuotes [test only]""""
//
//                    acc.replaceFirst(s"summary: $summary", summaryReplacement)
//                }
//                .getOrElse(acc)
//            })
//          }
//        }.getOrElse(yaml)
//    )

}
