set -eu
cd front
npm install
npx webpack
cd ..
npm install -g firebase-tools
firebase deploy --token $FIREBASE_TOKEN
