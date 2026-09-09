-- Negative entity IDs preserve orphan receipts without linking them to live records.
ALTER TABLE attachments DROP CONSTRAINT chk_attachment_entity_id;
ALTER TABLE attachments ADD CONSTRAINT chk_attachment_entity_id CHECK (entity_id <> 0);
