/* @license
 *
 *                Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
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

// Pure helpers behind the Variable Browser's "History" mode (#323) — no Vue, no network, so each
// rule here is unit-testable on its own (see variable-history.spec.js). The component wires them
// to the client and its own reactive state.
//
// The one thing to keep in mind throughout: history pages come from the server, newest first, by
// offset. Studio only ever holds the pages it has loaded so far, so anything done *in the browser*
// (a partial-name filter, oldest-first order) only covers those loaded entries — see
// needsLoadedOnlyHint().

/** Entries requested per "page" — the end-point's own default. */
export const HISTORY_PAGE_SIZE = 50;

/**
 * Display labels for a change's source, shared by the "Values" tooltip and the "History" rows.
 * `italic` marks the "we don't know" case.
 */
export const SOURCE_LABELS = {
    UNKNOWN:      { label: 'Unknown',          italic: true },
    DLB_SCRIPT:   { label: 'Script',           italic: false },
    INPUT_REPLY:  { label: 'User Input',       italic: false },
    WEB_SERVICE:  { label: 'Web Service',      italic: false },
    EXTERNAL:     { label: 'External Service', italic: false },
};

/**
 * Returns the display label for a source value, falling back to "Unknown" for anything missing
 * or unrecognised.
 *
 * @param {string|null|undefined} source e.g. `DLB_SCRIPT`.
 * @returns {{label: string, italic: boolean}}
 */
export function sourceLabel(source) {
    return SOURCE_LABELS[source] ?? SOURCE_LABELS.UNKNOWN;
}

/**
 * Formats an epoch-millisecond timestamp in the given time zone — always the time zone of the
 * person using Studio (#323), never the zone the value was recorded in.
 *
 * @param {number|null|undefined} epochMillis the timestamp.
 * @param {string} timeZone an IANA time zone, e.g. `Europe/Lisbon`.
 * @param {Intl.DateTimeFormatOptions} [style] defaults to a compact date + time.
 * @returns {string|null} the formatted time, or `null` if there is no timestamp.
 */
export function formatTimestamp(epochMillis, timeZone,
        style = { dateStyle: 'short', timeStyle: 'medium' }) {
    if (epochMillis == null) return null;
    return new Date(epochMillis).toLocaleString(undefined, { ...style, timeZone });
}

/**
 * Formats a history entry's value for display: strings as-is, anything structured as JSON.
 * Returns `null` for a removal — the template renders that as "removed".
 *
 * @param {*} value the entry's value.
 * @returns {string|null}
 */
export function formatHistoryValue(value) {
    if (value === null || value === undefined) return null;
    return typeof value === 'string' ? value : JSON.stringify(value);
}

/**
 * Appends a newly loaded page to the entries already shown, dropping any it repeats. Pages are by
 * offset, so when new changes are recorded between two requests, the next page starts with
 * entries the previous one already had — each entry's `id` is what recognises them.
 *
 * @param {Array<{id: number}>} loaded the entries already shown.
 * @param {Array<{id: number}>} page the newly loaded page.
 * @returns {Array<{id: number}>} a new array: `loaded` followed by the page's unseen entries.
 */
export function mergeHistoryPage(loaded, page) {
    const seen = new Set(loaded.map((entry) => entry.id));
    return [...loaded, ...page.filter((entry) => !seen.has(entry.id))];
}

/**
 * Whether the server may have more entries after a page: a full page means "maybe", a short one
 * means "no". (A full last page costs one extra, empty request — the end-point doesn't report a
 * total.)
 *
 * @param {Array} page the page just loaded.
 * @param {number} pageSize the page size it was requested with.
 * @returns {boolean}
 */
export function mayHaveMore(page, pageSize) {
    return page.length >= pageSize;
}

/**
 * Resolves the search box's text to an exact, known variable name — the case in which the server
 * is asked for that one variable's history (#323), so it pages correctly however busy the other
 * variables are. Anything else (empty, partial, or unknown text) returns `null`: the loaded
 * entries are then filtered in the browser instead.
 *
 * A leading `$` is ignored, since that's how names are displayed. Matching is case-insensitive,
 * but returns the name as it's actually spelled, preferring an exact-case match if two known names
 * differ only by case.
 *
 * @param {string} query the search box's text.
 * @param {Iterable<string>} knownNames every variable name Studio knows of for this project.
 * @returns {string|null} the canonical variable name, or `null`.
 */
export function resolveExactName(query, knownNames) {
    const wanted = (query ?? '').trim().replace(/^\$/, '');
    if (!wanted) return null;
    let caseInsensitiveMatch = null;
    for (const name of knownNames) {
        if (name === wanted) return name;
        if (caseInsensitiveMatch === null && name.toLowerCase() === wanted.toLowerCase())
            caseInsensitiveMatch = name;
    }
    return caseInsensitiveMatch;
}

/**
 * Filters and orders the loaded entries for display.
 *
 * When `exactName` is set, the server has already filtered to that variable, so no further
 * filtering happens here. Otherwise a non-empty `query` keeps entries whose name contains it
 * (case-insensitive, leading `$` ignored).
 *
 * @param {Array<{id: number, name: string}>} entries the loaded entries, newest first.
 * @param {Object} options
 * @param {string} [options.query] the search box's text.
 * @param {string|null} [options.exactName] see {@link resolveExactName}.
 * @param {'newest'|'oldest'} [options.order] defaults to newest first.
 * @returns {Array} a new array.
 */
export function filterSortHistory(entries, { query = '', exactName = null, order = 'newest' } = {}) {
    const needle = query.trim().replace(/^\$/, '').toLowerCase();
    const filtered = exactName || !needle
        ? entries
        : entries.filter((entry) => entry.name.toLowerCase().includes(needle));
    const direction = order === 'oldest' ? 1 : -1;
    return [...filtered].sort((a, b) => direction * (a.id - b.id));
}

/**
 * Whether to show the "only loaded entries" hint: when what's displayed was narrowed or reordered
 * in the browser (a partial-name filter, or oldest-first order) and the server may still have
 * older entries that aren't loaded yet. Newest-first and exact-name views are complete as far as
 * they go, so they never need it.
 *
 * @param {Object} options
 * @param {string} [options.query] the search box's text.
 * @param {string|null} [options.exactName] see {@link resolveExactName}.
 * @param {'newest'|'oldest'} [options.order]
 * @param {boolean} options.hasMore see {@link mayHaveMore}.
 * @returns {boolean}
 */
export function needsLoadedOnlyHint({ query = '', exactName = null, order = 'newest', hasMore }) {
    if (!hasMore) return false;
    const partialSearch = !exactName && query.trim().replace(/^\$/, '').length > 0;
    return partialSearch || order === 'oldest';
}
