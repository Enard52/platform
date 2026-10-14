package com.dialoguebranch.model.common;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DialogueBranchConstantsTest {

	@Test
	public void testReservedHeaderTagsContainsComment() {
		List<String> reservedTags = Arrays.asList(DialogueBranchConstants.DLB_RESERVED_HEADER_TAGS);
		assertTrue(reservedTags.contains("title"));
		assertTrue(reservedTags.contains("speaker"));
		assertTrue(reservedTags.contains("position"));
		assertTrue(reservedTags.contains("colorId"));
		assertTrue(reservedTags.contains("comment"));
	}

}
