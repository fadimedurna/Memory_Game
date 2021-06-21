import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Random;

/**
 Class/Thread for handling a session of Memory game between two players.
 */
public class SessionThread implements Runnable, Constants{

    private Socket player1;
    private Socket player2;
    private int numOfMatch = 0;
    private boolean player1Turn = true;
    private boolean player2Turn = false;
    private boolean done = false;
    private int player1Matched = 0;
    private int player2Matched = 0;
    public String[] values = new String[NUMBER_OF_CARDS];

    private DataInputStream fromPlayer1;
    private DataOutputStream toPlayer1;
    private DataInputStream fromPlayer2;
    private DataOutputStream toPlayer2;

    /**
     * Thread Constructor with shuffling card values method.
     */
    public SessionThread(Socket socket1, Socket socket2) {

        player1 = socket1;
        player2 = socket2;
        shuffleCards();

    }

    /**
     Creates 2 output streams and 2 input stream (1 for each player),
     and loops receiving commands from and sending responses to players.
     */
    @Override
    public void run() {
        try {
            fromPlayer1 = new DataInputStream(player1.getInputStream());
            toPlayer1 = new DataOutputStream(player1.getOutputStream());
            fromPlayer2 = new DataInputStream(player2.getInputStream());
            toPlayer2 = new DataOutputStream(player2.getOutputStream());

            while(!done) {
                //First, set whose turn it is now and show the number of pairs each player has matched.
                decidePlayerTurn();
                showProgress();

                //----------------------PLAYER 1------------------------------//

                //Receive the first move  from player1
                int move1 = fromPlayer1.readInt();

                if (move1 == QUIT)   //If player quits
                {
                    quitSession(1);
                    winner(2);
                    done = true;
                    break;
                } else if (move1 != QUIT) {    //otherwise make move

                    showCard(move1);
                }

                //Receive the second move  from player1
                int move2 = fromPlayer1.readInt();

                if (move2 == QUIT)   //If player quits
                {
                    quitSession(1);
                    winner(2);
                    done = true;
                    break;
                } else if (move2 != QUIT) {    //otherwise make move

                    showCard(move2);
                }

                //Check if the player has matched the cards.
                if(values[move1].equals(values[move2]))     //If cards are the same disables the matched pair, increases the counter for matched pairs
                {
                    removeCards(move1, move2);
                    numOfMatch += 1;
                    player1Matched += 1;
                    showProgress();         //Updates the player's progress, and checks if the game is won.
                    if(checkWin())          //Stops the game if all cards have been matched and determines the winner.
                    {
                        decideAWinner();
                        done = true;
                        break;
                    }
                }
                else                                        //If not, flips the cards back after a pause.
                {
                    Thread thread = new Thread(() -> {
                        try
                        {
                            Thread.sleep(500);
                            flipBack(move1, move2);
                        }
                        catch(InterruptedException exception)
                        {
                            System.err.println(exception);
                        }
                        catch(IOException ex)
                        {
                            System.err.println(ex);
                        }
                    });
                    thread.start();
                }
                //Switch turns
                player1Turn = false;
                player2Turn = true;
                decidePlayerTurn();


                //----------------------PLAYER 2------------------------------//
                //FOLLOWS THE SAME PATTERN AS PLAYER 1//
                int move3 = fromPlayer2.readInt();

                if(move3 == QUIT)
                {
                    quitSession(2);
                    winner(1);
                    done = true;
                    break;
                }
                else if(move3 != QUIT)
                {
                    showCard(move3);
                }

                int move4 = fromPlayer2.readInt();
                if(move4 == QUIT)
                {
                    quitSession(2);
                    winner(1);
                    done = true;
                    break;
                }
                else if(move4 != QUIT)
                {
                    showCard(move4);
                }

                //Check if the player has matched the cards.
                if(values[move3].equals(values[move4]))
                {
                    removeCards(move3, move4);
                    numOfMatch += 1;
                    player2Matched += 1;
                    showProgress();
                    if(checkWin())
                    {
                        decideAWinner();
                        done = true;
                        break;
                    }

                }else{
                    Thread thread = new Thread(() ->
                    {
                        try
                        {
                            Thread.sleep(500);
                            flipBack(move3, move4);
                        }
                        catch(InterruptedException exception)
                        {
                            System.err.println(exception);
                        }
                        catch(IOException ex)
                        {
                            System.err.println(ex);
                        }
                    });
                    thread.start();
                }

                player1Turn = true;
                player2Turn = false;
                decidePlayerTurn();
            }


        }catch (SocketException e){
            System.out.println("Players quited the server!");
            System.err.println(e);

        }catch (IOException e) {
            e.printStackTrace();
            System.err.println(e);
        }
    }

    /**
     * Initializing string array with values to be used for cards.
     * Values are 0 to numberOfCards-1.
     * After shuffles the values that are in the array.
     */
    public void shuffleCards() {
        for(int i = 0; i < NUMBER_OF_CARDS / 2; i++)
        {
            values[i] = "" + i; //i = 0,1,2,3,4,5,6,7
        }
        for(int i = NUMBER_OF_CARDS / 2; i < NUMBER_OF_CARDS; i++) //numberofCards/2 =8
        {
            values[i] = values[i - NUMBER_OF_CARDS / 2];
        }

        Random random = new Random();
        for(int i = values.length - 1; i > 0; i--)
        {
            int index = random.nextInt(i + 1); //1-8 arası random sayılar atar

            //!important! swapping algorithm
            String value = values[index];
            values[index] = values[i];
            values[i] = value;
        }

    }

