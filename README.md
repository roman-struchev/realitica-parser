# Motivation
This project was created as a parser of Montenegro rental ads from several sources to provide more flexible and convenient access and notifications.

The are several sources of ads:
- https://realtica.com - aggregator of real estate ads Montenegro and other countries. There is a lot of not actual ads and no ability to sort by `update date` attribute and filter by some of the attributes
- https://estitor.com - local Montenegro real estate ads portal

Sources can be extended by adding a new parser implementation

# Web
The project available by https://estate.struchev.site

# Run locally
### Using gradlew
1. Install jdk 21
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
The data storage is a local h2 file database by default