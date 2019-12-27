
#include <algorithm>
#include <array>
#include <cstdint>
#include <iostream>
#include <optional>
#include <sstream>

template <size_t N, typename T, size_t... Ns>
auto initAllHelper(std::index_sequence<Ns...>, T defaultValue)
    -> std::array<T, N>
{
    auto constDefault = [&](auto) { return defaultValue; };
    return {constDefault(Ns)...};
}

template <size_t N, typename T>
auto initAll(T defaultValue) -> std::array<T, N>
{
    return initAllHelper<N>(std::make_index_sequence<N>{}, defaultValue);
}

constexpr std::size_t BOARD_SIZE = 3;

enum class Player
{
    X,
    O
};

auto operator<<(std::ostream& out, Player p) -> std::ostream&
{
    switch (p)
    {
        case Player::X:
            return out << "X";
        case Player::O:
            return out << "O";
    }
}

enum class EndState
{
    XWon,
    OWon,
    Tie
};

enum class TileState
{
    X,
    O,
    Empty
};

auto operator<<(std::ostream& out, TileState ts) -> std::ostream&
{
    switch (ts)
    {
        case TileState::X:
            return out << "X";
        case TileState::O:
            return out << "O";
        case TileState::Empty:
            return out << " ";
    }
}

template <typename T>
class EqualTo
{
  private:
    T _value;

  public:
    explicit EqualTo(T v) : _value{std::move(v)} {}

    auto operator()(T other) -> bool { return other == _value; }
};

class GameState
{
  private:
    using Board = std::array<std::array<TileState, BOARD_SIZE>, BOARD_SIZE>;
    Board  _board;
    Player _toMove = Player::X;

    auto transposeBoard() -> Board
    {
        Board result;

        for (size_t i = 0; i < BOARD_SIZE; i++)
        {
            for (size_t j = 0; j < BOARD_SIZE; j++)
            {
                result[i][j] = _board[j][i];
            }
        }

        return result;
    }

    template <size_t... Ns>
    auto diagHelper(std::index_sequence<Ns...>, bool increasing)
        -> std::array<TileState, BOARD_SIZE>
    {
        if (increasing)
        {
            return {_board[Ns][Ns]...};
        }
        return {_board[Ns][BOARD_SIZE - 1 - Ns]...};
    }

    auto diag(bool increasing) -> std::array<TileState, BOARD_SIZE>
    {
        return diagHelper(std::make_index_sequence<BOARD_SIZE>{}, increasing);
    }

    auto checkForEnd() -> std::optional<EndState>
    {
        std::vector<std::array<TileState, BOARD_SIZE>> winRanges;

        auto rows    = _board;
        auto columns = transposeBoard();

        winRanges.insert(winRanges.end(), rows.begin(), rows.end());
        winRanges.insert(winRanges.end(), columns.begin(), columns.end());
        winRanges.push_back(diag(true));
        winRanges.push_back(diag(false));

        for (auto range : winRanges)
        {
            if (std::all_of(range.begin(), range.end(), EqualTo{TileState::X}))
            {
                return EndState::XWon;
            }
            if (std::all_of(range.begin(), range.end(), EqualTo{TileState::O}))
            {
                return EndState::OWon;
            }
        }

        if (std::all_of(rows.begin(), rows.end(), [](auto row) {
                return std::all_of(row.begin(), row.end(), [](auto tile) {
                    return tile != TileState::Empty;
                });
            }))
        {
            return EndState::Tie;
        }

        return {};
    }

    auto displayBoard() -> std::string
    {
        std::stringstream ss;

        std::string colSep = "|";
        std::string rowSep = "- ";

        bool firstRow = true;
        for (auto row : _board)
        {
            if (!firstRow)
            {
                for (auto tile : row)
                {
                    std::cout << rowSep;
                }
                std::cout << std::endl;
            }

            bool firstColumn = true;
            for (auto tile : row)
            {
                if (!firstColumn)
                {
                    std::cout << colSep;
                }

                std::cout << tile;

                firstColumn = false;
            }
            std::cout << std::endl;

            firstRow = false;
        }

        return ss.str();
    }

