# Make it executable
chmod +x performance-test.sh

# Run all tests
./performance-test.sh

# Or run specific test types:
# Quick single test
./performance-test.sh --quick
# Load tests only
./performance-test.sh --load
# Error scenarios
./performance-test.sh --errors
# Show help
./performance-test.sh --help