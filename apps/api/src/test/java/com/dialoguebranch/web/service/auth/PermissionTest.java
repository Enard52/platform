package com.dialoguebranch.web.service.auth;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The read-only marker on {@link Permission} decides where read-only delegation
 * ({@link Permission#USER_DELEGATE_READ}) is allowed (#326), so the exact set is pinned here: a
 * permission marked read-only by mistake would let a supervisor act on someone's behalf.
 */
class PermissionTest {

	@Test
	void exactlyTheOwnDataReadPermissionsAreReadOnly() {
		Set<Permission> readOnly = EnumSet.allOf(Permission.class).stream()
				.filter(Permission::isReadOnly)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
		assertEquals(EnumSet.of(Permission.VARIABLE_READ_OWN, Permission.VARIABLE_READ_OWN_HISTORY,
				Permission.LOG_READ_OWN), readOnly);
	}

}
