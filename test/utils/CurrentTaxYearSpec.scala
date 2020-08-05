/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import org.joda.time.DateTime
import support.UnitSpec

class CurrentTaxYearSpec extends UnitSpec {

  class TestTaxYear extends CurrentTaxYear

  lazy val dateAfterApril = DateTime.parse("2019-08-05")
  lazy val dateBeforeApril = DateTime.parse("2020-03-05")
  private val thisYear = 2020

  "getCurrentTaxYear" should {
    "return the current tax year" when {
      "a date after the start of the tax year is given" in {
        val currentTaxYear = new TestTaxYear()
        currentTaxYear.getCurrentTaxYear(dateAfterApril) shouldBe thisYear
      }
      "a date before the start of the tax year is given" in {
        val currentTaxYear = new TestTaxYear()
        currentTaxYear.getCurrentTaxYear(dateBeforeApril) shouldBe thisYear
      }
    }
  }
}
