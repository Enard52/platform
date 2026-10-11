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

package com.dialoguebranch.web.service.controller.schema;

import com.dialoguebranch.execution.VariableUpdatedSource;
import org.jspecify.annotations.Nullable;

/**
 * One entry in the {@code /variables/history} response: the value a Dialogue Branch Variable was
 * given by a single change, and when (#259).
 *
 * @param id               the entry's identifier: unique, and increasing in the order entries were
 *                         recorded. Lets a client paging through the history recognise an entry
 *                         it has already seen when new entries shift the pages in between.
 * @param name             the variable name.
 * @param value            the value the variable was given, or {@code null} if this change removed
 *                         it.
 * @param recordedTime     when the Web Service recorded the change, in epoch milliseconds.
 * @param recordedTimeZone the user's time zone at that moment (an IANA name, e.g.
 *                         {@code Europe/Lisbon}).
 * @param source           where the change came from (a script, a reply input, the Web Service
 *                         API, or the External Variable Service).
 */
public record VariableHistoryEntry(long id, String name, @Nullable Object value, long recordedTime,
		String recordedTimeZone, VariableUpdatedSource source) {
}