    auto askPosition() -> std::pair<size_t, size_t>
    {
        while (true)
        {
            std::cout << std::endl
                      << "Where does player " << _toMove
                      << " want to play? Give a row,column pair: ";
            std::cout.flush();

            std::string inputLine;
            std::cin >> inputLine;

            auto isspace = [](auto c) {
                return c == ' ' || c == '\t' || c == '\n' || c == '\r';
            };

            inputLine.erase(
                std::remove_if(inputLine.begin(), inputLine.end(), isspace),
                inputLine.end());

            std::vector<std::string> coords;
            size_t                   pos = 0;
            std::string              token;
            while ((pos = inputLine.find(',')) != std::string::npos)
            {
                coords.push_back(inputLine.substr(0, pos));
                inputLine.erase(0, pos + 1);
            }
            coords.push_back(inputLine);

            if (coords.size() != 2)
            {
                std::cout << "Expected two numbers separated by a comma"
                          << std::endl;
                continue;
            }

            try
            {
                auto row    = std::stoi(coords[0]);
                auto column = std::stoi(coords[1]);

                if (row < 1)
                {
                    std::cout << "Couldn't parse row/column, index must start "
                                 "from 1"
                              << std::endl;
                    continue;
                }

                if (column < 1)
                {
                    std::cout << "Couldn't parse row/column, index must start "
                                 "from 1"
                              << std::endl;
                    continue;
                }

                return {row - 1, column - 1};
            }
            catch (std::invalid_argument&)
            {
                std::cout << "Couldn't parse row/column, expected an integer"
                          << std::endl;
                continue;
            }
        }
    }

    auto tryPlace(size_t x, size_t y) -> bool
    {
        if (x >= BOARD_SIZE)
        {
            std::cout << "Column must be between 1 and " << BOARD_SIZE
                      << std::endl;
            return false;
        }

        if (y >= BOARD_SIZE)
        {
            std::cout << "Row must be between 1 and " << BOARD_SIZE
                      << std::endl;
            return false;
        }

        auto curr = _board[x][y];

        if (curr != TileState::Empty)
        {
            std::cout << "Cannot place in a cell which is already occupied!"
                      << std::endl;
            return false;
        }

        switch (_toMove)
        {
            case Player::X:
                _board[x][y] = TileState::X;
                break;
            case Player::O:
                _board[x][y] = TileState::O;
                break;
        }

        return true;
    }

    void makeMove()
    {
        while (true)
        {
            auto [x, y] = askPosition();
            if (tryPlace(x, y))
            {
                break;
            }
        }
    }

    void toggleMove()
    {
        switch (_toMove)
        {
            case Player::X:
                _toMove = Player::O;
                break;
            case Player::O:
                _toMove = Player::X;
                break;
        }
    }

  public:
    GameState()
        : _board{initAll<BOARD_SIZE>(initAll<BOARD_SIZE>(TileState::Empty))}
    {
    }

    void run(Player firstMove)
    {
        _toMove = firstMove;
        std::optional<EndState> endState;

        std::cout << std::endl << displayBoard();
        do
        {
            makeMove();
            std::cout << std::endl << displayBoard();
            toggleMove();
        } while (!(endState = checkForEnd()).has_value());

        switch (endState.value())
        {
            case EndState::XWon:
                std::cout << std::endl
                          << "Game Over: Player X Wins!" << std::endl;
                break;
            case EndState::OWon:
                std::cout << std::endl
                          << "Game Over: Player O Wins!" << std::endl;
                break;
            case EndState::Tie:
                std::cout << std::endl << "Game Over: Tie!" << std::endl;
                break;
        }
    }
};

auto main() -> int
{
    std::cout << "Welcome to tic-tac-toe!" << std::endl;
    GameState{}.run(Player::X);
    return 0;
}
