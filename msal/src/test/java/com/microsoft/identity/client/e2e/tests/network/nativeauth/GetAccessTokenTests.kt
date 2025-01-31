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

import com.microsoft.identity.client.e2e.shadows.ShadowBaseController
import com.microsoft.identity.client.e2e.utils.assertState
import com.microsoft.identity.internal.testutils.nativeauth.ConfigType
import com.microsoft.identity.internal.testutils.nativeauth.api.models.NativeAuthTestConfig
import com.microsoft.identity.nativeauth.statemachine.errors.GetAccessTokenError
import com.microsoft.identity.nativeauth.statemachine.errors.SignInError
import com.microsoft.identity.nativeauth.statemachine.results.GetAccessTokenResult
import com.microsoft.identity.nativeauth.statemachine.results.SignInResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import org.robolectric.annotation.Config

@Config(shadows = [ShadowBaseController::class])
class GetAccessTokenTests : NativeAuthPublicClientApplicationAbstractTest() {

    private lateinit var resources: List<NativeAuthTestConfig.Resource>

    override fun setup() {
        super.setup()
        resources = config.resources
    }

    override val configType = ConfigType.SIGN_IN_PASSWORD

    /**
     * Signing in with an invalid scope should make the API and the SDK return an error.
     */
    @Test
    fun testGetAccessTokenForInvalidScope() = runTest {
        // Turn valid scope into an invalid one
        val scopeA = resources[0].scopes[0] + "LorumIpsum"

        val password = getSafePassword()
        val result = application.signIn(
            username = config.email,
            password = password.toCharArray(),
            scopes = listOf(scopeA)
        )
        assertState<SignInError>(result)
        Assert.assertEquals("invalid_grant", (result as SignInError).error)
        Assert.assertNotNull(result.errorMessage)
        Assert.assertTrue(result.errorMessage!!.contains("AADSTS65001: The user or administrator has not consented to use the application"))
        Assert.assertNotNull(result.errorCodes)
        Assert.assertTrue(result.errorCodes!!.contains(65001))
    }

