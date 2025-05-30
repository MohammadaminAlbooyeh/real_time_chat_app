# Pokerito

**Pokerito** is a fun and easy-to-play console card game written in Java. Inspired by poker, it offers a simplified and fast-paced experience for all ages.

## Game Rules

- At the start, both you and the computer receive a random card.
- The dealer draws five "river" cards, one at a time.
- Each time a river card matches your card, you earn a point. If it matches the computer's card, the computer earns a point.
- After all five river cards are revealed, the player with the most matches wins.
- If both players have the same number of matches, the game ends in a tie.

## Getting Started

1. Ensure Java is installed on your computer.
2. Open a terminal and navigate to the project directory.
3. Compile the game:
    ```sh
    javac Pokerito.java
    ```
4. Run the game:
    ```sh
    java Pokerito
    ```

## Sample Gameplay

```
Welcome to Pokerito!
Your card:
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

The dealer will now draw five river cards. Press Enter to continue...
...
Your matches: 1
Computer's matches: 2
Computer wins!
```

## About the Code

The entire game logic is contained in the `Pokerito.java` file. The code uses arrays, loops, and Java's random number generator to simulate card drawing and scoring. The game is designed to be simple, readable, and easy to modify or extend.

---
Enjoy playing Pokerito!
