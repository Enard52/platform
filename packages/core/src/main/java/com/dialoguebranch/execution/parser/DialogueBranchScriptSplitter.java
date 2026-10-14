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

import com.dialoguebranch.model.common.DialogueBranchConstants;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits the text of a Dialogue Branch script ({@code .dlb}) into its raw nodes, and joins raw
 * nodes back into script text. This is the single definition of where one node ends and the next
 * begins, and of where a node's header ends and its body begins; {@link DialogueBranchParser}
 * recognises separator lines through the same {@link #isNodeSeparator} and {@link
 * #isHeaderSeparator}.
 *
 * <p>Splitting accepts any text and never fails. It does not interpret headers or bodies: whether
 * the result is a usable dialogue (every node has a header separator, a {@code title}, unique
 * titles) is for the caller to decide. The rules:</p>
 *
 * <ul>
 *   <li>LF, CRLF and a lone CR all end a line, as in {@link DialogueBranchParser}.</li>
 *   <li>Every node separator line ({@code ===}) ends a block. Text after the last one, or text
 *   without any, is the final block.</li>
 *   <li>Within a block, the first header separator line ({@code ---}) separates header from body.
 *   A block without one has all of its text as header, and no body ({@code null}).</li>
 *   <li>A block without a header separator whose lines are all blank or comment-only is dropped,
 *   as {@link DialogueBranchParser} ignores it too.</li>
 *   <li>Blank lines are removed from the header. Header lines are otherwise kept as they are,
 *   comments included, and the body is kept exactly.</li>
 * </ul>
 *
 * <p>{@link #join} writes plain separator lines and LF line endings, so text on the separator
 * lines themselves (a comment, or whitespace) is not kept. Splitting joined nodes gives back the
 * same nodes, and joining split nodes again gives identical text.</p>
 *
 * @author Harm op den Akker
 */
public final class DialogueBranchScriptSplitter {

	private DialogueBranchScriptSplitter() {
	}

	/**
	 * One node of a script as plain text, before any parsing.
	 *
	 * @param header    the header text: the lines above the header separator, without blank
	 *                  lines, joined with LF.
	 * @param body      the body text: the lines between the header separator and the node
	 *                  separator, joined with LF; {@code null} if the block had no header
	 *                  separator.
	 * @param startLine the 1-based line number in the source text where this node's first
	 *                  non-blank line is.
	 */
	public record RawNode(String header, @Nullable String body, int startLine) {

		/**
		 * Returns whether this node had a header separator, i.e. whether {@link #body()} is
		 * present.
		 *
		 * @return {@code true} if the node has a body.
		 */
		public boolean hasBody() {
			return body != null;
		}
	}

	/**
	 * Splits script text into its raw nodes, in their original order.
	 *
	 * @param script the script text; {@code null} is treated as empty.
	 * @return the raw nodes, possibly empty.
	 */
	public static List<RawNode> split(@Nullable String script) {
		List<RawNode> nodes = new ArrayList<>();
		if (script == null) return nodes;
		List<String> lines = splitLines(script);
		List<String> block = new ArrayList<>();
		int blockStart = 1;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (isNodeSeparator(line)) {
				addBlock(nodes, block, blockStart);
				block = new ArrayList<>();
				blockStart = i + 2;
			} else {
				block.add(line);
			}
		}
		addBlock(nodes, block, blockStart);
		return nodes;
	}

	/**
	 * Joins raw nodes into script text: each node's header, a header separator line and its body
	 * (or only its header if it has no body), followed by a node separator line, all with LF line
	 * endings.
	 *
	 * @param nodes the nodes to join.
	 * @return the script text.
	 */
	public static String join(List<RawNode> nodes) {
		StringBuilder script = new StringBuilder();
		for (RawNode node : nodes) {
			script.append(node.header()).append("\n");
			if (node.body() != null) {
				script.append(DialogueBranchConstants.DLB_HEADER_SEPARATOR).append("\n");
				script.append(node.body()).append("\n");
			}
			script.append(DialogueBranchConstants.DLB_NODE_SEPARATOR).append("\n");
		}
		return script.toString();
	}

	/**
	 * Returns whether a line is a node separator ({@code ===}), ignoring surrounding whitespace
	 * and a trailing {@code //} comment.
	 *
	 * @param line a single line, without its line ending.
	 * @return {@code true} for a node separator line.
	 */
	public static boolean isNodeSeparator(String line) {
		return lineContent(line).equals(DialogueBranchConstants.DLB_NODE_SEPARATOR);
	}

	/**
	 * Returns whether a line is a header separator ({@code ---}), ignoring surrounding whitespace
	 * and a trailing {@code //} comment.
	 *
	 * @param line a single line, without its line ending.
	 * @return {@code true} for a header separator line.
	 */
	public static boolean isHeaderSeparator(String line) {
		return lineContent(line).equals(DialogueBranchConstants.DLB_HEADER_SEPARATOR);
	}

	/**
	 * Returns a line without a {@code //} comment and without leading and trailing whitespace.
	 * Only meant for separator and header lines: it doesn't check whether {@code //} is inside a
	 * string literal.
	 *
	 * @param line a single line.
	 * @return the line's content.
	 */
	private static String lineContent(String line) {
		int commentIndex = line.indexOf("//");
		if (commentIndex != -1)
			line = line.substring(0, commentIndex);
		return line.trim();
	}

	private static void addBlock(List<RawNode> nodes, List<String> block, int blockStart) {
		int separator = -1;
		for (int i = 0; i < block.size(); i++) {
			if (isHeaderSeparator(block.get(i))) {
				separator = i;
				break;
			}
		}
		if (separator == -1 && block.stream().allMatch(line -> lineContent(line).isEmpty()))
			return;

		List<String> headerLines = separator == -1 ? block : block.subList(0, separator);
		List<String> header = new ArrayList<>();
		int startLine = -1;
		for (int i = 0; i < headerLines.size(); i++) {
			String line = headerLines.get(i);
			if (line.isBlank()) continue;
			if (startLine == -1) startLine = blockStart + i;
			header.add(line);
		}
		if (startLine == -1) startLine = blockStart + separator;
		String body = separator == -1 ? null
				: String.join("\n", block.subList(separator + 1, block.size()));
		nodes.add(new RawNode(String.join("\n", header), body, startLine));
	}

	private static List<String> splitLines(String text) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n' || c == '\r') {
				lines.add(line.toString());
				line.setLength(0);
				if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
			} else {
				line.append(c);
			}
		}
		if (!line.isEmpty()) lines.add(line.toString());
		return lines;
	}
}
