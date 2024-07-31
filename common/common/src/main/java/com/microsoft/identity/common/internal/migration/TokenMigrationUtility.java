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
package com.microsoft.identity.common.internal.migration;

import com.microsoft.identity.common.java.BaseAccount;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.cache.IShareSingleSignOnState;
import com.microsoft.identity.common.java.providers.oauth2.RefreshToken;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TokenMigrationUtility<T extends BaseAccount, U extends RefreshToken> {

    private static final String TAG = TokenMigrationUtility.class.getSimpleName();

    /**
     * ExecutorService to handle background computation.
     */
    private static final ExecutorService sBackgroundExecutor = Executors.newCachedThreadPool();

    /**
     * Imports key/value pairs of TokenCacheItems to the MSAL common cache.
     *
     * @param adapter     Adapter responsible for the deserialization of credentials.
     * @param credentials Key/Value (where value is JSON payload) of TokenCacheItems.
     * @param destination IShareSingleSignOnState instance to which migrated tokens should be written.
     * @param callback    Callback to receive the event when the import has finished.
     */
    public void _import(final IMigrationAdapter<T, U> adapter,
                        final Map<String, String> credentials,
                        final IShareSingleSignOnState<T, U> destination,
                        final TokenMigrationCallback callback) {
        final String methodTag = TAG + ":_import";
        // Do all work on a background thread
        sBackgroundExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // Keep a running total of the accounts added
                int accountsAdded = 0;

                // Iterate over the adapted accounts/tokens, incrementing if successfully added to
                // the cache.
                for (final Map.Entry<T, U> accountTokenKeyValuePair : adapter.adapt(credentials)) {
                    try {
                        destination.setSingleSignOnState(
                                accountTokenKeyValuePair.getKey(),
                                accountTokenKeyValuePair.getValue()
                        );
                        accountsAdded++;
                    } catch (ClientException e) {
                        Logger.warn(
                                methodTag,
                                "Failed to save account/refresh token . Skipping "
                        );
                    }
                }

                // Migration is complete, trigger the callback with added Account total.
                callback.onMigrationFinished(accountsAdded);
            }
        });
    }
}
