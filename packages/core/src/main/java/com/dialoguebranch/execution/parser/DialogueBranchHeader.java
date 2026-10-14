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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the header of a Dialogue Branch node: the {@code key: value} lines above its
 * {@code ---} separator. This is the single definition of what a header line is; {@link
 * DialogueBranchParser} reads header lines through {@link #readLine}, adding its own checks on
 * top (duplicate keys, a valid {@code title}, required tags).
 *
 * <p>A header is a model: an ordered set of tags. Each non-blank line is one tag, split at its
 * first {@code :} into a key and a value, both trimmed. Nothing else in a line has a special
 * meaning: {@code //} is part of the value. A key must match {@link #KEY_REGEX}. Blank lines are
 * ignored.</p>
 *
 * @author Harm op den Akker
 */
public final class DialogueBranchHeader {

	/** Regular expression that matches a valid header key. */
	public static final String KEY_REGEX = "[A-Za-z0-9_-]+";

	private DialogueBranchHeader() {
	}

	/** What kind of line a header line is. */
	public enum LineKind {
		/** An empty or whitespace-only line, which is ignored. */
		BLANK,
		/** A valid {@code key: value} tag. */
		TAG,
		/** A line without a {@code :}. */
		NO_SEPARATOR,
		/** A line with nothing but whitespace before its first {@code :}. */
		EMPTY_KEY,
		/** A line whose key doesn't match {@link #KEY_REGEX}. */
		INVALID_KEY
	}

	/**
	 * One header line, read.
	 *
	 * @param kind        what kind of line it is.
	 * @param key         the trimmed key, or {@code null} for a blank line or a line without
	 *                    {@code :}.
	 * @param value       the trimmed value, or {@code null} for a blank line or a line without
	 *                    {@code :}.
	 * @param keyColumn   the 1-based column where the key starts, or 1 if there is no key.
	 * @param valueColumn the 1-based column where the value starts, or 1 if there is no value.
	 */
	public record Line(LineKind kind, @Nullable String key, @Nullable String value, int keyColumn,
			int valueColumn) {
	}

	/**
	 * One tag of a header.
	 *
	 * @param key   the tag key.
	 * @param value the tag value.
	 */
	public record Tag(String key, String value) {
	}

	/**
	 * Reads one header line.
	 *
	 * @param line a single line, without its line ending.
	 * @return the line, read.
	 */
	public static Line readLine(String line) {
		if (line.isBlank()) return new Line(LineKind.BLANK, null, null, 1, 1);
		int sep = line.indexOf(':');
		if (sep == -1) return new Line(LineKind.NO_SEPARATOR, null, null, 1, 1);
		String keyUntrimmed = line.substring(0, sep);
		String key = keyUntrimmed.trim();
		String valueUntrimmed = line.substring(sep + 1);
		String value = valueUntrimmed.trim();
		int keyColumn = 1 + leadingWhitespace(keyUntrimmed);
		int valueColumn = sep + 2 + leadingWhitespace(valueUntrimmed);
		LineKind kind;
		if (key.isEmpty())
			kind = LineKind.EMPTY_KEY;
		else if (!key.matches(KEY_REGEX))
			kind = LineKind.INVALID_KEY;
		else
			kind = LineKind.TAG;
		return new Line(kind, key, value, keyColumn, valueColumn);
	}

	/**
	 * Reads the tags of a header, leniently: lines that aren't valid tags are skipped, and if a key
	 * occurs more than once, its first occurrence wins. For checking a header strictly, read its
	 * lines with {@link #readLine}.
	 *
	 * @param header the header text; {@code null} is treated as empty.
	 * @return the tags, in the order they first occur.
	 */
	public static Map<String, String> read(@Nullable String header) {
		Map<String, String> tags = new LinkedHashMap<>();
		for (String text : lines(header)) {
			Line line = readLine(text);
			if (line.kind() == LineKind.TAG)
				tags.putIfAbsent(line.key(), line.value());
		}
		return tags;
	}

	/**
	 * Reads the tags of a header as a list, in order, leniently as {@link #read}.
	 *
	 * @param header the header text; {@code null} is treated as empty.
	 * @return the tags, in the order they first occur.
	 */
	public static List<Tag> readTags(@Nullable String header) {
		List<Tag> tags = new ArrayList<>();
		for (Map.Entry<String, String> tag : read(header).entrySet())
			tags.add(new Tag(tag.getKey(), tag.getValue()));
		return tags;
	}

	/**
	 * Checks a header strictly: every non-blank line must be a valid tag, and no key may occur
	 * more than once. This checks the header's structure only, not which tags a node needs.
	 *
	 * @param header the header text; {@code null} is treated as empty.
	 * @return the problems found, each starting with the 1-based line number within the header;
	 * empty if there are none.
	 */
	public static List<String> check(@Nullable String header) {
		List<String> problems = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		List<String> lines = lines(header);
		for (int i = 0; i < lines.size(); i++) {
			Line line = readLine(lines.get(i));
			String where = "line " + (i + 1) + ": ";
			switch (line.kind()) {
				case BLANK -> { }
				case NO_SEPARATOR -> problems.add(where + "not a 'key: value' line");
				case EMPTY_KEY -> problems.add(where + "empty header key");
				case INVALID_KEY -> problems.add(where + "invalid header key: " + line.key());
				case TAG -> {
					if (keys.contains(line.key()))
						problems.add(where + "duplicate header key: " + line.key());
					keys.add(line.key());
				}
			}
		}
		return problems;
	}

	/**
	 * Returns the value of one tag of a header (its first occurrence).
	 *
	 * @param header the header text; {@code null} is treated as empty.
	 * @param key    the tag key.
	 * @return the value, or {@code null} if the header has no such tag.
	 */
	public static @Nullable String get(@Nullable String header, String key) {
		return read(header).get(key);
	}

	/**
	 * Sets one tag of a header: replaces the line of its first occurrence, or adds a line at the
	 * end if the header has no such tag. All other lines are kept exactly, so this is safe to use
	 * on a header that isn't valid as a whole.
	 *
	 * @param header the header text; {@code null} is treated as empty.
	 * @param key    the tag key; must match {@link #KEY_REGEX}.
	 * @param value  the new value, written trimmed.
	 * @return the new header text.
	 * @throws IllegalArgumentException if the key isn't valid, or the value contains a line break.
	 */
	public static String set(@Nullable String header, String key, String value) {
		checkTag(key, value);
		List<String> lines = new ArrayList<>(lines(header));
		String tagLine = key + ": " + value.trim();
		for (int i = 0; i < lines.size(); i++) {
			Line line = readLine(lines.get(i));
			if (line.kind() == LineKind.TAG && key.equals(line.key())) {
				lines.set(i, tagLine);
				return String.join("\n", lines);
			}
		}
		lines.add(tagLine);
		return String.join("\n", lines);
	}

	/**
	 * Writes tags as header text: one {@code key: value} line per tag, in their order, joined with
	 * LF. Reading the result with {@link #readTags} gives back the same tags.
	 *
	 * @param tags the tags to write.
	 * @return the header text.
	 * @throws IllegalArgumentException if a key isn't valid or occurs more than once, or a value
	 * contains a line break.
	 */
	public static String write(List<Tag> tags) {
		List<String> lines = new ArrayList<>();
		List<String> keys = new ArrayList<>();
		for (Tag tag : tags) {
			checkTag(tag.key(), tag.value());
			if (keys.contains(tag.key()))
				throw new IllegalArgumentException("Duplicate header key: " + tag.key());
			keys.add(tag.key());
			lines.add(tag.key() + ": " + tag.value().trim());
		}
		return String.join("\n", lines);
	}

	private static void checkTag(@Nullable String key, @Nullable String value) {
		if (key == null || value == null)
			throw new IllegalArgumentException("A header tag needs a key and a value");
		if (!key.matches(KEY_REGEX))
			throw new IllegalArgumentException("Invalid header key: " + key);
		if (value.contains("\n") || value.contains("\r"))
			throw new IllegalArgumentException("Header value for '" + key + "' spans more " +
					"than one line");
	}

	private static List<String> lines(@Nullable String header) {
		List<String> lines = new ArrayList<>();
		if (header == null || header.isEmpty()) return lines;
		for (String line : header.split("\r\n|\r|\n", -1)) lines.add(line);
		return lines;
	}

	private static int leadingWhitespace(String s) {
		int count = 0;
		while (count < s.length() && Character.isWhitespace(s.charAt(count))) count++;
		return count;
	}
}
