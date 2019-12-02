
#include <algorithm>
#include <array>
#include <cstdint>
#include <iostream>
#include <optional>
#include <sstream>

template <size_t N, typename T, size_t... Ns>
std::array<T, N> initAllHelper(std::index_sequence<Ns...>, T defaultValue)
{
    auto constDefault = [&](auto _) {
        return defaultValue;
    };
    return {constDefault(Ns)...};
}

template <size_t N, typename T>
std::array<T, N> initAll(T defaultValue)
{
    return initAllHelper<N>(std::make_index_sequence<N>{}, defaultValue);
}

constexpr std::size_t BOARD_SIZE = 3;

enum class Player
{
    X,
    O
};

std::ostream& operator<<(std::ostream& out, Player p)
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

std::ostream& operator<<(std::ostream& out, TileState ts)
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
    T value;

public:
    explicit EqualTo(T v) : value{std::move(v)} {}

    bool operator()(T other)
    {
        return other == value;
    }
};

class GameState
{
private:
    using Board = std::array<std::array<TileState, BOARD_SIZE>, BOARD_SIZE>;
    Board board;
    Player toMove;

    Board transposeBoard()
    {
        Board result;

        for (size_t i = 0; i < BOARD_SIZE; i++)
        {
            for (size_t j = 0; j < BOARD_SIZE; j++)
            {
                result[i][j] = board[j][i];
            }
        }

        return result;
    }

    template <size_t... Ns>
    std::array<TileState, BOARD_SIZE>
    diagHelper(std::index_sequence<Ns...>, bool increasing)
    {
        if (increasing)
        {
            return {board[Ns][Ns]...};
        }
        else
        {
            return {board[Ns][BOARD_SIZE - 1 - Ns]...};
        }
    }

    std::array<TileState, BOARD_SIZE> diag(bool increasing)
    {
        return diagHelper(std::make_index_sequence<BOARD_SIZE>{}, increasing);
    }

    std::optional<EndState> checkForEnd()
    {
        std::vector<std::array<TileState, BOARD_SIZE>> winRanges;

        auto rows    = board;
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

    std::string displayBoard()
    {
        std::stringstream ss;

        std::string colSep = "|";
        std::string rowSep = "- ";

        bool firstRow = true;
        for (auto row : board)
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

    std::pair<size_t, size_t> askPosition()
    {
        while (true)
        {
            std::cout << std::endl
                      << "Where does player " << toMove
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
            size_t pos = 0;
            std::string token;
            while ((pos = inputLine.find(",")) != std::string::npos)
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
            catch (std::invalid_argument)
            {
                std::cout << "Couldn't parse row/column, expected an integer"
                          << std::endl;
                continue;
            }
        }
    }

    bool tryPlace(size_t x, size_t y)
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

        auto curr = board[x][y];

        if (curr != TileState::Empty)
        {
            std::cout << "Cannot place in a cell which is already occupied!"
                      << std::endl;
            return false;
        }

        switch (toMove)
        {
        case Player::X:
            board[x][y] = TileState::X;
            break;
        case Player::O:
            board[x][y] = TileState::O;
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
        switch (toMove)
        {
        case Player::X:
            toMove = Player::O;
            break;
        case Player::O:
            toMove = Player::X;
            break;
        }
    }

public:
    GameState()
        : board{initAll<BOARD_SIZE>(initAll<BOARD_SIZE>(TileState::Empty))}
    {
    }

    void run(Player firstMove)
    {
        toMove = firstMove;
        std::optional<EndState> endState;

        do
        {
            makeMove();
            std::cout << std::endl << displayBoard() << std::endl;
            toggleMove();
        } while (!(endState = checkForEnd()).has_value());

        switch (endState.value())
        {
        case EndState::XWon:
            std::cout << std::endl << "Game Over: Player X Wins!" << std::endl;
            break;
        case EndState::OWon:
            std::cout << std::endl << "Game Over: Player O Wins!" << std::endl;
            break;
        case EndState::Tie:
            std::cout << std::endl << "Game Over: Player O Wins!" << std::endl;
            break;
        }
    }
};

int main()
{
    std::cout << "Welcome to tic-tac-toe!" << std::endl;
    GameState{}.run(Player::X);
    return 0;
}
