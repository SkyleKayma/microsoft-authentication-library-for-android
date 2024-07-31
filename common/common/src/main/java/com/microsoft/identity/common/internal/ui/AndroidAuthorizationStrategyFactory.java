// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.ui;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.microsoft.identity.common.internal.ui.browser.Browser;
import com.microsoft.identity.common.internal.ui.browser.DefaultBrowserAuthorizationStrategy;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy;
import com.microsoft.identity.common.internal.ui.browser.BrowserSelector;
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.java.strategies.IAuthorizationStrategyFactory;

import java.util.List;

import lombok.Builder;
import lombok.experimental.Accessors;

// Suppressing rawtype warnings due to the generic types AuthorizationStrategy, AuthorizationStrategyFactory, EmbeddedWebViewAuthorizationStrategy and BrowserAuthorizationStrategy
@SuppressWarnings(WarningType.rawtype_warning)
@Builder
@Accessors(prefix = "m")
public class AndroidAuthorizationStrategyFactory implements IAuthorizationStrategyFactory{
    private static final String TAG = AndroidAuthorizationStrategyFactory.class.getSimpleName();

    private final Context mContext;
    private final Activity mActivity;
    private final Fragment mFragment;

    @Override
    public IAuthorizationStrategy getAuthorizationStrategy(
            @NonNull final InteractiveTokenCommandParameters parameters) {
        final String methodTag = TAG + ":getAuthorizationStrategy";
        //Valid if available browser installed. Will fallback to embedded webView if no browser available.

        if (parameters.getAuthorizationAgent() == AuthorizationAgent.WEBVIEW) {
            Logger.info(methodTag, "Use webView for authorization.");
            return getGenericAuthorizationStrategy();
        }

        try {
            final Browser browser = BrowserSelector.select(
                    mContext,
                    parameters.getBrowserSafeList(),
                    parameters.getPreferredBrowser());

            Logger.info(methodTag, "Use browser for authorization.");
            return getBrowserAuthorizationStrategy(
                    browser,
                    (parameters instanceof BrokerInteractiveTokenCommandParameters));
        } catch (final ClientException e) {
            Logger.info(methodTag, "Unable to use browser to do the authorization because "
                    + ErrorStrings.NO_AVAILABLE_BROWSER_FOUND + " Use embedded webView instead.");
            return getGenericAuthorizationStrategy();
        }
    }

    private IAuthorizationStrategy getBrowserAuthorizationStrategy(@NonNull final Browser browser,
                                                                   final boolean isBrokerRequest) {
        if (LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()) {
            final CurrentTaskBrowserAuthorizationStrategy currentTaskBrowserAuthorizationStrategy =
                    new CurrentTaskBrowserAuthorizationStrategy(
                            mContext,
                            mActivity,
                            mFragment);
            currentTaskBrowserAuthorizationStrategy.setBrowser(browser);
            return currentTaskBrowserAuthorizationStrategy;
        } else {
            final DefaultBrowserAuthorizationStrategy defaultBrowserAuthorizationStrategy = new DefaultBrowserAuthorizationStrategy(
                    mContext,
                    mActivity,
                    mFragment,
                    isBrokerRequest
            );
            defaultBrowserAuthorizationStrategy.setBrowser(browser);
            return defaultBrowserAuthorizationStrategy;
        }
    }

    // Suppressing unchecked warnings due to casting of EmbeddedWebViewAuthorizationStrategy to GenericAuthorizationStrategy
    @SuppressWarnings(WarningType.unchecked_warning)
    private IAuthorizationStrategy getGenericAuthorizationStrategy() {
        return new EmbeddedWebViewAuthorizationStrategy(
                mContext,
                mActivity,
                mFragment);
    }
}
