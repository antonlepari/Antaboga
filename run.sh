#!/bin/bash
# ===========================================
# Antaboga ERP — Setup & Run Script
# ===========================================

set -e

echo ""
echo "  🐉 Antaboga ERP — Setup Script"
echo "  ================================"
echo ""

# Step 1: Fix Homebrew permissions (if needed)
echo "[1/5] Checking Homebrew permissions..."
if ! brew list &>/dev/null 2>&1; then
    echo "  → Fixing Homebrew permissions (requires sudo)..."
    sudo chown -R $(whoami) /usr/local/Cellar /usr/local/Homebrew /usr/local/bin /usr/local/etc /usr/local/lib /usr/local/opt /usr/local/sbin /usr/local/share /usr/local/var 2>/dev/null || true
fi
echo "  ✅ Homebrew OK"

# Step 2: Install Java 8
echo ""
echo "[2/5] Installing OpenJDK 8..."
if ! brew list openjdk@8 &>/dev/null 2>&1; then
    brew install openjdk@8
    # Create symlink
    sudo ln -sfn $(brew --prefix openjdk@8)/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-8.jdk 2>/dev/null || true
else
    echo "  ✅ OpenJDK 8 already installed"
fi

# Set JAVA_HOME
export JAVA_HOME=$(brew --prefix openjdk@8)/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
echo "  JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -1

# Step 3: Install Maven
echo ""
echo "[3/5] Installing Maven..."
if ! brew list maven &>/dev/null 2>&1; then
    brew install maven
else
    echo "  ✅ Maven already installed"
fi
mvn --version 2>&1 | head -1

# Step 4: Build
echo ""
echo "[4/5] Building Antaboga ERP..."
cd "$(dirname "$0")"
mvn clean package -DskipTests -q

# Step 5: Sign JAR for Java Web Start (JNLP)
echo ""
echo "[5/5] Signing JAR for Java Web Start..."
KEYSTORE="config/antaboga.jks"
JAR_FILE="target/antaboga-1.2.0.jar"
ALIAS="antaboga"
STOREPASS="${ANTABOGA_KEYSTORE_PASS:-antaboga-kamsib-erp}"

if [ ! -f "$KEYSTORE" ]; then
    echo "  → Generating self-signed certificate..."
    keytool -genkeypair \
        -alias "$ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity 3650 \
        -keystore "$KEYSTORE" \
        -storepass "$STOREPASS" \
        -keypass "$STOREPASS" \
        -dname "CN=Antaboga ERP, OU=Kamsib, O=Kamsib, L=Jakarta, ST=DKI Jakarta, C=ID" \
        -noprompt 2>/dev/null
    echo "  ✅ Certificate created"
else
    echo "  ✅ Certificate already exists"
fi

echo "  → Signing $JAR_FILE..."
jarsigner -keystore "$KEYSTORE" \
    -storepass "$STOREPASS" \
    -keypass "$STOREPASS" \
    "$JAR_FILE" "$ALIAS" 2>/dev/null
echo "  ✅ JAR signed"

echo ""
echo "  ╔══════════════════════════════════════╗"
echo "  ║  🐉 Build successful!               ║"
echo "  ║  Starting Antaboga ERP...            ║"
echo "  ╚══════════════════════════════════════╝"
echo ""
echo "  Web Launcher:  http://localhost:1337"
echo "  JSP Login:     http://localhost:1337/OA_HTML/AppsLocalLogin.jsp"
echo "  Desktop:       http://localhost:1337/OA_HTML/RF.jsp?function_id=report"
echo ""
echo "  🔑 Jika pertama kali, password admin akan ditampilkan di bawah."
echo ""

java -jar target/antaboga-1.2.0.jar
