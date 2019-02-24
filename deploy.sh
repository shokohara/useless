set -eu
cd front
npm install
npx webpack
sbt 'web/docker:publishLocal'
docker tag web:0.1.0-SNAPSHOT registry.heroku.com/fierce-depths-21005/web
heroku container:login
docker push registry.heroku.com/fierce-depths-21005/web
heroku container:release web -a fierce-depths-21005
