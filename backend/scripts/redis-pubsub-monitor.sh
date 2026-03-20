#!/bin/bash

# Redis Pub/Sub Real-time Message Monitor
# Monitors all chat messages being published

echo "========================================="
echo "Resume Chat - Pub/Sub Message Monitor"
echo "Exit: Ctrl+C"
echo "========================================="
echo ""
echo "Subscribing to all chat:* channels..."
echo "Waiting for messages..."
echo ""

# Subscribe to all chat channels and display messages
redis-cli PSUBSCRIBE "chat:*" | while read line; do
    # Parse Redis pub/sub output
    if [[ "$line" == "pmessage" ]]; then
        read pattern
        read channel
        read message

        # Format timestamp
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

        # Parse channel type
        if [[ "$channel" == *":typing" ]]; then
            CHANNEL_TYPE="TYPING"
        elif [[ "$channel" == *":presence" ]]; then
            CHANNEL_TYPE="PRESENCE"
        else
            CHANNEL_TYPE="MESSAGE"
        fi

        # Display formatted message
        echo "[$TIMESTAMP] [$CHANNEL_TYPE]"
        echo "Channel: $channel"
        echo "Message: $message"
        echo "---"
    fi
done
