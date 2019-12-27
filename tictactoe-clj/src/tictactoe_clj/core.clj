(ns tictactoe-clj.core
  (:gen-class)
  (:require [clojure.string :as string]))

; == General utility functions ==

(defn repeat-into-vec
  "Create a vector with an element x repeated n times."
  [n x]
  (into [] (repeat n x)))

(defn not-all-ints
  "Check if not every element of xs is an integer string."
  [xs]
  (not-every? #(re-find #"^-?\d+$" %) xs))

(defn not-all-positive
  "Check if not every element of xs is a positive integer string."
  [xs]
  (not-every? #(re-find #"^[1-9]+$" %) xs))

; == Board manipulation functions ==

(def board-size
  "The side-length of a board."
  3)

(def empty-board
  "The initial, empty board state."
  (repeat-into-vec board-size (repeat-into-vec board-size :empty)))

(defn get-owner
  "Get the owner of a cell on a board."
  [board x y]
  ((board y) x))

(defn set-owner
  "Set the owner of a cell on a board."
  [owner board x y]
  (assoc board y (assoc (board y) x owner)))

(defn try-set-owner
  "Return a board with the tile set to the given owner, or return false if it isn't a valid move."
  [owner board x y]
  (cond
    (>= x board-size)                   (do
                                          (println "Column must be between 1 and" board-size)
                                          false)
    (>= y board-size)                   (do
                                          (println "Row must be between 1 and" board-size)
                                          false)
    (not= :empty (get-owner board x y)) (do
                                          (println "Cannot place in a cell which is already occupied")
                                          false)
    :else                               (set-owner owner board x y)))

; == Board display functions ==

(def col-sep
  "The string separating adjacent horizontal cells when displaying a board."
  "|")

(def row-sep
  "The string separating successive rows of cells when displaying a board."
  (string/join "" (repeat board-size "- ")))

(defn owner-string
  "Get the string representing an owner of cells."
  [owner]
  (case owner
    :x "X"
    :o "O"
    :empty " "))

(defn display-row
  "Make a string representation of a single row from a board."
  [row]
  (string/join col-sep (map owner-string row)))

(defn display-board
  "Make a string representation of a board."
  [board]
  (string/join "\n" (interpose row-sep (map display-row board))))

; == Player interaction functions ==

(defn get-player-input
  "Ask for a player's chosen position to play at, returning a list (row column)."
  [player]
  (do
    (print (str "Where does player " (owner-string player) " want to play? Give a row,column pair: "))
    (flush)
    (let [elements (map string/trim (string/split (read-line) #","))]
      (cond
        (not= 2 (count elements))   (do
                                      (println "Expected two numbers separated by a comma")
                                      (get-player-input player))
        (not-all-ints elements)     (do
                                      (println "Couldn't parse row/column, expected an integer")
                                      (get-player-input player))
        (not-all-positive elements) (do
                                      (println "Couldn't parse row/column, index must start from 1")
                                      (get-player-input player))
        :else (apply list (map (comp dec read-string) elements))))))

(defn take-turn
  "Return the board state after a player's turn"
  [player board]
  (let [[y x] (get-player-input player)]
    (or (try-set-owner player board x y) (take-turn player board))))

(defn next-turn
  "Return the player whose turn is next given the last player to move."
  [player]
  (case player
    :x :o
    :o :x))

; == Win condition functions ==

(defn rows
  "Return a lazy sequence of the rows in a board."
  [board]
  (for [y (range board-size)]
    (for [x (range board-size)]
      (get-owner board x y))))

(defn columns
  "Return a lazy sequence of the columns in a board."
  [board]
  (for [x (range board-size)]
    (for [y (range board-size)]
      (get-owner board x y))))

(defn diagonals
  "Return a lazy sequence of the full diagonals in a board."
  [board]
  (list
   (for [x (range board-size)
         :let [y x]]
     (get-owner board x y))
   (for [x (range board-size)
         :let [y (- (dec board-size) x)]]
     (get-owner board x y))))

(defn win-ranges
  "Return a lazy sequence of the runs in the board that can trigger a win condition."
  [board]
  (concat (rows board) (columns board) (diagonals board)))

(defn check-win
  "Check if a player has won for a given range, returning the winning player or nil"
  [win-range]
  (first (for [player '(:x :o)
               :when (every? #(= player %) win-range)]
           player)))

(defn is-tie?
  "Return whether a board is in a tie state."
  [board]
  (not-any? (fn [row] (some #(= :empty %) row)) board))

; == Main game running functions ==

(defn run-game
  "Run the game, given a starting board state and the player to go first.
  Returns the winner, or :tie."
  [board player]
  (or
   (println (str "\n" (display-board board) "\n"))
   (some check-win (win-ranges board))
   (if (is-tie? board)
     :tie
     (run-game (take-turn player board) (next-turn player)))))

(defn -main
  "Run the game to completion."
  [& args]
  (do
    (println "Welcome to tic-tac-toe!")
    (case (run-game empty-board :x)
      :x (println "Game Over: Player X Wins!")
      :o (println "Game Over: Player O Wins!")
      :tie (println "Game Over: Tie!"))))
