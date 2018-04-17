# eic-docker

#required 
1. docker
2. docker-compose

#build
`docker-compose build`

#deploy
`docker-compose up` 
`docker-compose up -d` to run as daemon.

#remove
`docker-compose down` stops and deletes the containers.

#info
For development purposes this project assumes that the eic-registry `Dockerfile` is located in `../eic-registry`.
For production the latest war file from jenkins is going to be fetched.
