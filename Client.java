import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;


public class Client implements Runnable, Constants {
    private int WIDTH = 625;
    private int HEIGHT = 800;
    private boolean playerTurn = false;
    private int playerNumber;
    private boolean done = false;
    private JFrame playerWindow;
    private JPanel buttonPanel;
    private JPanel lowerPanel;
    private JPanel overallPanel;
    private JLabel warning;
    private JLabel currentTurn;
    private JLabel result;
    private PlayerCard[] buttons = new PlayerCard[NUMBER_OF_CARDS];
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private static String imagesDirectory = "images/";
    private static String host = "localhost";
    private static boolean images = true;
    private String back = "PlayerBackOfCard";

    //-------------------------MAIN--------------------------//
    public static void main(String args[]){

        /**
         * Client became both object class and method function with this
         * Remember client was also a thread with calling run() method bc. of Runnable
         */
        if(true)
        {
            new Client();
        }
    }


    //-------------------------CLIENT--------------------------//
    /**
     Creates a new Client.
     Creates the frame with all necessary elements (labels, buttons, etc.).
     Client became both object class and method function with this
     */
    public Client(){
        initializeButtons();
        overallPanel = new JPanel(new BorderLayout());          //BORDER:KENAR, ÇERÇEVELEMEK kenarlık atanır:BorderLayout
        buttonPanel = new JPanel(new GridLayout(4, 4));     //butonlar için bir grid oluşturulur: Gridlayout:matrix olarak atar

        for(int i = 0; i < NUMBER_OF_CARDS; i++)
        {
            if(!images)
            {
                buttons[i].setText("BACK");
            }
            else
            {
                buttons[i].setIcon(new ImageIcon(imagesDirectory + back + ".jpg"));
            }
            buttonPanel.add(buttons[i]);
        }
        lowerPanel = new JPanel(new GridLayout(4, 1));          //güneydeki lowepanelin matrix boyutunu ayarlar: Gridlayout:matrix olarak atar

        warning = new JLabel();
        currentTurn = new JLabel();
        result = new JLabel();
        result.setText("Number of matched pairs: " + 0);
        lowerPanel.add(currentTurn);
        lowerPanel.add(warning);
        lowerPanel.add(result);
        overallPanel.add(buttonPanel, BorderLayout.CENTER);
        overallPanel.add(lowerPanel, BorderLayout.SOUTH);

        playerWindow = new JFrame();
        playerWindow.setSize(WIDTH, HEIGHT);
        playerWindow.setTitle("Player Window");
        playerWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        playerWindow.add(overallPanel);
        playerWindow.setVisible(true);

        connectToServer();
    }

    /**
     Initializes the array of PlayerCards and attaches listeners to each card.
     */
    public void initializeButtons()
    {

        for(int i = 0; i < NUMBER_OF_CARDS; i++)
        {
            int j = i;
            buttons[i] = new PlayerCard(i);
            buttons[i].addActionListener(e ->
            {
                try
                {
                    flipCard(j);            //flipBack i server threadi olan SessionThreadde yapmıştık flipCard methodu burada yapılır
                }
                catch(IOException ex)
                {
                    System.err.println(ex);
                }
            });
        }
    }

    /**
     * Requests the server to flip/reveal the card.
     * 	If this is not this player's turn, the player is notified in one
     * 	of the labels and request to the server is not sent.
     * @param number card number that assigned
     * @throws IOException
     */
    public void flipCard(int number) throws IOException
    {
        if(playerTurn)
        {
            toServer.writeInt(number);
        }
        else
        {
            //!!!!!!!!!!!
            warning.setOpaque(true);
            warning.setBackground(Color.RED);
            warning.setText("This is not your turn");
        }
    }

    /**
     * Connects to the server with constant port.
     *Starting Client thread...
     */
    public void connectToServer(){

        try
        {
            Socket socket = new Socket(host, PORT);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        }
        catch(Exception e)
        {
            System.err.println(e);
        }

        Thread client = new Thread(this);
        client.start();
    }



