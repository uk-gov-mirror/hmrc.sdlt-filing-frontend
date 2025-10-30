/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.tasklist

import base.SpecBase
import models.{FullReturn, PrelimReturn}
import viewmodels.tasklist.{TLCannotStart, TLCompleted, TLFailed, TLInProgress, TLNotStarted, TaskListRowBuilder}

class TaskListRowBuilderSpec extends SpecBase {

  private val validPrelimReturn = PrelimReturn(
    stornId = "12345",
    purchaserIsCompany = "YES",
    surNameOrCompanyName = "Test Company",
    houseNumber = Some(23),
    addressLine1 = "Test Street",
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("TE23 5TT"),
    transactionType = "O"
  )

  private val fullReturnComplete = FullReturn(Some(validPrelimReturn))
  private val fullReturnIncomplete = FullReturn(None)

  "TaskListRowBuilder" - {

    "isComplete" - {

      "must return true when all checks are true" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true, true, true),
          prerequisites = _ => Seq()
        )

        builder.isComplete(fullReturnComplete) mustBe true
      }

      "must return false when some checks are false" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true, false, true),
          prerequisites = _ => Seq()
        )

        builder.isComplete(fullReturnComplete) mustBe false
      }

      "must return false when all checks are false" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(false, false, false),
          prerequisites = _ => Seq()
        )

        builder.isComplete(fullReturnComplete) mustBe false
      }

      "must return true when no checks are provided" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(),
          prerequisites = _ => Seq()
        )

        builder.isComplete(fullReturnComplete) mustBe true
      }

      "must evaluate checks based on FullReturn data" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = fr => Seq(fr.prelimReturn.isDefined),
          prerequisites = _ => Seq()
        )

        builder.isComplete(fullReturnComplete) mustBe true
        builder.isComplete(fullReturnIncomplete) mustBe false
      }
    }

    "prerequisitesMet" - {

      "must return true when no prerequisites exist" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        builder.prerequisitesMet(fullReturnComplete) mustBe true
      }

      "must return true when all prerequisites are complete" in {
        val prerequisite = TaskListRowBuilder(
          messageKey = _ => "prereq.key",
          url = _ => _ => "/prereq",
          tagId = "prereqId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(prerequisite)
        )

        builder.prerequisitesMet(fullReturnComplete) mustBe true
      }

      "must return false when prerequisites are not complete" in {
        val prerequisite = TaskListRowBuilder(
          messageKey = _ => "prereq.key",
          url = _ => _ => "/prereq",
          tagId = "prereqId",
          checks = _ => Seq(false),
          prerequisites = _ => Seq()
        )

        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(prerequisite)
        )

        builder.prerequisitesMet(fullReturnComplete) mustBe false
      }

      "must handle nested prerequisites" in {
        val level3 = TaskListRowBuilder(
          messageKey = _ => "level3.key",
          url = _ => _ => "/level3",
          tagId = "level3Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val level2 = TaskListRowBuilder(
          messageKey = _ => "level2.key",
          url = _ => _ => "/level2",
          tagId = "level2Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(level3)
        )

        val level1 = TaskListRowBuilder(
          messageKey = _ => "level1.key",
          url = _ => _ => "/level1",
          tagId = "level1Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(level2)
        )

        level1.prerequisitesMet(fullReturnComplete) mustBe true
      }

      "must return false when nested prerequisites fail" in {
        val level3 = TaskListRowBuilder(
          messageKey = _ => "level3.key",
          url = _ => _ => "/level3",
          tagId = "level3Id",
          checks = _ => Seq(false),
          prerequisites = _ => Seq()
        )

        val level2 = TaskListRowBuilder(
          messageKey = _ => "level2.key",
          url = _ => _ => "/level2",
          tagId = "level2Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(level3)
        )

        val level1 = TaskListRowBuilder(
          messageKey = _ => "level1.key",
          url = _ => _ => "/level1",
          tagId = "level1Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(level2)
        )

        level1.prerequisitesMet(fullReturnComplete) mustBe false
      }

      "must handle multiple prerequisites" in {
        val prereq1 = TaskListRowBuilder(
          messageKey = _ => "prereq1.key",
          url = _ => _ => "/prereq1",
          tagId = "prereq1Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val prereq2 = TaskListRowBuilder(
          messageKey = _ => "prereq2.key",
          url = _ => _ => "/prereq2",
          tagId = "prereq2Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(prereq1, prereq2)
        )

        builder.prerequisitesMet(fullReturnComplete) mustBe true
      }

      "must return false when any of multiple prerequisites fail" in {
        val prereq1 = TaskListRowBuilder(
          messageKey = _ => "prereq1.key",
          url = _ => _ => "/prereq1",
          tagId = "prereq1Id",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val prereq2 = TaskListRowBuilder(
          messageKey = _ => "prereq2.key",
          url = _ => _ => "/prereq2",
          tagId = "prereq2Id",
          checks = _ => Seq(false),
          prerequisites = _ => Seq()
        )

        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(prereq1, prereq2)
        )

        builder.prerequisitesMet(fullReturnComplete) mustBe false
      }
    }

    "build" - {

      "must return CannotStart status when prerequisites not met" in {
        val prerequisite = TaskListRowBuilder(
          messageKey = _ => "prereq.key",
          url = _ => _ => "/prereq",
          tagId = "prereqId",
          checks = _ => Seq(false),
          prerequisites = _ => Seq()
        )

        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(prerequisite)
        )

        val result = builder.build(fullReturnComplete)

        result.status mustBe TLCannotStart
      }

      "must return Failed status when error is true" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(),
          error = _ => true
        )

        val result = builder.build(fullReturnComplete)

        result.status mustBe TLFailed
      }

      "must return Completed status when all checks are true" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true, true),
          prerequisites = _ => Seq()
        )

        val result = builder.build(fullReturnComplete)

        result.status mustBe TLCompleted
      }

      "must return InProgress status when some checks are true" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true, false),
          prerequisites = _ => Seq()
        )

        val result = builder.build(fullReturnComplete)

        result.status mustBe TLInProgress
      }

      "must return NotStarted status when no checks are true" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(false, false),
          prerequisites = _ => Seq()
        )

        val result = builder.build(fullReturnComplete)

        result.status mustBe TLNotStarted
      }

      "must use messageKey from FullReturn" in {
        val builder = TaskListRowBuilder(
          messageKey = fr => if (fr.prelimReturn.isDefined) "complete.key" else "incomplete.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val resultComplete = builder.build(fullReturnComplete)
        val resultIncomplete = builder.build(fullReturnIncomplete)

        resultComplete.messageKey mustBe "complete.key"
        resultIncomplete.messageKey mustBe "incomplete.key"
      }

      "must set correct tagId" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "myCustomTagId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq()
        )

        val result = builder.build(fullReturnComplete)

        result.tagId mustBe "myCustomTagId"
      }

      "must use canEdit function to determine editability" in {
        val builder = TaskListRowBuilder(
          messageKey = _ => "test.key",
          url = _ => _ => "/test",
          tagId = "testId",
          checks = _ => Seq(true),
          prerequisites = _ => Seq(),
          canEdit = status => status == TLCompleted
        )

        val result = builder.build(fullReturnComplete)

        result.canEdit mustBe true
      }
    }
  }
}