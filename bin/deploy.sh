lein do clean, uberjar
heroku deploy:jar target/uberjar/wayne-standalone.jar --jdk=21.0.2 --app nameless-plains-89326

