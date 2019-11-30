use itertools::Itertools;
use std::convert;
use std::convert::TryInto;
use std::fmt;
use std::io;
use std::io::Write;
use std::num;
use std::str;

const BOARD_SIZE: usize = 3;

#[derive(Clone, Copy, PartialEq, Eq)]
enum Player {
    X,
    O,
}

impl Player {
    fn toggle(self) -> Player {
        match self {
            Player::X => Player::O,
            Player::O => Player::X,
        }
    }
}

impl fmt::Display for Player {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Player::X => write!(f, "X"),
            Player::O => write!(f, "O"),
        }
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
enum GameState {
    Running,
    Won(Player),
    Tie,
}

#[derive(Clone, Copy, PartialEq, Eq, Default)]
struct TileState(Option<Player>);

impl fmt::Display for TileState {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            TileState(Some(player)) => write!(f, "{}", player),
            TileState(None) => write!(f, " "),
        }
    }
}

struct Position {
    x: usize,
    y: usize,
}

struct ParsePositionError(String);

impl convert::From<num::ParseIntError> for ParsePositionError {
    fn from(_: num::ParseIntError) -> Self {
        ParsePositionError(format!("Couldn't parse row/column, expected an integer"))
    }
}

impl convert::From<num::TryFromIntError> for ParsePositionError {
    fn from(_: num::TryFromIntError) -> Self {
        ParsePositionError(format!(
            "Couldn't parse row/column, index must start from 1"
        ))
    }
}

impl fmt::Display for ParsePositionError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

impl str::FromStr for Position {
    type Err = ParsePositionError;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let without_space: String = s.chars().filter(|c| !c.is_whitespace()).collect();
        let coords: Vec<&str> = without_space.split(',').collect();

        if coords.len() != 2 {
            return Err(ParsePositionError(
                "Expected two numbers separated by a comma".to_string(),
            ));
        }

        let row: i32 = coords[0].parse()?;
        let column: i32 = coords[1].parse()?;

        Ok(Position {
            x: (column - 1).try_into()?,
            y: (row - 1).try_into()?,
        })
    }
}

#[derive(Clone, Copy, PartialEq, Eq)]
struct Game {
    state: GameState,
    turn: Player,
    first_turn: Player,
    board: [[TileState; BOARD_SIZE]; BOARD_SIZE],
}

enum TryPlaceResult {
    Success,
    Occupied,
    RowOutOfBounds,
    ColumnOutOfBounds,
}

impl Game {
    fn new(start: Player) -> Game {
        Game {
            state: GameState::Running,
            turn: start,
            first_turn: start,
            board: Default::default(),
        }
    }

    fn get_tile(&self, x: usize, y: usize) -> TileState {
        self.board[y][x]
    }

    fn set_tile(&mut self, x: usize, y: usize, player: Player) {
        self.board[y][x] = TileState(Some(player));
    }

    fn try_place(&mut self, x: usize, y: usize, player: Player) -> TryPlaceResult {
        if x > BOARD_SIZE {
            TryPlaceResult::ColumnOutOfBounds
        } else if y > BOARD_SIZE {
            TryPlaceResult::RowOutOfBounds
        } else if let TileState(Some(_)) = self.get_tile(x, y) {
            TryPlaceResult::Occupied
        } else {
            self.set_tile(x, y, player);
            TryPlaceResult::Success
        }
    }

    fn ask_position(&self) -> io::Result<Position> {
        println!("");
        print!(
            "Where does player {} want to play? Give a row,column pair: ",
            self.turn
        );
        io::stdout().flush()?;

        let mut input = String::new();
        io::stdin().read_line(&mut input)?;

        match input.parse() {
            Ok(pos) => Ok(pos),
            Err(e) => {
                println!("{}", e);
                self.ask_position()
            }
        }
    }

    fn get_row(&self, y: usize) -> Vec<TileState> {
        (0..BOARD_SIZE)
            .map(move |n| {
                let x = n;
                let y = y;
                self.board[y][x]
            })
            .collect()
    }

    fn get_column(&self, x: usize) -> Vec<TileState> {
        (0..BOARD_SIZE)
            .map(move |n| {
                let x = x;
                let y = n;
                self.board[y][x]
            })
            .collect()
    }

    fn get_diagonal(&self, increasing: bool) -> Vec<TileState> {
        (0..BOARD_SIZE)
            .map(move |n| {
                let x = n;
                let y = if increasing { n } else { BOARD_SIZE - 1 - n };
                self.board[y][x]
            })
            .collect()
    }

    fn range_won_by(range: Vec<TileState>) -> Option<Player> {
        if range.iter().all(|&TileState(opt)| opt == Some(Player::X)) {
            Some(Player::X)
        } else if range.iter().all(|&TileState(opt)| opt == Some(Player::O)) {
            Some(Player::O)
        } else {
            None
        }
    }

    fn get_win_ranges(&self) -> Vec<Vec<TileState>> {
        let mut ranges = vec![];

        ranges.push(self.get_diagonal(true));
        ranges.push(self.get_diagonal(false));

        for i in 0..BOARD_SIZE {
            ranges.push(self.get_row(i));
            ranges.push(self.get_column(i));
        }

        ranges
    }

    fn check_win_condition(&mut self) {
        if let Some(winner) = self
            .get_win_ranges()
            .into_iter()
            .map(|range| Self::range_won_by(range))
            .flatten()
            .next()
        {
            self.state = GameState::Won(winner);
            println!("");
            println!("Game Over: Player {} Wins!", winner);
        } else if self
            .board
            .into_iter()
            .flatten()
            .all(|TileState(cell)| cell.is_some())
        {
            self.state = GameState::Tie;
            println!("");
            println!("Game Over: Tie!");
        }
    }

    fn run(&mut self) -> io::Result<()> {
        println!("Welcome to tic-tac-toe!");
        self.turn = self.first_turn;

        while self.state == GameState::Running {
            loop {
                let Position { x, y } = self.ask_position()?;

                match self.try_place(x, y, self.turn) {
                    TryPlaceResult::Success => break,
                    TryPlaceResult::Occupied => {
                        println!("Cannot place in a cell which is already occupied!")
                    }
                    TryPlaceResult::RowOutOfBounds => {
                        println!("Row must be between 1 and {}", BOARD_SIZE)
                    }
                    TryPlaceResult::ColumnOutOfBounds => {
                        println!("Column must be between 1 and {}", BOARD_SIZE)
                    }
                }
            }

            println!("");
            println!("{}", self);
            self.check_win_condition();
            self.turn = self.turn.toggle();
        }

        Ok(())
    }
}

impl fmt::Display for Game {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{}",
            self.board
                .iter()
                .map(|row| row.iter().join("|"))
                .intersperse("- ".repeat(BOARD_SIZE))
                .join("\n")
        )
    }
}

fn main() -> io::Result<()> {
    Game::new(Player::X).run()
}
