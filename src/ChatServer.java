import java.net.*;
import java.io.*;
import java.util.LinkedList;
public class ChatServer implements Runnable {
    private static boolean isbot_coneccted = false;
    private static boolean shutdown = false;/*tells the main thread whether the user has typed exit or not*/
    private static LinkedList<Thread> threads = new LinkedList<>();/*stores the threads for the clients */
    private static Socket new_client = null;/*global field so that all threads can have the same reference for the field*/
    private static final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));/*read from the chat server terminal*/
    private static BufferedReader bot_read;/*read from the bot*/
    private static PrintWriter bot_send;/*send to the bot*/
    private static ChatServer cs;/*passed in as an argument when a thread is created, also global so that all threads have the same reference to the field*/
    private static String message;/*stores the current message to be broadcast to all clients(that are not connected to the bot)*/
    private static int action;/*determines which block of code the next thread created will execute*/
    private static int total;/*counts how many clients have received the current message*/
    private static Thread bot_thread;/*thread that handles reading from and writing to the bot*/
    private static Object p = new Object();/*these 4 Objects are used in synchronised blocks so that only one thread can execute the block depending on the object assigned to the block*/
    private static Object xy = new Object();
    private static Object bot_write = new Object();
    private static Object bot_in = new Object();
    private static ServerSocket ss;/*server socket */
    private static boolean set = false;/*global field that determines whether the message has been reset to null or not*/
    private static LinkedList<String> list_temporary = new LinkedList<>();/**/
    private static LinkedList<Thread> ids = new LinkedList<>();/*stores the threads that are connected to the bot*/
    private static int counter;/*this is incremented everytime a new thread is created*/
    private static int num_bots;/*stores how many clients are currently connected to the bot*/
    private static String input_from_bot;/*global field so that all threads have the same reference to the field, stores the response from the bot*/
    private static LinkedList<Boolean> list_bots= new LinkedList<>();/*stores whether a client is connected to a bot or not*/

    /*initialises some fields and creates the thread thread_server_input*/
    private static void field_initialisation() throws InterruptedException, IOException {
        cs = new ChatServer();
        action = 0;
        message = null;
        Thread thread_server_input = new Thread(cs);
        bot_thread = new Thread(cs);
        thread_server_input.start();/*this thread checks whether the user has input exit in the terminal or not*/
        synchronized (cs) {
            cs.wait();
        }/*waits until notified since it could happen that the bot is started and therefore bot_thread.isalive would be true and thread_server_input may not execute its block of code in run()*/
        System.out.println("if you want to use the chatbot please input yes, otherwise input no");
        /*before any clients connect the user is prompted whether they will want to run the bot or not*/
        if(br.readLine().equals("yes")){
            action = 0;
        }else{
            action = 1;
        }

    }
    /*processes the arguments from the terminal, ie the IP address and port number*/
    private static int parse_arguments(String[] args){
        int port = 14001;

        for(int i = 0; i < args.length; i++) {
            if (args[i].equals("-csp")) {
                port = Integer.parseInt(args[i + 1]);
            }
        }
        return port;
    }
    /*this method deals with accepting clients nad the bot*/
    private static void client_acceptance(ServerSocket ss){
        counter = 0;
        num_bots = 0;

        while (!shutdown){
            try{

                new_client= ss.accept();/*new connection, it could be the bot or a new client*/
                if (action == 0){/*therefore the corresponding action is checked and if its 0 then the fields for the bot are initialised*/
                    bot_read = new BufferedReader(new InputStreamReader(new_client.getInputStream()));
                    bot_send = new PrintWriter(new_client.getOutputStream(), true);
                    bot_thread.start();
                    synchronized (cs) {
                        cs.wait();
                    }/*waits until notified by the bot*/
                    action = 1;
                }else{

                    list_temporary.add("");
                    list_bots.add(false);
                    update_clients_threads( true, null);/*client threads are started*/
                }
            } catch(IOException | InterruptedException e){
                System.out.println("client couldn't connect or server has been closed");
            }

        }
    }
    public static void main(String[] args) throws IOException, InterruptedException {
        field_initialisation();
        ss= new ServerSocket(parse_arguments(args));
        client_acceptance(ss);

    }
    /*this method is called when a new client connects*/
    private static void add_threads() throws InterruptedException {
        for(int i= 0; i<2; i++){
            threads.add(new Thread(cs));
            threads.getLast().start();
            synchronized (cs) {
                cs.wait();
            }/*waits until notified by the threads in run()*/
            action = action + 1;
        }
        action = 1;
    }
    /*this method removes threads and interrupts them if the clients have disconnected or they were connected to the bot and the bot disconnects*/
    private static void remove_threads(Thread t){
        int index = threads.indexOf(t);/*an exception occurrs and it is catched by the thread that raises it, so it calls this method to remove all threads associated to its client*/
        if (index != -1){
            for(int i = 0; i<2; i++){
                threads.get(index).interrupt();
                threads.remove(index);

            }
        }
    }

    /* method to add and remove threads from the lists, such that two threads cannot edit the lists at the same time*/
    public static void update_clients_threads(Boolean y, Thread t) throws InterruptedException {
        synchronized (p){
            if (y){
                add_threads();
            }else{
                remove_threads(t);
            }

        }
    }
    /*synchronised method so that when two threads read data from their respective clients, only one of them can call this method*/
    private synchronized static void update_message(String str) throws InterruptedException {

        set = false;
        message = str;
        while(!(total >= (threads.size()/2)-num_bots)){/*message has been updated so it waits until it has been broadcast to the clients, the total number can be greater since it can happen that a client receives the mesage, so total is ince¡remented and right after it disconnects so the current threads.size is smaller */
            Thread.sleep(50);
        }
        message = null;
        total = 0;
        set = true;

    }
    /*notifies the main thread*/
    private void notify_main(){
        synchronized (cs){
            notify();

        }
    }
    /*if the client disconnects then -1 is returned by the bufferred reader so it catches an exception to remove the threads associated to the client*/
    private String read_client(int index, BufferedReader br) throws IOException, InterruptedException {
        String str = null;
        if (list_temporary.get(index).equals("")){
            try{
                list_temporary.set(index,String.valueOf((char)br.read()));

            }catch(SocketException e){
                Thread.currentThread().setPriority(9);
                if (list_bots.get(index)){/*if the client disconnects and it was connected to the bot then the number of clients connected to the bot decrements*/
                    num_bots = num_bots -1;
                }
                update_clients_threads(false, Thread.currentThread());
            }

            Thread.sleep(30);
        }else{
            str = br.readLine();

        }
        return str;
    }


    /*method checks whether there is an  input in the server terminal and whether it equals exit or not*/
    private static void check_server_input(){
        while(!shutdown){
            try {
                if(br.ready()){
                    Thread.currentThread().setPriority(10);
                    if(br.readLine().equalsIgnoreCase("exit")){
                        ss.close();
                        System.exit(10);
                    }
                }else{
                    if (threads.size()/2 - num_bots == 0){
                        message = null;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    /*the clients connected to the bot send their commands to the bot*/
    private static void data_from_bot(String e, int index, PrintWriter send_to_client) throws InterruptedException {
        Thread.sleep(10);
        if(Character.isDigit(e.charAt(e.length()-1))){/*if the string ends with a number then it is not a valid input as it wonn't be recognised by the server later on*/
            send_to_client.println("Sorry I cannot understand your command, please don't place a digit at the end of your sentence");
        }else{
            synchronized (bot_write){
                bot_send.println(e+ index);
                Thread.sleep(300);
            }
        }
    }
    /*this method checks the input from each client*/
    private static void send_data_to_client(String input, int index, PrintWriter send_to_client) throws InterruptedException {
        Thread.sleep(100);
        if (input.equalsIgnoreCase("bot")){
            if(isbot_coneccted){
                Thread.sleep(10);
                synchronized (xy) {/*synchronised so that if 2 or more clients request to connect to the bot they have to wait until the previous one has connected*/
                    Thread.sleep(10);
                    Thread.currentThread().setPriority(10);
                    list_bots.set(index, true);
                    num_bots = num_bots + 1;
                    ids.add(Thread.currentThread());
                    Thread.currentThread().setPriority(5);
                }
                send_to_client.println("connected to bot");
            }else{
                send_to_client.println("bot is not connected or has disconnected");
            }
        }else{
            /*if the data doesn't request to connect to the bot then it updates message so that it gets broadcast to clients*/
            update_message(input);
        }
    }
    /*this method reads data from the clients connected*/
    private void read_client_input(int index, BufferedReader read_from_client, PrintWriter send_to_client, Socket client) throws IOException, InterruptedException {
        notify_main();
        try{
            while(true){
                String input = null;
                while(input == null){
                    read_client(index, read_from_client);/*this updates the value of list_temporary corresponding to this thread*/
                    input = list_temporary.get(index) + read_client(index, read_from_client);
                    list_temporary.set(index, "");
                    if (list_bots.get(index)){
                        /*if the cclinet is connected to the bot it calls this method*/
                        data_from_bot(input, index, send_to_client);
                    }else{
                        /*otherwise it calls this method*/
                        send_data_to_client(input, index, send_to_client);
                    }
                }
            }
        }catch(InterruptedException r){

            read_from_client.close();
            send_to_client.close();
            client.close();
        }

    }
    /*this method checks whether there is a response from the bot and if there is it checks if the string ends with the same number as the clients'index*/
    private void send_to_client_connected_to_bot(int index, PrintWriter send_to_client) throws InterruptedException {
        Thread.sleep(50);
        if (input_from_bot != null){
            Thread.sleep(30);
            if(input_from_bot.endsWith(String.valueOf(index))){
                Thread.sleep(10);
                send_to_client.println(input_from_bot.substring(0, input_from_bot.length() - String.valueOf(index).length()));
                input_from_bot = null;

            }

        }

    }
    /*this method checks if there is a message to be sent to the client*/
    private void send_to_client_not_connected_tobot(PrintWriter send_to_client) throws InterruptedException {
        if(message != null){
            send_to_client.println(message);
            total = total + 1;
            while(!(total >= (threads.size()/2)-num_bots) && ((threads.size()/2)-num_bots > 0)){/*the thread waits until the message is sent  ot every client that isnt connected to a bot*/
                Thread.sleep(5);
            }
            while(!set){/*waits until the message is set to null and set to true*/
                Thread.sleep(5);
            }
        }else{
            Thread.sleep(50);

        }
    }

    private void send_message_to_clients(int index, PrintWriter send_to_client, BufferedReader read_from_client, Socket client) throws InterruptedException, IOException {
        notify_main();
        try{
            while(true){
                Thread.sleep(10);
                if (!list_bots.get(index)){
                    /*if the thread's respective client is not connected to a bot then it calls the following method*/
                    send_to_client_not_connected_tobot(send_to_client);
                }else{
                    /*otherwise, it calls this method*/
                    send_to_client_connected_to_bot(index, send_to_client);
                }
            }
        }catch(InterruptedException u){

            read_from_client.close();
            send_to_client.close();
            client.close();
        }

    }
    /*method called by the threads that are not either the thread_server_input or the bot thread*/
    private void client_threads(int index, Socket client){
        /*notice the index passed in to read_client_input and send_message_to_clients*/
        /*they are different since everytime a thread is started, the index is incremented*/
        /*list_temporary's size only increments it doesn't decrement so it represent all the clients that have connected to the server*/
        /*so the index depends on the size of list_temporary*/
        try {
            PrintWriter send_to_client = new PrintWriter(client.getOutputStream(), true);
            BufferedReader read_from_client = new BufferedReader(new InputStreamReader(client.getInputStream()));
            if(action == 1){
                /*thread will handle reading from the client*/

                read_client_input(index-(list_temporary.size()-1), read_from_client, send_to_client,  client);
            }else{
                /*this thread checks if there are any messages to be sent to clients and if there are it broadcasts them*/
                if (action == 2){
                    send_message_to_clients(index-list_temporary.size(), send_to_client, read_from_client,client);
                }
            }
        } catch (InterruptedException | IOException e) {
            System.out.println("caught");
            return;
        }
    }
    /*method called when the bot has disconnected, it calls update threads to remove the clients that were connected to the bot*/
    private static void remove_client_bot() throws InterruptedException {
        System.out.println("bot has disconnected");
        isbot_coneccted = false;
        for (int i = 0; i< ids.size(); i++){
            update_clients_threads(false, ids.get(i));
        }
    }
    /*this method handles reading from the bot, it waits until the response from the bot is sent to the respective client before it continues reading from  the bot*/
    private static void bot_thread_reading() throws InterruptedException {
        while(true){
            while(input_from_bot == null){
                int p;
                try {
                    p = bot_read.read();
                    if(p == -1){
                        remove_client_bot();
                        return;
                    }
                    input_from_bot = (char)p + bot_read.readLine();

                } catch ( IOException e) {
                    remove_client_bot();
                    return;
                }
            }
            Thread.sleep(100);
        }
    }

    @Override/*the thread created will be handling the reading from clients and sending messages to clients, or the reading from the server terminal and the bot thread*/
    public void run() {
        if(action == 0){
            notify_main();
            if (!bot_thread.isAlive()){/*if the bot thread hasn't started*/
                check_server_input();
            }else{
                try {/*if the bot thread has started*/
                    isbot_coneccted = true;
                    bot_thread_reading();
                } catch (InterruptedException e) {
                    return;
                }
            }

        }else{
            Socket client = new_client;
            int index = counter;
            counter =counter + 1;
            /*the writing to client thread will have index> than the reading from client thread*/
            client_threads(index, client);
            return;
        }
    }
}

