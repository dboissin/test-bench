ALTER TABLE processing_messages MODIFY created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE processing_messages MODIFY processed_at TIMESTAMP;
