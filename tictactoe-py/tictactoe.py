"""Main module to run the tictactoe game."""
from enum import Enum

BOARD_SIZE = 3


def index(tile_x, tile_y):
    """Return the index into the tiles list for a given tile position."""
    return tile_x + tile_y * BOARD_SIZE


class Player(Enum):
    """Enumeration of possible players."""

    NOUGHTS = 0
    CROSSES = 1

    def __str__(self):
        return "O" if self == Player.NOUGHTS else "X"

    def next_turn(self):
        """Return the player whose turn is next."""
        return Player(1 - self.value)


def intersperse(iterable, delimiter):
    """Intersperse a delimiter within an iterable. Returns a generator."""
    iteration = iter(iterable)
    try:
        yield next(iteration)
    except StopIteration:
        # If the iterable is empty return nothing
        return
    for element in iteration:
        yield delimiter
        yield element


class Board:
    """Class representing the board state."""

    def __init__(self, player):
        self.current = player
        self.tiles = [None] * BOARD_SIZE * BOARD_SIZE

    def __str__(self):
        rows = [
            [self.get(x, y) for x in range(0, BOARD_SIZE)] for y in range(0, BOARD_SIZE)
        ]
        col_sep = "|"
        row_sep = "- " * BOARD_SIZE
        grid = intersperse([intersperse(row, col_sep) for row in rows], row_sep)
        lines = [
            "".join(" " if item is None else str(item) for item in row) for row in grid
        ]
        return "\n".join(lines)

    def set(self, tile_x, tile_y, player):
        """Set a tile to a given player."""
        self.tiles[index(tile_x, tile_y)] = player

    def get(self, tile_x, tile_y):
        """Return the owner of a given tile."""
        return self.tiles[index(tile_x, tile_y)]

    def sequences(self):
        """Return a list of all ranges of tiles to test for a win."""
        rows = [
            [self.get(x, y) for x in range(0, BOARD_SIZE)] for y in range(0, BOARD_SIZE)
        ]

        columns = [
            [self.get(x, y) for y in range(0, BOARD_SIZE)] for x in range(0, BOARD_SIZE)
        ]

        up_diag = [self.get(n, n) for n in range(0, BOARD_SIZE)]

        down_diag = [self.get(n, BOARD_SIZE - 1 - n) for n in range(0, BOARD_SIZE)]

        return rows + columns + [up_diag, down_diag]

    def ask_position(self):
        """Ask the current player for a position to play on, returning the parsed result."""
        while True:
            print(
                f"\nWhere does player {self.current} want to play? Give a row,column pair: ",
                end="",
            )
            input_line = input()
            without_space = "".join(input_line.split())
            coords = without_space.split(",")

            if len(coords) != 2:
                print("Expected two numbers separated by a comma")
                continue

            try:
                row = int(coords[0])
                column = int(coords[1])
            except ValueError:
                print("Couldn't parse row/column, expected an integer")
                continue

            if row < 1:
                print("Couldn't parse row/column, index must start from 1")
                continue

            if column < 1:
                print("Couldn't parse row/column, index must start from 1")
                continue

            return (column - 1, row - 1)

    def try_place(self, tile_x, tile_y):
        """Place at a given tile for the current player, returning whether the move was valid."""
        if tile_x >= BOARD_SIZE:
            print(f"Column must be between 1 and {BOARD_SIZE}")
            return False

        if tile_y >= BOARD_SIZE:
            print(f"Row must be between 1 and {BOARD_SIZE}")
            return False

        if self.get(tile_x, tile_y) is not None:
            print("Cannot place in a cell which is already occupied!")
            return False

        self.set(tile_x, tile_y, self.current)
        return True

    def make_move(self):
        """Make a single move from the current player."""
        while True:
            tile_x, tile_y = self.ask_position()
            if self.try_place(tile_x, tile_y):
                break

    def won(self, player):
        """Return whether a given player has won the game."""
        return any(
            all(tile == player for tile in sequence) for sequence in self.sequences()
        )

    def tied(self):
        """Return whether the game is tied."""
        return all(tile is not None for tile in self.tiles)

    def step(self):
        """Execute a single turn of the game, returning whether to continue playing."""
        self.make_move()
        print(f"\n{self}")
        if self.won(Player.NOUGHTS):
            print("\nGame Over: Player O Wins!")
        elif self.won(Player.CROSSES):
            print("\nGame Over: Player X Wins!")
        elif self.tied():
            print("\nGame Over: Tie!")
        else:
            return True
        return False

    def run(self):
        """Run the game."""
        print("Welcome to tic-tac-toe!")
        while self.step():
            self.current = self.current.next_turn()


if __name__ == "__main__":
    Board(Player.CROSSES).run()
