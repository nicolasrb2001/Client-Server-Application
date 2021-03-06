# Client Server Application
This is my implementation of a multi-threaded client server application that supports having multiple clients connected to a server and communicating with each other.

### Motivation :rocket:
------------------
One of my coursework consisted in creating a client server application. My main motivation was implementing one of the additional **challenges** such as having a chat bot.

### Features

- By default, the clients connect to a server port specified in the code. Although, if run on a terminal, the user can pass in as arguments the server port by typing -ccp port where "port" is the number and -cca host where host is the ip address that the clients connect to.
- Clients can communicate with each other. If a client sends a message to the server, this message is **broadcast** to all clients connected (except those connected to a chat bot see below).
- **Chat bot**, this is a client that connects to the server and is able to communicate with clients that have requested connection with the bot. It has some predetermined responses depending on the commands that the clients send. ie if input = "hello" it responds "hi there" and many more. 
- It supports maintaining connections between clients and the chat  bot (if they are connnected) and between clients that are not connected to the bot at the same time.
- Makes use of **synchronised** blocks/statements to handle clients connecting/disconnecting at the exact same time.
### Usage and instructions

1. ChatServer.java must be the **first** file executed.
2. If the user wishes to have any of the clients connect to the bot then he must **type yes** in the chatserver terminal and run ChatBot.java **before** running any of the ChatClient.java.
3. If the Chat bot is connected, clients that wish to be connected to the chat bot must send the word **"bot"** to the server. 
4. If the clients do not connect to the bot they can keep communicating with each other. If they wish to connect to the chat bot later on they can still do so unless the bot/server has disconnected.
5. If the user wished to have the bot disconnect, then stop running **ChatBot.java**, the ChatServer.java terminal will output " bot has disconnected" and all clients that were connected to the bot will disconnect from the server and stop running. 
6. The other clients that were not connected to the bot can still communicate with each other. If one of them tries to connect to the bot they will be denied. 
7. The user can close the server by entering the word "exit" in the ChatServer terminal or simply close ChatServer and all ChatClients will stop running.

### Technical Information 

This was developed using [IntelliJ IDEA](https://www.jetbrains.com/idea/). The code implements the **Runnable interface** to support multithreading and it includes **Java APIs** such as Socket or IO amongst others.

The folder in the master branch [src](/src) cointains replicates of ChatClient.java, namely ChatClient2.java and ChatClient3.java. This is because the IDE did not support running multiple instances of the same class and as a consequence I created 2 new classes of the same code for testing purposes.
### Known Issues/Limitations 

My implementation of the Chat bot is quite similar to the regular ChatClient.java. They connect to the server in the same way, this is why:
  1. ChatBot.java must be run before any of the clients if the user wishes to have the bot connected to the server. This way the server can **properly initialise** the stream readers and writers from/to the chat bot.
  2. This implies that when the chat bot disconnects from the server and connects once more, the server **cannot distinguish** the chat bot from a regular client; so interactions between the chat bot and clients would not be possible hence why clients' requests to connect to the chat bot are denied.

### Screenshots

Here I have connected the chat bot to the server but none of the other 3 clients are connected to it. ChatClient has sent a message to the server which broadcasts it to all connected clients (except bot).

![](/screenshots/screenshot1.png)

In this one,  ChatClient2 has connected to the bot and is communicating with it whilst ChatClient and ChatClient3 are communicating with each other.

![](/screenshots/screenshot2.png)

Lastly, ChatBot is stopped so it disconnects and ChatClient2 is disconnected from the server. ChatClient3 requests connection to the ChatBot but is denied and ChatClient and ChatClient3 can still communicate.

![](/screenshots/screenshot3.png)
