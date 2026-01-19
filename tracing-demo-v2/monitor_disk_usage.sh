#!/bin/bash

# Disk usage monitoring script
# Alerts when disk usage exceeds thresholds and provides cleanup suggestions

set -e

# Configuration
WARNING_THRESHOLD=80
CRITICAL_THRESHOLD=90
CHECK_INTERVAL=300  # 5 minutes

# Colors
RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

check_disk_usage() {
    local mount_point="${1:-/home/yaziz}"
    
    # Get disk usage percentage
    local usage=$(df -h "$mount_point" | tail -1 | awk '{print $5}' | sed 's/%//')
    local available=$(df -h "$mount_point" | tail -1 | awk '{print $4}')
    local used=$(df -h "$mount_point" | tail -1 | awk '{print $3}')
    local total=$(df -h "$mount_point" | tail -1 | awk '{print $2}')
    
    echo "Disk Usage: ${usage}% (${used}/${total} used, ${available} available)"
    
    if [ "$usage" -ge "$CRITICAL_THRESHOLD" ]; then
        echo -e "${RED}CRITICAL: Disk usage is ${usage}% (threshold: ${CRITICAL_THRESHOLD}%)${NC}"
        suggest_cleanup
        return 2
    elif [ "$usage" -ge "$WARNING_THRESHOLD" ]; then
        echo -e "${YELLOW}WARNING: Disk usage is ${usage}% (threshold: ${WARNING_THRESHOLD}%)${NC}"
        suggest_cleanup
        return 1
    else
        echo -e "${GREEN}OK: Disk usage is ${usage}%${NC}"
        return 0
    fi
}

suggest_cleanup() {
    echo ""
    echo "Cleanup suggestions:"
    echo "  1. Check for large log files:"
    echo "     find logs/ -name '*.log' -size +100M -ls"
    echo ""
    echo "  2. Clean old Gradle daemon logs:"
    echo "     ./cleanup_gradle_logs.sh"
    echo ""
    echo "  3. Clean old log files (> 7 days):"
    echo "     find logs/ -name '*.log' -mtime +7 -delete"
    echo ""
    echo "  4. Check for large files in workspace:"
    echo "     du -h --max-depth=2 . | sort -hr | head -20"
    echo ""
    echo "  5. Clean Docker volumes (if not needed):"
    echo "     docker system prune -a --volumes"
}

check_large_logs() {
    echo ""
    echo "Checking for large log files..."
    
    local large_logs=$(find logs/ -name "*.log" -type f -size +100M 2>/dev/null | head -10)
    
    if [ -n "$large_logs" ]; then
        echo -e "${YELLOW}Large log files found (>100MB):${NC}"
        while IFS= read -r logfile; do
            if [ -f "$logfile" ]; then
                size=$(du -h "$logfile" | cut -f1)
                echo "  - $logfile ($size)"
            fi
        done <<< "$large_logs"
    else
        echo -e "${GREEN}No large log files found${NC}"
    fi
}

# Main execution
main() {
    echo "=== Disk Usage Monitor ==="
    echo "Mount point: /home/yaziz"
    echo "Warning threshold: ${WARNING_THRESHOLD}%"
    echo "Critical threshold: ${CRITICAL_THRESHOLD}%"
    echo ""
    
    check_disk_usage "/home/yaziz"
    local status=$?
    
    check_large_logs
    
    echo ""
    echo "=== Monitoring complete ==="
    
    exit $status
}

# Run if executed directly
if [ "${BASH_SOURCE[0]}" == "${0}" ]; then
    main "$@"
fi
