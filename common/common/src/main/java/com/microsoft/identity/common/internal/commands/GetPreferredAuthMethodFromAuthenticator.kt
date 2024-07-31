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
package com.microsoft.identity.common.internal.commands

import com.microsoft.identity.common.java.commands.BaseCommand
import com.microsoft.identity.common.java.commands.CommandCallback
import com.microsoft.identity.common.java.commands.parameters.CommandParameters
import com.microsoft.identity.common.java.controllers.IControllerFactory
import com.microsoft.identity.common.java.ui.PreferredAuthMethod
import lombok.EqualsAndHashCode

/**
 * Command class to call controllers to get the preferred authentication method from the authenticator app.
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
@EqualsAndHashCode(callSuper = true)
class GetPreferredAuthMethodFromAuthenticator(
    parameters: CommandParameters,
    controllerFactory: IControllerFactory,
    callback: CommandCallback<*, *>,
    publicApiId: String
) : BaseCommand<PreferredAuthMethod?>(parameters, controllerFactory, callback, publicApiId) {

    override fun execute(): PreferredAuthMethod {
        return controllerFactory.getDefaultController().preferredAuthMethod
    }

    override fun isEligibleForEstsTelemetry(): Boolean {
        return false
    }
}
