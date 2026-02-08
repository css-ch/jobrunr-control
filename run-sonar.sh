#!/bin/bash

# SonarQube Analysis Script
# This script runs SonarQube analysis on the JobRunr Control project
# Prerequisites: SonarQube server running on http://localhost:9000

echo "Starting SonarQube analysis..."
echo "SonarQube server: http://localhost:9000"

# Run Maven build with SonarQube analysis
./mvnw clean verify sonar:sonar \
  -Dsonar.host.url=http://localhost:9000

echo ""
echo "SonarQube analysis completed!"
echo "View results at: http://localhost:9000"
