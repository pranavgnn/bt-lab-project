#!/bin/bash

echo "Starting BT Bank Microservices..."
echo

echo "Starting Customer Service on port 8081..."
gnome-terminal -- bash -c "cd customer && mvn spring-boot:run; exec bash" &

sleep 5

echo "Starting Product Service on port 8082..."
gnome-terminal -- bash -c "cd product-pricing && mvn spring-boot:run; exec bash" &

sleep 5

echo "Starting FD Calculator Service on port 8083..."
gnome-terminal -- bash -c "cd fd-calculator && mvn spring-boot:run; exec bash" &

sleep 5

echo "Starting Accounts Service on port 8084..."
gnome-terminal -- bash -c "cd accounts && mvn spring-boot:run; exec bash" &

sleep 10

echo "Starting Main Gateway with Frontend on port 8080..."
gnome-terminal -- bash -c "cd main && mvn spring-boot:run; exec bash" &

echo
echo "All services are starting..."
echo
echo "Service URLs:"
echo "- Frontend: http://localhost:8080"
echo "- Customer Service: http://localhost:8081"
echo "- Product Service: http://localhost:8082"
echo "- FD Calculator Service: http://localhost:8083"
echo "- Accounts Service: http://localhost:8084"
echo "- API Documentation: http://localhost:8080/swagger-ui.html"
echo
echo "Please wait for all services to start completely before testing."
echo "Check the individual terminal windows for startup status."
