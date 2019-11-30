module Lib
    ( Player(..)
    , createGame
    , runGame
    )
where

import           Prelude                 hiding ( replicate )
import           Data.Sequence                  ( Seq
                                                , update
                                                , index
                                                , replicate
                                                )
import           Control.Error.Operator         ( (<?>)
                                                , assert
                                                )
import           Data.Char                      ( isSpace )
import           Data.List.Split                ( splitOn
                                                , chunksOf
                                                )
import           Data.List                      ( intersperse
                                                , intercalate
                                                , transpose
                                                )
import           Data.Foldable                  ( toList )
import           Data.Maybe                     ( isJust )
import           Text.Read                      ( readMaybe )
import           Control.Monad                  ( when
                                                , forM_
                                                )
import           System.IO                      ( stdout
                                                , hFlush
                                                )

data Player = X | O deriving (Show, Eq)
data EndState = Won Player | Tie
data Game = Game { board :: Board, currentPlayer :: Player }
data PlaceAttempt = Success | Occupied | RowOutOfBounds | ColumnOutOfBounds
type TileState = Maybe Player
newtype Board = Board (Seq TileState)

boardSize :: Int
boardSize = 3

firstPlayer :: Player
firstPlayer = X

togglePlayer :: Game -> Game
togglePlayer game = case currentPlayer game of
    X -> game { currentPlayer = O }
    O -> game { currentPlayer = X }

tileChar :: TileState -> Char
tileChar Nothing  = ' '
tileChar (Just X) = 'X'
tileChar (Just O) = 'O'

getIndex :: Int -> Int -> Int
getIndex x y = x + y * boardSize

tryPlace :: Int -> Int -> Board -> PlaceAttempt
tryPlace x y (Board tiles)
    | x >= boardSize = ColumnOutOfBounds
    | y >= boardSize = RowOutOfBounds
    | otherwise = case index tiles (getIndex x y) of
        Nothing -> Success
        Just _  -> Occupied

place :: Int -> Int -> Player -> Board -> Board
place x y player (Board tiles) = Board $ update index (Just player) tiles
    where index = getIndex x y

parsePosition :: String -> Either String (Int, Int)
parsePosition input = do
    let elements = splitOn "," . filter (not . isSpace) $ input
    assert (length elements == 2) "Expected two numbers separated by a comma"
    values <-
        mapM readMaybe elements
            <?> "Couldn't parse row/column, expected an integer"
    let [row, column] = values
    assert (row >= 1 && column >= 1)
           "Couldn't parse row/column, index must start from 1"
    return (column - 1, row - 1)

instance Show Board where
    show (Board tiles) =
        let rows   = chunksOf boardSize . map tileChar . toList $ tiles
            rowSep = concat . replicate boardSize $ "- "
            colSep = '|'
        in  intercalate "\n"
                . intersperse rowSep
                . map (intersperse colSep)
                $ rows

createGame :: Player -> Game
createGame player = Game
    { board         = Board $ replicate (boardSize * boardSize) Nothing
    , currentPlayer = player
    }

checkForEnd :: Game -> Maybe EndState
checkForEnd game =
    let (Board tiles) = board game
        list          = toList tiles
        rows          = chunksOf boardSize list
        columns       = transpose rows
        upDiag = map (\n -> index tiles (getIndex n n)) [0 .. boardSize - 1]
        downDiag      = map (\n -> index tiles (getIndex n (boardSize - 1 - n)))
                            [0 .. boardSize - 1]
        ranges = rows ++ columns ++ [upDiag, downDiag]
        wonByX = filter (all (== Just X)) ranges
        wonByO = filter (all (== Just O)) ranges
    in  case (wonByX, wonByO) of
            ([], []) -> if all isJust list then Just Tie else Nothing
            (_ , []) -> Just (Won X)
            ([], _ ) -> Just (Won O)
            (_ , _ ) -> error "invalid game state"

prompt :: String -> IO String
prompt text = do
    putStr text
    hFlush stdout
    getLine

askPosition :: Player -> IO (Int, Int)
askPosition player = do
    input <-
        prompt
        $  "\nWhere does player "
        ++ show player
        ++ " want to play? Give a row,column pair: "
    case parsePosition input of
        Left  err      -> putStrLn err >> askPosition player
        Right position -> return position

makeMove :: Game -> IO Game
makeMove game =
    let
        b = board game
        p = currentPlayer game
    in
        do
            (x, y) <- askPosition p
            case tryPlace x y b of
                Success  -> return game { board = place x y p b }
                Occupied -> do
                    putStrLn "Cannot place in a cell which is already occupied!"
                    makeMove game
                RowOutOfBounds -> do
                    putStrLn $ "Row must be between 1 and " ++ show boardSize
                    makeMove game
                ColumnOutOfBounds -> do
                    putStrLn $ "Column must be between 1 and " ++ show boardSize
                    makeMove game

stepGame :: Game -> IO (Maybe Game)
stepGame game = do
    game <- makeMove game
    putStrLn $ "\n" ++ show (board game)
    case checkForEnd game of
        Just (Won winner) ->
            putStrLn ("\nGame Over: Player " ++ show winner ++ " Wins!")
                >> return Nothing
        Just Tie -> putStrLn "\nGame Over: Tie!" >> return Nothing
        Nothing  -> return $ Just (togglePlayer game)


runGame :: Game -> IO ()
runGame game = do
    putStrLn "Welcome to tic-tac-toe!"
    go game
    where go = (mapM_ go =<<) . stepGame
