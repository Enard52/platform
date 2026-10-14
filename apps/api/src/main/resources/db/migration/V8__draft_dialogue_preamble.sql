-- A dialogue's preamble (issue #345): the comment lines at the very start of its script, before
-- the first node, about the script as a whole. Stored per draft dialogue, and written back at the
-- top of the script when it is reconstructed, so it is part of the published content as well.
-- NULL and an empty string both mean there is none.
ALTER TABLE draft_dialogues ADD COLUMN preamble TEXT NULL;
