import java.io.*;
import java.net.*;

public class Server implements Constants{

    private int player = 0;
    private ServerSocket serverSocket;

    /**
     * main method new server
     */
    public static void main(String[] args){
        new Server();
    }

    /**
     * Server's no arg constructor.
     * 	Creates a server socket, waits for 2 players to connect and starts a new session when
     * 	2 players have connected.
     * 	Server became both object class and method with this
     */

    private Server() {
        try{
            serverSocket = new ServerSocket(PORT); //main socket
            System.out.println("Waiting for connection...");
            while (true){
                Socket socket1 = serverSocket.accept();
                System.out.println("Client "+ player+ " connected." );
                System.out.println("Player1: "  + socket1.getInetAddress().getHostAddress() + '\n'); //When client connected to the server!!!!!!!!!!
                new DataOutputStream(socket1.getOutputStream()).writeInt(player);

                Socket socket2 = serverSocket.accept();
                System.out.println("Client " + player + " connected.");
                System.out.println("Player2: "  + socket2.getInetAddress().getHostAddress() + '\n');
                new DataOutputStream(socket2.getOutputStream()).writeInt(player);
                player++;

                System.out.println("Server starting the game...");

                SessionThread gameServer = new SessionThread(socket1, socket2);
                new Thread(gameServer).start();



            }


        } catch (IOException e) {
            System.err.println(e);
            e.printStackTrace();
        }

    }


}
