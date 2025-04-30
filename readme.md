# Java Object Serializer
This is a basic, proof-of-concept Java object serializer that shows how such a problem can be approached using reflection in Java. The serializer (and deserializer) is capable of handling both JSON and XML formats. To simulate a realistic use case, the serialized string is sent over a socket to simulate sending object data over a network. 

This program utilizes the following 3rd party libraries for functionality that are included in `pom.xml`:
- Glassfish Javax JSON
- W3C DOM

It also utilizes the following libraries for mocking/testing
- JUnit Jupiter (JUnit 5)
- Mockito

## Setup
`Visualizer.java` and `ObjectCreator.java` must either be run in an IDE project that has the glassfish `javax.json` dependency installed or that dependency, as well as `Serializer.class`, `Deserializer.class`, and `Inspector.class`, must be in the classpath if compiled and run manually in the command line.

Ensure that all the example classes to be serialized are compiled into `.class` files, otherwise the `ObjectCreator` will not work, to do so open a terminal window in the `src/main/java/CPSC501W25A4` directory of the project, then run:
```shell
javac Date.java Birthdate.java Name.java Person.java People.java Car.java
```

The necessary classes are now set up and the `ObjectCreator.java` and `Visualizer.java` can be run.

## Use
When running any of the programs in this package or the unit tests, ensure that the following VM flags are added to the command line or IDE build/run profile if applicable
```shell
--add-opens java.base/java.util=ALL-UNNAMED
--add-opens java.base/java.lang=ALL-UNNAMED
```
Failure to do so could lead to an `IllegalAccessException`
### ObjectCreator
To use, from a running instance of `ObjectCreator`, specify an option from the choices provided, then populate the fields with appropriate values. Once finished the program will queue up the objects to send and open a socket and wait for an incoming connection. At this point if an instance of `Visualizer` is running, it will serialize the first object to send, then send it and then close the socket. As long as there are still more objects waiting to be sent this process will repeat. Once finished `ObjectCreator` will terminate. 

For arrays (and ArrayList), the user will be required to set the exact size of the array that they want to populate. When setting up a `Name` object for example, the user must state how long the name is first and then insert each character individually into the character array (this is not the optimal way but is best to allow for more generalized array building code).

For any `int` field, do not set a value beyond the max of the `int` type.

There is an option to allow for the output of the serialized data in a filed that can be used for inspection. It will output in the single-line format by default as this is consistent with what is sent over the TCP socket connection.

### Visualizer
To use, simply run the program and it will connect to the server and listen on the socket for incoming serialized object strings, once it receives one, it will deserialize it and then proceed to run it through `Inspector` to get an in-depth look at the object.
