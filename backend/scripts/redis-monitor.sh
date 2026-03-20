#!/bin/bash

# Redis Chat System Monitoring Script

echo "========================================="
echo "Resume Chat - Redis Monitoring"
echo "========================================="
echo ""

# Check Redis connection
echo "1. Redis Connection Status"
redis-cli ping > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Redis server connected"
else
    echo "❌ Redis server connection failed"
    exit 1
fi
echo ""

# Memory usage
echo "2. Memory Usage"
redis-cli INFO memory | grep used_memory_human
redis-cli INFO memory | grep used_memory_peak_human
redis-cli INFO memory | grep maxmemory_human
echo ""

# Connected clients
echo "3. Connected Clients"
redis-cli INFO clients | grep connected_clients
echo ""

# Chat message statistics
echo "4. Chat Message Statistics"
MESSAGE_COUNT=$(redis-cli KEYS "chat:message:*" | wc -l)
SESSION_COUNT=$(redis-cli KEYS "chat:messages:*" | wc -l)
ONLINE_COUNT=$(redis-cli KEYS "chat:online:*" | wc -l)
TYPING_COUNT=$(redis-cli KEYS "chat:typing:*" | wc -l)
UNREAD_COUNT=$(redis-cli KEYS "chat:unread:*" | wc -l)

echo "  - Stored messages: $MESSAGE_COUNT"
echo "  - Active sessions: $SESSION_COUNT"
echo "  - Online users: $ONLINE_COUNT"
echo "  - Typing: $TYPING_COUNT"
echo "  - Unread messages: $UNREAD_COUNT"
echo ""

# Recent active sessions
echo "5. Recent Active Sessions (Top 5)"
redis-cli KEYS "chat:messages:*" | head -5 | while read key; do
    if [ -n "$key" ]; then
        SESSION_TOKEN=$(echo $key | sed 's/chat:messages://')
        MESSAGE_COUNT=$(redis-cli LLEN $key)
        echo "  - Session $SESSION_TOKEN: $MESSAGE_COUNT messages"
    fi
done
echo ""

# Pub/Sub channels
echo "6. Pub/Sub Channels (Active subscriptions)"
redis-cli PUBSUB CHANNELS "chat:*" | head -10
echo ""

# Key expiration time (TTL)
echo "7. Key Expiration Sample"
redis-cli KEYS "chat:message:*" | head -3 | while read key; do
    if [ -n "$key" ]; then
        TTL=$(redis-cli TTL $key)
        if [ "$TTL" -eq -1 ]; then
            echo "  - $key: No expiration"
        elif [ "$TTL" -eq -2 ]; then
            echo "  - $key: Key not found"
        else
            echo "  - $key: Expires in ${TTL}s"
        fi
    fi
done
echo ""

# Slow queries
echo "8. Slow Queries (Recent 5)"
redis-cli SLOWLOG GET 5 | grep -E "timestamp|duration|command"
echo ""

echo "========================================="
echo "Monitoring Complete"
echo "========================================="
