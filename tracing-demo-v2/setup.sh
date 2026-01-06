#!/bin/bash

# Script to install Spring Boot 4.0.1 parent POMs to local Maven repository
# This is needed because Maven validates POMs before resolving parent dependencies

echo "Installing Spring Boot 4.0.1 parent POMs to local repository..."

cd /tmp

# Download and install spring-boot-dependencies
echo "Downloading spring-boot-dependencies..."
curl -s "https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.0.1/spring-boot-dependencies-4.0.1.pom" -o /tmp/deps.pom
mvn install:install-file -Dfile=/tmp/deps.pom -DgroupId=org.springframework.boot -DartifactId=spring-boot-dependencies -Dversion=4.0.1 -Dpackaging=pom -q

# Download and install spring-boot-starter-parent
echo "Downloading spring-boot-starter-parent..."
curl -s "https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-starter-parent/4.0.1/spring-boot-starter-parent-4.0.1.pom" -o /tmp/parent.pom
mvn install:install-file -Dfile=/tmp/parent.pom -DgroupId=org.springframework.boot -DartifactId=spring-boot-starter-parent -Dversion=4.0.1 -Dpackaging=pom -q

echo "✅ Parent POMs installed successfully!"
echo "You can now run: bash run_all.sh"
