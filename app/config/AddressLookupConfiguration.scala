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

package config

import models.address._
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.{Call, RequestHeader}

import javax.inject.Inject

class AddressLookupConfiguration @Inject()(implicit appConfig: FrontendAppConfig, messages: MessagesApi) {

  def apply(journeyId: AddressLookupJourneyIdentifier.Value,
            continueRoute: Call,
            useUkMode: Boolean,
            optName: Option[String] = None,
            mandatoryFieldsConfigModel: MandatoryFieldsConfigModel)(implicit rh: RequestHeader): AddressLookupConfigurationModel = {
    val english = Lang("en")
    val welsh = Lang("cy")

    AddressLookupConfigurationModel(
      version = 2,
      options = AddressLookupOptionsModel(
        continueUrl = appConfig.host + continueRoute.url,
        signOutHref = Some(appConfig.feedbackUrl),
        phaseFeedbackLink = Some(appConfig.feedbackUrl),
        deskProServiceName = Some(appConfig.contactFormServiceIdentifier),
        showPhaseBanner = Some(true),
        showBackButtons = Some(true),
        includeHMRCBranding = Some(true),
        ukMode = Some(useUkMode),
        selectPageConfig = AddressLookupSelectConfigModel (
          showSearchAgainLink = Some(false)
        ),
        confirmPageConfig = AddressLookupConfirmConfigModel(
          showChangeLinkcontinueUrl = Some(true),
          showSubHeadingAndInfo = Some(false),
          showSearchAgainLink = Some(false),
          showConfirmChangeText = Some(false)
        ),
        manualAddressEntryConfig = ManualAddressEntryConfig(
          mandatoryFields = mandatoryFieldsConfigModel
        )
      ),
      labels = AddressMessageLanguageModel(
        en = AddressMessagesModel.forJourney(journeyId.toString, english, useUkMode, optName),
        cy = AddressMessagesModel.forJourney(journeyId.toString, welsh, useUkMode, optName)
      )
    )
  }

}
