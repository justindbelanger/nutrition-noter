(set-env!
  :source-paths #{"src/cljs"}
  :resource-paths #{"resources"}

  :dependencies  '[[org.clojure/clojure "1.8.0"]         ;; add CLJ
                   [org.clojure/clojurescript "1.9.494"] ;; add CLJS
                   [adzerk/boot-cljs "1.7.228-2"]
                   [pandeiro/boot-http "0.7.6"]
                   [adzerk/boot-reload "0.5.1"]
                   [adzerk/boot-cljs-repl "0.3.3"]       ;; add bREPL
                   [com.cemerick/piggieback "0.2.1"]     ;; needed by bREPL
                   [weasel "0.7.0"]                      ;; needed by bREPL
                   [org.clojure/tools.nrepl "0.2.12"]    ;; needed by bREPL
                   [hiccups "0.3.0"]
                   [compojure "1.5.2"]                   ;; for routing
                   [javax.servlet/javax.servlet-api "3.1.0"]
                   [adzerk/boot-test "1.2.0"]
                   [crisptrutski/boot-cljs-test "0.2.1-SNAPSHOT"]
                   [reagent "0.6.0"]
                   [cljsjs/js-yaml "3.3.1-0"]
                   [cljsjs/moment "2.17.1-1"]])

(require '[adzerk.boot-cljs :refer [cljs]]
         '[pandeiro.boot-http :refer [serve]]
         '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
         '[adzerk.boot-test :refer [test]]
         '[crisptrutski.boot-cljs-test :refer [test-cljs]])

(deftask add-source-paths
  "Add paths to :source-paths environment variable"
  []
  (merge-env! :source-paths #{"test/cljs"})
  identity)

(deftask run-tests []
  "Run unit tests"
  (comp
    (add-source-paths)
    (test-cljs)))

(deftask dev
  "Launch immediate feedback dev environment"
  []
  (let [target-dir "target/public"]
   (comp
     (serve :dir target-dir :httpkit true)
     (watch)
     (reload)
     (cljs-repl)
     (cljs :source-map true :optimizations :none)
     (target))))

(deftask build []
  (comp (cljs :optimizations :advanced) (target)))
