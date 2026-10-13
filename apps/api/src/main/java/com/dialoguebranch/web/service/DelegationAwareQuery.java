/*
 *
 *                 Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 *
 *     This material is part of the Dialogue Branch Platform, and is covered by the MIT License
 *                                        as outlined below.
 *
 *                                            ----------
 *
 * Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dialoguebranch.web.service;

import com.dialoguebranch.web.service.auth.DialogueBranchUserId;
import com.dialoguebranch.web.service.exception.HttpException;

/**
 * A query for {@link QueryRunner} that also needs to know whether it runs as the authenticated
 * caller or as another user they delegate for. Most queries don't care, and use {@link AuthQuery}.
 * A delegated <em>read</em> does: it must not change the delegated user's state or create anything
 * for them, so it reads stored data directly instead of going through their live
 * {@link com.dialoguebranch.web.service.execution.UserService} (#327, #328).
 *
 * @param <T> the type of the query result
 */
@FunctionalInterface
public interface DelegationAwareQuery<T> {

	/**
	 * Runs the query.
	 *
	 * @param version the protocol version
	 * @param user the {@code (issuer, subject)} identity the query runs as
	 * @param delegated {@code true} if {@code user} is another user than the authenticated caller
	 *                  (a {@code delegateUser} naming someone else)
	 * @return the query result
	 * @throws HttpException if the query should return an HTTP error status
	 * @throws Exception if an unexpected error occurs. This results in HTTP error status 500
	 *                   Internal Server Error.
	 */
	T runQuery(ProtocolVersion version, DialogueBranchUserId user, boolean delegated)
			throws HttpException, Exception;
}
