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

import api.controllers.ControllerBaseSpec
import com.typesafe.config.ConfigFactory
import config.DocumentationController.filenameWithFeatureName
import controllers.{Assets, AssetsConfiguration, DefaultAssetsMetadata}
import definition.ApiDefinitionFactory
import mocks.MockAppConfig
import play.api.Configuration
import play.api.http.{DefaultFileMimeTypes, DefaultHttpErrorHandler, FileMimeTypesConfiguration, HttpConfiguration}
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.matching.Regex

class DocumentationControllerSpec extends ControllerBaseSpec with MockAppConfig {

  private case object AFeature extends OpenApiFeature {
    val key                      = "aFeature"
    val version                  = "any"
    val fileMatchers: Seq[Regex] = Nil
  }

  "/file endpoint" should {
    "return a file" in new Test {
      val response: Future[Result] = controller.file("1.0", "application.yaml")(fakeGetRequest.withHeaders(ACCEPT -> "text/yaml"))
      status(response) shouldBe OK
      await(response).body.contentLength.getOrElse(-99L) should be > 0L
    }
  }

  "fileToReturn()" should {
    "return the expected filename" when {

      "the feature is disabled" in new Test {
        override protected def featureEnabled = false

        private val result = controller.fileToReturn("1.0", "employment_expenses_retrieve.yaml")
        result shouldBe "employment_expenses_retrieve.yaml"
      }

      "the feature is enabled" in new Test {
        private val result = controller.fileToReturn("1.0", "employment_expenses_retrieve.yaml")
        result shouldBe "employment_expenses_retrieve_openApiFeatureTest.yaml"
      }
    }
  }

  trait Test {
    val hc: HeaderCarrier = HeaderCarrier()

    protected def featureEnabled: Boolean = true

    MockedAppConfig.featureSwitches returns Configuration("openApiFeatureTest.enabled" -> featureEnabled)

    private val apiFactory = new ApiDefinitionFactory(mockAppConfig)

    private val config    = new Configuration(ConfigFactory.load())
    private val mimeTypes = HttpConfiguration.parseFileMimeTypes(config) ++ Map("yaml" -> "text/yaml", "md" -> "text/markdown")

    private val assetsMetadata =
      new DefaultAssetsMetadata(
        AssetsConfiguration(textContentTypes = Set("text/yaml", "text/markdown")),
        path => {
          Option(getClass.getResource(path))
        },
        new DefaultFileMimeTypes(FileMimeTypesConfiguration(mimeTypes))
      )

    private val errorHandler = new DefaultHttpErrorHandler()
    private val assets       = new Assets(errorHandler, assetsMetadata)
    protected val controller = new DocumentationController(apiFactory, cc, assets)(mockAppConfig)
  }

  "filenameWithFeatureName" should {
    "return a filename with the feature name inserted at the end" when {

      "it's a YAML file" in {
        val result = filenameWithFeatureName("subfolder/long.named.application.yaml", AFeature)
        result shouldBe "subfolder/long.named.application_aFeature.yaml"
      }

      "it's a Markdown file" in {
        val result = filenameWithFeatureName("application.md", AFeature)
        result shouldBe "application_aFeature.md"
      }

      "there's no filename extension" in {
        val result = filenameWithFeatureName("application", AFeature)
        result shouldBe "application_aFeature"
      }

      "the name ends with ." in {
        val result = filenameWithFeatureName("application.", AFeature)
        result shouldBe "application_aFeature."
      }

    }
  }

}
