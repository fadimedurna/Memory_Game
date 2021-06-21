import javax.swing.*;

/**
 * Every Button=> represent cards
 */
public class PlayerCard extends JButton {
    private String back = "BACK";
    private String value;
    private int number;
    private boolean pressed = false;

    /**
     Creates a card assigning numbers.
     @param number the number of the card
     */
    public PlayerCard(int number)
    {
        this.number = number;
    }

    /**
     Returns the back of the card.
     @return back of the card
     */
    public String getBack()
    {
        return back;
    }
}
