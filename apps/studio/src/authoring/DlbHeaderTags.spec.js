import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { parseHeaderTags, serializeHeaderTags, getPosition, setPosition, RESERVED_HEADER_TAGS } from './DlbHeaderTags.js';

describe('parseHeaderTags', () => {
    it('keeps // as part of a value', () => {
        const tags = parseHeaderTags('title: Start\nimage: https://example.com/a.png // not a comment');
        expect(tags.get('image')).toBe('https://example.com/a.png // not a comment');
    });

    it('splits at the first colon, trims, and lets the first occurrence win', () => {
        const tags = parseHeaderTags('title:  Start \r\ntime: 10:30\nnot a tag\ntitle: Ignored');
        expect([...tags.entries()]).toEqual([['title', 'Start'], ['time', '10:30']]);
    });

    it('round-trips through serializeHeaderTags', () => {
        const header = 'title: Start\nspeaker: Robin\ncomment: see https://example.com';
        expect(serializeHeaderTags(parseHeaderTags(header))).toBe(header);
    });
});

describe('position', () => {
    it('reads and sets the position tag', () => {
        const tags = parseHeaderTags('title: Start\nposition: 10,20');
        expect(getPosition(tags)).toEqual({ x: 10, y: 20 });
        expect(serializeHeaderTags(setPosition(tags, 30.4, 40.6))).toBe('title: Start\nposition: 30,41');
    });
});

describe('RESERVED_HEADER_TAGS', () => {
    it('matches DialogueBranchConstants.DLB_RESERVED_HEADER_TAGS in core', () => {
        // Vitest runs from apps/studio.
        const constants = readFileSync(resolve(process.cwd(),
            '../../packages/core/src/main/java/com/dialoguebranch/model/common/DialogueBranchConstants.java'),
            'utf8');
        const declaration = constants.match(/DLB_RESERVED_HEADER_TAGS\s*=\s*\{([^}]*)\}/)[1];
        const coreTags = [...declaration.matchAll(/"([^"]+)"/g)].map((match) => match[1]);
        expect(RESERVED_HEADER_TAGS).toEqual(coreTags);
        expect(RESERVED_HEADER_TAGS).toContain('comment');
    });
});
