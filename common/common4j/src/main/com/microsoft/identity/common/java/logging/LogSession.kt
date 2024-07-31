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
package com.microsoft.identity.common.java.logging

import com.microsoft.identity.common.java.logging.Logger.LogLevel
import java.util.Collections.replaceAll

/**
 * LogSession provides a wrapper on the Logger class from package
 * com.microsoft.identity.common.java.logging.
 */
class LogSession {
    companion object {
        /** logMethodCall is used to log the name of the method at info log level.
         * @param tag          Used to identify the source of a log message.
         *                     It usually identifies the class or activity where the log call occurs.
         * @param methodName   The methodName to log.
         */
        fun logMethodCall(tag: String, correlationId: String?, methodName: String) {
            Logger.info(tag, correlationId, methodName)
        }
    }
}
