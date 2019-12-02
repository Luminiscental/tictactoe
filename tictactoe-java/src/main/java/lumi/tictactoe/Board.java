package lumi.tictactoe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Board {
    private class Tile {
        private Optional<Player> value = Optional.empty();

        private void set(final Player player) {
            value = Optional.of(player);
        }

        private boolean isEmpty() {
            return value.isEmpty();
        }

        private boolean ownedBy(final Player player) {
            return value.equals(Optional.of(player));
        }

        public String toString() {
            return value.map(Player::toString).orElse(" ");
        }
    }

    private static final int BOARD_SIZE = 3;
    private final Tile[][] tiles = new Tile[BOARD_SIZE][BOARD_SIZE];

    Board() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                tiles[i][j] = new Tile();
            }
        }
    }

    private Tile getTile(final int x, final int y) {
        return tiles[x][y];
    }

    private List<List<Tile>> rows() {
        return IntStream.range(0, BOARD_SIZE).mapToObj(i -> row(i)).collect(Collectors.toList());
    }

    private List<List<Tile>> columns() {
        return IntStream.range(0, BOARD_SIZE).mapToObj(i -> column(i)).collect(Collectors.toList());
    }

    private void setTile(final int x, final int y, final Player player) {
        tiles[x][y].set(player);
    }

    private List<Tile> row(final int index) {
        return IntStream.range(0, BOARD_SIZE).mapToObj(i -> tiles[index][i]).collect(Collectors.toList());
    }

    private List<Tile> column(final int index) {
        return IntStream.range(0, BOARD_SIZE).mapToObj(i -> tiles[i][index]).collect(Collectors.toList());
    }

    private List<Tile> diag(final boolean increasing) {
        IntFunction<Tile> indexer;
        if (increasing) {
            indexer = i -> tiles[i][i];
        } else {
            indexer = i -> tiles[i][BOARD_SIZE - 1 - i];
        }
        return IntStream.range(0, BOARD_SIZE).mapToObj(indexer).collect(Collectors.toList());
    }

    private List<List<Tile>> getWinRanges() {
        final var ranges = new ArrayList<List<Tile>>();
        ranges.add(diag(true));
        ranges.add(diag(false));
        ranges.addAll(rows());
        ranges.addAll(columns());
        return ranges;
    }

    Optional<Player> checkForWin() {
        final var ranges = getWinRanges();

        for (final var range : ranges) {
            if (range.stream().allMatch(tile -> tile.ownedBy(Player.X))) {
                return Optional.of(Player.X);
            }
            if (range.stream().allMatch(tile -> tile.ownedBy(Player.O))) {
                return Optional.of(Player.O);
            }
        }

        return Optional.empty();
    }

    boolean checkForTie() {
        return Arrays.stream(tiles).flatMap(row -> Arrays.stream(row)).allMatch(tile -> !tile.isEmpty());
    }

    boolean tryPlace(final int x, final int y, final Player player) {
        if (x >= BOARD_SIZE) {
            System.out.println("Column must be between 1 and " + BOARD_SIZE);
            return false;
        }

        if (y >= BOARD_SIZE) {
            System.out.println("Row must be between 1 and " + BOARD_SIZE);
            return false;
        }

        if (!getTile(x, y).isEmpty()) {
            System.out.println("Cannot place in a cell which is already occupied!");
            return false;
        }

        setTile(x, y, player);
        return true;
    }

    public String toString() {
        final var colSep = "|";
        final var rowSep = "\n" + "- ".repeat(BOARD_SIZE) + "\n";
        return rows().stream().map(row -> row.stream().map(tile -> tile.toString()).collect(Collectors.joining(colSep)))
                .collect(Collectors.joining(rowSep));
    }
}
