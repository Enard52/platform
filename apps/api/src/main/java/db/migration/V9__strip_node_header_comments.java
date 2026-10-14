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

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes {@code //} comments from node headers in stored content (#343). A node header now
 * consists of {@code key: value} tags only, and {@code //} is part of a value; before, it started
 * a comment that the parser ignored. Removing those comments therefore changes nothing about what
 * a dialogue does, and keeps stored dialogues readable under the new rule.
 *
 * <ul>
 *   <li>{@code draft_nodes.header}: a trailing comment is cut from a header line, and a
 *   comment-only line is removed. The comment lines at the start of a dialogue's first node are
 *   the script's preamble (#345): they move to {@code draft_dialogues.preamble}, unless the
 *   dialogue already has one.</li>
 *   <li>{@code published_dialogues.content}: the same for every node header in the script text,
 *   keeping the comment lines at the very start of the script, which are its preamble.</li>
 * </ul>
 *
 * <p>Deliberately self-contained, using the comment rule as it was before this change, so that
 * it keeps working however the core library changes later.</p>
 */
public class V9__strip_node_header_comments extends BaseJavaMigration {

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		migrateDraftNodes(connection);
		migratePublishedDialogues(connection);
	}

	private void migrateDraftNodes(Connection connection) throws SQLException {
		List<String[]> nodeUpdates = new ArrayList<>();
		List<String[]> preambleUpdates = new ArrayList<>();
		String previousDialogue = null;
		try (Statement statement = connection.createStatement();
				ResultSet rows = statement.executeQuery("SELECT n.id, n.draft_dialogue_id, n.header, "
						+ "d.preamble FROM draft_nodes n JOIN draft_dialogues d "
						+ "ON d.id = n.draft_dialogue_id "
						+ "ORDER BY n.draft_dialogue_id, n.created_at, n.id")) {
			while (rows.next()) {
				String nodeId = rows.getString(1);
				String dialogueId = rows.getString(2);
				String header = rows.getString(3);
				String preamble = rows.getString(4);
				boolean firstNode = !dialogueId.equals(previousDialogue);
				previousDialogue = dialogueId;
				if (header == null || !header.contains("//")) continue;
				String[] split = firstNode ? splitLeadingComments(header) : new String[] {"", header};
				String newHeader = stripHeaderComments(split[1]);
				if (!split[0].isEmpty() && (preamble == null || preamble.isEmpty()))
					preambleUpdates.add(new String[] {split[0], dialogueId});
				if (!newHeader.equals(header))
					nodeUpdates.add(new String[] {newHeader, nodeId});
			}
		}
		update(connection, "UPDATE draft_nodes SET header = ? WHERE id = ?", nodeUpdates);
		update(connection, "UPDATE draft_dialogues SET preamble = ? WHERE id = ?", preambleUpdates);
	}

	private void migratePublishedDialogues(Connection connection) throws SQLException {
		List<String[]> updates = new ArrayList<>();
		try (Statement statement = connection.createStatement();
				ResultSet rows = statement.executeQuery(
						"SELECT id, content FROM published_dialogues")) {
			while (rows.next()) {
				String content = rows.getString(2);
				if (content == null || !content.contains("//")) continue;
				String newContent = stripScriptHeaderComments(content);
				if (!newContent.equals(content))
					updates.add(new String[] {newContent, rows.getString(1)});
			}
		}
		update(connection, "UPDATE published_dialogues SET content = ? WHERE id = ?", updates);
	}

	private static void update(Connection connection, String sql, List<String[]> rows)
			throws SQLException {
		if (rows.isEmpty()) return;
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (String[] row : rows) {
				statement.setString(1, row[0]);
				statement.setString(2, row[1]);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	/**
	 * Splits the comment lines at the start of a header from the rest: {@code {comments, rest}}.
	 * Blank lines among those comment lines are left out.
	 */
	static String[] splitLeadingComments(String header) {
		String[] lines = header.split("\n", -1);
		List<String> comments = new ArrayList<>();
		int i = 0;
		while (i < lines.length && (lines[i].isBlank() || isCommentLine(lines[i]))) {
			if (!lines[i].isBlank()) comments.add(lines[i]);
			i++;
		}
		List<String> rest = new ArrayList<>();
		for (int j = i; j < lines.length; j++) rest.add(lines[j]);
		return new String[] {String.join("\n", comments), String.join("\n", rest)};
	}

	/** Cuts trailing comments from header lines and removes comment-only lines. */
	static String stripHeaderComments(String header) {
		List<String> result = new ArrayList<>();
		for (String line : header.split("\n", -1)) {
			int comment = line.indexOf("//");
			if (comment == -1) {
				result.add(line);
			} else if (!isCommentLine(line)) {
				result.add(line.substring(0, comment).stripTrailing());
			}
		}
		return String.join("\n", result);
	}

	/**
	 * Applies {@link #stripHeaderComments} to every node header in a script, keeping the comment
	 * lines at the very start of the script (its preamble), separator lines and node bodies as
	 * they are.
	 */
	static String stripScriptHeaderComments(String script) {
		String[] lines = script.split("\n", -1);
		List<String> result = new ArrayList<>();
		boolean inHeader = true;
		boolean inPreamble = true;
		for (String line : lines) {
			String content = contentOf(line);
			if (content.equals("===")) {
				inHeader = true;
				inPreamble = false;
				result.add(line);
			} else if (inHeader && content.equals("---")) {
				inHeader = false;
				inPreamble = false;
				result.add(line);
			} else if (!inHeader) {
				result.add(line);
			} else if (inPreamble && (line.isBlank() || isCommentLine(line))) {
				result.add(line);
			} else {
				inPreamble = false;
				int comment = line.indexOf("//");
				if (comment == -1) {
					result.add(line);
				} else if (!isCommentLine(line)) {
					result.add(line.substring(0, comment).stripTrailing());
				}
			}
		}
		return String.join("\n", result);
	}

	private static boolean isCommentLine(String line) {
		return !line.isBlank() && contentOf(line).isEmpty();
	}

	private static String contentOf(String line) {
		int comment = line.indexOf("//");
		return (comment == -1 ? line : line.substring(0, comment)).trim();
	}
}
