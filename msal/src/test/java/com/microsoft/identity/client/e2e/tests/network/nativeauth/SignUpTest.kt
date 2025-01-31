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

import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.TemporaryEmailService
import com.microsoft.identity.nativeauth.statemachine.errors.SignUpError
import com.microsoft.identity.nativeauth.statemachine.results.SignUpResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class SignUpTest : NativeAuthPublicClientApplicationAbstractTest() {

    private val tempEmailApi = TemporaryEmailService()

    override val configType = ConfigType.SIGN_UP_PASSWORD

    @Test
    fun testSignUpErrorSimple() = runTest {
        val user = tempEmailApi.generateRandomEmailAddress()
        val result = application.signUp(user, "invalidpassword".toCharArray())
        Assert.assertTrue(result is SignUpError)
        Assert.assertTrue((result as SignUpError).isInvalidPassword())
    }

    /**
     * Running with runBlocking to avoid default 10 second execution timeout.
     */
    @Test
    fun testSignUpSuccessSimple() = runBlocking {
        var retryCount = 0
        var signUpResult: SignUpResult
        var otp: String
        var shouldRetry = true

        while (shouldRetry) {
            try {
                val user = tempEmailApi.generateRandomEmailAddress()
                val password = getSafePassword()
                signUpResult = application.signUp(user, password.toCharArray())
                Assert.assertTrue(signUpResult is SignUpResult.CodeRequired)
                otp = tempEmailApi.retrieveCodeFromInbox(user)
                val submitCodeResult = (signUpResult as SignUpResult.CodeRequired).nextState.submitCode(otp)
                Assert.assertTrue(submitCodeResult is SignUpResult.Complete)
                shouldRetry = false
                break
            } catch (e: IllegalStateException) {
                // Re-run this test if the OTP retrieval fails. 1SecMail is known for emails to sometimes never arrive.
                // In that case, restart the test case with a new email address and try again, to make test less flaky.
                if (retryCount == 3) {
                    Assert.fail()
                    shouldRetry = false
                }
                retryCount++
            }
        }
    }
}