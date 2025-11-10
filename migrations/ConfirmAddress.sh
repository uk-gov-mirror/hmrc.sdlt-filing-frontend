#!/bin/bash

echo ""
echo "Applying migration ConfirmAddress"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /confirmAddress                        controllers.ConfirmAddressController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /confirmAddress                        controllers.ConfirmAddressController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeConfirmAddress                  controllers.ConfirmAddressController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeConfirmAddress                  controllers.ConfirmAddressController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "confirmAddress.title = confirmAddress" >> ../conf/messages.en
echo "confirmAddress.heading = confirmAddress" >> ../conf/messages.en
echo "confirmAddress.option1 = Option 1" >> ../conf/messages.en
echo "confirmAddress.option2 = Option 2" >> ../conf/messages.en
echo "confirmAddress.checkYourAnswersLabel = confirmAddress" >> ../conf/messages.en
echo "confirmAddress.error.required = Select confirmAddress" >> ../conf/messages.en
echo "confirmAddress.change.hidden = ConfirmAddress" >> ../conf/messages.en

echo "Adding to ModelGenerators"
awk '/trait ModelGenerators/ {\
    print;\
    print "";\
    print "  implicit lazy val arbitraryConfirmAddress: Arbitrary[ConfirmAddress] =";\
    print "    Arbitrary {";\
    print "      Gen.oneOf(ConfirmAddress.values.toSeq)";\
    print "    }";\
    next }1' ../test-utils/generators/ModelGenerators.scala > tmp && mv tmp ../test-utils/generators/ModelGenerators.scala

echo "Migration ConfirmAddress completed"
