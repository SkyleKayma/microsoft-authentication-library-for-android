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
//  FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.msal.automationapp.testpass.broker.crosscloud;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthResult;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalAuthTestParams;
import com.microsoft.identity.client.msal.automationapp.sdk.MsalSdk;
import com.microsoft.identity.client.msal.automationapp.testpass.broker.AbstractGuestAccountMsalBrokerUiTest;
import com.microsoft.identity.client.ui.automation.TokenRequestTimeout;
import com.microsoft.identity.client.ui.automation.annotations.RetryOnFailure;
import com.microsoft.identity.client.ui.automation.interaction.OnInteractionRequired;
import com.microsoft.identity.client.ui.automation.interaction.PromptHandlerParameters;
import com.microsoft.identity.client.ui.automation.interaction.PromptParameter;
import com.microsoft.identity.client.ui.automation.interaction.microsoftsts.AadPromptHandler;
import com.microsoft.identity.common.java.util.ThreadUtils;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomeAzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.GuestHomedIn;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

// Acquire Token from Cross Cloud after acquiring token from home cloud
// https://identitydivision.visualstudio.com/DefaultCollection/IDDP/_workitems/edit/1592465
@RunWith(Parameterized.class)
@RetryOnFailure(retryCount = 2)
public class TestCase1592465 extends AbstractGuestAccountMsalBrokerUiTest {

    private final GuestHomeAzureEnvironment mGuestHomeAzureEnvironment;
    private final String mHomeCloud;
    private final String mCrossCloud;

    public TestCase1592465(final String name, final @NonNull GuestHomeAzureEnvironment guestHomeAzureEnvironment, final String homeCloud, final String crossCloud) {
        mGuestHomeAzureEnvironment = guestHomeAzureEnvironment;
        mHomeCloud = homeCloud;
        mCrossCloud = crossCloud;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection guestHomeAzureEnvironment() {
        return Arrays.asList(new Object[][]{
                {"AZURE_US_GOV", GuestHomeAzureEnvironment.AZURE_US_GOVERNMENT, /*homeCloud*/"https://login.microsoftonline.us", /*crossCloud*/"https://login.microsoftonline.com"},
        });
    }

    @Test
    public void test_1592465() throws Throwable {
        final String userName = mGuestUser.getHomeUpn();
        final String password = mLabClient.getPasswordForGuestUser(mGuestUser);

        final OnInteractionRequired homeCloudInteractionHandler = () -> {
            final PromptHandlerParameters promptHandlerParameters =
                    PromptHandlerParameters.builder()
                    .prompt(PromptParameter.SELECT_ACCOUNT)
                    .loginHint(userName)
                    .broker(mBroker)
                    .build();
            final AadPromptHandler promptHandler = new AadPromptHandler(promptHandlerParameters);
            promptHandler.handlePrompt(userName, password);
        };

        final MsalAuthTestParams acquireTokenHomeCloudAuthParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(userName)
                .scopes(Arrays.asList(getScopes()))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(getHomeCloudAuthority())
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        final MsalSdk msalSdk = new MsalSdk();
        // Acquire token interactively from home cloud
        final MsalAuthResult acquireTokenHomeCloudResult = msalSdk.acquireTokenInteractive(acquireTokenHomeCloudAuthParams, homeCloudInteractionHandler, TokenRequestTimeout.SHORT);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenHomeCloudResult.getAccessToken()));

        final MsalAuthTestParams acquireTokenCrossCloudAuthParams = MsalAuthTestParams.builder()
                .activity(mActivity)
                .loginHint(userName)
                .scopes(Arrays.asList(getScopes()))
                .promptParameter(Prompt.SELECT_ACCOUNT)
                .authority(getCrossCloudAuthority())
                .msalConfigResourceId(getConfigFileResourceId())
                .build();

        // Acquire token silently from cross cloud, expected to throw UiRequired exception
        final MsalAuthResult acquireTokenSilentlyCrossCloudResult = msalSdk.acquireTokenSilent(acquireTokenCrossCloudAuthParams, TokenRequestTimeout.SHORT);
        final MsalUiRequiredException exception = (MsalUiRequiredException) acquireTokenSilentlyCrossCloudResult.getException();
        Assert.assertNotNull("Verify Exception is returned", exception);
        Assert.assertEquals("Verify error code is invalid_grant", "invalid_grant", exception.getErrorCode());

        ThreadUtils.sleepSafely(3000, "Sleep failed", "Interrupted");

        // Acquire token interactively from cross cloud, expected to get a new access token
        // We expect that this does not prompt for credentials
        final OnInteractionRequired crossCloudInteractionHandler = () -> { };
        final MsalAuthResult acquireTokenCrossCloudResult = msalSdk.acquireTokenInteractive(acquireTokenCrossCloudAuthParams, crossCloudInteractionHandler, TokenRequestTimeout.SILENT);
        Assert.assertFalse("Verify accessToken is not empty", TextUtils.isEmpty(acquireTokenCrossCloudResult.getAccessToken()));

        Assert.assertNotEquals("CrossCloud request gets new access token", acquireTokenCrossCloudResult.getAccessToken(), acquireTokenHomeCloudResult.getAccessToken());
    }

    @Override
    public LabQuery getLabQuery() {
        return LabQuery.builder()
                .userType(UserType.GUEST)
                .guestHomeAzureEnvironment(mGuestHomeAzureEnvironment)
                .guestHomedIn(GuestHomedIn.HOST_AZURE_AD)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .build();
    }

    @Override
    public String[] getScopes() {
        return new String[]{"User.read"};
    }

    @Override
    public String getAuthority() {
        return getCrossCloudAuthority();
    }

    private String getHomeCloudAuthority() {
        return mHomeCloud + "/" + "common";
    }

    private String getCrossCloudAuthority() {
        return mCrossCloud + "/" + mGuestUser.getGuestLabTenants().get(0);
    }
}
