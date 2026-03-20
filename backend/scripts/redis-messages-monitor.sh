#!/bin/bash

# Redis Chat Messages Monitor
# Shows stored messages in Redis with details

echo "========================================="
echo "Resume Chat - Stored Messages Monitor"
echo "========================================="
echo ""

# Function to display a single message
display_message() {
    local msg_key=$1

    # Get message details
    local msg_id=$(redis-cli HGET "$msg_key" messageId 2>/dev/null)
    local session=$(redis-cli HGET "$msg_key" sessionToken 2>/dev/null)
    local sender=$(redis-cli HGET "$msg_key" senderType 2>/dev/null)
    local content=$(redis-cli HGET "$msg_key" content 2>/dev/null)
    local created=$(redis-cli HGET "$msg_key" createdAt 2>/dev/null)
    local read_status=$(redis-cli HGET "$msg_key" readStatus 2>/dev/null)

    if [ -n "$msg_id" ]; then
        echo "Message ID: $msg_id"
        echo "Session: $session"
        echo "Sender: $sender"
        echo "Content: $content"
        echo "Created: $created"
        echo "Read: $read_status"
        echo "---"
    fi
}

# 1. Show total counts
echo "📊 Message Statistics"
MESSAGE_COUNT=$(redis-cli KEYS "chat:message:*" | wc -l | tr -d ' ')
SESSION_COUNT=$(redis-cli KEYS "chat:messages:*" | wc -l | tr -d ' ')
echo "Total messages in Redis: $MESSAGE_COUNT"
echo "Total sessions: $SESSION_COUNT"
echo ""

# 2. Show messages by session
echo "📝 Messages by Session (Recent 3 sessions)"
redis-cli KEYS "chat:messages:*" | head -3 | while read session_key; do
    if [ -n "$session_key" ]; then
        SESSION_TOKEN=$(echo $session_key | sed 's/chat:messages://')
        MESSAGE_COUNT=$(redis-cli LLEN $session_key)

        echo ""
        echo "═══════════════════════════════════════"
        echo "Session: $SESSION_TOKEN"
        echo "Messages: $MESSAGE_COUNT"
        echo "═══════════════════════════════════════"

        # Get recent 5 messages from this session
        redis-cli LRANGE "$session_key" 0 4 | while read msg_id; do
            if [ -n "$msg_id" ]; then
                msg_key="chat:message:$msg_id"
                display_message "$msg_key"
            fi
        done
    fi
done

echo ""
echo "========================================="
echo "Use './scripts/redis-pubsub-monitor.sh' to see real-time messages"
echo "========================================="
