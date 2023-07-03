package config.rewriters

import mocks.MockAppConfig
import play.api.Configuration
import support.UnitSpec

class OasFeatureRewriterSpec extends UnitSpec with MockAppConfig {

  MockAppConfig.featureSwitches returns Configuration("oasFeature.enabled" -> true)
  val rewriter = new OasFeatureRewriter()(mockAppConfig)

  MockAppConfig.endpointsEnabled("1.0") returns true

  "check and rewrite" should {

    val (check, rewrite) = rewriter.rewriteOasFeature

    "confirm it wants to rewrite the file" in {
      val confirmed = check("1.0", "employment_expenses_retrieve.yaml")
      confirmed shouldBe true
    }

    "rewrite using Handlebars" in {
      val yaml =
        """
          |summary: Retrieve Employment Expenses
          |description: |
          |  This endpoint enables you to retrieve existing employment expenses.
          |  A National Insurance number and tax year must be provided.
          |
          |  ### Test data
          |  {{#if (enabled "oasFeature")}}
          |  <p>Scenario simulations using Gov-Test-Scenario headers ARE ONLY AVAILABLE IN the sandbox environment.</p>
          |  {{else}}
          |  <p>Scenario simulations using Gov-Test-Scenario headers are only available in the sandbox environment.</p>
          |  {{/if}}
          |
          |tags:
          |  - Employment Expenses
          |""".stripMargin

      val expected =
        s"""
          |summary: Retrieve Employment Expenses
          |description: |
          |  This endpoint enables you to retrieve existing employment expenses.
          |  A National Insurance number and tax year must be provided.
          |
          |  ### Test data
          |${" "}${" "}
          |  <p>Scenario simulations using Gov-Test-Scenario headers ARE ONLY AVAILABLE IN the sandbox environment.</p>
          |${" "}${" "}
          |
          |tags:
          |  - Employment Expenses
          |""".stripMargin

      val result = rewrite("/...", "something.yaml", yaml)
      result shouldBe expected
    }
  }

}
