# Prereqs
To build examples, you'll need:

- Clojure cli tools
- (Example 7 only) emscripten installed.

# Build

Build all examples:

```bash
./buildall.sh
```
Alternatively, you can build any particular example:
```bash
cd day1/ex01/
clj -A:prod
```

# REPL

To launch cider repl, just use the :dev alias:
```bash
clj -A:dev
```
You'll have to launch figwheel inside the cider repl after connecting:
```clojure
(do (require '[figwheel.main.api :as api]) (api/start :dev))
```

# Serve

python:

```bash
python -m SimpleHTTPServer 8080
```
Emacs (using simple-httpd):
```emacs
emacsclient --no-wait --eval "(progn (require 'simple-httpd) (httpd-serve-directory \".\"))"
```
Or of course you can just execute `M-x httpd-serve-diretory` inside emacs.

Then just nav to an example in your browser. For example:
```bash
brave-browser http://localhost:8080/day1/ex03/resources/public/
```
