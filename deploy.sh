#Copyright 2019 Google, Inc.
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#See the License for the specific language governing permissions and
#limitations under the License.
#!/bin/bash
cd web-client && npm run build && cd ..
zip -r web-client.zip web-client/build/*
scp web-client.zip $DROPLET:~/web-client.zip
ssh $DROPLET 'cd ~ && rm -rf web-client && unzip web-client.zip'
gw :artifactfinder:shadowDistZip
scp artifactfinder/build/distributions/artifactfinder-shadow.zip $DROPLET:~/artifactfinder.zip
ssh $DROPLET 'cd ~ && rm -rf artifactfinder-shadow && unzip artifactfinder.zip'
