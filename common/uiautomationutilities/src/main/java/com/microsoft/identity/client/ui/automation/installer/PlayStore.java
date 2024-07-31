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
package com.microsoft.identity.client.ui.automation.installer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.isStringPackageName;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.launchApp;

public class PlayStore implements IAppInstaller {

    private final static String TAG = PlayStore.class.getSimpleName();
    private static final String GOOGLE_PLAY_PACKAGE_NAME = "com.android.vending";
    private static final String INSTALL_APP = "Install";
    private static final String UPDATE_APP = "Update";
    private boolean shouldHandleTermsAndServices = true;

    // wait at least 5 mins for app download/install from Play Store
    private static final long PLAY_STORE_INSTALL_OR_UPDATE_APP_TIMEOUT = TimeUnit.MINUTES.toMillis(5);
    // During update scenario, we check end of installation with 2 separate button checks, so creating a second timeout
    private static final long PLAY_STORE_INSTALL_STEP_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
    // Timeout for finding a ui object during playstore installation/update
    private static final long PLAY_STORE_UI_OBJECT_TIMEOUT = TimeUnit.SECONDS.toMillis(10);

    private void launchMarketPageForPackage(final String appPackageName) {
        Logger.i(TAG, "Launch Market Page For " + appPackageName + " Package..");
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)); //sets the intent to start your app
        intent.setPackage("com.android.vending");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);  //clear out any previous task, i.e., make sure it starts on the initial screen
        context.startActivity(intent);
    }

    private void searchAppOnGooglePlay(@NonNull final String hint) {
        Logger.i(TAG, "Search " + hint + "App on Google Play Store..");
        launchApp(GOOGLE_PLAY_PACKAGE_NAME);

        final UiObject searchButton = UiAutomatorUtils.obtainUiObjectWithResourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "search_bar_hint"),
                PLAY_STORE_UI_OBJECT_TIMEOUT);
        try {
            searchButton.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        final UiObject searchTextField = UiAutomatorUtils.obtainUiObjectWithResourceId(
                getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "search_bar_text_input"),
                PLAY_STORE_UI_OBJECT_TIMEOUT);
        try {
            searchTextField.setText(hint);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }

        UiAutomatorUtils.pressEnter();
    }

    private void selectGooglePlayAppFromAppList() throws UiObjectNotFoundException {
        Logger.i(TAG, "Select Google Play App From App Search List..");
        UiAutomatorUtils.handleButtonClick(getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "bucket_items"), PLAY_STORE_UI_OBJECT_TIMEOUT);
    }

    private void selectGooglePlayAppFromInstallBar() throws UiObjectNotFoundException {
        Logger.i(TAG, "Select Google Play App From Install Bar..");
        UiAutomatorUtils.handleButtonClick(getResourceId(GOOGLE_PLAY_PACKAGE_NAME, "install_bar"), PLAY_STORE_UI_OBJECT_TIMEOUT);
    }

    private void selectGooglePlayAppFromAppName() {
        Logger.i(TAG, "Select Google Play App From App Name..");
        try {
            selectGooglePlayAppFromInstallBar();
        } catch (final UiObjectNotFoundException e) {
            try {
                selectGooglePlayAppFromAppList();
            } catch (final UiObjectNotFoundException ex) {
                throw new AssertionError(e);
            }
        }
    }

    private void installOrUpdateAppFromMarketPage(String playStoreAction) {
        try {
            installOrUpdateAppFromMarketPageInternal(playStoreAction);
        } catch (final UiObjectNotFoundException e) {
            acceptGooglePlayTermsOfService();
            try {
                installOrUpdateAppFromMarketPageInternal(playStoreAction);
            } catch (UiObjectNotFoundException ex) {
                throw new AssertionError(ex.getMessage(), e);
            }
        }
    }

    private void installOrUpdateAppFromMarketPageInternal(String playStoreAction) throws UiObjectNotFoundException {
        Logger.i(TAG, "Performing " + playStoreAction + " App From Market Page Internal..");

        // There is some inconsistency in how the PlayStore buttons are appearing, regardless of API level
        try {
            final UiObject uiObjBtn = UiAutomatorUtils.obtainUiObjectWithUiSelector(
                    new UiSelector().description(playStoreAction),
                    PLAY_STORE_UI_OBJECT_TIMEOUT);
            uiObjBtn.click();
            if (playStoreAction.equals(INSTALL_APP)) {
                checkButtonAfterPlayStoreAction(new UiSelector().description("Uninstall"), PLAY_STORE_INSTALL_OR_UPDATE_APP_TIMEOUT);
            } else {
                // In update scenario, Uninstall button is visible before download is finished, so we check for open instead
                checkButtonAfterPlayStoreAction(new UiSelector().description("Open"), PLAY_STORE_INSTALL_OR_UPDATE_APP_TIMEOUT);
                // Open button is visible before install is finished, so we check for uninstall button after
                checkButtonAfterPlayStoreAction(new UiSelector().description("Uninstall"), PLAY_STORE_INSTALL_STEP_TIMEOUT);
            }
        } catch (final UiObjectNotFoundException e) {
            Logger.i(TAG, "Got exception when trying to find Install/Update button for PlayStore: " + e.getMessage());
            final UiObject uiObjBtn = UiAutomatorUtils.obtainUiObjectWithUiSelector(
                    new UiSelector().className(Button.class).text(playStoreAction).enabled(true),
                    PLAY_STORE_UI_OBJECT_TIMEOUT);
            uiObjBtn.click();
            if (playStoreAction.equals(INSTALL_APP)) {
                checkButtonAfterPlayStoreAction(new UiSelector().className(Button.class).text("Uninstall").enabled(true), PLAY_STORE_INSTALL_OR_UPDATE_APP_TIMEOUT);
            } else {
                // In update scenario, Uninstall button is visible before download is finished, so we check for open instead
                checkButtonAfterPlayStoreAction(new UiSelector().className(Button.class).text("Open").enabled(true), PLAY_STORE_INSTALL_OR_UPDATE_APP_TIMEOUT);
                // Open button is visible before install is finished, however, so we check for uninstall button after
                checkButtonAfterPlayStoreAction(new UiSelector().className(Button.class).text("Uninstall").enabled(true), PLAY_STORE_INSTALL_STEP_TIMEOUT);
            }
        }
    }

    private void checkButtonAfterPlayStoreAction(@NonNull final UiSelector selector, final long existsTimeout) {
        final UiObject openButton = UiAutomatorUtils.obtainUiObjectWithUiSelector(selector, existsTimeout);
        Assert.assertTrue(openButton.exists());
    }

    private void acceptGooglePlayTermsOfService() {
        if (shouldHandleTermsAndServices) {
            Logger.i(TAG, "Accept Google Play Terms Of Service while installing App from Playstore..");
            final UiObject termsOfService = UiAutomatorUtils.obtainUiObjectWithText("Terms of Service");
            Assert.assertTrue(termsOfService.exists());
            final UiObject acceptBtn = UiAutomatorUtils.obtainUiObjectWithText("ACCEPT");
            Assert.assertTrue(acceptBtn.exists());
            try {
                acceptBtn.click();
            } catch (UiObjectNotFoundException e) {
                throw new AssertionError(e);
            }

            shouldHandleTermsAndServices = false;
        }
    }

    @Override
    public void installApp(@NonNull final String searchHint) {
        if (isStringPackageName(searchHint)) {
            launchMarketPageForPackage(searchHint);
        } else {
            searchAppOnGooglePlay(searchHint);
            selectGooglePlayAppFromAppName();
        }

        installOrUpdateAppFromMarketPage(INSTALL_APP);
    }

    @Override
    public void updateApp(@NonNull final String searchHint) {
        if (isStringPackageName(searchHint)) {
            launchMarketPageForPackage(searchHint);
        } else {
            searchAppOnGooglePlay(searchHint);
            selectGooglePlayAppFromAppName();
        }

        installOrUpdateAppFromMarketPage(UPDATE_APP);
    }
}
