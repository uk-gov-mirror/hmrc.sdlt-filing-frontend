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

import com.google.inject.Inject
import models.requests.DataRequest
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import services.crossflow.fields.CrossFlowValidationService
import utils.LoggingUtil

import scala.concurrent.{ExecutionContext, Future}

class CrossFlowCheckAction @Inject()(
  crossFlowService: CrossFlowValidationService
)(implicit val executionContext: ExecutionContext) extends ActionFilter[DataRequest] with LoggingUtil {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    val submissionExists = request.userAnswers.fullReturn.flatMap(_.submission).isDefined

    if (submissionExists) {
      Future.successful(None)
    } else {
      val failureCount = crossFlowService.failureCount(request.userAnswers)

      if (failureCount > 0) {
        logger.warn(s"Blocking submission, $failureCount crossflow errors on the return")
        Future.successful(Some(Redirect(controllers.routes.ReturnTaskListController.onPageLoad())))
      } else {
        Future.successful(None)
      }
    }
  }
}
