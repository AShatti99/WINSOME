client_api := $(wildcard WinsomeClient/api/*java)
client_impl := $(wildcard WinsomeClient/impl/*java)
client_main := $(wildcard WinsomeClient/*java) 

server_api := $(wildcard WinsomeServer/api/*java)
server_impl := $(wildcard WinsomeServer/impl/*java)
server_model := $(wildcard WinsomeServer/model/*java)
server_utils := $(wildcard WinsomeServer/utils/*java)
server_main := $(wildcard WinsomeServer/*java)

.PHONY: clean test all

all: Client Server

Client: 
	@echo "compiling client..."
	@javac $(client_api) $(client_impl) $(client_main)

Server:
	@echo "compiling server..."
	@javac -cp .:./WinsomeServer/lib/gson-2.8.9.jar $(server_api) $(server_impl) $(server_model) $(server_utils) $(server_main)
	@echo "Done, type make clean for remove .class files"
	

	
clean:
	@echo "Deleting .class files..."
	@find . -type f -name "*.class" -delete
