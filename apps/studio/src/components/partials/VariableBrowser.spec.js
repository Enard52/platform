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

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';
import { ref } from 'vue';

// The Variable Browser's "History" mode (#323), mounted against a mocked client. The pure rules
// (paging, de-duplication, exact-name resolution, filtering, the hint) have their own spec in
// composables/variable-history.spec.js; this one covers the wiring: when history is requested,
// with which parameters, and what ends up on screen.

const playback = {
    getVariables: vi.fn(),
    getVariableHistory: vi.fn(),
    setVariable: vi.fn(),
};
const authoring = {
    listProjectVariables: vi.fn(),
    listSupportedVariables: vi.fn(),
};

vi.mock('@/composables/client.js', () => ({
    useClient: () => playback,
    useAuthoringClient: () => authoring,
    getBrowserTimeZone: () => 'Asia/Tokyo',
}));

const { default: VariableBrowser } = await import('./VariableBrowser.vue');

const PAGE_SIZE = 50;

/** A history entry as client-js returns it (plain object is enough for the template). */
function entry(id, name, value = 'x', source = 'WEB_SERVICE') {
    // 2026-10-11T11:46:38Z — 20:46:38 in Tokyo, the mocked Studio user's time zone.
    return { id, name, value, recordedTime: Date.UTC(2026, 9, 11, 11, 46, 38),
        recordedTimeZone: 'Europe/Lisbon', source };
}

/** A full page (so the browser offers "Load more") of ids from `firstId` downwards. */
function fullPage(firstId, name = 'busy') {
    return Array.from({ length: PAGE_SIZE }, (_, i) => entry(firstId - i, name));
}

/** A promise plus its resolver, to control the order responses arrive in. */
function deferred() {
    let resolve;
    const promise = new Promise((r) => { resolve = r; });
    return { promise, resolve };
}

function mountBrowser() {
    return mount(VariableBrowser, {
        global: {
            provide: { state: ref({ selectedProject: { slug: 'default-test' } }) },
            stubs: { FontAwesomeIcon: true },
        },
    });
}

async function openHistory(wrapper) {
    const button = wrapper.findAll('button').find((b) => b.text() === 'History');
    await button.trigger('click');
    await flushPromises();
}

const shownNames = (wrapper) =>
    wrapper.findAll('[data-test="history-entry-name"]').map((n) => n.text());
const lastHistoryCall = () => playback.getVariableHistory.mock.calls.at(-1)[0];

beforeEach(() => {
    vi.clearAllMocks();
    playback.getVariables.mockResolvedValue([{ name: 'mood', value: 'fine' }]);
    playback.getVariableHistory.mockResolvedValue([]);
    authoring.listProjectVariables.mockResolvedValue([{ name: 'currentTime', read: true, written: false }]);
    authoring.listSupportedVariables.mockResolvedValue([]);
});

