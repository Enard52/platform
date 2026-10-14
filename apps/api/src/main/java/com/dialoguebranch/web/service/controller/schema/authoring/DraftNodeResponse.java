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

import com.dialoguebranch.execution.parser.DialogueBranchHeader;
import com.dialoguebranch.execution.parser.DialogueBranchHeader.Tag;
import com.dialoguebranch.web.service.storage.model.DBDraftNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A draft node as returned by the authoring end-points: its header as tags, in order, rather than
 * header text (#350).
 *
 * @param id        the node's id.
 * @param title     the node's title (its {@code title} tag).
 * @param tags      the node's header tags, in order.
 * @param body      the node's raw body text.
 * @param createdAt when the node was created.
 * @param updatedAt when the node was last changed.
 * @author Harm op den Akker
 */
public record DraftNodeResponse(UUID id, String title, List<Tag> tags, String body,
		Instant createdAt, Instant updatedAt) {

	/**
	 * Creates the response for a stored draft node.
	 *
	 * @param node the stored draft node.
	 * @return the response.
	 */
	public static DraftNodeResponse from(DBDraftNode node) {
		return new DraftNodeResponse(node.getId(), node.getTitle(),
				DialogueBranchHeader.readTags(node.getHeader()), node.getBody(),
				node.getCreatedAt(), node.getUpdatedAt());
	}
}
