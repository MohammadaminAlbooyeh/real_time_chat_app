import java.util.Scanner;


/**
 * Pokerito is a simplified card game inspired by Poker.
 * Game Rules:
 * Gets a random number between 1 and 13.
 * Return a card that matches the random number.
 * The player and the computer each get one card.
 * The dealer draws five cards (the river).
 * Each player counts how many of their cards match the river cards.
 * The player with the most river matches wins.
 * If both players have the same number of matches, everyone is a winner.
 * @return (String)
 */


public class Pokerito {

    public static void main(String[] args) {
        System.out.println("Welcome to Pokerito!");

        int playerCard = getRandomCardNumber();
        int computerCard = getRandomCardNumber();

        System.out.println("Player's card:\n" + getCardArt(playerCard));
        System.out.println("Computer's card:\n" + getCardArt(computerCard));

        int yourMatches = 0;
        int computerMatches = 0;

        System.out.println("Now, the dealer will draw five river cards. Press Enter to continue...");
        Scanner scanner = new Scanner(System.in);
        scanner.nextLine(); // Wait for the player to press Enter

        for (int i = 0; i < 5; i++) {
            System.out.println("Press Enter to draw river card " + (i + 1) + "...");
            scanner.nextLine(); // Wait for the player to press Enter
            int riverCard = getRandomCardNumber();
            System.out.println("River card " + (i + 1) + ":\n" + getCardArt(riverCard));

            if (riverCard == playerCard) {
                yourMatches++;
                System.out.println("You matched your card!");
            }
            if (riverCard == computerCard) {
                computerMatches++;
                System.out.println("Computer matched its card!");
            }
        }
        scanner.close();

        System.out.println("\nYour matches: " + yourMatches);
        System.out.println("Computer's matches: " + computerMatches);

        if (yourMatches > computerMatches) {
            System.out.println("You win!");
        } else if (computerMatches > yourMatches) {
            System.out.println("Computer wins!");
        } else {
            System.out.println("It's a tie! Everyone wins!");
        }
    }

    public static int getRandomCardNumber() {
        return (int) (Math.random() * 13) + 1; // Generates a random number between 1 and 13
    }

    public static String getCardArt(int cardNumber) {
        switch (cardNumber) {
            case 1:
                return  "   _____\n" +
                        "  |A _  |\n" +
                        "  | ( ) |\n" +
                        "  |(_'_)|\n" +
                        "  |  |  |\n" +
                        "  |____V|\n";
            case 2:
                return  "   _____\n" +
                        "  |2    |\n" +
                        "  |  o  |\n" +
                        "  |     |\n" +
                        "  |  o  |\n" +
                        "  |____Z|\n";
            case 3:
                return  "   _____\n" +
                        "  |3    |\n" +
                        "  | o o |\n" +
                        "  |     |\n" +
                        "  |  o  |\n" +
                        "  |____E|\n";
            case 4:
                return  "   _____\n" +
                        "  |4    |\n" +
                        "  | o o |\n" +
                        "  |     |\n" +
                        "  | o o |\n" +
                        "  |____h|\n";
            case 5:
                return  "   _____\n" +
                        "  |5    |\n" +
                        "  | o o |\n" +
                        "  |  o  |\n" +
                        "  | o o |\n" +
                        "  |____S|\n";
            case 6:
                return  "   _____\n" +
                        "  |6    |\n" +
                        "  | o o |\n" +
                        "  | o o |\n" +
                        "  | o o |\n" +
                        "  |____6|\n";
            case 7:
                return  "   _____\n" +
                        "  |7    |\n" +
                        "  | o o |\n" +
                        "  |o o o|\n" +
                        "  | o o |\n" +
                        "  |____7|\n";
            case 8:
                return  "   _____\n" +
                        "  |8    |\n" +
                        "  |o o o|\n" +
                        "  | o o |\n" +
                        "  |o o o|\n" +
                        "  |____8|\n";
            case 9:
                return  "   _____\n" +
                        "  |9    |\n" +
                        "  |o o o|\n" +
                        "  |o o o|\n" +
                        "  |o o o|\n" +
                        "  |____9|\n";
            case 10:
                return  "   _____\n" +
                        "  |10   |\n" +
                        "  |o o o|\n" +
                        "  |o o o|\n" +
                        "  |o o o|\n" +
                        "  |o o o|\n" +
                        "  |___10|\n";
            case 11:
                return  "   _____\n" +
                        "  |J  ww|\n" +
                        "  | o {)|\n" +
                        "  |o o% |\n" +
                        "  | | % |\n" +
                        "  |__%%[|\n";
            case 12:
                return  "   _____\n" +
                        "  |Q  ww|\n" +
                        "  | o {(|\n" +
                        "  |o o%%|\n" +
                        "  | |%%%|\n" +
                        "  |_%%%O|\n";
            case 13:
                return  "   _____\n" +
                        "  |K  WW|\n" +
                        "  | o {)|\n" +
                        "  |o o%%|\n" +
                        "  | |%%%|\n" +
                        "  |_%%%>|\n";
            default:
                return "Invalid Card"; // This should never happen
        }
    }

}
