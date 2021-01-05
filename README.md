# amz-watchlist project

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

### Starting local mongodb
```shell script
docker run -ti --rm -p 27017:27017 mongo:4.0
```

## Environment setup
All profiles other than dev requires following environment variables set:
- MONGO_DB_USER
- MONGO_DB_PASSWORD
- MONGO_DB_HOST
- MONGO_DB_PORT

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `amazon-scraper-0.1-SNAPSHOT-runner.jar` file in the `/target` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/lib` directory.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/amazon-scraper-0.1-SNAPSHOT-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Pnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Pnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/amazon-scraper-0.1-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.html.
