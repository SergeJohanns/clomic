# Clomic

[![Clojure CI](https://github.com/SergeJohanns/clomic/workflows/Clojure%20CI/badge.svg)](https://github.com/SergeJohanns/clomic/actions?query=workflow%3A%22Clojure+CI%22)

A telegram bot that automatically sends you new issues of webcomics you follow.

## Usage
### Bot setup
First, create a new telegram bot and obtain a token
[the usual way](https://core.telegram.org/bots).

### Configuration
Create a config file `.config/clomic` called `config.yaml`. In the file should
be a key 'feeds', which contains a list of feeds. A feed is defined as a key,
which will be the name the user sees, associated with two fields: 'url', and
'parser'. 'url' should point to the url used to fetch the content, which is
probably the url of the comic's RSS/Atom feed, although a parser can
technically use any url. 'parser' should be the name of a content parser, which
are explained [below](#Content-parsers). The only built-in parser is
'xkcd_parser', but you can add custom ones as well. Overall, `config.yaml` will
look something like the example below.

```yaml
feeds:
    xkcd:
        url: https://xkcd.com/atom.xml
        parser: xkcd_parser
    not_xkcd:
        url: https://not-xkcd.org/rss.xml
        parser: not_xkcd_parser
% etc.
```

Additional parsers can be stored in `.config/clomic/parsers`, and should be used
in the config with the same name as the file, minus the `.clj` extension. Custom
parsers always override built-in parsers with the same name.

### Running
First, set the environment variable `BOT_TOKEN` to the token you were assigned
when making a telegram bot with `BOT_TOKEN='your-secret-token-here'`. Then,
install the dependencies and run the project with
[Leiningen](https://leiningen.org/) using the command `lein run`.

### The bot
The bot has three core commands: /feeds, which shows an overview of all of the
feeds that the bot knows, and /subscribe and /unsubscribe, which both take the
name of a feed after them and will subscribe/unsubscribe, the chat the command
was posted in (which can also be a private chat) to/from the given feed. It also
has a /start and /help command to help explain usage.

The bot will message users new content from feeds they are subscribed to every
10 minutes once it is running, and immediately every time it starts up.

### Content parsers
A content parser is a clojure source file which defines a function called
'parser'. This function takes a url as an argument, which is the url of the
content feed, and returns a list of maps with (at least) three keys:

* `:timestamp`: The time at which the content was posted. Must be at or before
  the time at which the content was retrieved, to prevent repeated updates.
* `:image`: The image of the comic.
* `:alt-text`: The 'alt-text' or 'hovertext' of the comic.

For instance, the built-in parser for `xkcd` is:

```clojure
(require '[feedparser-clj.core :refer [parse-feed]]
         '[clojure.zip :as zip]
         '[clojure.xml :as xml])
(import java.io.ByteArrayInputStream)

(defn parser [url]
  (let [zip-str
        (fn [s] (zip/xml-zip (xml/parse (ByteArrayInputStream. (.getBytes s)))))
        convert
        (fn [{timestamp :updated-date {summary :value} :description}]
          (let [[{fields :attrs}] (zip-str summary)]
            {:timestamp timestamp
             :image (fields :src)
             :alt-text (fields :alt)}))]
    (map convert ((parse-feed url) :entries))))
```

## License

Copyright © 2020 Serge Johanns

Distributed under the Eclipse Public License either version 2.0 or (at
your option) any later version.
