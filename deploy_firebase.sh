set -eu
cd front
npx webpack
cd ..
npm install -g firebase-tools
firebase deploy --token $FIREBASE_TOKEN
