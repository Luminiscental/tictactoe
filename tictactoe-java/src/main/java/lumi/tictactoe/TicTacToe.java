package lumi.tictactoe;

import java.util.Optional;
import java.util.Scanner;

class TicTacToe {

    private final Board board = new Board();
    private Player playerToMove;

    private class Point {
        private final int x;
        private final int y;

        private Point(final int x, final int y) {
            this.x = x;
            this.y = y;
        }

        private int getX() {
            return x;
        }

        private int getY() {
            return y;
        }
    }

    private TicTacToe(final Player firstPlayer) {
        playerToMove = firstPlayer;
    }

    private void togglePlayer() {
        if (playerToMove == Player.X) {
            playerToMove = Player.O;
        } else {
            playerToMove = Player.X;
        }
    }

    private Point askPosition() {
        while (true) {
            System.out.println();
            System.out
                    .print("Where does player " + playerToMove.toString() + " want to play? Give a row,column pair: ");
            @SuppressWarnings("resource")
            final var inputLine = new Scanner(System.in).nextLine();
            final var withoutSpace = inputLine.replaceAll("\\s+", "");
            final var coords = withoutSpace.split(",");

            if (coords.length != 2) {
                System.out.println("Expected two numbers separated by a comma");
                continue;
            }

            try {
                int row = Integer.parseInt(coords[0]);
                int column = Integer.parseInt(coords[1]);

                if (row < 1 || column < 1) {
                    System.out.println("Couldn't parse row/column, index must start from 1");
                }

                return new Point(row - 1, column - 1);
            } catch (NumberFormatException e) {
                System.out.println("Couldn't parse row/column, expected an integer");
                continue;
            }
        }
    }

    private void makeMove() {
        while (true) {
            final var position = askPosition();
            if (board.tryPlace(position.getX(), position.getY(), playerToMove)) {
                break;
            }
        }
    }

    private boolean stepGame() {
        makeMove();

        System.out.println();
        System.out.println(board);

        final var checkedWin = board.checkForWin();
        if (checkedWin.equals(Optional.of(Player.X))) {
            System.out.println();
            System.out.println("Game Over: Player X Wins!");
            return false;
        }

        if (checkedWin.equals(Optional.of(Player.O))) {
            System.out.println();
            System.out.println("Game Over: Player O Wins!");
            return false;
        }

        if (board.checkForTie()) {
            System.out.println();
            System.out.println("Game Over: Tie!");
            return false;
        }

        return true;
    }

    private void run() {
        System.out.println("Welcome to tic-tac-toe!");
        System.out.println();
        System.out.println(board);
        while (stepGame()) {
            togglePlayer();
        }
    }

    public static void main(final String[] args) {
        new TicTacToe(Player.X).run();
    }
}
