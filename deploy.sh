#!/bin/bash
cd web-client && npm run build && cd ..
zip -r web-client.zip web-client/build/*
scp web-client.zip $DROPLET:~/web-client.zip
ssh $DROPLET 'cd ~ && rm -rf web-client && unzip web-client.zip'
gw :artifactfinder:shadowDistZip
scp artifactfinder/build/distributions/artifactfinder-shadow.zip $DROPLET:~/artifactfinder.zip
ssh $DROPLET 'cd ~ && rm -rf artifactfinder-shadow && unzip artifactfinder.zip'
