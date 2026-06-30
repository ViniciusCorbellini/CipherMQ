# Defaults
#DEFAULT BROKER 
HOST ?= 127.0.0.1
PORT ?= 8080

# BROKER ON KMS' VIEW
BHOST ?= 127.0.0.1
BPORT ?= 8080

# KMS ON USER'S VIEW
KHOST ?= 127.0.0.1
KPORT ?= 9090

USERNAME ?= user
BROKER_DIR = $(HOME)/.ciphermq/broker
BACKUP_NAME = ciphermq-broker-keys.tar.gz

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

# exemple: make run_cli HOST=172.26.222.123 PORT=8080 CA_CERT=~/Downloads/ca.crt
# 		   make run_cli HOST=172.26.222.123 PORT=8080 KHOST=172.26.222.123 KPORT=9090 CA_CERT=C:\Users\Cliente\Downloads\ca.crt
# --kms <kms-host>:<kms-port>
run_cli:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App \
	  cli --connect $(HOST):$(PORT) --kms $(KHOST):$(KPORT) --ca-cert $(CA_CERT)

# exemple: make run_kms PORT=9090 BHOST=172.26.222.123 BPORT=8080 CA_CERT=C:\Users\Cliente\Downloads\ca.crt
run_kms:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App \
	  kms --port $(PORT) --broker $(BHOST):$(BPORT) --ca-cert $(CA_CERT)

# example: make sign USERNAME=alice
sign:
	java -cp target/CipherMQ-1.0-SNAPSHOT.jar com.manocorbas.ciphermq.App sign --username $(USERNAME)

# Compacta as chaves do broker para transferir
export_broker:
	tar -czf $(BACKUP_NAME) -C $(HOME)/.ciphermq broker/
	@echo "Salvo em $(BACKUP_NAME) - copie para o Drive"
# para rodar no pc da faculdade:
# No PowerShell do Windows, assumindo que as chaves estão em:
# C:\Users\<usuario>\.ciphermq\broker\
# ps:
# Compress-Archive -Path "$env:USERPROFILE\.ciphermq\broker" -DestinationPath "$env:USERPROFILE\ciphermq-broker-keys.zip"


# Restaura as chaves do broker de um backup
import_broker:
	mkdir -p $(HOME)/.ciphermq
	tar -xzf $(BACKUP_NAME) -C $(HOME)/.ciphermq
	@echo "Chaves restauradas em $(BROKER_DIR)"
# para rodar no pc da faculdade:
# # Cria o diretório se não existir
# New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ciphermq"

# # Extrai o zip
# Expand-Archive -Path "$env:USERPROFILE\Downloads\ciphermq-broker-keys.tar.gz" -DestinationPath "$env:USERPROFILE\.ciphermq" -Force
# OU: tar -xzf "$env:USERPROFILE\Downloads\ciphermq-broker-keys.tar.gz" -C "$env:USERPROFILE\.ciphermq"

# Testar o TCP em powershell:
# Test-NetConnection -ComputerName 10.151.32.96 -Port 8080