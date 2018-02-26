(ns nutrition-noter.core
  (:require [reagent.core :as r :refer [render]]
            [cljs.reader :as cr :refer [read-string]]
            [cljsjs.js-yaml]
            [cljsjs.moment]
            [clojure.string :as cs :refer [replace]]))

(defn dissoc-idx [v i]
  "Provides the vector v without the element located at index i."
  (vec (concat (subvec v 0 i) (subvec v (inc i)))))

(defn conj-or-create [coll creator member]
  "Provides the coll with the member appended to it when coll is not nil. Otherwise, provides a new collection created via creator and containing only member."
  (conj (if (nil? coll)
          (creator coll) coll) member))

(defn convert-universal-to-local [datetime default offset]
  "Provides the converted string datetime from the universal time zone offset to the given offset. If datetime is nil or empty, then default is converted, instead."
  (.format (.utcOffset (js/moment (if (or (nil? datetime) (empty? datetime))
             default
             (js/moment datetime))) offset)
           "YYYY-MM-DD[T]HH:mm:ss.SSS"))

(defn convert-local-to-universal [datetime]
  "Provides the string datetime converted to the universal time zone offset."
  (-> datetime
      js/moment
      .toISOString))

(defn datetime-designer [d]
  "Editor for moments in time; that is, for dates and times."
  (let [now (js/moment) derefed (convert-universal-to-local @d now (.utcOffset now))]
    [:input {:class "label-designer"
             :type "datetime-local"
             :value derefed
             :on-change #(reset! d (-> %
                                       .-target
                                       .-value
                                       convert-local-to-universal))}]))

(defn text-designer [ph d]
  "Editor for small amounts of plain text."
  (let [derefed @d]
    [:input {:class "label-designer"
             :type "text"
             :placeholder ph
             :value derefed
             :on-change #(reset! d (-> %
                                       .-target
                                       .-value))}]))

(defn recipe-designer [ph d recipes-list-id]
  "Editor for recipes. Uses the list identified by recipes-list-id as its source of autocompletion suggestions."
  (let [derefed @d]
    [:input {:class "label-designer"
             :list recipes-list-id
             :placeholder ph
             :value derefed
             :on-change #(reset! d (-> %
                                       .-target
                                       .-value))}]))

(defn parse-data [data]
  "Provide the deserialized Clojure(Script) data structures represented by the serialized YAML data."
  (-> data
      js/jsyaml.load
      (js->clj :keywordize-keys true)))

(defn sanitize-file-name [filename]
  "Provides filename with only valid characters for file names in various operating systems and file systems. See https://github.com/parshap/node-sanitize-filename for more information."
  (let [illegal #"[\/\?<>\\\:\*\|\"\^]"
        control #"[\x00-\x1f\x80-\x9f]"
        reserved #"^\.+$"
        windows-reserved #"^(con|prn|aux|nul|com[0-9]|lpt[0-9])(\..*)?$"
        windows-trailing-space #"[\.]+$"
        replacement ""]
    (-> filename
        (replace illegal replacement)
        (replace control replacement)
        (replace reserved replacement)
        (replace windows-reserved replacement)
        (replace windows-trailing-space replacement)
        (subs 0 256))))

(defn make-file-name [data]
  "Sanitizes the data's author for use in a file name; if the title is nil or empty, then the default file name is used, instead."
  (let [author (get data :author) default-name "nutrition notes.yaml"]
    (sanitize-file-name
     (if author
       (str author " " default-name)
       default-name))))

(defn serialize-data [data]
  (-> data
      clj->js
      js/jsyaml.dump
      seq))

(defn read-first-file [event on-load]
  (let [first-file (-> event
                       .-target
                       .-files
                       (aget 0))
        reader (js/window.FileReader.)]
    (set! (.-onload reader) on-load)
    (.readAsText reader first-file)))

(defn file-loader [data selector-name]
    [:span {:class "file-loader"}
     [:button {:type "button"
               :on-click (fn [click-event]
                           (-> click-event
                               .-target
                               .-parentElement
                               (.querySelector (str "[name=" selector-name "]"))
                               .click))}
      "Load from File"]
     [:input {:type "file"
              :name selector-name
              :on-change (fn [change-event]
                           (read-first-file change-event
                                            (fn [load-event]
                                              (reset! data
                                                      (-> load-event
                                                          .-target
                                                          .-result
                                                          parse-data)))))}]])

(defn save-to-file [data filename]
  (let [blob (js/window.Blob. data)]
    (if (-> js/window
            .-navigator
            .-msSaveOrOpenBlob)
      (-> js/window
          .-navigator
          (.msSaveBlob blob filename))
      (let [elem (doto (-> js/window
                           .-document
                           (.createElement "a"))
                   (.setAttribute "href" (-> js/window
                                             .-URL
                                             (.createObjectURL blob)))
                   (.setAttribute "download" filename))]
        (-> js/window
            .-document
            .-body
            (.appendChild elem))
        (.click elem)
        (-> js/window
            .-document
            .-body
            (.removeChild elem))))))

(defn file-saver [data]
  (let [deref @data]
    [:button {:type "button"
              :on-click (fn []
                          (save-to-file
                           (serialize-data deref)
                           (make-file-name deref)))}
     "Save to File"]))

(defn serving-designer [o recipes-list-id]
  [:div {:class "serving-edit"}
   [text-designer "Meal Name" (r/cursor o [:meal])]
   [recipe-designer "Recipe" (r/cursor o [:recipe]) recipes-list-id]
   [text-designer "Amount" (r/cursor o [:amount])]
   [datetime-designer (r/cursor o [:when])]])

(defn remove-serving [servings index]
  (dissoc-idx servings index))

(defn create-serving [servings]
  (conj-or-create servings vec {:when ""}))

(defn servings-designer [d recipes-list-id]
  [:div
   [:ol
    (for [c (map-indexed vector @d)]
      (let [index (first c)]
      ^{:key index}
      [:li
       [serving-designer (r/cursor d [index]) recipes-list-id]
       [:button {:type "button"
                 :on-click (fn [] (swap! d remove-serving index))}
        "Remove"]]))]
   [:button {:type "button"
             :on-click (fn [] (swap! d create-serving))}
    "Add"]])

(defn get-recipes [servings]
  "Provides the seq of recipes for each serving in the servings collection."
  (map (fn [s] (get s :recipe)) servings))

(defn notes-designer [n]
  (let [servings (r/cursor n [:servings]) recipes (get-recipes @servings) recipes-list-id "recipes-list"]
    [:div {:class "notes-edit"}
     [:div {:class "actions"}
      [file-loader n "file-selector"]
      [file-saver n]]
     [:div {:class "properties"}
      [text-designer "Author" (r/cursor n [:author])]
      [servings-designer
       servings
       recipes-list-id]
      [:datalist {:id recipes-list-id}
       (for [recipe recipes]
         ^{:key recipe} [:option {:value recipe}])]]]))

(defn ^:export init []
  (let [n (r/atom nil)]
    (render [notes-designer n]
      (-> js/window
          .-document
          (.getElementById "notes-designer")))))