    //-------------------------THREAD--------------------------//
    /**
     Implements run method from Runnable interface.
     Listens to requests from the server and executes commands
     corresponding to the server's request.
     */
    //thread to handle incoming server requests
    @Override
    public void run() {

        try {
            //Receives the player number
            playerNumber = fromServer.readInt();

            while(!done)
            {
                //Reads the command from the server
                //by receiving a character representing the command.
                char msg = fromServer.readChar();

                //Sets if this player takes current turn
                //The status is displayed in one of the labels.
                if(msg == TURN)
                {
                    playerTurn = fromServer.readBoolean();
                    if(playerTurn)
                    {
                        currentTurn.setText("YOUR TURN");
                        warning.setOpaque(false);
                        warning.setText("");
                    }
                    else
                    {
                        currentTurn.setText("OTHER PLAYER'S TURN");
                    }
                }

                //Reveals the value/face of the card
                if(msg == SHOW_CARD)
                {
                    int length = fromServer.readInt();
                    String message = "";
                    for(int i = 0; i < length; i++)
                    {
                        message += fromServer.readChar();
                    }
                    int card = fromServer.readInt();
                    if(!images)
                    {
                        buttons[card].setText(message);
                    }
                    else
                    {
                        buttons[card].setIcon(new ImageIcon(imagesDirectory + message + ".jpg"));
                    }
                }
                //Disables two cards (if the pair was matched).
                else if(msg == REMOVE)
                {
                    int card1 = fromServer.readInt();
                    int card2 = fromServer.readInt();
                    buttons[card1].setEnabled(false);
                    buttons[card2].setEnabled(false);
                }
                //Shows how many pairs this player has matched.
                //The result is displayed in one of the labels.
                else if(msg == PROGRESS)
                {
                    int progress = fromServer.readInt();
                    result.setText("Number of matched pairs: " + progress);
                }
                //Flips the cards back (hides their values)
                else if(msg == BACK)
                {
                    int card1 = fromServer.readInt();
                    int card2 = fromServer.readInt();
                    if(!images)
                    {
                        String back = buttons[card1].getBack();
                        buttons[card1].setText(back);
                        buttons[card2].setText(back);
                    }
                    else
                    {
                        buttons[card1].setIcon(new ImageIcon(imagesDirectory + back + ".jpg"));
                        buttons[card2].setIcon(new ImageIcon(imagesDirectory + back + ".jpg"));
                    }
                }
                //Quits from the game and loses the game.
                //Notifies the player in a label. The game is over.
                else if(msg == QUIT_SERVER)
                {
                    try {
                        int length = fromServer.readInt();
                        String message = "";
                        for (int i = 0; i < length; i++) {
                            message += fromServer.readChar();
                        }
                        System.out.println(message);
                        /*result.setOpaque(true);
                        result.setBackground(Color.RED);
                        result.setText(message);*/
                        done = true;
                    }catch (SocketException e){
                        e.printStackTrace();
                    }


                }
                //Wins the game.
                //Notifies the player in a label. The game is over.
                else if(msg == WIN)
                {
                    int length = fromServer.readInt();
                    String message = "";
                    for(int i = 0; i < length; i++)
                    {
                        message += fromServer.readChar();
                    }
                    result.setOpaque(true);
                    result.setBackground(Color.GREEN);
                    result.setText(message);
                    done = true;
                    break;
                }
                //Loses the game.
                //Notifies the player in a label. The game is over.
                else if(msg == LOSE)
                {
                    int length = fromServer.readInt();
                    String message = "";
                    for(int i = 0; i < length; i++)
                    {
                        message += fromServer.readChar();
                    }
                    result.setOpaque(true);
                    result.setBackground(Color.RED);
                    result.setText(message);
                    done = true;
                    break;
                }
                //The game ended draw
                //Notifies the players in a label. The game is over.
                else if(msg == EQUAL)
                {
                    int length = fromServer.readInt();
                    String message = "";
                    for(int i = 0; i < length; i++)
                    {
                        message += fromServer.readChar();
                    }
                    result.setOpaque(true);
                    result.setBackground(Color.ORANGE);
                    result.setText(message);
                    done = true;
                    break;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
