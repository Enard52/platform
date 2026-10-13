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

package com.dialoguebranch.web.service.storage;

import com.dialoguebranch.web.service.auth.DialogueBranchUserId;
import com.dialoguebranch.web.service.exception.DatabaseException;
import com.dialoguebranch.web.service.repository.DBLoggedDialogueRepository;
import com.dialoguebranch.web.service.repository.DBUserRepository;
import com.dialoguebranch.web.service.storage.model.DBLoggedDialogue;
import com.dialoguebranch.web.service.storage.model.DBUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads a user's logged dialogues without creating anything for them. A {@link LoggedDialogueStore}
 * belongs to a live {@link com.dialoguebranch.web.service.execution.UserService} and creates the
 * user's row on construction; a delegated read must leave the delegated user untouched instead,
 * and report an unknown user as absent (#328).
 *
 * @author Harm op den Akker
 */
@Service
public class LoggedDialogueReader {

	private final DBUserRepository userRepository;
	private final DBLoggedDialogueRepository loggedDialogueRepository;

	/**
	 * Creates a new {@link LoggedDialogueReader}. Instances are constructed through Spring.
	 *
	 * @param userRepository repository used to look the user up.
	 * @param loggedDialogueRepository repository holding the logged dialogues.
	 */
	public LoggedDialogueReader(DBUserRepository userRepository,
								DBLoggedDialogueRepository loggedDialogueRepository) {
		this.userRepository = userRepository;
		this.loggedDialogueRepository = loggedDialogueRepository;
	}

	/**
	 * Returns all logged dialogues of a user's session, oldest first.
	 *
	 * @param id the {@code (issuer, subject)} identity of the user.
	 * @param sessionId the session to read.
	 * @return the session's logged dialogues (possibly empty), or an empty {@link Optional} if no
	 *         such user is known.
	 * @throws DatabaseException if stored interactions cannot be parsed.
	 */
	@Transactional(readOnly = true)
	public Optional<List<ServerLoggedDialogue>> readSession(DialogueBranchUserId id,
			String sessionId) throws DatabaseException {
		Optional<DBUser> user = findUser(id);
		if (user.isEmpty()) return Optional.empty();
		List<ServerLoggedDialogue> result = new ArrayList<>();
		for (DBLoggedDialogue db :
				loggedDialogueRepository.findByUserAndSessionIdOrderByUtcTimeAsc(user.get(), sessionId)) {
			result.add(LoggedDialogueStore.toServerLoggedDialogue(db, id.subject()));
		}
		return Optional.of(result);
	}

	/**
	 * Returns whether a user already has logged dialogues with the given session id.
	 *
	 * @param id the {@code (issuer, subject)} identity of the user.
	 * @param sessionId the session id to check.
	 * @return whether the session id is in use, or an empty {@link Optional} if no such user is
	 *         known.
	 */
	@Transactional(readOnly = true)
	public Optional<Boolean> existsSessionId(DialogueBranchUserId id, String sessionId) {
		return findUser(id).map(user ->
				loggedDialogueRepository.existsByUserAndSessionId(user, sessionId));
	}

	private Optional<DBUser> findUser(DialogueBranchUserId id) {
		return userRepository.findByIssuerAndSubject(id.issuer(), id.subject());
	}

}
