lein do clean, uberjar
heroku deploy:jar target/uberjar/wayne-0.1.0-SNAPSHOT-standalone.jar --jdk=21.0.2
