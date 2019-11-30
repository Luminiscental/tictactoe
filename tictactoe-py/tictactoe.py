from enum import Enum

BOARD_SIZE = 3


class Player(Enum):
    NOUGHTS = 0
    CROSSES = 1

    def __str__(self):
        return "O" if self == Player.NOUGHTS else "X"

    def toggle(self):
        return Player(1 - self.value)


def intersperse(iterable, delimiter):
    it = iter(iterable)
    yield next(it)
    for x in it:
        yield delimiter
        yield x


class Board:
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

    def index(self, x, y):
        return x + y * BOARD_SIZE

    def set(self, x, y, player):
        self.tiles[self.index(x, y)] = player

    def get(self, x, y):
        return self.tiles[self.index(x, y)]

    def sequences(self):
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
            except ValueError:
                print("Couldn't parse row/column, expected an integer")
                continue

            try:
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

    def try_place(self, x, y):
        if x >= BOARD_SIZE:
            print(f"Column must be between 1 and {BOARD_SIZE}")
            return False

        if y >= BOARD_SIZE:
            print(f"Row must be between 1 and {BOARD_SIZE}")
            return False

        if self.get(x, y) is not None:
            print("Cannot place in a cell which is already occupied!")
            return False

        self.set(x, y, self.current)
        return True

    def make_move(self):
        while True:
            x, y = self.ask_position()
            if self.try_place(x, y):
                break

    def won(self, player):
        return any(
            all(tile == player for tile in sequence) for sequence in self.sequences()
        )

    def tied(self):
        return all(tile is not None for tile in self.tiles)

    def step(self):
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
        print("Welcome to tic-tac-toe!")
        while self.step():
            self.current = self.current.toggle()


if __name__ == "__main__":
    Board(Player.CROSSES).run()
