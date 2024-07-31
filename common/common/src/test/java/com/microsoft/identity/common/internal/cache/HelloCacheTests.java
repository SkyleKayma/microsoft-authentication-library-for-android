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
package com.microsoft.identity.common.internal.cache;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.HELLO_ERROR_MESSAGE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.NEGOTIATED_BP_VERSION_KEY;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.UnsupportedBrokerException;
import com.microsoft.identity.common.java.interfaces.INameValueStorage;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.N})
public class HelloCacheTests {

    public static String protocolA = "MOCK_PROTOCOL_A";
    public static String protocolB = "MOCK_PROTOCOL_B";
    public static String brokerAppName = "MOCK_BROKER_APP_NAME";
    public static String appVersion = "1.0";
    public static String newAppVersion = "2.0";

    @After
    public void cleanUp() {
        getHelloCache(protocolA).clearCache();
        getHelloCache(protocolB).clearCache();
    }

    @Test
    public void testReadWrite() {
        final HelloCache cacheWrite = getHelloCache(protocolA);
        final HelloCache cacheRead = getHelloCache(protocolA);

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(result);
        Assert.assertEquals(negotiatedVer, result.getNegotiatedProtocolVersion());
    }

    @Test
    public void testReadWrite_MinVersionNull() {
        final HelloCache cacheWrite = getHelloCache(protocolA);
        final HelloCache cacheRead = getHelloCache(protocolA);

        final String minimumVer = null;
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";
        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(result);
        Assert.assertEquals(negotiatedVer, result.getNegotiatedProtocolVersion());
    }

    @Test
    public void testReadAfterUpdateMinVersion() {
        final HelloCache cacheWrite = getHelloCache(protocolA);
        final HelloCache cacheRead = getHelloCache(protocolA);

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        final String newMinimumVer = "1.2";

        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        Assert.assertNull(cacheRead.getHelloCacheResult(newMinimumVer, maximumVer));
    }

    @Test
    public void testReadAfterUpdateMaxVersion() {
        final HelloCache cacheWrite = getHelloCache(protocolA);
        final HelloCache cacheRead = getHelloCache(protocolA);

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        final String newMaximumVer = "2.7";

        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        Assert.assertNull(cacheRead.getHelloCacheResult(minimumVer, newMaximumVer));
    }

    @Test
    public void testReadForProtocolAAfterWritingForProtocolB() {
        final HelloCache cacheProtocolA = getHelloCache(protocolA);
        final HelloCache cacheProtocolB = getHelloCache(protocolB);

        final String minimumVerProtocolA = "1.0";
        final String maximumVerProtocolA = "2.5";
        final String negotiatedVerProtocolA = "2.0";

        final String minimumVerProtocolB = "100.0";
        final String maximumVerProtocolB = "200.5";
        final String negotiatedVerProtocolB = "200.0";

        cacheProtocolA.saveNegotiatedProtocolVersion(minimumVerProtocolA, maximumVerProtocolA, negotiatedVerProtocolA);
        cacheProtocolB.saveNegotiatedProtocolVersion(minimumVerProtocolB, maximumVerProtocolB, negotiatedVerProtocolB);

        final HelloCacheResult resultA = cacheProtocolA.getHelloCacheResult(minimumVerProtocolA, maximumVerProtocolA);
        Assert.assertNotNull(resultA);
        Assert.assertEquals(negotiatedVerProtocolA, resultA.getNegotiatedProtocolVersion());
        Assert.assertNull(cacheProtocolA.getHelloCacheResult(minimumVerProtocolB, maximumVerProtocolB));

        final HelloCacheResult resultB = cacheProtocolB.getHelloCacheResult(minimumVerProtocolB, maximumVerProtocolB);
        Assert.assertNotNull(resultB);
        Assert.assertEquals(negotiatedVerProtocolB, resultB.getNegotiatedProtocolVersion());
        Assert.assertNull(cacheProtocolB.getHelloCacheResult(minimumVerProtocolA, maximumVerProtocolA));
    }

    @Test
    public void testReadAfterUpdateTargetApp() {
        final HelloCache cacheBeforeUpdate = getHelloCache(protocolA);
        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        cacheBeforeUpdate.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);

