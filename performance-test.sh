#!/bin/bash

# Performance Testing Script for Events API
# Tests the /search endpoint with various scenarios and measures response times

set -e

# Configuration
BASE_URL="http://localhost:8080"
ENDPOINT="/search"
OUTPUT_DIR="./performance-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}🚀 Events API Performance Testing Script${NC}"
echo "================================================="
echo "Base URL: $BASE_URL"
echo "Timestamp: $TIMESTAMP"
echo "Results will be saved to: $OUTPUT_DIR"
echo ""

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Function to check if server is running
check_server() {
    print_info "Checking if server is running..."
    if curl -s "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        print_status "Server is running"
    else
        print_error "Server is not running at $BASE_URL"
        print_info "Please start the server with: ./gradlew bootRun"
        exit 1
    fi
}

# Function to create curl timing format file
create_curl_format() {
    cat > "$OUTPUT_DIR/curl-format.txt" << 'EOF'
\n==================== TIMING BREAKDOWN ====================\n
     DNS Lookup: %{time_namelookup}s\n
    TCP Connect: %{time_connect}s\n
   TLS Handshake: %{time_appconnect}s\n
  Pre-transfer: %{time_pretransfer}s\n
     Redirect: %{time_redirect}s\n
Start Transfer: %{time_starttransfer}s\n
                     ----------\n
    TOTAL TIME: %{time_total}s\n
   HTTP Status: %{http_code}\n
  Content Type: %{content_type}\n
     Size (KB): %{size_download} bytes\n
========================================================\n
EOF
}

# Function to test single request with detailed timing
test_single_request() {
    local test_name="$1"
    local url="$2"
    local output_file="$OUTPUT_DIR/${test_name}_${TIMESTAMP}.txt"

    print_info "Testing: $test_name"
    echo "URL: $url"

    # Test the request and capture detailed timing
    curl -w "@$OUTPUT_DIR/curl-format.txt" \
         -o "$output_file" \
         -s "$url" \
         -H "Accept: application/json"

    # Show response content
    echo "Response preview:"
    head -n 10 "$output_file" | jq . 2>/dev/null || cat "$output_file" | head -n 10
    echo ""
}

# Function to run load test (sequential requests)
run_load_test() {
    local test_name="$1"
    local url="$2"
    local num_requests="$3"
    local results_file="$OUTPUT_DIR/${test_name}_load_test_${TIMESTAMP}.csv"

    print_info "Load Testing: $test_name ($num_requests requests)"

    # Create CSV header
    echo "request_num,response_time_ms,http_code,size_bytes" > "$results_file"

    local total_time=0
    local successful_requests=0
    local failed_requests=0

    for i in $(seq 1 $num_requests); do
        # Measure response time
        start_time=$(date +%s%N)
        response=$(curl -s -o /dev/null -w "%{http_code},%{size_download},%{time_total}" "$url")
        end_time=$(date +%s%N)

        # Calculate response time in milliseconds
        response_time_ms=$(echo "scale=2; ($end_time - $start_time) / 1000000" | bc)

        # Parse curl output
        IFS=',' read -r http_code size_bytes curl_time <<< "$response"

        # Log to CSV
        echo "$i,$response_time_ms,$http_code,$size_bytes" >> "$results_file"

        # Update counters
        if [ "$http_code" = "200" ]; then
            successful_requests=$((successful_requests + 1))
        else
            failed_requests=$((failed_requests + 1))
        fi

        total_time=$(echo "$total_time + $response_time_ms" | bc)

        # Progress indicator
        if [ $((i % 10)) -eq 0 ]; then
            echo -n "."
        fi
    done

    echo "" # New line after progress dots

    # Calculate statistics
    local avg_time=$(echo "scale=2; $total_time / $num_requests" | bc)
    local success_rate=$(echo "scale=2; $successful_requests * 100 / $num_requests" | bc)

    print_status "Load test completed!"
    echo "  Total requests: $num_requests"
    echo "  Successful: $successful_requests"
    echo "  Failed: $failed_requests"
    echo "  Success rate: $success_rate%"
    echo "  Average response time: ${avg_time}ms"
    echo "  Results saved to: $results_file"
    echo ""

    # Check if we meet the <100ms requirement
    if (( $(echo "$avg_time < 100" | bc -l) )); then
        print_status "✅ PASSED: Average response time ($avg_time ms) < 100ms requirement"
    else
        print_warning "⚠️  ATTENTION: Average response time ($avg_time ms) exceeds 100ms requirement"
    fi
    echo ""
}

# Function to test concurrent requests
test_concurrent_requests() {
    local test_name="$1"
    local url="$2"
    local concurrent_users="$3"
    local requests_per_user="$4"

    print_info "Concurrent Testing: $test_name ($concurrent_users users, $requests_per_user requests each)"

    local pids=()
    local start_time=$(date +%s%N)

    # Start concurrent processes
    for i in $(seq 1 $concurrent_users); do
        {
            for j in $(seq 1 $requests_per_user); do
                curl -s -o /dev/null "$url" > /dev/null 2>&1
            done
        } &
        pids+=($!)
    done

    # Wait for all processes to complete
    for pid in "${pids[@]}"; do
        wait $pid
    done

    local end_time=$(date +%s%N)
    local total_time_ms=$(echo "scale=2; ($end_time - $start_time) / 1000000" | bc)
    local total_requests=$((concurrent_users * requests_per_user))
    local avg_time_per_request=$(echo "scale=2; $total_time_ms / $total_requests" | bc)

    print_status "Concurrent test completed!"
    echo "  Concurrent users: $concurrent_users"
    echo "  Requests per user: $requests_per_user"
    echo "  Total requests: $total_requests"
    echo "  Total time: ${total_time_ms}ms"
    echo "  Average time per request: ${avg_time_per_request}ms"
    echo ""
}

