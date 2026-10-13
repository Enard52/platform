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

import com.dialoguebranch.execution.parser.ScriptLoader;
import com.dialoguebranch.model.common.ResourceType;
import com.dialoguebranch.model.execute.ResourcePointer;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link ScriptLoader} implementation that serves dialogue script and translation content from
 * pre-built in-memory maps, originally sourced from published database records. This allows the
 * execution engine to run dialogues from a published project version stored in the database without
 * any classpath or file system access.
 *
 * <p>Callers are responsible for building the content maps (from JPA entities) before constructing
 * this loader, so that no lazy JPA associations are accessed inside it.</p>
 *
 * @author Harm op den Akker
 */
public class DatabasePublishedScriptLoader implements ScriptLoader {

	private final String sourceLanguage;

	/** dialogueName → script content */
	private final Map<String, String> scriptContents;

	/** language → dialogueName → translation content */
	private final Map<String, Map<String, String>> translationContents;

	/**
	 * The project's translation language codes, including any without translation content (a
	 * language registered for the project whose dialogues aren't translated yet).
	 */
	private final List<String> translationLanguages;

	// -------------------------------------------------------- //
	// -------------------- Constructor(s) -------------------- //
	// -------------------------------------------------------- //

	/**
	 * Creates a {@link DatabasePublishedScriptLoader} from pre-built content maps.
	 *
	 * @param sourceLanguage      the source language code for the project (e.g. {@code "en"}).
	 * @param scriptContents      map of dialogue name → script content.
	 * @param translationContents map of language code → (dialogue name → translation content).
	 */
	public DatabasePublishedScriptLoader(String sourceLanguage,
									   Map<String, String> scriptContents,
									   Map<String, Map<String, String>> translationContents) {
		this(sourceLanguage, new ArrayList<>(translationContents.keySet()), scriptContents,
				translationContents);
	}

	/**
	 * Creates a {@link DatabasePublishedScriptLoader} from pre-built content maps, with the
	 * project's translation languages given explicitly, so that a registered language without any
	 * translation content yet still counts as one of the project's languages.
	 * @param sourceLanguage       the source language code for the project (e.g. {@code "en"}).
	 * @param translationLanguages the project's translation language codes.
	 * @param scriptContents       map of dialogue name → script content.
	 * @param translationContents  map of language code → (dialogue name → translation content).
	 */
	public DatabasePublishedScriptLoader(String sourceLanguage, List<String> translationLanguages,
									   Map<String, String> scriptContents,
									   Map<String, Map<String, String>> translationContents) {
		this.sourceLanguage = sourceLanguage;
		this.translationLanguages = List.copyOf(translationLanguages);
		this.scriptContents = new LinkedHashMap<>(scriptContents);
		this.translationContents = new LinkedHashMap<>(translationContents);
	}

	/**
	 * Returns the source language code of the project this loader loads (e.g. {@code "en"}).
	 *
	 * @return the source language code.
	 */
	public String getSourceLanguage() {
		return sourceLanguage;
	}

	/**
	 * Returns every language code of the project this loader loads: its source language, followed
	 * by its translation languages.
	 *
	 * @return the project's language codes.
	 */
	public List<String> getLanguageCodes() {
		List<String> result = new ArrayList<>();
		result.add(sourceLanguage);
		for (String language : translationLanguages) {
			if (!result.contains(language)) result.add(language);
		}
		return result;
	}

	// ------------------------------------------------------- //
	// -------------------- ScriptLoader API -------------------- //
	// ------------------------------------------------------- //

	/**
	 * Returns {@link ResourcePointer}s for all scripts (using the source language) and all
	 * translation files (using each translation's language code).
	 *
	 * @return the list of all available file descriptors.
	 * @throws IOException never thrown by this implementation.
	 */
	@Override
	public List<ResourcePointer> listDialogueBranchFiles() throws IOException {
		List<ResourcePointer> result = new ArrayList<>();

		for (String dialogueName : scriptContents.keySet()) {
			result.add(new ResourcePointer(sourceLanguage, dialogueName, ResourceType.SCRIPT));
		}

		for (Map.Entry<String, Map<String, String>> langEntry : translationContents.entrySet()) {
			String language = langEntry.getKey();
			for (String dialogueName : langEntry.getValue().keySet()) {
				result.add(new ResourcePointer(language, dialogueName, ResourceType.TRANSLATION));
			}
		}

		return result;
	}

	/**
	 * Opens the content for the file identified by the given {@link ResourcePointer} as a
	 * {@link StringReader}.
	 *
	 * @param fileDescriptor the descriptor of the file to open.
	 * @return a {@link Reader} over the stored content string.
	 * @throws IOException if the requested file is not present in this loader.
	 */
	@Override
	public Reader openFile(ResourcePointer fileDescriptor) throws IOException {
		String content;
		if (fileDescriptor.getResourceType() == ResourceType.SCRIPT) {
			content = scriptContents.get(fileDescriptor.getDialogueName());
			if (content == null) {
				throw new IOException("Published script not found: " + fileDescriptor.getDialogueName());
			}
		} else {
			Map<String, String> langMap = translationContents.get(fileDescriptor.getLanguage());
			content = langMap != null ? langMap.get(fileDescriptor.getDialogueName()) : null;
			if (content == null) {
				throw new IOException("Published translation not found: " +
						fileDescriptor.getLanguage() + "/" + fileDescriptor.getDialogueName());
			}
		}
		return new StringReader(content);
	}

}
