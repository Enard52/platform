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

package com.dialoguebranch.execution.parser;

import com.dialoguebranch.execution.parser.DialogueBranchScriptSplitter.RawNode;
import com.dialoguebranch.model.execute.Node;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DialogueBranchScriptSplitter}: splitting accepts any text, follows the same
 * separator rules as {@link DialogueBranchParser}, and round-trips through {@link
 * DialogueBranchScriptSplitter#join}.
 */
public class DialogueBranchScriptSplitterTest {

	private static final String SCRIPT =
			"title: Start\n---\nHello.\n[[Next|Second]]\n===\ntitle: Second\n---\nBye.\n===\n";

	private static void assertTwoNodes(List<RawNode> nodes) {
		assertEquals(2, nodes.size());
		assertEquals(new RawNode("title: Start", "Hello.\n[[Next|Second]]", 1), nodes.get(0));
		assertEquals("title: Second", nodes.get(1).header());
		assertEquals("Bye.", nodes.get(1).body());
	}

	@Test
	public void splitsLfScript() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(SCRIPT);
		assertTwoNodes(nodes);
		assertEquals(6, nodes.get(1).startLine());
	}

	@Test
	public void splitsCrlfScript() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(SCRIPT.replace("\n", "\r\n")));
	}

	@Test
	public void splitsCrScript() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(SCRIPT.replace("\n", "\r")));
	}

	@Test
	public void splitsWithWhitespaceAroundSeparators() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(
				SCRIPT.replace("===\n", "  ===\t\n").replace("---\n", "--- \n")));
	}

	@Test
	public void splitsWithCommentsOnSeparators() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(
				SCRIPT.replace("===\n", "=== // end of node\n").replace("---\n", "---// body\n")));
	}

	@Test
	public void splitsWithoutFinalNewline() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(SCRIPT.substring(0, SCRIPT.length() - 1)));
	}

	@Test
	public void splitsWithoutFinalNodeSeparator() {
		assertTwoNodes(DialogueBranchScriptSplitter.split(SCRIPT.substring(0, SCRIPT.length() - 4)));
	}

	@Test
	public void keepsSeparatorLikeTextThatIsNotASeparatorLine() {
		String script = "title: Start\n---\n=== not a separator\na --- b\n====\n===\n";
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(script);
		assertEquals(1, nodes.size());
		assertEquals("=== not a separator\na --- b\n====", nodes.get(0).body());
	}

	@Test
	public void onlyTheFirstHeaderSeparatorSplitsHeaderFromBody() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split("title: A\n---\none\n---\ntwo\n===\n");
		assertEquals("one\n---\ntwo", nodes.get(0).body());
	}

	@Test
	public void removesBlankLinesFromHeaderButKeepsComments() {
		String script = "\n\n  \ntitle: Start\n\n// a comment\nspeaker: Lizz\n \n---\nHello.\n";
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(script);
		assertEquals(1, nodes.size());
		assertEquals("title: Start\n// a comment\nspeaker: Lizz", nodes.get(0).header());
		assertEquals(4, nodes.get(0).startLine());
	}

	@Test
	public void keepsBodyExactly() {
		String body = "\n  Hello.  \n\n// not a comment in a body\n\n";
		List<RawNode> nodes = DialogueBranchScriptSplitter.split("title: A\n---\n" + body + "\n===\n");
		assertEquals(body, nodes.get(0).body());
	}

	@Test
	public void emptyBodyIsPresentButEmpty() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split("title: A\n---\n===\n");
		assertTrue(nodes.get(0).hasBody());
		assertEquals("", nodes.get(0).body());
	}

	@Test
	public void blockWithoutHeaderSeparatorHasNoBody() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(
				"title: A\n---\nHi.\n===\ntitle: B\nHello.\n===\n");
		assertEquals(2, nodes.size());
		assertFalse(nodes.get(1).hasBody());
		assertNull(nodes.get(1).body());
		assertEquals("title: B\nHello.", nodes.get(1).header());
	}

	@Test
	public void plainTextIsOneBlockWithoutBody() {
		String text = "Shall I compare thee to a summer's day?\nThou art more lovely and more temperate:\n";
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(text);
		assertEquals(1, nodes.size());
		assertEquals(text.trim(), nodes.get(0).header());
		assertNull(nodes.get(0).body());
	}

	@Test
	public void emptyAndBlankTextGiveNoNodes() {
		assertTrue(DialogueBranchScriptSplitter.split(null).isEmpty());
		assertTrue(DialogueBranchScriptSplitter.split("").isEmpty());
		assertTrue(DialogueBranchScriptSplitter.split("\n \n\r\n===\n\n===").isEmpty());
	}

	@Test
	public void dropsCommentOnlyBlockWithoutHeaderSeparator() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split(SCRIPT + "\n// end of file\n");
		assertTwoNodes(nodes);
	}

	@Test
	public void keepsBlockWithHeaderSeparatorButNoHeader() {
		List<RawNode> nodes = DialogueBranchScriptSplitter.split("\n---\nHello.\n===\n");
		assertEquals(1, nodes.size());
		assertEquals("", nodes.get(0).header());
		assertEquals("Hello.", nodes.get(0).body());
		assertEquals(2, nodes.get(0).startLine());
	}

	@Test
	public void joinWritesTheStandardFormat() {
		assertEquals(SCRIPT, DialogueBranchScriptSplitter.join(DialogueBranchScriptSplitter.split(SCRIPT)));
		assertEquals("title: A\n===\n",
				DialogueBranchScriptSplitter.join(List.of(new RawNode("title: A", null, 1))));
	}

	@Test
	public void roundTrips() {
		String messy = "\r\n// leading comment\r\ntitle: Start\r\n\r\n--- // body\r\n  Hello.\r\n\r\n=== \r\n"
				+ "title: Second\r\n---\r\n\r\n===\r\ntitle: Loose\r\nno body here";
		List<RawNode> split = DialogueBranchScriptSplitter.split(messy);
		String joined = DialogueBranchScriptSplitter.join(split);
		List<RawNode> resplit = DialogueBranchScriptSplitter.split(joined);
		assertEquals(3, split.size());
		for (int i = 0; i < split.size(); i++) {
			assertEquals(split.get(i).header(), resplit.get(i).header());
			assertEquals(split.get(i).body(), resplit.get(i).body());
		}
		assertEquals(joined, DialogueBranchScriptSplitter.join(resplit));
	}

	@Test
	public void agreesWithTheParserOnNodeTitles() throws IOException {
		String script = SCRIPT.replace("title: Start", "title: Start\nspeaker: Lizz")
				.replace("title: Second", "title: Second\nspeaker: Lizz");
		String[] variants = {
				script,
				script.replace("\n", "\r\n"),
				script.replace("===\n", "=== // end\n"),
				script.replace("---\n", "--- \n"),
				script.substring(0, script.length() - 1),
				script + "// end of file\n",
				"\n\n" + script.replace("===\ntitle", "===\n\n\ntitle"),
		};

		for (String variant : variants) {
			List<String> splitTitles = DialogueBranchScriptSplitter.split(variant).stream()
					.map(node -> node.header().replaceAll("(?s).*title:\\s*(\\S+).*", "$1"))
					.toList();
			ParserResult result;
			try (DialogueBranchParser parser = new DialogueBranchParser("test",
					new ByteArrayInputStream(variant.getBytes(StandardCharsets.UTF_8)))) {
				result = parser.readDialogue();
			}
			assertTrue(variant + result.getParseErrors(), result.getParseErrors().isEmpty());
			List<String> parsedTitles = result.getDialogue().getNodes().stream()
					.map(Node::getTitle).toList();
			assertEquals(variant, parsedTitles, splitTitles);
		}
	}
}
