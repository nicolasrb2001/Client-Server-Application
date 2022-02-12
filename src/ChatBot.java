import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class ChatBot implements Runnable {

    private static ChatBot client_bot;/*global field so that both threads can access the same instance*/
    private int action;/*determines which block of code the next thread created will run*/
    private PrintWriter send_to_server;
    private BufferedReader read_from_server;
    private static LinkedList<Thread> threads = new LinkedList<>();/*linked list that contains the currently running threads*/
    private String[] responses;/*pre-scripted responses to the commands send by the clients*/
    private String[] input;/*commands sent from the clients to the bot*/
    private static String command;/*this field is global so that its value is the same for all threads*/
    private static Socket s;/*socket with the server*/
    private static boolean processed = false;/*determines whether the command input from a client has been dealt with or not*/



    /*this method parses the arguments from the terminal, it has the same code as the one for the client*/
    private static String[] process_arguments(String[] args) {
        String port = "14001";
        String host = "localhost";
        String[] x = new String[2];
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-cbp")) {
                port = args[i + 1];
            } else if (args[i].equals("-cca")) {
                host = args[i + 1];
            }
        }
        x[0] = port;
        x[1] = host;
        return x;
    }
    /*this method passes the arguments from the terminal to process_arguments and with the resulting port and IP it sets up the connection*/
    private static Socket setup_client(String[] args) throws IOException {
        args = process_arguments(args);

        int port = Integer.parseInt(args[0]);
        String host = args[1];
        Socket s;
        s = new Socket(host, port);
        return s;
    }
    public ChatBot(Socket s) throws IOException {
        this.action = 0;
        this.send_to_server = new PrintWriter(s.getOutputStream(), true);
        this.read_from_server = new BufferedReader(new InputStreamReader(s.getInputStream()));
        this.input = new String[]{"hello", "how are you", "the weather today is nice", "what do you think about this coursework?", "Bye"};
        this.responses = new String[]{"hi there", "I'm fine thank you ;)", "I disagree...:(", "I'm really enjoying it but its very hard", "Bye hope to see you again ^_^"};

        }
    private static void setup_client_threads(ChatBot client_bot) throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            threads.add(new Thread(client_bot));
            threads.getLast().start();
            synchronized (client_bot) {/*this block is synchronised so that it starts the last thread added to threads*/
                client_bot.wait();     /*and waits until it gets notified to increment action, otherwise it could happen*/
            }                          /*that the first thread is starts and before the main thread increments action, the second thread starts*/
            client_bot.action = client_bot.action+ 1;/*and it executes the same block of code in run() as the first thread*/
                                                     /*so this is prevented by telling the main thread to wait until it gets notified by the thread that has just started*/
        }
    }
    /*this method simply notifies the main method that it can continue creating the next thread*/
    /*since client_bot is global, the treads block on the same instance of the field*/
    private void notify_main() {
        synchronized (client_bot) {
            notify();
        }
    }
    public static void main(String args[]) throws IOException, InterruptedException {
        s = setup_client(args);
        client_bot = new ChatBot(s);/*sets up connection with server*/
        setup_client_threads(client_bot);
        System.out.println("success");
        while(true){
            Thread.sleep(100000);}/*this keeps the bot connected until the server disconnects*/
    }

    /*this method handles reading from the server*/
    private void server_input() throws IOException, InterruptedException {
        command = null;
        notify_main();/*notifies the main thread so that it can continue creating the next thread*/
        while (true) {
            while(command== null){/* command is assigned null when the thread that writes to the server sends a response to the server*/
                try{
                    command =  client_bot.read_from_server.readLine();/*blocks until it reads a command from the server*/
                    processed = false;/*sets the value of the field processed to false*/
                    while(!processed){/*until the input hasn't been dealt with this thread will sleep*/
                        Thread.sleep(10);

                    }

                }catch(SocketException v){/*if the server disconnects the chat bot shuts down */
                    System.out.println("server disconnected");
                    s.close();
                    System.exit(1);
                    return;
                }
            }
            Thread.sleep(30);/*thread put to sleep before it tries to read another command*/
        }
    }
    private void send_server() throws InterruptedException {
        notify_main();
        while(true){
            boolean sent = false;
            while(command == null){/*the thread sleeps until the global field e is updated*/
                Thread.sleep(300);
            }
            /*once the command is updated, it starts processing it*/
            /*each command comes with a number at the end, which represents the client to which it has to sebd a response back*/
            /*index stores the last occurrence of a character */
            int index = 0;
            for(int i = 0; i< command.length(); i++){
                if(!Character.isDigit(command.charAt(i))){
                    index = i;
                }
            }
            /*compares the substring from the character at the 0 position to the character at the index+1 position, ie the last character input that isn't a number*/
            for (int i = 0; i < input.length; i++){
                if(command.substring(0, index+1).equalsIgnoreCase(input[i])){
                    send_to_server.println(responses[i]+command.substring(index+1));
                    sent = true;
                    break;
                }
            }
            /*if the command doesn't match any of the input[] then it sends to the client "sorry I dont understand that command"*/
            if (!sent){
                send_to_server.println("Sorry I don't understand that command"+ command.substring(index+1));
            }

            command = null;
            processed = true;/*this tells the other thread that the command has been dealt with and that it can carry on reading from  the server*/
            Thread.sleep(50);
        }

    }

    @Override
    public void run() {
        try {
            if (client_bot.action == 0) {
                /*this thread handles reading from server*/
                server_input();
            } else {
                if (client_bot.action == 1) {
                    /*this thread handles writing back to server*/
                    send_server();
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("server has disconnected");
            return;
        }
    }
}