describe('VariableBrowser — History mode', () => {
    it('does not request history until History is opened', async () => {
        mountBrowser();
        await flushPromises();
        expect(playback.getVariableHistory).not.toHaveBeenCalled();
    });

    it('requests the first page for the current project when opened', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        expect(playback.getVariableHistory).toHaveBeenCalledTimes(1);
        expect(lastHistoryCall()).toEqual({ projectSlug: 'default-test', variableName: undefined,
            page: 0, pageSize: PAGE_SIZE });
    });

    it('shows each change: name, value or "removed", source and time in the Studio user\'s zone', async () => {
        playback.getVariableHistory.mockResolvedValue([
            entry(2, 'mood', null, 'WEB_SERVICE'),
            entry(1, 'mood', { week: 3 }, 'DLB_SCRIPT'),
        ]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        const rows = wrapper.findAll('[data-test="history-entry"]');
        expect(rows).toHaveLength(2);
        expect(rows[0].get('[data-test="history-entry-value"]').text()).toBe('removed');
        expect(rows[0].get('[data-test="history-entry-source"]').text()).toBe('Web Service');
        expect(rows[1].get('[data-test="history-entry-value"]').text()).toBe('{"week":3}');
        expect(rows[1].get('[data-test="history-entry-source"]').text()).toBe('Script');
        // Tokyo (the mocked browser zone: 20:46:38, or 8:46:38 PM in a 12-hour locale), not
        // Lisbon (the zone it was recorded in, which would read 12:46:38).
        expect(rows[0].get('[data-test="history-entry-time"]').text()).toMatch(/\b(20|8):46:38\b/);
    });

    it('says so when there is no history at all', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);
        expect(wrapper.text()).toContain('No variable changes recorded yet.');
    });

    it('re-requests the first page when loadVariables() runs while History is showing', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        wrapper.vm.loadVariables(); // what MainPage calls after every dialogue step
        await flushPromises();

        expect(playback.getVariableHistory).toHaveBeenCalledTimes(2);
        expect(lastHistoryCall().page).toBe(0);
    });

    it('does not request history when loadVariables() runs in another mode', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        wrapper.vm.loadVariables();
        await flushPromises();
        expect(playback.getVariableHistory).not.toHaveBeenCalled();
    });

    it('offers "Load more" after a full page and appends the next page without repeats', async () => {
        playback.getVariableHistory
            .mockResolvedValueOnce(fullPage(100))
            // New rows were recorded meanwhile, so page 1 overlaps page 0 by two entries.
            .mockResolvedValueOnce([entry(52, 'busy'), entry(51, 'busy'), entry(50, 'old')]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        await wrapper.get('[data-test="history-load-more"]').trigger('click');
        await flushPromises();

        expect(lastHistoryCall().page).toBe(1);
        expect(wrapper.findAll('[data-test="history-entry"]')).toHaveLength(PAGE_SIZE + 1);
        expect(shownNames(wrapper).at(-1)).toBe('$old');
        // A short page means there's nothing more to load.
        expect(wrapper.find('[data-test="history-load-more"]').exists()).toBe(false);
    });

    it('starts over from the first page on a refresh after "Load more"', async () => {
        playback.getVariableHistory
            .mockResolvedValueOnce(fullPage(100))
            .mockResolvedValueOnce([entry(50, 'old')])
            .mockResolvedValueOnce([entry(101, 'newest')]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);
        await wrapper.get('[data-test="history-load-more"]').trigger('click');
        await flushPromises();

        wrapper.vm.loadVariables();
        await flushPromises();

        expect(lastHistoryCall().page).toBe(0);
        expect(shownNames(wrapper)).toEqual(['$newest']);
    });

    it('asks the server for one variable when the search names a known variable exactly', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        await wrapper.get('input[type="text"]').setValue('$currentTime'); // known via "Used"
        await flushPromises();

        expect(lastHistoryCall()).toMatchObject({ variableName: 'currentTime', page: 0 });
    });

    it('filters loaded entries in the browser for partial text, with a hint while more may exist', async () => {
        playback.getVariableHistory.mockResolvedValue([
            ...fullPage(100, 'busy').slice(0, PAGE_SIZE - 1), entry(1, 'mood'),
        ]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);
        const callsBefore = playback.getVariableHistory.mock.calls.length;

        await wrapper.get('input[type="text"]').setValue('mo');
        await flushPromises();

        expect(playback.getVariableHistory.mock.calls.length).toBe(callsBefore); // no new request
        expect(shownNames(wrapper)).toEqual(['$mood']);
        expect(wrapper.find('[data-test="history-loaded-only-hint"]').exists()).toBe(true);
    });

    it('narrows to a variable when its name is clicked', async () => {
        playback.getVariableHistory.mockResolvedValue([entry(2, 'mood'), entry(1, 'energy')]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        await wrapper.findAll('[data-test="history-entry-name"]')[1].trigger('click');
        await flushPromises();

        expect(wrapper.get('input[type="text"]').element.value).toBe('energy');
        expect(lastHistoryCall()).toMatchObject({ variableName: 'energy', page: 0 });
    });

    it('offers only newest/oldest order, and oldest first reverses the loaded entries', async () => {
        playback.getVariableHistory.mockResolvedValue([entry(3, 'c'), entry(2, 'b'), entry(1, 'a')]);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);

        const select = wrapper.get('select');
        expect(select.findAll('option').map((o) => o.element.value)).toEqual(['newest', 'oldest']);
        await select.setValue('oldest');

        expect(shownNames(wrapper)).toEqual(['$a', '$b', '$c']);
    });

    it('keeps the name sort for the other modes', async () => {
        const wrapper = mountBrowser();
        await flushPromises();
        expect(wrapper.get('select').findAll('option').map((o) => o.element.value))
            .toEqual(['name-asc', 'name-desc']);
    });

    it('ignores a slow response that arrives after a newer one', async () => {
        const slow = deferred();
        const fast = deferred();
        playback.getVariableHistory
            .mockReturnValueOnce(slow.promise)
            .mockReturnValueOnce(fast.promise);
        const wrapper = mountBrowser();
        await flushPromises();
        await openHistory(wrapper);   // request 1 (slow)
        wrapper.vm.loadVariables();   // request 2 (fast)

        fast.resolve([entry(2, 'fresh')]);
        await flushPromises();
        slow.resolve([entry(1, 'stale')]);
        await flushPromises();

        expect(shownNames(wrapper)).toEqual(['$fresh']);
    });
});
