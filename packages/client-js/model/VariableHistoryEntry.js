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

/**
 * A VariableHistoryEntry is one recorded change to a Dialogue Branch Variable, as returned by the
 * Web Service's `GET /variables/history` end-point: the value a variable was given (or `null`, if
 * the change removed it), when the Web Service recorded the change, and where it came from.
 *
 * Unlike {@link Variable}, entries are read-only — the history is append-only, so there is nothing
 * to set on one.
 */
export class VariableHistoryEntry {

    // ------------------------------------
    // ---------- Constructor(s) ----------
    // ------------------------------------

    /**
     * @param {number} id The entry's identifier: unique, and increasing in recording order.
     * @param {string} name The variable's name (without the leading `$`).
     * @param {*} value The value the variable was given, or `null` if the change removed it.
     * @param {number} recordedTime When the Web Service recorded the change, in epoch milliseconds.
     * @param {string} recordedTimeZone The user's IANA time zone at that moment.
     * @param {string} source The change's source: `DLB_SCRIPT`, `INPUT_REPLY`, `WEB_SERVICE` or
     * `EXTERNAL`.
     */
    constructor(id, name, value, recordedTime, recordedTimeZone, source) {
        this._id = id;
        this._name = name;
        this._value = value;
        this._recordedTime = recordedTime;
        this._recordedTimeZone = recordedTimeZone;
        this._source = source;
    }

    /**
     * Builds a VariableHistoryEntry from the JSON form the Web Service sends.
     *
     * @param {Object} json `{ id, name, value, recordedTime, recordedTimeZone, source }`.
     * @returns {VariableHistoryEntry} The parsed entry.
     */
    static fromJSON(json) {
        return new VariableHistoryEntry(json.id, json.name, json.value ?? null, json.recordedTime,
            json.recordedTimeZone, json.source);
    }

    // -----------------------------
    // ---------- Getters ----------
    // -----------------------------

    /**
     * Returns the entry's identifier — unique, and increasing in the order entries were recorded,
     * so it can be used to recognise an entry already seen while paging through the history.
     * @returns {number} the entry's identifier.
     */
    get id() {
        return this._id;
    }

    /**
     * Returns the name of the variable that changed.
     * @returns {string} the variable's name.
     */
    get name() {
        return this._name;
    }

    /**
     * Returns the value the variable was given, or `null` if this change removed it.
     * @returns {*} the value, or `null`.
     */
    get value() {
        return this._value;
    }

    /**
     * Returns whether this change removed the variable (its value is `null`).
     * @returns {boolean} `true` for a removal.
     */
    get isRemoval() {
        return this._value === null;
    }

    /**
     * Returns when the Web Service recorded this change, in epoch milliseconds.
     * @returns {number} the recorded time.
     */
    get recordedTime() {
        return this._recordedTime;
    }

    /**
     * Returns the user's IANA time zone at the moment this change was recorded.
     * @returns {string} the time zone, e.g. `"Europe/Lisbon"`.
     */
    get recordedTimeZone() {
        return this._recordedTimeZone;
    }

    /**
     * Returns where this change came from: `DLB_SCRIPT` (a `<<set>>` in a dialogue),
     * `INPUT_REPLY` (a reply's input field), `WEB_SERVICE` (a `/variables/set*` call) or
     * `EXTERNAL` (the External Variable Service).
     * @returns {string} the source.
     */
    get source() {
        return this._source;
    }

}
