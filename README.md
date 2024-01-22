# Motivation
The https://realtica.com is an aggregator of ads in Montenegro and other countries, but there is a lot of not actual ads and no ability to sort by `update date` attribute and filter by some of the attributes.

This project was created as a parser of Montenegro rental ads from https://realtica.com to provide more flexible and convenient access to ads.

# Web
The project available by http://realitica.struchev.site

# Run locally
### Using gradlew
1. Install jdk 17
2. Execute
```shell
./gradlew bootRun
```
3. Open http://localhost
### Using docker
1. Install docker
2. Execute
```shell
docker run -v $PWD/data:./data -p 80:80 --rm romanew/realitica:latest
```
3. Open http://localhost

# Details
The data storage is a local h2 file database by default.
When the application starts, it goes to load ads from https://realitica.com. Also, ads reload every 6 hours