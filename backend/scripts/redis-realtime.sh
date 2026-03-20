#!/bin/bash

# Redis Real-time Monitoring Script
# Refreshes every 1 second

echo "========================================="
echo "Resume Chat - Redis Real-time Monitor"
echo "Exit: Ctrl+C"
echo "========================================="
echo ""

while true; do
    clear
    echo "========================================="
    echo "Resume Chat - Redis Real-time Monitor"
    echo "Time: $(date '+%Y-%m-%d %H:%M:%S')"
    echo "========================================="
    echo ""

    # Memory usage
    echo "📊 Memory Usage"
    redis-cli INFO memory | grep -E "used_memory_human|used_memory_peak_human" | sed 's/^/  /'
    echo ""

    # Connected clients
    echo "👥 Connected Clients"
    CLIENTS=$(redis-cli INFO clients | grep connected_clients | cut -d: -f2 | tr -d '\r')
    echo "  - Connections: $CLIENTS"
    echo ""

    # Chat statistics
    echo "💬 Chat Statistics"
    MESSAGE_COUNT=$(redis-cli KEYS "chat:message:*" | wc -l | tr -d ' ')
    SESSION_COUNT=$(redis-cli KEYS "chat:messages:*" | wc -l | tr -d ' ')
    ONLINE_COUNT=$(redis-cli KEYS "chat:online:*" | wc -l | tr -d ' ')
    TYPING_COUNT=$(redis-cli KEYS "chat:typing:*" | wc -l | tr -d ' ')
    UNREAD_COUNT=$(redis-cli KEYS "chat:unread:*" | wc -l | tr -d ' ')

    echo "  - Stored messages: $MESSAGE_COUNT"
    echo "  - Active sessions: $SESSION_COUNT"
    echo "  - Online users: $ONLINE_COUNT"
    echo "  - Typing: $TYPING_COUNT"
    echo "  - Unread message keys: $UNREAD_COUNT"
    echo ""

    # Command statistics
    echo "📈 Command Stats (per second)"
    redis-cli INFO stats | grep instantaneous_ops_per_sec | sed 's/^/  /'
    echo ""

    # Pub/Sub channels
    echo "📡 Active Pub/Sub Channels (Top 5)"
    redis-cli PUBSUB CHANNELS "chat:*" | head -5 | sed 's/^/  - /'
    echo ""

    # Recent active sessions
    echo "🔥 Recent Active Sessions (Top 3)"
    redis-cli KEYS "chat:messages:*" | head -3 | while read key; do
        if [ -n "$key" ]; then
            SESSION_TOKEN=$(echo $key | sed 's/chat:messages://' | cut -c1-20)
            MESSAGE_COUNT=$(redis-cli LLEN $key)
            echo "  - ${SESSION_TOKEN}...: $MESSAGE_COUNT messages"
        fi
    done
    echo ""

    sleep 1
done
