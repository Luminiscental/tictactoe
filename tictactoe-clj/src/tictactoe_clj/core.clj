(ns tictactoe-clj.core
  (:gen-class)
  (:require [clojure.string :as string]))

(defn repeat-into-vec
  "Create a vector with an element x repeated n times."
  [n x]
  (into [] (repeat n x)))

(def board-size
  "The side-length of a board."
  3)

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
    :empty " "
    "!"))

(defn display-row
  "Make a string representation of a single row from a board."
  [row]
  (string/join col-sep (map owner-string row)))

(defn display-board
  "Make a string representation of a board."
  [board]
  (string/join "\n" (interpose row-sep (map display-row board))))

(defn empty-board
  "Make an empty board."
  []
  (repeat-into-vec board-size (repeat-into-vec board-size :empty)))

(defn get-owner
  "Get the owner of a cell on a board."
  [board x y]
  ((board y) x))

(defn set-owner
  "Set the owner of a cell on a board."
  [owner board x y]
  (assoc board y (assoc (board y) x owner)))

(defn get-player-input
  "Ask for a player's chosen position to play at."
  [player]
  (do
    (print (str "Where does player " (owner-string player) " want to play? Give a row,column pair: "))
    (flush)
    (read-line)))

(defn -main
  "Run the game to completion."
  [& args]
  (let [input (get-player-input :x)
        board (set-owner :o (set-owner :x (empty-board) 1 1) 0 2)]
    (println (str "input:\n" input "\nboard:\n" (display-board board)))))