    /**
     * 1. Sign in with scope A. This should store the token in cache.
     * 2. Fetch token for scope A, without forceRefresh. This should retrieve the token from cache.
     * 3. Fetch token for scope A, with forceRefresh. This should retrieve the token from API.
     */
    @Test
    fun testGetAccessTokenCompareForceRefreshBehaviour() = runTest {
        val scopeA = resources[0].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(scopeA)
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult = accountState.getAccessToken()
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val retrievedAccessToken = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be refreshed, and so should not be the same as the previously returned token
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = true)
        Assert.assertTrue(getAccessTokenResult2 is GetAccessTokenResult.Complete)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val refreshedAccessToken = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue.accessToken
        Assert.assertNotEquals(refreshedAccessToken, retrievedAccessToken)
    }

    /**
     * 1. Sign in with scope A. This should store the token in cache.
     * 2. Fetch token without specifying explicit scopes. As there is only one token in cache, the token from sign in should be retrieved.
     *    This is validated by ensuring the token is retrieved from cache, i.e. no API call is made.
     * 3. Fetch token with specifying explicit scope A. These scopes should match the token that's in cache (due to previous sign in),
     *    so the token should be retrieved from cache.
     */
    @Test
    fun testGetAccessTokenFromCache() = runTest {
        val scopeA = resources[0].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(scopeA)
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult = accountState.getAccessToken()
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult = (getAccessTokenResult as GetAccessTokenResult.Complete).resultValue
        val accessTokenForImplicitScopes = authResult.accessToken
        Assert.assertTrue(authResult.scope.contains(scopeA))

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should not be refreshed, and so should be the same as the previously returned token
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(scopeA))
        Assert.assertTrue(getAccessTokenResult2 is GetAccessTokenResult.Complete)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        val accessTokenForExplicitScopes = authResult2.accessToken
        Assert.assertEquals(accessTokenForExplicitScopes, accessTokenForImplicitScopes)
        Assert.assertTrue(authResult2.scope.contains(scopeA))
    }

    /**
     * 1. Sign in with scope A1 (resource A, scope 1). This should store the token in cache.
     * 2. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from cache, as it was stored as part of the
     *    previous signIn().
     * 3. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous token requests.
     */
    @Test
    fun testGetAccessTokenWith1CustomApiResource() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeB1 = resources[1].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(scopeA1)
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken()
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult1)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(scopeA1))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(
            forceRefresh = false,
            scopes = listOf(scopeB1)
        )
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult2)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(scopeB1))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    /**
     * 1. Sign in without scopes.
     * 2. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous sign in request.
     * 3. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous token requests.
     */
    @Test
    fun testGetAccessTokenWith2CustomApiResources() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeB1 = resources[1].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(scopeA1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult1)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(scopeA1))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult2)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(scopeB1))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    /**
     * 1. Sign in with scope A1 (resource A, scope 1). This will return a token for all Employee API scopes (API returns the superset of scopes).
     * 2. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from cache, as it was stored as part of the
     *    previous signIn().
     * 3. Given the API's superset scope behaviour, we verify whether all the resource A's scopes are present in the cached token.
     * 4. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous token requests. This will return a token for all Customer API scopes (API returns the superset of scopes).
     * 5. Given the API's superset scope behaviour, we verify whether all the resource B's scopes are present in the cached token.
     */
    @Test
    fun testSuperSetOfScopesFor1APIResource() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeA2 = resources[0].scopes[1]
        val scopeB1 = resources[1].scopes[0]
        val scopeB2 = resources[1].scopes[1]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray(),
            scopes = listOf(scopeA1)
        )

        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(scopeA1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult1)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(scopeA1))
        Assert.assertTrue(authResult1.scope.contains(scopeA2))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult2 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult2)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(scopeB1))
        Assert.assertTrue(authResult2.scope.contains(scopeB2))
        val tokenWithCustomerScope = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)
    }

    /**
     * The API doesn't allow the combining of scopes from different resources, so will return an error.
     */
    @Test
    fun testGetAccessTokenWithMultipleAPIResourceScopesShouldReturnError() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeB1 = resources[1].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )

        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from cache
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(scopeA1, scopeB1))
        assertState<GetAccessTokenError>(getAccessTokenResult1)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
    }

    /**
     * 1. Sign in without scopes.
     * 2. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous sign in request.
     * 3. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from cache, as it was stored as part of the
     *    the previous call.
     * 4. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous token requests.
     * 5. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from cache, as it was stored as part of the
     *    the previous call.
     */
    @Test
    fun testGetAccessTokenWith2CustomApiResourcesComplexCacheVerification() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeB1 = resources[1].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(scopeA1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult1)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(scopeA1))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from cache this time
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(scopeA1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult2)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(scopeA1))
        val tokenWithEmployeeScope2 = authResult2.accessToken
        Assert.assertEquals(tokenWithEmployeeScope, tokenWithEmployeeScope2)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource
        val getAccessTokenResult3 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult3)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult3 = (getAccessTokenResult3 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult3.scope.contains(scopeB1))
        val tokenWithCustomerScope = authResult3.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from cache this time
        val getAccessTokenResult4 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult4)
        Assert.assertFalse(wasRenewAccessTokenInvoked)
        val authResult4 = (getAccessTokenResult4 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult4.scope.contains(scopeB1))
        val tokenWithCustomerScope2 = authResult4.accessToken
        Assert.assertEquals(tokenWithCustomerScope, tokenWithCustomerScope2)
    }

    /**
     * 1. Sign in without scopes.
     * 2. Fetch token for scope A1 (resource A, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous sign in request.
     * 3. Fetch token for scope A1 (resource A, scope 1), with forceRefresh. This should retrieve the token the API, due to force refresh.
     * 4. Fetch token for scope B1 (resource B, scope 1), without forceRefresh. This should retrieve the token from API, as this token doesn't exist in the
     *    cache yet, due to the scope being different than previous token requests.
     * 5. Fetch token for scope B1 (resource B, scope 1), with forceRefresh. This should retrieve the token the API, due to force refresh.
     */
    @Test
    fun testGetAccessTokenWithForceRefresh() = runTest {
        val scopeA1 = resources[0].scopes[0]
        val scopeB1 = resources[1].scopes[0]

        val username = config.email
        val password = getSafePassword()
        val result = application.signIn(
            username = username,
            password = password.toCharArray()
        )
        assertState<SignInResult.Complete>(result)
        val accountState = (result as SignInResult.Complete).resultValue

        // Var to keep track of whether BaseController.renewAccessToken() was called. This method calls the API to refresh the access token, for example if it's expired or not available in cache.
        var wasRenewAccessTokenInvoked = false
        ShadowBaseController.setOnRenewAccessTokenInvokedCallback { wasRenewAccessTokenInvoked = true }

        // Token should be retrieved from API
        val getAccessTokenResult1 = accountState.getAccessToken(scopes = listOf(scopeA1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult1)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult1 = (getAccessTokenResult1 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult1.scope.contains(scopeA1))
        val tokenWithEmployeeScope = authResult1.accessToken

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, due to force_refresh
        val getAccessTokenResult2 = accountState.getAccessToken(scopes = listOf(scopeA1), forceRefresh = true)
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult2)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult2 = (getAccessTokenResult2 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult2.scope.contains(scopeA1))
        val tokenWithEmployeeScope2 = authResult2.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithEmployeeScope2) // New token received

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, as the scope belongs to a different API resource and wasn't requested before
        val getAccessTokenResult3 = accountState.getAccessToken(forceRefresh = false, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult3)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult3 = (getAccessTokenResult3 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult3.scope.contains(scopeB1))
        val tokenWithCustomerScope = authResult3.accessToken
        Assert.assertNotEquals(tokenWithEmployeeScope, tokenWithCustomerScope)

        // Reset
        wasRenewAccessTokenInvoked = false

        // Token should be retrieved from API, due to force_refresh
        val getAccessTokenResult4 = accountState.getAccessToken(forceRefresh = true, scopes = listOf(scopeB1))
        assertState<GetAccessTokenResult.Complete>(getAccessTokenResult4)
        Assert.assertTrue(wasRenewAccessTokenInvoked)
        val authResult4 = (getAccessTokenResult4 as GetAccessTokenResult.Complete).resultValue
        Assert.assertTrue(authResult4.scope.contains(scopeB1))
        val tokenWithCustomerScope2 = authResult4.accessToken
        Assert.assertNotEquals(tokenWithCustomerScope, tokenWithCustomerScope2) // New token received
    }
}