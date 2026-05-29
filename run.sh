#!/usr/bin/env bash
# Mugetsu Client — launcher with automatic Wayland/HiDPI scaling

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR="$SCRIPT_DIR/dist/injector.jar"

# Detect scale factor from GNOME / Wayland env
SCALE=""
if [ -n "$GDK_SCALE" ]; then
    SCALE="$GDK_SCALE"
elif [ -n "$GDK_DPI_SCALE" ]; then
    SCALE="$GDK_DPI_SCALE"
elif command -v gsettings &>/dev/null; then
    SF=$(gsettings get org.gnome.desktop.interface scaling-factor 2>/dev/null | tr -d "'" | xargs)
    [ "$SF" != "0" ] && [ -n "$SF" ] && SCALE="$SF"
fi

JVM_ARGS="-Dawt.useSystemAAFontSettings=on -Dswing.aatext=true"
[ -n "$SCALE" ] && JVM_ARGS="$JVM_ARGS -Dsun.java2d.uiScale=$SCALE"

exec java $JVM_ARGS -jar "$JAR" "$@"
