set -eu
npm install -g firebase-tools
firebase deploy --token $FIREBASE_TOKEN
APP='jpcounderscorewww'
cd web/front
npm install
npx webpack
cd ../..
sbt 'web/docker:publishLocal'
docker tag web:0.1.0-SNAPSHOT registry.heroku.com/$APP/web
heroku container:login
docker push registry.heroku.com/$APP/web
heroku container:release web -a $APP