        final HelloCache cacheAfterUpdate = getHelloCache(protocolA, newAppVersion);
        Assert.assertNull(cacheAfterUpdate.getHelloCacheResult(minimumVer, maximumVer));
    }

    @Test
    public void testReadAfterTargetAppUninstalled() {
        final HelloCache cacheBeforeUninstall = getHelloCache(protocolA);
        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        cacheBeforeUninstall.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);

        final HelloCache cacheAfterUninstall = getHelloCache(protocolA, null);
        Assert.assertNull(cacheAfterUninstall.getHelloCacheResult(minimumVer, maximumVer));
    }

    @Test
    public void testHelloShouldOnlyTriggerOnce() {

        final String minimumVer = "1.0";
        final String negotiatedVer = "2.5";

        class MockStrategy implements IIpcStrategy {
            int triggered = 0;

            @Override
            @Nullable
            public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                triggered += 1;
                if (triggered == 2) {
                    Assert.fail("Should never be triggered");
                }

                final Bundle resultBundle = new Bundle();
                resultBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedVer);
                return resultBundle;
            }

            @Override
            @NonNull
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@lombok.NonNull String targetedBrokerPackageName) {
                return true;
            }
        }

        class MockController extends BrokerMsalController {
            public MockController(Context applicationContext) {
                super(applicationContext,
                        AndroidPlatformComponentsFactory.createFromContext(applicationContext),
                        brokerAppName);
            }

            @Override
            public HelloCache getHelloCache() {
                return HelloCacheTests.this.getHelloCache(protocolA);
            }
        }

        final MockController controller = new MockController(ApplicationProvider.getApplicationContext());
        final MockStrategy strategy = new MockStrategy();
        final CommandParameters parameters = CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .requiredBrokerProtocolVersion(minimumVer).build();

        try {
            final String negotiatedProtocolVersion = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            final String negotiatedProtocolVersion2 = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            Assert.assertEquals(negotiatedProtocolVersion, negotiatedProtocolVersion2);
        } catch (BaseException e) {
            Assert.fail();
        }
    }

    @SneakyThrows
    @Test
    public void testReadWrite_Expiry() {
        final HelloCache cacheWrite = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));
        final HelloCache cacheRead = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getNegotiatedProtocolVersion(), negotiatedVer);
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        Assert.assertNull(cacheRead.getHelloCacheResult(minimumVer, maximumVer));
    }

    @Test
    public void testReadWrite_HandshakeError() {
        final HelloCache cacheWrite = getHelloCache(protocolA);
        final HelloCache cacheRead = getHelloCache(protocolA);

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";

        cacheWrite.saveHandshakeError(minimumVer, maximumVer);
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isHandShakeError());
    }

    @SneakyThrows
    @Test
    public void testReadWrite_HandshakeError_Expiry() {
        final HelloCache cacheWrite = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));
        final HelloCache cacheRead = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";

        cacheWrite.saveHandshakeError(minimumVer, maximumVer);
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isHandShakeError());
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        // cache entry expired
        Assert.assertNull(cacheRead.getHelloCacheResult(minimumVer, maximumVer));
    }

    @Test
    public void testHelloShouldOnlyTriggerOnce_AfterHandShakeError() {
        final String minimumVer = "1.0";

        class MockStrategy implements IIpcStrategy {
            int triggered = 0;

            @Nullable @Override public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                triggered += 1;
                if (triggered == 2) {
                    Assert.fail("Should never be triggered");
                }

                final Bundle resultBundle = new Bundle();
                resultBundle.putString(HELLO_ERROR_CODE, ErrorStrings.UNSUPPORTED_BROKER_VERSION_ERROR_CODE);
                resultBundle.putString(HELLO_ERROR_MESSAGE, ErrorStrings.UNSUPPORTED_BROKER_VERSION_ERROR_MESSAGE);
                return resultBundle;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull final String targetedBrokerPackageName) {
                return true;
            }

            @Override
            @NonNull
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        }

        class MockController extends BrokerMsalController {
            public MockController(Context applicationContext) {
                super(applicationContext,
                        AndroidPlatformComponentsFactory.createFromContext(applicationContext),
                        brokerAppName);
            }

            @Override
            public HelloCache getHelloCache() {
                return HelloCacheTests.this.getHelloCache(protocolA);
            }
        }

        final MockController controller = new MockController(ApplicationProvider.getApplicationContext());
        final MockStrategy strategy = new MockStrategy();
        final CommandParameters parameters = CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .requiredBrokerProtocolVersion(minimumVer).build();

        try {
            final String negotiatedProtocolVersion = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            Assert.fail("hello should have failed");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof UnsupportedBrokerException);
        }

        try {
            // This time error is thrown from cache
            final String negotiatedProtocolVersion = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            Assert.fail("hello should have failed");
        } catch (final BaseException e) {
            Assert.assertTrue(e instanceof UnsupportedBrokerException);
        }
    }

    @SneakyThrows
    @Test
    public void testReadWriteAfter_MsalUpdate() {
        final HelloCache cacheWrite = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));
        final HelloCache cacheRead = getHelloCache(protocolA, appVersion, TimeUnit.SECONDS.toMillis(1));

        final String minimumVer = "1.0";
        final String maximumVer = "2.5";
        final String negotiatedVer = "2.0";

        // setup value before MSAL update
        this.setupValueInSharedHelloCacheSharedCacheStore(protocolA, minimumVer, maximumVer, negotiatedVer, brokerAppName, appVersion);

        // read after MSAL update
        final HelloCacheResult result = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNull(result);
        cacheWrite.saveNegotiatedProtocolVersion(minimumVer, maximumVer, negotiatedVer);
        final HelloCacheResult newResult = cacheRead.getHelloCacheResult(minimumVer, maximumVer);
        Assert.assertNotNull(newResult);
        Assert.assertEquals(negotiatedVer, newResult.getNegotiatedProtocolVersion());
    }

    @Test
    public void testHelloShouldTrigger_MsalUpdate() {

        final String minimumVer = "1.0";
        final String maximumVer = "3.0";
        final String negotiatedVer = "2.5";

        class MockStrategy implements IIpcStrategy {
            int triggered = 0;
            @Nullable @Override public Bundle communicateToBroker(@NonNull BrokerOperationBundle bundle) throws BrokerCommunicationException {
                if (triggered == 1) {
                    Assert.fail("Unexpected! Should have already triggered once before.");
                }
                triggered++;
                final Bundle resultBundle = new Bundle();
                resultBundle.putString(NEGOTIATED_BP_VERSION_KEY, negotiatedVer);
                return resultBundle;
            }

            @Override
            public boolean isSupportedByTargetedBroker(@NonNull final String targetedBrokerPackageName) {
                return true;
            }

            @Override
            @NonNull
            public Type getType() {
                return Type.CONTENT_PROVIDER;
            }
        }

        class MockController extends BrokerMsalController {
            public MockController(Context applicationContext) {
                super(applicationContext,
                        AndroidPlatformComponentsFactory.createFromContext(applicationContext),
                        brokerAppName);
            }

            @Override
            public HelloCache getHelloCache() {
                return HelloCacheTests.this.getHelloCache(protocolA);
            }
        }

        final MockController controller = new MockController(ApplicationProvider.getApplicationContext());
        final MockStrategy strategy = new MockStrategy();
        final CommandParameters parameters = CommandParameters.builder()
                .platformComponents(AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext()))
                .requiredBrokerProtocolVersion(minimumVer).build();

        // setup before MSAL update
        this.setupValueInSharedHelloCacheSharedCacheStore(protocolA, minimumVer, maximumVer, negotiatedVer, brokerAppName, appVersion);

        try {
            String negotiatedProtocolVersion = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            Assert.assertEquals(negotiatedVer, negotiatedProtocolVersion);
            negotiatedProtocolVersion = controller.hello(strategy, parameters.getRequiredBrokerProtocolVersion());
            Assert.assertEquals(negotiatedVer, negotiatedProtocolVersion);
        } catch (BaseException e) {
            Assert.fail();
        }
    }

    private void setupValueInSharedHelloCacheSharedCacheStore(
            @NonNull final String protocolName,
            @Nullable final String minimumVer,
            @NonNull final String maximumVer,
            @NonNull final String negotiatedVer,
            @NonNull final String brokerName,
            @NonNull final String brokerVersion
    ) {
        // setup value before MSAL update
        final IPlatformComponents components = AndroidPlatformComponentsFactory.createFromContext(ApplicationProvider.getApplicationContext());
        INameValueStorage<String> fileManager = components.getStorageSupplier().getUnencryptedNameValueStore(
                "com.microsoft.common.ipc.hello.cache", String.class);
        final String key = protocolName + "[" + minimumVer + "," + maximumVer + "]:" + brokerName + "[" + brokerVersion + "]";
        fileManager.put(key, negotiatedVer);
        Assert.assertEquals(negotiatedVer, fileManager.get(key));
    }

    private HelloCache getHelloCache(@NonNull final String protocol) {
        return getHelloCache(protocol, appVersion);
    }

    private HelloCache getHelloCache(@NonNull final String protocol,
                                     @Nullable final String appVersionCode) {
        return this.getHelloCache(protocol, appVersionCode, TimeUnit.HOURS.toMillis(4));
    }

    private HelloCache getHelloCache(@NonNull final String protocol,
                                     @Nullable final String appVersionCode,
                                     final long timeoutInMs) {

        class HelloCacheMock extends HelloCache {
            public HelloCacheMock(@NonNull Context context, @NonNull String protocolName, @NonNull String targetAppPackageName) {
                super(context, protocolName, targetAppPackageName, AndroidPlatformComponentsFactory.createFromContext(context), timeoutInMs);
            }

            @NonNull @Override public String getVersionCode() throws PackageManager.NameNotFoundException {
                if (StringUtil.isEmpty(appVersionCode)) {
                    throw new PackageManager.NameNotFoundException("error!");
                }
                return appVersionCode;
            }
        }

        return new HelloCacheMock(ApplicationProvider.getApplicationContext(), protocol, brokerAppName);
    }
}
