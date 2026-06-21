# Defaults
HOST ?= 127.0.0.1
PORT ?= 8080
USERNAME ?= user

# Compiles the project and generates the .jar
build:
	mvn clean package -DskipTest

# Cleans target/
clean:
	mvn clean

# Runs the jar as specified in maven.apache.org guides
# example: make run_srv PORT=8080 BROKER_CERT=C:\Users\Cliente\Downloads\broker.crt
# testing: make run_srv PORT=8080 BROKER_CERT=
run_srv:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App srv --port $(PORT) --broker-cert $(BROKER_CERT)

# exemplo: make run_cli HOST=172.26.222.123 PORT=8080 CA_CERT=~/Downloads/ca.crt
# 		   make run_cli HOST=172.26.222.123 PORT=8080 CA_CERT=C:\Users\Cliente\Downloads\ca.crt
run_cli:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App \
	  cli --connect $(HOST):$(PORT) --ca-cert $(CA_CERT)

# example: make sign USERNAME=alice
sign:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App sign --username $(USERNAME)
