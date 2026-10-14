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

package db.migration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the text transformations of {@link V9__strip_node_header_comments}. The migration
 * itself runs against MariaDB through Flyway, which the H2 test profile doesn't use.
 */
class V9StripNodeHeaderCommentsTest {

	@Test
	void cutsTrailingCommentsAndRemovesCommentLines() {
		assertEquals("title: Start\nspeaker: Robin\nposition: 0,0",
				V9__strip_node_header_comments.stripHeaderComments(
						"title: Start // entry point\n// a note\nspeaker: Robin\nposition: 0,0 //x"));
	}

	@Test
	void leavesHeadersWithoutCommentsAlone() {
		String header = "title: Start\n\nspeaker: Robin";
		assertEquals(header, V9__strip_node_header_comments.stripHeaderComments(header));
	}

	@Test
	void splitsLeadingCommentsOfAFirstNode() {
		assertArrayEquals(new String[] {"// About this script.\n  // Second line.", "title: Start\nspeaker: Robin"},
				V9__strip_node_header_comments.splitLeadingComments(
						"// About this script.\n\n  // Second line.\ntitle: Start\nspeaker: Robin"));
		assertArrayEquals(new String[] {"", "title: Start"},
				V9__strip_node_header_comments.splitLeadingComments("title: Start"));
	}

	@Test
	void stripsEveryHeaderInAScriptButKeepsPreambleBodiesAndSeparators() {
		String script = "// The preamble.\n\n// Kept.\ntitle: Start // entry point\n// a note\nspeaker: Robin\n"
				+ "--- // body\nHello. // a body comment\n=== // end\ntitle: Second\nspeaker: Robin // who\n"
				+ "---\nBye.\n===\n";
		assertEquals("// The preamble.\n\n// Kept.\ntitle: Start\nspeaker: Robin\n"
				+ "--- // body\nHello. // a body comment\n=== // end\ntitle: Second\nspeaker: Robin\n"
				+ "---\nBye.\n===\n",
				V9__strip_node_header_comments.stripScriptHeaderComments(script));
	}

	@Test
	void stripsCommentsAboveALaterNode() {
		assertEquals("title: Start\n---\nHi.\n===\ntitle: Next\n---\nBye.\n===\n",
				V9__strip_node_header_comments.stripScriptHeaderComments(
						"title: Start\n---\nHi.\n===\n// about Next\ntitle: Next\n---\nBye.\n===\n"));
	}
}
