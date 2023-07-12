//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.msalonly.basic;

import android.text.TextUtils;

import com.microsoft.identity.client.msal.automationapp.R;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractMsalBrokerTest;
import com.microsoft.identity.client.ui.automation.annotations.LTWTests;
import com.microsoft.identity.client.ui.automation.app.MsalTestApp;
import com.microsoft.identity.client.ui.automation.app.OneAuthTestApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.interaction.FirstPartyAppPromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.MicrosoftStsPromptHandlerParameters;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;

import org.junit.Assert;
import org.junit.Test;

//  Updated LTW, Updated Auth app and uninstall LTW
// https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2572249
@LTWTests
public class TestCase2572249 extends AbstractMsalBrokerTest {

    @Test
    public void test_2572249() throws Throwable {
        final String username = mLabAccount.getUsername();
        final String password = mLabAccount.getPassword();

        mBroker.uninstall();

        // install updated LTW
        final BrokerLTW brokerLTW = new BrokerLTW();
        brokerLTW.uninstall();
        brokerLTW.install();

        // install updated auth app
        final BrokerMicrosoftAuthenticator brokerMicrosoftAuthenticator = new BrokerMicrosoftAuthenticator();
        brokerMicrosoftAuthenticator.uninstall();
        brokerMicrosoftAuthenticator.install();

        // acquire token interactively in MsalTestApp
        final MsalTestApp msalTestApp = new MsalTestApp();
        msalTestApp.uninstall();
        msalTestApp.install();
        msalTestApp.launch();
        msalTestApp.handleFirstRun();

        final MicrosoftStsPromptHandlerParameters promptHandlerParametersMsal = MicrosoftStsPromptHandlerParameters.builder()
                .prompt(PromptParameter.SELECT_ACCOUNT)
                .loginHint(username)
                .sessionExpected(false)
                .broker(mBroker)
                .expectingBrokerAccountChooserActivity(false)
                .expectingProvidedAccountInBroker(false)
                .expectingLoginPageAccountPicker(false)
                .expectingProvidedAccountInCookie(false)
                .consentPageExpected(false)
                .passwordPageExpected(true)
                .speedBumpExpected(false)
                .registerPageExpected(false)
                .enrollPageExpected(false)
                .staySignedInPageExpected(false)
                .verifyYourIdentityPageExpected(false)
                .howWouldYouLikeToSignInExpected(false)
                .build();

        String tokenMsal = msalTestApp.acquireToken(username, password, promptHandlerParametersMsal, false);
        Assert.assertNotNull(tokenMsal);

        // uninstall LTW
        brokerLTW.uninstall();

        // install OneAuthTestApp
        final OneAuthTestApp oneAuthTestApp = new OneAuthTestApp();
        oneAuthTestApp.uninstall();
        oneAuthTestApp.install();
        oneAuthTestApp.handleFirstRun();

        // sign in to OneAuthTestApp
        // should not prompt for password
        final FirstPartyAppPromptHandlerParameters promptHandlerParametersOneAuth = FirstPartyAppPromptHandlerParameters.builder()
                .broker(mBroker)
                .prompt(PromptParameter.LOGIN)
                .loginHint(username)
                .passwordPageExpected(false)
                .consentPageExpected(false)
                .speedBumpExpected(false)
                .sessionExpected(false)
                .expectingBrokerAccountChooserActivity(false)
                .expectingLoginPageAccountPicker(false)
                .enrollPageExpected(false)
                .build();

        final String tokenOneAuth = oneAuthTestApp.acquireTokenInteractive(username, password, promptHandlerParametersOneAuth);
        Assert.assertFalse(TextUtils.isEmpty(tokenOneAuth));

    }


    @Override
    public LabQuery getLabQuery() {
        return null;
    }

    @Override
    public TempUserType getTempUserType() {
        return TempUserType.BASIC;
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return mApplication.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
    }

    @Override
    public int getConfigFileResourceId() {
        return R.raw.msal_config_default;
    }
}
