#!/usr/bin/env bb
(require '[babashka.process :as shell])
(require '[babashka.fs :as fs])

(def HOME (System/getenv "HOME"))
(def symfiles {:deps.edn "deps.edn" :user.clj "preload.clj"})

(defn pathjoin [root path]
  (str root (if (= (last root) \/) "" "/") path))

(defn run [& args]
  (-> (apply shell/sh {:continue true} args)
      :out
      (clojure.string/split-lines)))

(defn has-dir [dir]
         (-> dir 
             (java.io.File.)
             .isDirectory))

(defn has-file? [f]
    (or (fs/sym-link? f)
        (fs/regular-file? f)))

(defn filter-missing-files [files]
  (filter (fn [f] (if (has-file? f) f nil)) files))


(defn symlink! [src dest]
  (println (str "running ln -sfn " src " " dest))
  (run "ln" "-sfn" src dest))

(defn get-pwd []
  (-> (run "pwd")
      (first)))

(when (not (has-dir (pathjoin HOME ".clojure")))
  (run "mkdir" (str HOME "/.clojure")))


(defn symfiles->paths [symfiles prefix]
  (->> (keys symfiles)
       (vec)
       (mapv #(pathjoin
               prefix (symfiles %)))))

(defn symfiles->removable-files [symfiles]
  (->> (symfiles->paths symfiles (pathjoin HOME ".clojure"))
       (filter (fn [f] (if (has-file? f) f nil)))))

(with-redefs [symfiles->removable-files
              (fn [symfiles]
                (->> (symfiles->paths symfiles (pathjoin HOME ".clojure"))
                     (filter (fn [f] (if (has-file? f) f nil)))
                     (mapv #(println (str "removing " %)))))]
  (symfiles->removable-files symfiles))

(defn remove-files! [files]
  (mapv #(run "rm" "-rf" %) files)
  nil)

(defn files->symlink! [files]
  (let [keys (-> (keys files) (vec))
        src-files (zipmap
                   keys
                   (symfiles->paths symfiles (pathjoin (get-pwd) ".clojure")))
        dest-files (zipmap
                    keys
                    (symfiles->paths symfiles (pathjoin HOME ".clojure")))]
    (mapv #(symlink! (src-files %) (dest-files %)) keys)
    nil))
(symfiles->paths
  symfiles
  (pathjoin HOME ".clojure"))

(println "found .clojure dir!")
(println "creating symlinks...")
(println "removing old clojure config...")
(remove-files!
 (->
  (symfiles->paths
   symfiles
   (pathjoin HOME ".clojure"))
  (filter-missing-files))
 )
(files->symlink! symfiles)
(println "done creating symlinks!")
(println "Succesfully installed .config!")
