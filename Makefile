# Defaults
HOST ?= 127.0.0.1
PORT ?= 8080


# Compiles the project and generates the .jar
build:
	mvn clean package

# Cleans target/
clean:
	mvn clean

# Runs the jar as specified in maven.apache.org guides
# example: make run_srv PORT=8008
run_srv:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App srv --port $(PORT)

# example: make run_cli HOST=192.168.0.10 PORT=8008
run_cli:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App cli --connect $(HOST):$(PORT)