# Main testing function
run_tests() {
    print_info "Starting performance tests..."

    # Create curl format file
    create_curl_format

    # Test scenarios based on your sample data
    echo "=========================================="
    echo "🧪 SINGLE REQUEST TESTS"
    echo "=========================================="

    # Test 1: All events (wide range)
    test_single_request "all_events" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2026-12-31"

    # Test 2: 2021 events only
    test_single_request "2021_events" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31"

    # Test 3: Single event
    test_single_request "single_event" \
        "$BASE_URL$ENDPOINT?starts_at=2021-06-30&ends_at=2021-06-30"

    # Test 4: No results
    test_single_request "no_results" \
        "$BASE_URL$ENDPOINT?starts_at=2020-01-01&ends_at=2020-12-31"

    # Test 5: Future events
    test_single_request "future_events" \
        "$BASE_URL$ENDPOINT?starts_at=2025-01-01&ends_at=2025-12-31"

    echo "=========================================="
    echo "⚡ LOAD TESTS (Sequential)"
    echo "=========================================="

    # Load test 1: Light load
    run_load_test "light_load" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" \
        50

    # Load test 2: Medium load
    run_load_test "medium_load" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" \
        100

    # Load test 3: Heavy load
    run_load_test "heavy_load" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" \
        200

    echo "=========================================="
    echo "🔥 CONCURRENT TESTS"
    echo "=========================================="

    # Concurrent test 1: 10 users, 10 requests each
    test_concurrent_requests "concurrent_light" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" \
        10 10

    # Concurrent test 2: 20 users, 5 requests each
    test_concurrent_requests "concurrent_medium" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" \
        20 5

    echo "=========================================="
    echo "📊 SUMMARY"
    echo "=========================================="

    print_status "All performance tests completed!"
    print_info "Results saved to: $OUTPUT_DIR"
    print_info "Timestamp: $TIMESTAMP"

    # List generated files
    echo ""
    echo "Generated files:"
    ls -la "$OUTPUT_DIR"/*"$TIMESTAMP"*
}

# Error test function
test_error_scenarios() {
    echo "=========================================="
    echo "❌ ERROR SCENARIO TESTS"
    echo "=========================================="

    # Test missing parameters
    test_single_request "missing_starts_at" \
        "$BASE_URL$ENDPOINT?ends_at=2021-12-31"

    test_single_request "missing_ends_at" \
        "$BASE_URL$ENDPOINT?starts_at=2021-01-01"

    test_single_request "missing_both" \
        "$BASE_URL$ENDPOINT"

    # Test invalid date formats
    test_single_request "invalid_date_format" \
        "$BASE_URL$ENDPOINT?starts_at=invalid-date&ends_at=2021-12-31"

    # Test invalid date range
    test_single_request "invalid_date_range" \
        "$BASE_URL$ENDPOINT?starts_at=2021-12-31&ends_at=2021-01-01"
}

# Help function
show_help() {
    echo "Events API Performance Testing Script"
    echo ""
    echo "Usage: $0 [OPTION]"
    echo ""
    echo "Options:"
    echo "  --all, -a       Run all tests (default)"
    echo "  --quick, -q     Run quick tests only"
    echo "  --load, -l      Run load tests only"
    echo "  --errors, -e    Run error scenario tests"
    echo "  --help, -h      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                 # Run all tests"
    echo "  $0 --quick        # Run quick tests"
    echo "  $0 --load         # Run load tests only"
    echo "  $0 --errors       # Test error scenarios"
}

# Main script logic
main() {
    case "${1:-}" in
        --help|-h)
            show_help
            exit 0
            ;;
        --quick|-q)
            check_server
            print_info "Running quick tests only..."
            test_single_request "quick_test" "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31"
            ;;
        --load|-l)
            check_server
            print_info "Running load tests only..."
            run_load_test "load_only" "$BASE_URL$ENDPOINT?starts_at=2021-01-01&ends_at=2021-12-31" 100
            ;;
        --errors|-e)
            check_server
            test_error_scenarios
            ;;
        --all|-a|"")
            check_server
            run_tests
            test_error_scenarios
            ;;
        *)
            print_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
}

# Check for required tools
check_dependencies() {
    local missing_tools=()

    if ! command -v curl &> /dev/null; then
        missing_tools+=("curl")
    fi

    if ! command -v jq &> /dev/null; then
        print_warning "jq not found - JSON formatting will be disabled"
    fi

    if ! command -v bc &> /dev/null; then
        missing_tools+=("bc")
    fi

    if [ ${#missing_tools[@]} -ne 0 ]; then
        print_error "Missing required tools: ${missing_tools[*]}"
        print_info "Please install missing tools and try again"
        exit 1
    fi
}

# Script entry point
check_dependencies
main "$@"