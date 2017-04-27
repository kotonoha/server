# Overview

Kotonoha is a yet another flashcard software for learning japanese words.

Unlike anki, it is specialized only for japanese and has focus on quick new word addition.

# Dependencies

* Mongodb
* Node.JS 6.0 (7.0 are not supported until https://github.com/sbt/sbt-js-engine/issues/52)

# Local installation

```
$ sbt
> compile
> test
> jetty:start
```

Tests should pass cleanly.

After the test pass, navigate to http://localhost:8080.

When you create a server the admin account is created automatically.
Default username/password is admin@(none) / admin,
but you can change them or setup in a configuration file.
