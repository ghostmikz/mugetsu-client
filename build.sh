#!/bin/bash
set -e

echo "Building Cactus Speed Injector..."

cd "$(dirname "$0")"

# Check for JDK (not JRE — need javac and attach API)
if ! command -v javac &>/dev/null; then
    echo "ERROR: JDK not found. Install JDK 11+ (not just JRE)."
    echo "  Ubuntu/Debian: sudo apt install openjdk-21-jdk"
    echo "  Arch:          sudo pacman -S jdk-openjdk"
    exit 1
fi

echo "JDK: $(java -version 2>&1 | head -1)"

./gradlew :agent:jar :injector:jar --no-daemon -q

echo ""
echo "Build complete. Output:"
ls -lh dist/
echo ""
echo "To run the injector:"
echo "  java -jar dist/injector.jar"
