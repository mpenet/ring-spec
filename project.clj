(defproject cc.qbits/ring-spec "0.1.0-SNAPSHOT"
  :description "RING Spec"
  :url "https://github.com/mpenet/ring-spec"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/test.check "0.9.0"]
                 [ring/ring-core "1.6.0-beta4"]
                 [cc.qbits/spex "0.1.2"]]
  :source-paths ["src/clj"]
  ;; :java-source-paths ["src/java"]
  ;; :javac-options ["-source" "1.6" "-target" "1.6" "-g"]
  :global-vars {*warn-on-reflection* true})
