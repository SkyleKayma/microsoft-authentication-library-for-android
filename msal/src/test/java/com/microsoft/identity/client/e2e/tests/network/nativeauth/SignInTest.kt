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

package com.microsoft.identity.client.e2e.tests.network.nativeauth

import com.microsoft.identity.internal.testutils.TestConstants.Configurations.NATIVE_AUTH_SIGN_IN_TEST_CONFIG_FILE_PATH
import com.microsoft.identity.internal.testutils.nativeauth.NativeAuthCredentialHelper
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SignInTest : NativeAuthPublicClientApplicationAbstractTest() {

    override fun getConfigFilePath(): String = NATIVE_AUTH_SIGN_IN_TEST_CONFIG_FILE_PATH

    /**
     * Use email and password to get token (hero scenario 15, use case 1.2.1) - Test case 37
     */
    @Test
    fun testSuccessEmailPassword() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = NativeAuthCredentialHelper.nativeAuthSignInPassword
        val result = application.signIn(username, password.toCharArray())
        Assert.assertTrue(result is SignInResult.Complete)
    }

    /**
     * Use email and password to get token while user is not registered with given email (use case 1.2.2) - Test case 38
     */
    @Test
    fun testErrorEmailPasswordIsUserCancel() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = NativeAuthCredentialHelper.nativeAuthSignInPassword
        // Turn an existing username to a non-existing username
        val alteredUsername = username.replace("@", "1234@")
        val result = application.signIn(alteredUsername, password.toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isUserNotFound())
    }

    /**
     * Use email and password to get token while password is incorrect (use case 1.2.3) - Test case 39
     */
    @Test
    fun testErrorEmailPasswordIsInvalidCredentials() = runTest {
        val username = NativeAuthCredentialHelper.nativeAuthSignInUsername
        val password = NativeAuthCredentialHelper.nativeAuthSignInPassword
        // Turn correct password into an incorrect one
        val alteredPassword = password + "1234"
        val result = application.signIn(username, alteredPassword.toCharArray())
        Assert.assertTrue(result is SignInError)
        Assert.assertTrue((result as SignInError).isInvalidCredentials())
    }
}