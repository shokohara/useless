set -eu
cd front
npm install
npx webpack
cd ..
npm install firebase-tools
npx firebase deploy --token $FIREBASE_TOKEN
