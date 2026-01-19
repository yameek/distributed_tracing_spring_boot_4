#!/bin/bash

# Script to clean up old Gradle daemon log files
# Prevents disk space exhaustion from runaway Gradle processes

set -e

GRADLE_DAEMON_DIR="$HOME/.gradle/daemon"
MAX_LOG_SIZE_MB=100
MAX_LOG_AGE_DAYS=7

echo "Cleaning up Gradle daemon logs..."

if [ ! -d "$GRADLE_DAEMON_DIR" ]; then
    echo "Gradle daemon directory not found: $GRADLE_DAEMON_DIR"
    exit 0
fi

# Find and delete old log files (> 7 days)
echo "Deleting log files older than $MAX_LOG_AGE_DAYS days..."
find "$GRADLE_DAEMON_DIR" -name "*.out.log" -type f -mtime +$MAX_LOG_AGE_DAYS -delete

# Find and warn about large log files (> 100MB)
echo "Checking for large log files (> ${MAX_LOG_SIZE_MB}MB)..."
find "$GRADLE_DAEMON_DIR" -name "*.out.log" -type f -size +${MAX_LOG_SIZE_MB}M | while read -r logfile; do
    size=$(du -h "$logfile" | cut -f1)
    echo "WARNING: Large log file found: $logfile ($size)"
    echo "  Consider investigating the process that created this log."
done

# Count remaining log files
remaining=$(find "$GRADLE_DAEMON_DIR" -name "*.out.log" -type f | wc -l)
echo "Cleanup complete. Remaining log files: $remaining"
