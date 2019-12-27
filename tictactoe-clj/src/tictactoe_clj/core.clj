(ns tictactoe-clj.core
  (:gen-class)
  (:require [clojure.string :as string]))

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

(defn try-set-owner
  "Set the owner of a cell on a board and return true, or return false if it is invalid."
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
    :else                               (do
                                          (set-owner owner board x y)
                                          true)))

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

(defn -main
  "Run the game to completion."
  [& args]
  (let [input (get-player-input :x)
        board (set-owner :o (set-owner :x (empty-board) 1 1) 0 2)]
    (println (string/join "\n" (list "input:" input "board:" (display-board board))))))
