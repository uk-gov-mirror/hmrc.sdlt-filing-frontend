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

package services.crossflow.fields

import models.UserAnswers
import services.crossflow.*
import utils.LoggingUtil
import models.Land
import services.crossflow.errors.CrossFlowProjections

import javax.inject.{Inject, Singleton}

@Singleton
class CrossFlowValidationService @Inject() (
                                             rules:     Set[CrossFlowRule],
                                             landRules: Set[LandRule]
                                           ) extends LoggingUtil:
  
  private val ruleList     = rules.toSeq
  private val landRuleList = landRules.toSeq

  
  private def allLands(ua: UserAnswers): Seq[Land] = {
    val sessionLand    = CrossFlowProjections.currentSessionLand(ua).toSeq
    val committedLands = ua.fullReturn.flatMap(_.land).getOrElse(Seq.empty)
    sessionLand ++ committedLands
  }

  private def committedLands(ua: UserAnswers): Seq[Land] =
    ua.fullReturn.flatMap(_.land).getOrElse(Seq.empty)

  def landFailuresOnly(ruleIds: Set[String], ua: UserAnswers): Seq[(Land, Seq[CrossFlowFailure])] =
    landFailuresGrouped(ua)
      .map { case (land, failures) => land -> failures.filter(f => ruleIds.contains(f.ruleId)) }
      .filter(_._2.nonEmpty)

  def landFailuresExcluding(ruleIds: Set[String], ua: UserAnswers): Seq[(Land, Seq[CrossFlowFailure])] =
    landFailuresGrouped(ua)
      .map { case (land, failures) => land -> failures.filterNot(f => ruleIds.contains(f.ruleId)) }
      .filter(_._2.nonEmpty)

  def landFailures(ua: UserAnswers): Seq[(Land, CrossFlowFailure)] =
    for {
      land    <- allLands(ua)
      rule    <- landRuleList
      failure <- rule.validate(land, ua)
    } yield land -> failure
  
  def landFailuresGrouped(ua: UserAnswers): Seq[(Land, Seq[CrossFlowFailure])] =
    (for {
      land    <- committedLands(ua)
      rule    <- landRuleList
      failure <- rule.validate(land, ua)
    } yield land -> failure)
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toSeq
      .filter(_._2.nonEmpty)

  private val byAffectedSection: Map[ReturnSection, Seq[CrossFlowRule]] =
    ruleList.groupBy(_.affects)

  private val byTargetPage: Map[PageId, Seq[CrossFlowRule]] =
    ruleList.flatMap(r => r.targets.map(_.page).distinct.map(_ -> r))
      .groupBy(_._1).view.mapValues(_.map(_._2)).toMap

  def validateAll(ua: UserAnswers): Seq[CrossFlowFailure] =
    ruleList.flatMap(_.validate(ua)).sortBy(_.ruleId)

  def failuresAffecting(section: ReturnSection, ua: UserAnswers): Seq[CrossFlowFailure] = {
    val fromCrossFlowRules =
      byAffectedSection.getOrElse(section, Nil).flatMap(_.validate(ua))

    val fromLandRules =
      if (section == ReturnSection.Land) landFailures(ua).map(_._2) else Nil

    (fromCrossFlowRules ++ fromLandRules).sortBy(_.ruleId)
  }

  def failuresForPage(page: PageId, ua: UserAnswers): Seq[CrossFlowFailure] = {
    val fromCrossFlowRules =
      byTargetPage.getOrElse(page, Nil)
        .filterNot(_.aggregateOnly)
        .flatMap(_.validate(ua))
        .filter(_.appearsOn(page))

    val fromLandRules =
      CrossFlowProjections.currentSessionLand(ua).toSeq.flatMap { sessionLand =>
        landRuleList
          .filterNot(_.aggregateOnly)
          .filter(_.targets.exists(_.page == page))
          .flatMap(_.validate(sessionLand, ua))
          .filter(_.appearsOn(page))
      }

    (fromCrossFlowRules ++ fromLandRules).sortBy(_.ruleId)
  }

  def isSectionValid(section: ReturnSection, ua: UserAnswers): Boolean =
    byAffectedSection.getOrElse(section, Nil).forall(_.validate(ua).isEmpty)

  def invalidSections(ua: UserAnswers): Set[ReturnSection] = {
    val fromRules     = validateAll(ua).map(_.affects).toSet
    val landIsInvalid = landFailures(ua).nonEmpty
    if (landIsInvalid) fromRules + ReturnSection.Land else fromRules
  }

  def hasFailures(ua: UserAnswers): Boolean =
    rules.exists(_.validate(ua).isDefined) || landFailures(ua).nonEmpty

  def failureCount(ua: UserAnswers): Int =
    validateAll(ua).size + landFailures(ua).size

  def sectionStatus(section: ReturnSection, ua: UserAnswers): SectionStatus =
    val fs = failuresAffecting(section, ua)
    if fs.isEmpty then SectionStatus.empty(section)
    else SectionStatus(
      section     = section,
      hasFailures = true,
      ruleIds     = fs.map(_.ruleId),
      messageKeys = fs.map(_.messageKey).distinct,
      targets     = fs.flatMap(_.targets).distinct
    )

  def sectionStatuses(ua: UserAnswers): Map[ReturnSection, SectionStatus] = {
    val fromCrossFlowRules: Map[ReturnSection, SectionStatus] =
      validateAll(ua).groupBy(_.affects).map { case (sec, fs) =>
        sec -> SectionStatus(
          section     = sec,
          hasFailures = true,
          ruleIds     = fs.map(_.ruleId),
          messageKeys = fs.map(_.messageKey).distinct,
          targets     = fs.flatMap(_.targets).distinct
        )
      }

    val fromLandRules: Option[SectionStatus] = {
      val fs = landFailures(ua).map(_._2)
      if (fs.isEmpty) None
      else Some(SectionStatus(
        section     = ReturnSection.Land,
        hasFailures = true,
        ruleIds     = fs.map(_.ruleId).distinct,
        messageKeys = fs.map(_.messageKey).distinct,
        targets     = fs.flatMap(_.targets).distinct
      ))
    }

    fromLandRules match {
      case Some(landStatus) =>
        val existing = fromCrossFlowRules.getOrElse(ReturnSection.Land,
          SectionStatus.empty(ReturnSection.Land))
        val merged = SectionStatus(
          section     = ReturnSection.Land,
          hasFailures = true,
          ruleIds     = (existing.ruleIds ++ landStatus.ruleIds).distinct,
          messageKeys = (existing.messageKeys ++ landStatus.messageKeys).distinct,
          targets     = (existing.targets ++ landStatus.targets).distinct
        )
        fromCrossFlowRules + (ReturnSection.Land -> merged)
      case None => fromCrossFlowRules
    }
  }

  def rulesForPage(page: PageId): Seq[CrossFlowRule] =
    byTargetPage.getOrElse(page, Nil)

  def rulesTriggeredBy(section: ReturnSection): Seq[CrossFlowRule] =
    ruleList.filter(_.inputs.contains(section))