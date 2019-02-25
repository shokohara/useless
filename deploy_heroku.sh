set -eu
APP='jpcounderscorewww'
sbt 'web/docker:publishLocal'
docker tag web:0.1.0-SNAPSHOT registry.heroku.com/$APP/web
heroku container:login
docker push registry.heroku.com/$APP/web
heroku container:release web -a $APP
