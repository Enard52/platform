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

package com.dialoguebranch.web.service.execution;

import com.dialoguebranch.execution.User;
import com.dialoguebranch.execution.VariableStore;
import com.dialoguebranch.execution.VariableStoreChange;
import com.dialoguebranch.execution.VariableUpdatedSource;
import com.dialoguebranch.web.service.DlbProperties;
import com.dialoguebranch.web.service.auth.DialogueBranchUserId;
import com.dialoguebranch.web.service.repository.DBLoggedDialogueRepository;
import com.dialoguebranch.web.service.repository.DBUserRepository;
import com.dialoguebranch.web.service.storage.VariableHistoryStorageHandler;
import com.dialoguebranch.web.service.storage.VariableStoreDatabaseStorageHandler;
import com.dialoguebranch.web.service.storage.model.DBUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the variable-history listener a {@link UserService} registers on each of its
 * per-project {@link VariableStore}s (#259), with every collaborator mocked: what it records, with
 * which timestamp, and that a failure to record never breaks the change itself.
 */
class UserServiceVariableHistoryListenerTest {

	private static final ZoneId USER_ZONE = ZoneId.of("Europe/Lisbon");
	private static final DialogueBranchUserId USER_ID =
			new DialogueBranchUserId("https://issuer.test", "sub-1", "alice");

	private VariableStoreDatabaseStorageHandler storageHandler;
	private VariableHistoryStorageHandler historyHandler;
	private UserService userService;

	@BeforeEach
	void setUp() throws Exception {
		ApplicationManager applicationManager = mock(ApplicationManager.class);
		// External Variable Service disabled (the default), so only the DB and history listeners
		// are registered.
		when(applicationManager.getDlbProperties()).thenReturn(new DlbProperties());

		DBUserRepository userRepository = mock(DBUserRepository.class);
		when(userRepository.findByIssuerAndSubject(anyString(), anyString())).thenReturn(
				Optional.of(new DBUser(USER_ID.issuer(), USER_ID.subject(), USER_ID.username())));

		storageHandler = mock(VariableStoreDatabaseStorageHandler.class);
		when(storageHandler.read(eq(USER_ID), anyString(), any()))
				.thenAnswer(call -> new VariableStore(call.getArgument(2, User.class)));
		historyHandler = mock(VariableHistoryStorageHandler.class);

		userService = new UserService(USER_ID, new User(USER_ID.subject(), USER_ZONE),
				applicationManager, storageHandler, historyHandler, userRepository,
				mock(DBLoggedDialogueRepository.class));
	}

	@SuppressWarnings("unchecked")
	private ArgumentCaptor<Collection<VariableStoreChange>> changesCaptor() {
		return ArgumentCaptor.forClass(Collection.class);
	}

	@Test
	void aChangeIsRecordedForTheUserAndTheStoresProject() {
		VariableStore store = userService.getVariableStore("project-a");

		store.setValue("mood", "fine", true, ZonedDateTime.now(USER_ZONE),
				VariableUpdatedSource.WEB_SERVICE);

		ArgumentCaptor<Collection<VariableStoreChange>> changes = changesCaptor();
		verify(historyHandler).record(eq(USER_ID), eq("project-a"), changes.capture(), any());
		VariableStoreChange change = changes.getValue().iterator().next();
		VariableStoreChange.Put put = assertInstanceOf(VariableStoreChange.Put.class, change);
		assertEquals("fine", put.getVariables().get("mood"));
		assertEquals(VariableUpdatedSource.WEB_SERVICE, put.getSource());
	}

	@Test
	void eachProjectsStoreRecordsUnderItsOwnSlug() {
		ZonedDateTime now = ZonedDateTime.now(USER_ZONE);
		userService.getVariableStore("project-a").setValue("x", 1, true, now);
		userService.getVariableStore("project-b").setValue("x", 2, true, now);

		verify(historyHandler).record(eq(USER_ID), eq("project-a"), any(), any());
		verify(historyHandler).record(eq(USER_ID), eq("project-b"), any(), any());
	}

	@Test
	void theRecordingTimeIsNowInTheUsersTimeZoneNotTheChangesOwnTime() {
		ZonedDateTime oldEvsTime = ZonedDateTime.now(ZoneId.of("UTC")).minusDays(3);
		long before = System.currentTimeMillis();

		userService.getVariableStore("project-a").setValue("mood", "fine", true, oldEvsTime,
				VariableUpdatedSource.EXTERNAL);

		long after = System.currentTimeMillis();
		ArgumentCaptor<ZonedDateTime> recorded = ArgumentCaptor.forClass(ZonedDateTime.class);
		verify(historyHandler).record(any(), any(), any(), recorded.capture());
		assertEquals(USER_ZONE, recorded.getValue().getZone());
		long recordedMillis = recorded.getValue().toInstant().toEpochMilli();
		assertTrue(recordedMillis >= before && recordedMillis <= after,
				"recorded time should be 'now', not the change's own (three days old) time");
	}

	@Test
	void aChangeWithoutNotificationIsNotRecorded() {
		userService.getVariableStore("project-a").setValue("mood", "fine", false,
				ZonedDateTime.now(USER_ZONE));

		verify(historyHandler, never()).record(any(), any(), any(), any());
	}

	@Test
	void aFailureToRecordHistoryNeitherFailsTheChangeNorSkipsPersistingIt() throws Exception {
		doThrow(new IllegalStateException("database down"))
				.when(historyHandler).record(any(), any(), any(), any());
		VariableStore store = userService.getVariableStore("project-a");

		assertDoesNotThrow(() -> store.setValue("mood", "fine", true,
				ZonedDateTime.now(USER_ZONE)));

		assertEquals("fine", store.getValue("mood"));
		verify(storageHandler).write(USER_ID, "project-a", store);
	}

}
