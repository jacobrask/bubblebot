;; Parse an IRC message into useful bits and pieces.
;; All comment annotations in Augmented Backus–Naur Form.
(ns bubbleirc.msg-parser
  (:require [clojure.string :as str]))

(defn- get-trailing
  "Split a string in two parts, where the last part is everything after the
  first ' :'"
  [s]
  (let [[more & trail] (str/split s #" :")]
    [more (str/join " :" trail)]))

;; servername / ( nickname [ [ "!" user ] "@" host ] )
(defn parse-prefix
  "Parse the prefix part of an IRC message"
  [pfx]
  (let [parts (rest (str/split pfx #":|!|@"))]
    (zipmap (if (< 1 (count parts)) [:nick :user :host] [:servername])
            parts)))

;; message =  [ ":" prefix SPACE ] command [ params ] crlf
;; prefix  =  servername / ( nickname [ [ "!" user ] "@" host ] )
;; command =  1*letter / 3digit
;; params  =  *14( SPACE middle ) [ SPACE ":" trailing ]
;;         =/ 14( SPACE middle ) [ SPACE [ ":" ] trailing ]
(defn parse-message
  "Parse an IRC message to a map of components"
  [raw]
  (let [[part trail] (get-trailing raw)
        [pfx & [cmd & mid :as more]] (str/split part #" ")]
    (into {} (remove (comp empty? val)
      (conj
        {:message raw}
        (if (= \: (first pfx))
          (conj {:command cmd :middle mid :trailing trail} (parse-prefix pfx))
          ; prefix was actually command, shift some things
          {:command pfx :middle more :trailing trail}))))))
