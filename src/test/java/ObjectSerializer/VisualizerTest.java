package ObjectSerializer;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import ObjectSerializer.exampleClasses.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VisualizerTest {

    //Test public field object read(Socket socket)
    @Test
    public void publicFields() throws IOException {
        Date date = new Date();
        date.year = 2025;
        date.month = 1;
        date.day = 2;

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_1.json");

        Date result = (Date) Visualizer.read(reader);
        assert result != null;

        assertEquals(date.year, result.year);
        assertEquals(date.month, result.month);
        assertEquals(date.day, result.day);
    }


    //Test private field object (no setters) read(Socket socket)
    @Test
    public void privateFields() throws IOException, IllegalAccessException {
        Birthdate bDate = new Birthdate();

        Class<?> clazz = bDate.getClass();
        int i = 0;
        int[] valsT1 = { 1995, 1, 2 };
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(bDate, valsT1[i++]);
        }

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_2.json");

        Birthdate result = (Birthdate) Visualizer.read(reader);
        assert result != null;

        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            assertEquals(field.get(bDate), field.get(result));
        }
    }

    //Test circular reference read(Socket socket)
    @Test
    public void circularReference() throws IOException, IllegalAccessException {

        // A Person object has a "spouse" field that is another Person object that has a reciprocal (circular) relationship
        Person bob = makeBob();

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_4.json");

        Person result = (Person) Visualizer.read(reader);
        assert result != null;

        // Testing each field, if each field is the same, then the reciprocal relationship is correct as well
        // Names
        assertEquals(bob.personName.firstName.length, result.personName.firstName.length);
        assertEquals(bob.personName.lastName.length, result.personName.lastName.length);
        assertEquals(bob.spouse.personName.firstName.length, result.spouse.personName.firstName.length);
        assertEquals(bob.spouse.personName.lastName.length, result.spouse.personName.lastName.length);

        for (int i = 0; i < bob.personName.firstName.length; i++) {
            assertEquals(result.personName.firstName[i], bob.personName.firstName[i]);
        }

        for (int i = 0; i < bob.personName.lastName.length; i++) {
            assertEquals(result.personName.lastName[i], bob.personName.lastName[i]);
        }

        for (int i = 0; i < bob.spouse.personName.firstName.length; i++) {
            assertEquals(result.spouse.personName.firstName[i], bob.spouse.personName.firstName[i]);
        }

        for (int i = 0; i < bob.spouse.personName.lastName.length; i++) {
            assertEquals(result.spouse.personName.lastName[i], bob.spouse.personName.lastName[i]);
        }

        // Cars
        assertEquals(bob.cars.length, result.cars.length);
        assertEquals(bob.spouse.cars.length, result.spouse.cars.length);

        for (int i = 0; i < bob.cars.length; i++) {
            assertEquals(result.cars[i].VIN, bob.cars[i].VIN);
        }

        for (int i = 0; i < bob.spouse.cars.length; i++) {
            assertEquals(result.spouse.cars[i].VIN, bob.spouse.cars[i].VIN);
        }

        // Expiry date (for drivers license ID or something)
        assertEquals(bob.expiryDate.year, result.expiryDate.year);
        assertEquals(bob.expiryDate.month, result.expiryDate.month);
        assertEquals(bob.expiryDate.day, result.expiryDate.day);
        assertEquals(bob.spouse.expiryDate.year, result.spouse.expiryDate.year);
        assertEquals(bob.spouse.expiryDate.month, result.spouse.expiryDate.month);
        assertEquals(bob.spouse.expiryDate.day, result.spouse.expiryDate.day);

        Class<?> clazz = bob.birthdate.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            assertEquals(field.get(bob.birthdate), field.get(result.birthdate));
            assertEquals(field.get(bob.spouse.birthdate), field.get(result.spouse.birthdate));
        }

        assertTrue((bob.spouse.spouse == bob) && (result.spouse.spouse == result));
    }

    //Test primitive array read(Socket socket)

    @Test
    public void primArray() throws IOException {
        // Configure classes
        Name name = new Name();
        name.firstName = new char[]{'B', 'o', 'b'};
        name.lastName = new char[]{'R', 'o', 's', 's'};

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_3.json");

        Name result = (Name) Visualizer.read(reader);
        assert result != null;

        assertEquals(name.firstName.length, result.firstName.length);
        assertEquals(name.lastName.length, result.lastName.length);

        for (int i = 0; i < name.firstName.length; i++) {
            assertEquals(result.firstName[i], name.firstName[i]);
        }

        for (int i = 0; i < name.lastName.length; i++) {
            assertEquals(result.lastName[i], name.lastName[i]);
        }
    }
    //Test object array read(Socket socket)

    @Test
    public void objectArray() throws IOException {
        CarDatabase carDatabase = new CarDatabase();
        Car car1 = new Car();
        car1.VIN = 11223344;
        Car car2 = new Car();
        car2.VIN = 44332211;
        carDatabase.cars = new Car[]{car1, car2};

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_5.json");

        CarDatabase result = (CarDatabase) Visualizer.read(reader);
        assert result != null;

        assertEquals(carDatabase.cars.length, result.cars.length);

        for (int i = 0; i < carDatabase.cars.length; i++) {
            assertEquals(carDatabase.cars[i].VIN, carDatabase.cars[i].VIN);
        }

    }
    //Test collection read(Socket socket)

    @Test
    public void collection() throws IOException, IllegalAccessException {
        // Configure classes
        Person bob = makeBob();
        People people = new People();
        people.people = new ArrayList<>();
        people.people.add(bob);

        // Reading the predetermined output with a mocked socket
        BufferedReader reader = mockSocketAndReadJson("out_6.json");

        People result = (People) Visualizer.read(reader);
        assert result != null;

        // Testing each field of the person in the people ArrayList, if each field is the same, then the reciprocal relationship is correct as well
        // If this works than the ArrayList can serialization can be presumed correct for more inputs as the only difference would be the internal array of references which has already been tested
        // Names
        assertEquals(people.people.getFirst().personName.firstName.length, result.people.getFirst().personName.firstName.length);
        assertEquals(people.people.getFirst().personName.lastName.length, result.people.getFirst().personName.lastName.length);
        assertEquals(people.people.getFirst().spouse.personName.firstName.length, result.people.getFirst().spouse.personName.firstName.length);
        assertEquals(people.people.getFirst().spouse.personName.lastName.length, result.people.getFirst().spouse.personName.lastName.length);

        for (int i = 0; i < people.people.getFirst().personName.firstName.length; i++) {
            assertEquals(result.people.getFirst().personName.firstName[i], people.people.getFirst().personName.firstName[i]);
        }

        for (int i = 0; i < people.people.getFirst().personName.lastName.length; i++) {
            assertEquals(result.people.getFirst().personName.lastName[i], people.people.getFirst().personName.lastName[i]);
        }

        for (int i = 0; i < people.people.getFirst().spouse.personName.firstName.length; i++) {
            assertEquals(result.people.getFirst().spouse.personName.firstName[i], people.people.getFirst().spouse.personName.firstName[i]);
        }

        for (int i = 0; i < people.people.getFirst().spouse.personName.lastName.length; i++) {
            assertEquals(result.people.getFirst().spouse.personName.lastName[i], people.people.getFirst().spouse.personName.lastName[i]);
        }

        // Cars
        assertEquals(people.people.getFirst().cars.length, result.people.getFirst().cars.length);
        assertEquals(people.people.getFirst().spouse.cars.length, result.people.getFirst().spouse.cars.length);

        for (int i = 0; i < people.people.getFirst().cars.length; i++) {
            assertEquals(result.people.getFirst().cars[i].VIN, people.people.getFirst().cars[i].VIN);
        }

        for (int i = 0; i < people.people.getFirst().spouse.cars.length; i++) {
            assertEquals(result.people.getFirst().spouse.cars[i].VIN, people.people.getFirst().spouse.cars[i].VIN);
        }

        // Expiry date (for drivers license ID or something)
        assertEquals(people.people.getFirst().expiryDate.year, result.people.getFirst().expiryDate.year);
        assertEquals(people.people.getFirst().expiryDate.month, result.people.getFirst().expiryDate.month);
        assertEquals(people.people.getFirst().expiryDate.day, result.people.getFirst().expiryDate.day);
        assertEquals(people.people.getFirst().spouse.expiryDate.year, result.people.getFirst().spouse.expiryDate.year);
        assertEquals(people.people.getFirst().spouse.expiryDate.month, result.people.getFirst().spouse.expiryDate.month);
        assertEquals(people.people.getFirst().spouse.expiryDate.day, result.people.getFirst().spouse.expiryDate.day);

        Class<?> clazz = people.people.getFirst().birthdate.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            assertEquals(field.get(people.people.getFirst().birthdate), field.get(result.people.getFirst().birthdate));
            assertEquals(field.get(people.people.getFirst().spouse.birthdate), field.get(result.people.getFirst().spouse.birthdate));
        }

        assertTrue((people.people.getFirst().spouse.spouse == people.people.getFirst()) && (result.people.getFirst().spouse.spouse == result.people.getFirst()));
    }
    /**
     * A defined method that produces a person pair to be used in testing; allows the actual unit test to be more concise
     * @return bob
     * @throws IllegalAccessException
     */
    private Person makeBob() throws IllegalAccessException {
        Car carB = new Car();
        carB.VIN = 12345;

        Name nameB = new Name();
        nameB.firstName = new char[]{'B', 'o', 'b'};
        nameB.lastName = new char[]{'B', 'o', 'b', 'b', 'y'};

        Date dateB = new Date();
        dateB.year = 2026;
        dateB.month = 1;
        dateB.day = 2;

        Person bob = new Person();
        bob.cars = new Car[]{ carB };
        bob.expiryDate = dateB;
        bob.personName = nameB;

        Car carA = new Car();
        carA.VIN = 54321;

        Name nameA = new Name();
        nameA.firstName = new char[]{'A', 'l', 'i', 'c', 'e'};
        nameA.lastName = new char[]{'B', 'o', 'b', 'b', 'y'};

        Date dateA = new Date();
        dateA.year = 2027;
        dateA.month = 2;
        dateA.day = 1;

        Person alice = new Person();
        alice.cars = new Car[]{ carA };
        alice.expiryDate = dateA;
        alice.personName = nameA;

        int[] valsB = { 1960, 1, 2 };
        Birthdate bDateB = new Birthdate();

        int[] valsA = { 1963, 2, 1 };
        Birthdate bDateA = new Birthdate();

        Class<?> clazz = bDateB.getClass();
        int j = 0;
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            field.set(bDateB, valsB[j]);
            field.set(bDateA, valsA[j++]);
        }

        bob.birthdate = bDateB;
        alice.birthdate = bDateA;

        bob.spouse = alice;
        alice.spouse = bob;

        return bob;
    }

    /**
     * A defined method that sets up the mocked socket and the BufferedReader and reads a pre-determined correct JSON file; would use @BeforeEach except that it would be
     * hard to determine which JSON file to open as the testing order is random, this way it can be controlled by the unit test.
     * @param name
     * @return A BufferedReader
     * @throws IOException
     */
    private BufferedReader mockSocketAndReadJson(String name) throws IOException {
        String json;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());
        Socket socket = mock(Socket.class);
        when(socket.getInputStream()).thenReturn(inputStream);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        return reader;
    }

}