import { describe, it, expect } from 'vitest';
import {
    RESERVED_HEADER_TAGS,
    parseHeaderTags,
    serializeHeaderTags,
    getPosition,
    setPosition,
} from './DlbHeaderTags.js';

describe('DlbHeaderTags', () => {
    it('includes comment in RESERVED_HEADER_TAGS', () => {
        expect(RESERVED_HEADER_TAGS).toEqual([
            'title',
            'speaker',
            'position',
            'colorId',
            'comment',
        ]);
    });

    it('parses comment header tag along with other tags', () => {
        const header = [
            'title: Step1',
            'speaker: User',
            'comment: Greeting node note',
            'colorId: 2',
            'position: 10,20',
        ].join('\n');
        const tags = parseHeaderTags(header);
        expect(tags.get('comment')).toBe('Greeting node note');
        expect(tags.get('title')).toBe('Step1');
    });

    it('serializes header tags preserving comment tag', () => {
        const tags = new Map([
            ['title', 'Step1'],
            ['comment', 'My note'],
        ]);
        expect(serializeHeaderTags(tags)).toBe('title: Step1\ncomment: My note');
    });
});
