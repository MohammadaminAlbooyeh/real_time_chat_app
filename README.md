# Pokerito

Pokerito is a simple console-based card game written in Java. The game is inspired by poker, but with much simpler rules and gameplay.

## How to Play

1. When you start the game, both you and the computer are dealt a random card.
2. The dealer will then draw five "river" cards, one at a time.
3. Each time a river card is drawn, if it matches your card, you get a point. If it matches the computer's card, the computer gets a point.
4. After all five river cards are drawn, whoever has the most matches wins!
5. If both have the same number of matches, it's a tie.

## How to Run

1. Make sure you have Java installed on your system.
2. Compile the code:
    ```sh
    javac Pokerito.java
    ```
3. Run the game:
    ```sh
    java Pokerito
    ```

## Example Output

```
Welcome to Pokerito!
Player's card:
   _____
  |A _  |
  | ( ) |
  |(_'_)|
  |  |  |
  |____V|

Computer's card:
   _____
  |2    |
  |  o  |
  |     |
  |  o  |
  |____Z|

Now, the dealer will draw five river cards. Press Enter to continue...
...
Your matches: 1
Computer's matches: 2
Computer wins!
```

## Code Overview
The game is implemented in a single Java class `Pokerito`. It uses basic Java features such as arrays, loops, and random number generation to simulate the card drawing and scoring.
