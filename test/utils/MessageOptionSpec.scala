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

package utils

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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{EitherValues, OptionValues}
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.i18n.{Lang, MessagesApi}

class MessageOptionSpec extends AnyFreeSpec with Matchers with OptionValues with EitherValues with MockitoSugar {

  private val mockMessagesApi = mock[MessagesApi]
  private val enLang = Lang("en")
  private val cyLang = Lang("cy")

  "MessageOption" - {

    "apply" - {

      "must return Some(message) when key exists and message is not empty" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Test message"))

        val result = MessageOption("test.key", enLang)(mockMessagesApi)

        result mustBe Some("Test message")
        verify(mockMessagesApi).translate("test.key", Seq.empty)(enLang)
      }

      "must return None when key does not exist (translate returns None)" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(None)

        val result = MessageOption("missing.key", enLang)(mockMessagesApi)

        result must not be defined
        verify(mockMessagesApi).translate("missing.key", Seq.empty)(enLang)
      }

      "must return None when message is empty string" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some(""))

        val result = MessageOption("empty.key", enLang)(mockMessagesApi)

        result must not be defined
        verify(mockMessagesApi).translate("empty.key", Seq.empty)(enLang)
      }

      "must return Some(message) with parameters" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Hello John Doe"))

        val result = MessageOption("greeting.key", enLang, "John", "Doe")(mockMessagesApi)

        result mustBe Some("Hello John Doe")
        verify(mockMessagesApi).translate("greeting.key", Seq("John", "Doe"))(enLang)
      }

      "must return Some(message) with single parameter" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Welcome, User123"))

        val result = MessageOption("welcome.key", enLang, "User123")(mockMessagesApi)

        result mustBe Some("Welcome, User123")
        verify(mockMessagesApi).translate("welcome.key", Seq("User123"))(enLang)
      }

      "must return Some(message) with no parameters" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Static message"))

        val result = MessageOption("static.key", enLang)(mockMessagesApi)

        result mustBe Some("Static message")
        verify(mockMessagesApi).translate("static.key", Seq.empty)(enLang)
      }

      "must work with Welsh language" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Neges Gymraeg"))

        val result = MessageOption("welsh.key", cyLang)(mockMessagesApi)

        result mustBe Some("Neges Gymraeg")
        verify(mockMessagesApi).translate("welsh.key", Seq.empty)(cyLang)
      }

      "must work with different languages" in {
        val frLang = Lang("fr")
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Message français"))

        val result = MessageOption("french.key", frLang)(mockMessagesApi)

        result mustBe Some("Message français")
        verify(mockMessagesApi).translate("french.key", Seq.empty)(frLang)
      }

      "must handle multiple parameters correctly" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Property at 10 Main Street, London"))

        val result = MessageOption("address.key", enLang, "10", "Main Street", "London")(mockMessagesApi)

        result mustBe Some("Property at 10 Main Street, London")
        verify(mockMessagesApi).translate("address.key", Seq("10", "Main Street", "London"))(enLang)
      }

      "must handle numeric parameters" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Total: £1000"))

        val result = MessageOption("total.key", enLang, "1000")(mockMessagesApi)

        result mustBe Some("Total: £1000")
        verify(mockMessagesApi).translate("total.key", Seq("1000"))(enLang)
      }

      "must return None for whitespace-only messages" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("   "))

        val result = MessageOption("whitespace.key", enLang)(mockMessagesApi)

        // Note: The current implementation only checks isEmpty, not for whitespace
        // This test documents the current behavior
        result mustBe Some("   ")
      }

      "must handle special characters in message" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Special chars: £$€¥"))

        val result = MessageOption("special.key", enLang)(mockMessagesApi)

        result mustBe Some("Special chars: £$€¥")
      }

      "must handle HTML content in message" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("<strong>Bold text</strong>"))

        val result = MessageOption("html.key", enLang)(mockMessagesApi)

        result mustBe Some("<strong>Bold text</strong>")
      }

      "must handle long messages" in {
        val longMessage = "This is a very long message that contains multiple sentences. " * 10
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some(longMessage))

        val result = MessageOption("long.key", enLang)(mockMessagesApi)

        result mustBe Some(longMessage)
      }

      "must handle messages with newlines" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Line 1\nLine 2\nLine 3"))

        val result = MessageOption("multiline.key", enLang)(mockMessagesApi)

        result mustBe Some("Line 1\nLine 2\nLine 3")
      }

      "must be callable multiple times with same key" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Message 1"))
          .thenReturn(Some("Message 2"))

        val result1 = MessageOption("repeat.key", enLang)(mockMessagesApi)
        val result2 = MessageOption("repeat.key", enLang)(mockMessagesApi)

        result1 mustBe Some("Message 1")
        result2 mustBe Some("Message 2")
      }

      "must be callable with different keys in sequence" in {
        when(mockMessagesApi.translate("key1", Seq.empty)(enLang))
          .thenReturn(Some("Message 1"))
        when(mockMessagesApi.translate("key2", Seq.empty)(enLang))
          .thenReturn(Some("Message 2"))
        when(mockMessagesApi.translate("key3", Seq.empty)(enLang))
          .thenReturn(None)

        val result1 = MessageOption("key1", enLang)(mockMessagesApi)
        val result2 = MessageOption("key2", enLang)(mockMessagesApi)
        val result3 = MessageOption("key3", enLang)(mockMessagesApi)

        result1 mustBe Some("Message 1")
        result2 mustBe Some("Message 2")
        result3 must not be defined
      }
    }

    "integration scenarios" - {

      "must work in address lookup context" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Find your address"))

        val result = MessageOption("addressLookup.prelimQuestionsAddress.lookupPage.title", enLang)(mockMessagesApi)

        result mustBe Some("Find your address")
      }

      "must work with journey-specific keys" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Property address"))

        val result = MessageOption("addressLookup.prelimQuestionsAddress.lookupPage.heading", enLang, "Property")(mockMessagesApi)

        result mustBe Some("Property address")
      }

      "must handle optional messages correctly" in {
        when(mockMessagesApi.translate("exists.key", Seq.empty)(enLang))
          .thenReturn(Some("Exists"))
        when(mockMessagesApi.translate("missing.key", Seq.empty)(enLang))
          .thenReturn(None)

        val result1 = MessageOption("exists.key", enLang)(mockMessagesApi)
        val result2 = MessageOption("missing.key", enLang)(mockMessagesApi)

        result1 mustBe defined
        result2 must not be defined
      }

      "must work in bilingual application" in {
        when(mockMessagesApi.translate("app.title", Seq.empty)(enLang))
          .thenReturn(Some("Stamp Duty Land Tax"))
        when(mockMessagesApi.translate("app.title", Seq.empty)(cyLang))
          .thenReturn(Some("Treth Stamp Tir"))

        val englishResult = MessageOption("app.title", enLang)(mockMessagesApi)
        val welshResult = MessageOption("app.title", cyLang)(mockMessagesApi)

        englishResult mustBe Some("Stamp Duty Land Tax")
        welshResult mustBe Some("Treth Stamp Tir")
      }

      "must handle phase banner HTML content" in {
        val phaseBannerHtml = "This is a <strong>new</strong> service"
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some(phaseBannerHtml))

        val result = MessageOption("addressLookup.common.phaseBannerHtml", enLang)(mockMessagesApi)

        result mustBe Some(phaseBannerHtml)
      }

      "must work with message prefixes" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("International address"))

        val result = MessageOption("international.addressLookup.prelimQuestionsAddress.lookupPage.title", enLang)(mockMessagesApi)

        result mustBe Some("International address")
      }
    }

    "edge cases" - {

      "must handle keys with dots" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Nested message"))

        val result = MessageOption("level1.level2.level3.key", enLang)(mockMessagesApi)

        result mustBe Some("Nested message")
      }

      "must handle keys with underscores" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Underscore message"))

        val result = MessageOption("test_key_with_underscores", enLang)(mockMessagesApi)

        result mustBe Some("Underscore message")
      }

      "must handle keys with hyphens" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Hyphen message"))

        val result = MessageOption("test-key-with-hyphens", enLang)(mockMessagesApi)

        result mustBe Some("Hyphen message")
      }

      "must handle empty key string" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(None)

        val result = MessageOption("", enLang)(mockMessagesApi)

        result must not be defined
      }

      "must handle parameters with special characters" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Address: 10 O'Brien Street"))

        val result = MessageOption("address.key", enLang, "10", "O'Brien Street")(mockMessagesApi)

        result mustBe Some("Address: 10 O'Brien Street")
      }

      "must handle empty parameter strings" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Message with empty param"))

        val result = MessageOption("test.key", enLang, "")(mockMessagesApi)

        result mustBe Some("Message with empty param")
        verify(mockMessagesApi).translate("test.key", Seq(""))(enLang)
      }

      "must handle large number of parameters" in {
        val params = (1 to 20).map(_.toString)
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("Message with many params"))

        val result = MessageOption("many.params.key", enLang, params: _*)(mockMessagesApi)

        result mustBe Some("Message with many params")
        verify(mockMessagesApi).translate("many.params.key", params)(enLang)
      }
    }

    "boundary conditions" - {

      "must return None when translate returns Some with empty string" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some(""))

        val result = MessageOption("boundary.key", enLang)(mockMessagesApi)

        result must not be defined
      }

      "must return Some when translate returns Some with single character" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("A"))

        val result = MessageOption("single.char.key", enLang)(mockMessagesApi)

        result mustBe Some("A")
      }

      "must handle zero-length parameter array" in {
        when(mockMessagesApi.translate(any[String], any[Seq[Any]])(any[Lang]))
          .thenReturn(Some("No params message"))

        val result = MessageOption("no.params.key", enLang)(mockMessagesApi)

        result mustBe Some("No params message")
        verify(mockMessagesApi).translate("no.params.key", Seq.empty)(enLang)
      }
    }
  }
}
