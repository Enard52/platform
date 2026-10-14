import { describe, it, expect } from 'vitest';
import { parseHeaderTags, serializeHeaderTags, getPosition, setPosition } from './DlbHeaderTags.js';

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
