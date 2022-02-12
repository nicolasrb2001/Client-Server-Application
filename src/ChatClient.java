import java.net.*;
import java.io.*;
import java.util.LinkedList;

public class ChatClient implements Runnable {

    private PrintWriter send_to_server;/*send data to server*/
    private BufferedReader read_from_server;/*read data from server*/
    private BufferedReader keyboard_input = new BufferedReader(new InputStreamReader(System.in));/*read data from keyboard*/
    private static Socket s;/*shared by all threads so that if one of them is interrupted then it closes the socket for all of them*/
    private int action;/*represents the block of code that the nest thread will be executing in run()*/
    private static LinkedList<Thread> threads = new LinkedList<>();/*linked list that contains the threads that will be started*/
    private static ChatClient client;


    /*when the connection to the server is started the constructor is called by creating the object client*/
    public ChatClient(Socket socket) throws IOException {
        this.action = 0;
        this.send_to_server = new PrintWriter(socket.getOutputStream(), true);
        this.read_from_server = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /*this method parses the arguments from the terminal and returns them to setup_clients*/
    private static String[] process_arguments(String[] args) {
        String port = "14001";
        String host = "localhost";
        String[] x = new String[2];
        /*this loop checks what type of parameter has been passed in, whether it is the IP address or the port number*/
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-ccp")) {
                port = args[i + 1];
            } else if (args[i].equals("-cca")) {
                host = args[i + 1];
            }
        }
        x[0] = port;
        x[1] = host;
        return x;
    }
    private static Socket setup_client(String[] args) throws IOException {
        args = process_arguments(args);/*the value of args is replaced with the new array returned from process_arguments*/
        int port = Integer.parseInt(args[0]);
        String host = args[1];
        Socket s = new Socket(host, port);
        return s;
    }
    /*this method starts the threads that handle reading from and writing to the server and reading from the keyboard*/
    private static void setup_client_threads(ChatClient client) throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            threads.add(new Thread(client));
            threads.getLast().start();
            synchronized (client) {/*this block of ode is synchronised with client as it is a global field so the threads can notify this block since they have the same reference to the object as the main thread has*/
                client.wait();
            }
            client.action += 1;

        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        s = setup_client(args);
        client = new ChatClient(s);
        setup_client_threads(client);
        /*the connection has been set up and the threads hav started so the main thread is just put to sleep until the server or bot shutsdown*/
        while (true) {
            Thread.sleep(100000);
        }
    }

    /*notifies the main thread*/
    private void notify_main() {
        synchronized (client) {
            notify();
        }
    }

    /*this method reads data from the server*/
    private void server_input() throws IOException {
        notify_main();
        while (true) {
            String e;
            int z;
            try{
                z = client.read_from_server.read();/*the read() method blocks until data has been input, if the socket has closed in the server side*/
                if ( z == -1){                     /*it returns -1 which means the connection has ended and therefore it shuts down the client*/
                    disconnect();
                }
                e = (char)z  + client.read_from_server.readLine();/*since every character has an integer representation, the integer returned by read() is converted to its corresponding character */
            }catch(SocketException v){
                disconnect();
                return;
            }
            System.out.println(e);


        }
    }
    /*this method disconnects the client from the server if an exception has occurred*/
    private void disconnect() throws IOException {
        System.out.println("bot or server disconnected  bye bye");
        s.close();
        System.exit(1);
    }
    /*this method handles writing to the server*/
    private void client_output() throws IOException {
        notify_main();
        try{
            while (true) {
            String str = String.valueOf((char)client.keyboard_input.read());/*this blocks until data has been entered by the client*/
            if (str.equals(String.valueOf((char)(10)))){/*If the enter tab is pressed, ie the string entered by the client is "empty" then the string is changed to " "*/
                str = " ";
            }else{
                str = str +  client.keyboard_input.readLine();/*otherwise the rest of the data in the stream is read*/
            }
            client.send_to_server.println(str);/*data sent to server*/
            }
        }catch(IOException x){
            disconnect();

        }

    }

    @Override
    public void run() {
        try {
            if (client.action == 0) {
                /*this thread handles input from the server*/
                server_input();
            } else {
                if (client.action == 1) {
                    /*this thread handles input from the keyboard and sending data to the server*/
                    client_output();
                }
            }
        } catch (IOException e) {
            try {
                disconnect();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }
}