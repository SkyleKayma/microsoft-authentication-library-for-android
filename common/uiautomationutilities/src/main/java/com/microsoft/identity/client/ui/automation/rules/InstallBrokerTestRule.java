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
package com.microsoft.identity.client.ui.automation.rules;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.logging.Logger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to install the provided broker on the device prior to executing the test case.
 */
public class InstallBrokerTestRule implements TestRule {

    private final static String TAG = InstallBrokerTestRule.class.getSimpleName();

    private final ITestBroker broker;

    public InstallBrokerTestRule(@NonNull final ITestBroker broker) {
        this.broker = broker;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                Logger.i(TAG, "Installing broker: " + ((App) broker).getAppName());

                // This checks if the "-PpreInstallLtw" command line option was passed.
                // If yes, pre install LTW whenever a broker test case is ran before the designated broker.
                // If not, only install the specified broker.
                // When running this as part of the pipeline, this option should be passed when building
                // the automation apps, probably should be set based on a parameter or variable.
                if (BuildConfig.PRE_INSTALL_LTW) {
                    final BrokerLTW brokerLTW = new BrokerLTW();
                    // Commenting this out until LTW is supported (need package name and an apk)
                     brokerLTW.install();
                }

                broker.install();
                base.evaluate();
            }
        };
    }
}
