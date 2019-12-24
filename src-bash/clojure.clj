#!/usr/bin/env bb

(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[clojure.java.shell :refer [sh]])

;; # function join { local d=$1; shift; echo -n "$1"; shift; printf "%s" "${@/#/$d}"; }

(def help-text (str/trim "
Usage: clojure [dep-opt*] [init-opt*] [main-opt] [arg*]
       clj     [dep-opt*] [init-opt*] [main-opt] [arg*]

The clojure script is a runner for Clojure. clj is a wrapper
for interactive repl use. These scripts ultimately construct and
invoke a command-line of the form:

java [java-opt*] -cp classpath clojure.main [init-opt*] [main-opt] [arg*]

The dep-opts are used to build the java-opts and classpath:
 -Jopt          Pass opt through in java_opts, ex: -J-Xmx512m
 -Oalias...     Concatenated jvm option aliases, ex: -O:mem
 -Ralias...     Concatenated resolve-deps aliases, ex: -R:bench:1.9
 -Calias...     Concatenated make-classpath aliases, ex: -C:dev
 -Malias...     Concatenated main option aliases, ex: -M:test
 -Aalias...     Concatenated aliases of any kind, ex: -A:dev:mem
 -Sdeps EDN     Deps data to use as the last deps file to be merged
 -Spath         Compute classpath and echo to stdout only
 -Scp CP        Do NOT compute or cache classpath, use this one instead
 -Srepro        Ignore the ~/.clojure/deps.edn config file
 -Sforce        Force recomputation of the classpath (don't use the cache)
 -Spom          Generate (or update existing) pom.xml with deps and paths
 -Stree         Print dependency tree
 -Sresolve-tags Resolve git coordinate tags to shas and update deps.edn
 -Sverbose      Print important path info to console
 -Sdescribe     Print environment and command parsing info as data
 -Strace        Write a trace.edn file that traces deps expansion

init-opt:
 -i, --init path     Load a file or resource
 -e, --eval string   Eval exprs in string; print non-nil values
 --report target     Report uncaught exception to \"file\" (default), \"stderr\", or \"none\",
                     overrides System property clojure.main.report

main-opt:
 -m, --main ns-name  Call the -main function from namespace w/args
 -r, --repl          Run a repl
 path                Run a script from a file or resource
 -                   Run a script from standard input
 -h, -?, --help      Print this help message and exit

For more info, see:
 https://clojure.org/guides/deps_and_cli
 https://clojure.org/reference/repl_and_main
"))

(def parse-opts->keyword
  {"-J" :jvm-opts
   "-R" :resolve-aliases
   "-C" :classpath-aliases
   "-O" :jvm-aliases
   "-M" :main-aliases
   "-A" :all-aliases})

(def bool-opts->keyword
  {"-Spath" :print-classpath
   "-Sverbose" :verbose
   "-Strace" :trace
   "-Sdescribe" :describe
   "-Sforce" :force
   "-Srepro" :repro
   "-Stree" :tree
   "-Spom" :pom
   "-Sresolve-tags" :resolve-tags})

(def args "the parsed arguments"
  (loop [command-line-args (seq *command-line-args*)
         acc {}]
    (if command-line-args
      (let [arg (first command-line-args)
            bool-keyword (get bool-opts->keyword arg)]
        (cond (some #(str/starts-with? arg %) ["-J" "-R" "-C" "-O" "-M" "-A"])
              (recur (next command-line-args)
                     (update acc (get parse-opts->keyword (subs arg 0 2))
                             str (subs arg 2)))
              bool-keyword (recur
                            (next command-line-args)
                            (assoc acc bool-keyword true))
              (= "-Sdeps" arg) (recur
                                (nnext command-line-args)
                                (assoc acc :deps-data (second command-line-args)))
              (= "-Scp" arg) (recur
                              (nnext command-line-args)
                              (assoc acc :force-cp (second command-line-args)))
              (str/starts-with? arg "-S") (binding [*out* *err*]
                                            (println "Invalid option:" arg)
                                            (System/exit 1))
              (and
               (not (some acc [:main-aliases :all-aliases]))
               (or (= "-h" arg)
                   (= "--help" arg))) (assoc acc :help true)
              :else acc))
      acc)))

(when (:help args)
  (println help-text)
  (System/exit 0))

(def java-cmd "the java executable"
  (let [java-cmd (str/trim (:out (sh "type" "-p" "java")))]
    (if (str/blank? java-cmd)
      (let [java-home (System/getenv "JAVA_HOME")]
        (if-not (str/blank? java-home)
          (let [f (io/file java-home "bin" "java")]
            (if (and (.exists f)
                     (.canExecute f))
              (.getCanonicalPath f)
              (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
          (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
      java-cmd)))

(def install-dir
  (let [clojure-on-path (str/trim (:out (sh "type" "-p" "clojure")))
        f (io/file clojure-on-path)
        f (io/file (.getCanonicalPath f))
        parent (.getParent f)
        parent (.getParent (io/file parent))]
    parent))

(def tools-cp
  (let [files (.listFiles (io/file install-dir "libexec"))
        jar (some #(let [name (.getName %)]
                     (when (and (str/starts-with? name "clojure-tools")
                                (str/ends-with? name ".jar"))
                       %))
                  files)]
    (.getCanonicalPath jar)))

(when (:resolve-tags args)
  (let [f (io/file "deps.edn")]
    (if (.exists f)
      (do (sh java-cmd "-Xms256m" "-classpath" tools-cp
              "clojure.main" "-m" "clojure.tools.deps.alpha.script.resolve-tags"
              "--deps-file=deps.edn")
          (System/exit 0))
      (binding [*out* *err*]
        (println "deps.edn does not exist")
        (System/exit 1)))))

(def config-dir
  (or (System/getenv "CLJ_CONFIG")
      (when-let [xdg-config-home (System/getenv "XDG_CONFIG_HOME")]
        (.getPath (io/file xdg-config-home "clojure")))
      (.getPath (io/file (System/getProperty "user.home") ".clojure"))))

;; If user config directory does not exist, create it
(when-not (.exists (io/file config-dir))
  (.mkdirs config-dir))

(let [config-deps-edn (io/file config-dir "deps.edn")]
  (when-not (.exists config-deps-edn)
    (io/copy (io/file install-dir "example-deps.edn")
             config-deps-edn)))

;; Determine user cache directory
(def user-cache-dir
  (or (System/getenv "CLJ_CACHE")
      (when-let [xdg-config-home (System/getenv "XDG_CACHE_HOME")]
        (.getPath (io/file xdg-config-home "clojure")))
      (.getPath (io/file config-dir ".cpcache"))))

;; Chain deps.edn in config paths. repro=skip config dir
(def config-user
  (when-not (:repro args)
    (.getPath (io/file config-dir "deps.edn"))))

(def config-project "deps.edn")
(def config-paths
  (if (:repro args)
    ;; TODO: we could come up with a setting that also ignores the install-dir for babashka
    [(.getPath (io/file install-dir "deps.edn")) "deps.edn"]
    [(.getPath (io/file install-dir "deps.edn"))
     (.getPath (io/file config-dir "deps.edn"))
     "deps.edn"]))

(def config-str (str/join "," config-paths))

;; Determine whether to use user or project cache
(def cache-dir
  (if (.exists (io/file "deps.edn"))
    ".cpcache"
    user-cache-dir))

;; Construct location of cached classpath file
(def val*
  (str/join "|"
            (concat [(:resolve-aliases args)
                     (:classpath-aliases args)
                     (:all-aliases args)
                     (:jvm-aliases args)
                     (:main-aliases args)
                     (:deps-data args)]
                    (map (fn [config-path]
                           (if (.exists (io/file config-path))
                             config-path
                             "NIL"))
                         config-paths))))

(def ck (-> (sh "cksum" :in val*)
            :out
            (str/split #" ")
            first))

(def libs-file (.getPath (io/file cache-dir (str ck ".libs"))))
(def cp-file (.getPath (io/file cache-dir (str ck ".cp"))))
(def jvm-file (.getPath (io/file cache-dir (str ck ".jvm"))))
(def main-file (.getPath (io/file cache-dir (str ck ".main"))))

(when (:verbose args)
  (println "version      =  1.10.1.478") ;; TODO
  (println "install_dir  =" install-dir)
  (println "config_dir   =" config-dir)
  (println "config_paths =" (str/join " " config-paths))
  (println "cache_dir    =" cache-dir)
  (println "cp_file      =" cp-file)
  (println))

(def stale "true if classpath file is stale"
  (or (:force args)
      (:trace args)
      (not (.exists (io/file cp-file)))
      (let [cp-file (io/file cp-file)]
        (some (fn [config-path]
                (let [f (io/file config-path)]
                  (or (not (.exists f))
                      (> (.lastModified f)
                         (.lastModified cp-file))))) config-paths))))

(def tools-args
  (when (or stale (:pom args))
    (cond-> []
      (not (str/blank? (:deps-data args)))
      (conj "--config-data" (:deps-data args))
      (:resolve-aliases args)
      (conj (str "-R" (:resolve-aliases args)))
      (:classpath-aliases args)
      (conj (str "-C" (:classpath-aliases args)))
      (:jvm-aliases args)
      (conj (str "-J" (:jvm-aliases args)))
      (:main-aliases args)
      (conj (str "-M" (:main-aliases args)))
      (:all-aliases args)
      (conj (str "-A" (:all-aliases args)))
      (:force-cp args)
      (conj "--skip-cp")
      (:trace args)
      (conj "--trace"))))

;;  If stale, run make-classpath to refresh cached classpath
(when (and stale (not (:describe args)))
  (sh java-cmd "-Xms256m"
      "-classpath" tools-cp
      "clojure.main" "-m" "clojure.tools.deps.alpha.script.make-classpath2"
      "--config-user" config-user
      "--config-project" config-project
      "--libs-file" libs-file
      "--cp-file" cp-file
      "--jvm-file" jvm-file
      "--main-file" main-file))

(def cp
  (cond (:describe args) nil
        (not (str/blank? (:force-cp args))) (:force-cp args)
        :else (slurp cp-file)))

(cond (:pom args)
      (sh java-cmd "-Xms256m"
          "-classpath" tools-cp
          "clojure.main" "-m" "clojure.tools.deps.alpha.script.generate-manifest2"
          "--config-user" config-user
          "--config-project" config-project
          "--gen=pom" (str/join " " tools-args))
      (:print-classpath args)
      (println cp)
      (:describe args)
      ::TODO
      )

;; elif "$describe"; then
;; for config_path in "${config_paths[@]}"; do
;; if [[ -f "$config_path" ]]; then
;; path_vector="$path_vector\"$config_path\" "
;; fi
;; done
;; cat <<-END
;; {:version "${project.version}"
;;  :config-files [$path_vector]
;;  :config-user "$config_user"
;;  :config-project "$config_project"
;;  :install-dir "$install_dir"
;;  :config-dir "$config_dir"
;;  :cache-dir "$cache_dir"
;;  :force $force
;;  :repro $repro
;;  :resolve-aliases "$(join '' ${resolve_aliases[@]})"
;;  :classpath-aliases "$(join '' ${classpath_aliases[@]})"
;;  :jvm-aliases "$(join '' ${jvm_aliases[@]})"
;;  :main-aliases "$(join '' ${main_aliases[@]})"
;;  :all-aliases "$(join '' ${all_aliases[@]})"}
;; END
;; elif "$tree"; then
;; exec "$JAVA_CMD" -Xms256m -classpath "$tools_cp" clojure.main -m clojure.tools.deps.alpha.script.print-tree --libs-file "$libs_file"
;; elif "$trace"; then
;; echo "Writing trace.edn"
;; else
;; set -f
;; if [[ -e "$jvm_file" ]]; then
;; jvm_cache_opts=($(cat "$jvm_file"))
;; fi
;; if [[ -e "$main_file" ]]; then
;; main_cache_opts=($(cat "$main_file"))
;; fi
;; exec "$JAVA_CMD" "${jvm_cache_opts[@]}" "${jvm_opts[@]}" "-Dclojure.libfile=$libs_file" -classpath "$cp" clojure.main "${main_cache_opts[@]}" "$@"
;; fi
