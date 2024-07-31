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
package com.microsoft.identity.common.internal.broker

import android.content.Context
import com.microsoft.identity.common.internal.util.PackageUtils
import com.microsoft.identity.common.logging.Logger
import java.security.cert.X509Certificate

/**
 * A class for validating if a given app is a valid broker app.
 * NOTE: Marked it as 'open' to make it mockable by ADAL's mockito.
 * */
open class BrokerValidator: IBrokerValidator {

    constructor(context: Context) {
        allowedBrokerApps = BrokerData.getKnownBrokerApps()
        getSigningCertificateForApp = { packageName: String ->
            PackageUtils.readCertDataForApp(packageName, context)
        }
        validateSigningCertificate = Companion::validateSigningCertificate
    }

    constructor(allowedBrokerApps: Set<BrokerData>,
                getSigningCertificateForApp: (packageName: String) -> List<X509Certificate>,
                validateSigningCertificate: (expectedSigningCertificateSignature: String,
                                             signingCertificates: List<X509Certificate>) -> Unit) {
        this.allowedBrokerApps = allowedBrokerApps
        this.getSigningCertificateForApp = getSigningCertificateForApp
        this.validateSigningCertificate = validateSigningCertificate
    }

    /**
     * Set of broker apps to validate against.
     **/
    private val allowedBrokerApps: Set<BrokerData>

    /**
     * Set of broker apps to validate against.
     **/
    private val getSigningCertificateForApp: (packageName: String) -> List<X509Certificate>

    /**
     * Validate if given list of signing certificate can generate the matching thumbprint.
     **/
    private val validateSigningCertificate: (
        expectedSigningCertificateThumbprint: String,
        signingCertificates: List<X509Certificate>
    ) -> Unit;

    companion object {
        private val TAG = BrokerValidator::class.simpleName

        fun validateSigningCertificate(
            expectedSigningCertificateThumbprint: String,
            signingCertificates: List<X509Certificate>
        ) {
            // Verify the cert list contains the cert we trust.
            PackageUtils.verifySignatureHash(
                signingCertificates,
                setOf(expectedSigningCertificateThumbprint).iterator()
            )

            // Perform the certificate chain validation. If there is only one cert returned,
            // no need to perform certificate chain validation.
            if (signingCertificates.size > 1) {
                PackageUtils.verifyCertificateChain(signingCertificates)
            }
        }
    }

    /**
     * Kept for backward-compatibility with ADAL.
     * Marked it as 'open' to make it mockable by ADAL's mockito.
     * TODO: Next time we're making a breaking change with ADAL, get rid of this.
     **/
    open fun verifySignature(packageName: String): Boolean {
        return isValidBrokerPackage(packageName)
    }

    override fun isValidBrokerPackage(packageName: String): Boolean {
        val methodTag = "$TAG:isValidBrokerPackage"

        val matchingApp = allowedBrokerApps.filter {
            it.packageName.equals(packageName, ignoreCase = true)
        }.firstOrNull {
            isSignedByKnownKeys(it)
        }

        if (matchingApp != null)
            return true

        Logger.info(methodTag, "$packageName does not match with any known broker apps.")
        return false
    }

    override fun isSignedByKnownKeys(brokerData: BrokerData): Boolean {
        val methodTag = "$TAG:isSignedByKnownKeys"
        return try {
            val signingCertificate = getSigningCertificateForApp(brokerData.packageName)
            validateSigningCertificate(brokerData.signingCertificateThumbprint, signingCertificate)
            Logger.verbose(methodTag, "$brokerData is a valid broker app.")
            true
        } catch (t: Throwable) {
            Logger.verbose(methodTag, "$brokerData verification failed: " + t.message)
            false
        }
    }
}