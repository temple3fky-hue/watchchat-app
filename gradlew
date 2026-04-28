#!/usr/bin/env sh

set -e

APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
WRAPPER_PROPERTIES="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
GRADLE_USER_HOME_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}"
JAVA_HOME_21="$HOME/.local/share/mise/installs/java/21.0.2"

if [ -d "$JAVA_HOME_21" ]; then
  export JAVA_HOME="$JAVA_HOME_21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ ! -f "$WRAPPER_PROPERTIES" ]; then
  echo "Missing $WRAPPER_PROPERTIES"
  exit 1
fi

DISTRIBUTION_URL=$(grep '^distributionUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2- | sed 's#\\:#:#g')
GRADLE_VERSION=$(echo "$DISTRIBUTION_URL" | sed -n 's#.*gradle-\([0-9][^/-]*\)-bin.zip#\1#p')

if [ -z "$GRADLE_VERSION" ]; then
  echo "Unable to parse Gradle version from distributionUrl: $DISTRIBUTION_URL"
  exit 1
fi

GRADLE_DIR="$GRADLE_USER_HOME_DIR/wrapper/dists/gradle-$GRADLE_VERSION-bin"
GRADLE_HOME="$GRADLE_DIR/gradle-$GRADLE_VERSION"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"
ZIP_PATH="$GRADLE_DIR/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_DIR"

  if [ ! -f "$ZIP_PATH" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -L --fail -o "$ZIP_PATH" "$DISTRIBUTION_URL" || true
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_PATH" "$DISTRIBUTION_URL" || true
    else
      echo "curl or wget is required to download Gradle."
      exit 1
    fi
  fi

  if [ -f "$ZIP_PATH" ]; then
    echo "Unpacking Gradle $GRADLE_VERSION..."
    unzip -q -o "$ZIP_PATH" -d "$GRADLE_DIR"
  fi
fi

if [ ! -x "$GRADLE_BIN" ] && command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

exec "$GRADLE_BIN" "$@"
