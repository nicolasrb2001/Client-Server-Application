# Client Server application
This is a multi-threaded client server application that supports having multiple clients connected to a server and communicating with each other.

### Motivation :rocket:
------------------
By default, the clients connect to a server port specified in the code. Although, if run on a terminal, the user can specify the server port which the clients connect to. 
Features:
-Clients can communicate with each other. If a client sends a message to the server, this message is broadcast to all clients connected (expect those connected to a chat bot see below).
-Chat bot, this is a client that connects to the server and is able to communicate with clients that have requested connection with the bot. It has some predetermined responses determined by the commands that the clients send. ie if input = "hello" it responds "hi there" and many more. 
Usage and instructions:
ChatServer.java must be the first file executed.
If the user wishes to have any of the clients connect to the bot then he must type yes in the chatserver terminal and run ChatBot.java before running any of the ChatClient.java
Clients that wish to be connected to the chat bot must send the word "bot" to the server. Otherwise they can communicate with each other. If they wish to connect to the chat bot then they can do so later on.
If the user wished to have the bot disconnect, then stop running ChatBot.java, the ChatServer.java terminal will output " bot has disconnected" and all clients that were connected to the bot will disconnect from the server and stop running. The other clients that were not connected to the bot can still communicate with each other. If one of them tries to connect to the bot they will be denied. Runnig ChatBot.java again will not work unless ChatServer.java is closed and run again. 
