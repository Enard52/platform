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

package com.dialoguebranch.web.service.controller.schema.authoring;

import com.dialoguebranch.execution.parser.DialogueBranchHeader.Tag;

import java.util.List;

/**
 * Request payload for creating a new draft node: its header as tags (#350), and its body. The
 * node's title is its {@code title} tag.
 *
 * @author Harm op den Akker
 */
public class CreateNodePayload {

	/** Creates a new, empty {@link CreateNodePayload}. */
	public CreateNodePayload() { }

	private List<Tag> tags;
	private String body;

	/**
	 * Returns the header tags of the draft node to create, in order.
	 *
	 * @return the header tags.
	 */
	public List<Tag> getTags() { return tags; }

	/**
	 * Sets the header tags of the draft node to create, in order.
	 *
	 * @param tags the header tags.
	 */
	public void setTags(List<Tag> tags) { this.tags = tags; }

	/**
	 * Returns the raw body text of the draft node to create.
	 *
	 * @return the raw body text.
	 */
	public String getBody() { return body; }

	/**
	 * Sets the raw body text of the draft node to create.
	 *
	 * @param body the raw body text.
	 */
	public void setBody(String body) { this.body = body; }

}
