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
package com.microsoft.identity.common.java.authscheme;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

/**
 * Abstract representation of any Authentication Scheme which may be token based.
 */
@SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
                    justification = "Lombok inserts redundant null checks into the code")
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Accessors(prefix = "m")
public abstract class TokenAuthenticationScheme
        extends AbstractAuthenticationScheme
        implements ITokenAuthenticationSchemeInternal {

    public static final String SCHEME_DELIMITER = " ";
    private static final long serialVersionUID = 8007073391895830795L;

    TokenAuthenticationScheme(@NonNull final String name) {
        super(name);
    }
}