    /**
     * Decides player turn that can be either true or false.
     * @throws IOException
     */
    public void decidePlayerTurn() throws IOException{

        toPlayer1.writeChar(TURN);
        toPlayer1.writeBoolean(player1Turn);
        toPlayer1.flush();

        toPlayer2.writeChar(TURN);
        toPlayer2.writeBoolean(player2Turn);
        toPlayer2.flush();

    }

    /**
     Shows the players the number of pairs they have matched.
     */
    public void showProgress() throws IOException{
        toPlayer1.writeChar(PROGRESS);
        toPlayer1.writeInt(player1Matched);
        toPlayer1.flush();

        toPlayer2.writeChar(PROGRESS);
        toPlayer2.writeInt(player2Matched);
        toPlayer2.flush();
    }

    /**
     * Quits the player from the game.
     * @param player who requested to quit
     * @throws IOException
     */
    public void quitSession(int player) throws IOException{

        if(player == 1)
        {
            toPlayer1.writeInt(QUIT_SERVER);

            String message = "You quit the game. You lose!";
            toPlayer1.writeInt(message.length());
            toPlayer1.writeChars(message);

            toPlayer1.flush();
        }
        else if(player == 2)
        {
            toPlayer2.writeInt(QUIT_SERVER);

            String message = "You quit the game. You lose!";
            toPlayer2.writeInt(message.length());
            toPlayer2.writeChars(message);

            toPlayer2.flush();
        }
    }

    /**
     * Informs the player who won.
     * @param player who won
     * @throws IOException
     */
    public void winner(int player) throws IOException{
        if(player == 1)
        {
            toPlayer1.writeChar(WIN);

            String message = "You won! Congratulations!";
            toPlayer1.writeInt(message.length());
            toPlayer1.writeChars(message);

            toPlayer1.flush();
        }
        else if(player == 2)
        {
            toPlayer2.writeChar(WIN);

            String message = "You won! Congratulations!";
            toPlayer2.writeInt(message.length());
            toPlayer2.writeChars(message);

            toPlayer2.flush();
        }
    }

    /**
     * Informs the user/player who lost.
     * @param player
     * @throws IOException
     */
    public void loser(int player) throws IOException{
        if(player ==1){
            toPlayer1.writeChar(LOSE);

            String message = "You lose! Sorry :(";
            toPlayer1.writeInt(message.length());
            toPlayer1.writeChars(message);

            toPlayer1.flush();
        }else if(player == 2){
            toPlayer2.writeChar(LOSE);

            String message = "You lose! Sorry :(";
            toPlayer2.writeInt(message.length());
            toPlayer2.writeChars(message);

            toPlayer2.flush();
        }
    }

    /**
     * Shows the value of card both client/player.
     * @param move the number of card to be shown.
     * @throws IOException
     */
    public void showCard(int move) throws IOException{
        toPlayer1.writeChar(SHOW_CARD);

        toPlayer1.writeInt(values[move].length());
        toPlayer1.writeChars(values[move]);
        toPlayer1.writeInt(move);

        toPlayer1.flush();


        toPlayer2.writeChar(SHOW_CARD);

        toPlayer2.writeInt(values[move].length());
        toPlayer2.writeChars(values[move]);
        toPlayer2.writeInt(move);

        toPlayer2.flush();
    }

    /**
     * If matched, remove both cards for both player.
     * @param card1 first card
     * @param card2 second card
     * @throws IOException
     */
    public void removeCards(int card1, int card2) throws IOException{ //card1:move1, card2:move2

        toPlayer1.writeChar(REMOVE);

        toPlayer1.writeInt(card1);
        toPlayer1.writeInt(card2);

        toPlayer1.flush();

        toPlayer2.writeChar(REMOVE);

        toPlayer2.writeInt(card1);
        toPlayer2.writeInt(card2);

        toPlayer2.flush();
    }

    /**
     * Game is won, means all pairs matched.
     * @return true if it is wini, otherwise false.
     */
    public boolean checkWin(){
        if (numOfMatch == (NUMBER_OF_CARDS /2)) {
            return true;
        }
        return false;
    }


    /**
     * The player who has matched more pairs wins.
     * @throws IOException
     */
    public void decideAWinner() throws IOException{
        if(player1Matched > player2Matched){
            winner(1);
            loser(2);
        }else if(player2Matched > player1Matched){
            winner(2);
            loser(1);
        }else if(player1Matched == player2Matched){     //BAK!!!!!!!!!!!!!!!!!!!!
            equalMatch();
        }
    }

    public void equalMatch() throws IOException{
        toPlayer1.writeChar(EQUAL);

        String message = "Players had same amount of matches.";
        toPlayer1.writeInt(message.length());
        toPlayer1.writeChars(message);

        toPlayer1.flush();

        toPlayer2.writeChar(EQUAL);
        toPlayer2.writeInt(message.length());
        toPlayer2.writeChars(message);

        toPlayer2.flush();
    }

    /**
     * If not matched, closing both values for card by flipping back
     * @param move1
     * @param move2
     * @throws IOException
     */
    public void flipBack(int move1, int move2) throws IOException{
        toPlayer1.writeChar(BACK);

        toPlayer1.writeInt(move1);
        toPlayer1.writeInt(move2);

        toPlayer1.flush();


        toPlayer2.writeChar(BACK);

        toPlayer2.writeInt(move1);
        toPlayer2.writeInt(move2);

        toPlayer2.flush();
    }





}
