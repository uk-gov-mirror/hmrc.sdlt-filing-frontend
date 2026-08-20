/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.actions

import base.SpecBase
import constants.FullReturnConstants.{completeFullReturn, completeSubmission}
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.crossflow.fields.CrossFlowValidationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CrossFlowCheckActionSpec extends SpecBase with MockitoSugar {

  val mockCrossFlowService: CrossFlowValidationService = mock[CrossFlowValidationService]

  class Harness extends CrossFlowCheckAction(mockCrossFlowService) {
    def callFilter[A](request: DataRequest[A]): Future[Option[Result]] = filter(request)
  }

  "CrossFlowCheckAction" - {

    "must allow the request to continue when the return has no crossflow errors" in {
      when(mockCrossFlowService.failureCount(any())).thenReturn(0)

      val userAnswers = emptyUserAnswers.copy(fullReturn = Some(completeFullReturn.copy(submission = None)))
      val result      = new Harness().callFilter(DataRequest(FakeRequest(), "id", userAnswers = userAnswers)).futureValue

      result mustBe None
    }

    "must redirect to the task list when the return has crossflow errors" in {
      when(mockCrossFlowService.failureCount(any())).thenReturn(2)

      val userAnswers = emptyUserAnswers.copy(fullReturn = Some(completeFullReturn.copy(submission = None)))
      val result      = new Harness().callFilter(DataRequest(FakeRequest(), "id", userAnswers = userAnswers)).futureValue

      result mustBe defined

      val redirectResult = result.value

      redirectResult.header.status mustEqual SEE_OTHER

      redirectResult.header.headers("Location") mustEqual
        controllers.routes.ReturnTaskListController.onPageLoad().url
    }

    "must allow the request to continue when a submission already exists, even with crossflow errors" in {
      when(mockCrossFlowService.failureCount(any())).thenReturn(2)

      val userAnswers = emptyUserAnswers.copy(fullReturn = Some(completeFullReturn.copy(submission = Some(completeSubmission))))
      val result      = new Harness().callFilter(DataRequest(FakeRequest(), "id", userAnswers = userAnswers)).futureValue

      result mustBe None
    }

    "must allow the request to continue when there is no return" in {
      when(mockCrossFlowService.failureCount(any())).thenReturn(0)

      val result = new Harness().callFilter(DataRequest(FakeRequest(), "id", userAnswers = emptyUserAnswers)).futureValue

      result mustBe None
    }
  }
}
