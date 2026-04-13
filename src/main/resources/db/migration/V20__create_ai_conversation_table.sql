-- Sprint 11: AI Assistant Integration
-- Create ai_conversations table to store conversation history

-- Table: ai_conversations
-- Purpose: Store AI assistant conversation sessions with message history
-- Message format: JSON array of {role, content, timestamp} objects
CREATE TABLE ai_conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    messages TEXT NOT NULL,  -- JSON array of conversation messages
    title VARCHAR(200),      -- Optional conversation title
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Index: Optimize queries by user_id (most common filter)
CREATE INDEX idx_ai_conversation_user_id ON ai_conversations(user_id);

-- Index: Optimize sorting by creation date (for conversation list display)
CREATE INDEX idx_ai_conversation_created_at ON ai_conversations(created_at DESC);

-- Index: Optimize sorting by update date (for "most recent" queries)
CREATE INDEX idx_ai_conversation_updated_at ON ai_conversations(updated_at DESC);
