package ObjectSerializer;

import ObjectSerializer.exampleClasses.Person;
import org.w3c.dom.Document;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ObjectCreator {

    enum Choice { JSON, XML }
    enum Save { YES, NO }
    static int objType;

    public static void main(String[] args) throws IOException {
        System.out.println("Server up.");
        int port = 5000;

        //Make objects (students need to do this with user control so TA can see objects aren't hard coded
        Scanner sc = new Scanner(System.in);
        String selection;
        Object object;
        Class<?> clazz;
        ArrayList<Object> toSend = new ArrayList<>();

        while (true) {
            System.out.println("Please select an Object to create or 'exit':");
            System.out.println("\t1. Date\n\t2. Birthdate\n\t3. Name\n\t4. Person\n\t5. Car Database\n\t6. People");
            int choice = sc.nextInt();
            if (choice == 1) {
                selection = "CPSC501W25A4.Date";
                objType = 1;
                break;
            } else if (choice == 2) {
                selection = "CPSC501W25A4.Birthdate";
                objType = 2;
                break;
            } else if (choice == 3) {
                selection = "CPSC501W25A4.Name";
                objType = 3;
                break;
            } else if (choice == 4) {
                selection = "CPSC501W25A4.Person";
                objType = 4;
                break;
            } else if (choice == 5) {
                selection = "CPSC501W25A4.CarDatabase";
                objType = 5;
                break;
            } else if (choice == 6) {
                selection = "CPSC501W25A4.People";
                objType = 6;
                break;
            }
        }

        try {
            clazz = Class.forName(selection);
            object = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();
            int i = 0;
            while ( i < fields.length) {
                try {
                    fields[i].setAccessible(true);

                    Class<?> type = fields[i].getType();

                    if (clazz.equals(Person.class) && type.equals(Person.class)) {
                        fields[i].set(object, buildSpouse(sc, type, (Person) object));
                    } else {
                        handleFieldType(sc, object, fields[i], type);
                    }
                    i++;
                } catch (Exception e) {
                    System.out.println(e);
                    sc = new Scanner(System.in);
                }
            }
            toSend.add(object);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Objects made.");

        Choice choice;
        while (true) {
            System.out.println("Please select serialization format:");
            System.out.println("\t1. JSON\n\t2. XML");
            String sel = sc.next();
            if (sel.equals("1")) {
                choice = Choice.JSON;
                break;
            } else if (sel.equals("2")) {
                choice = Choice.XML;
                break;
            }
        }

        Save save;
        while (true) {
            System.out.println("Would you like to save the output?");
            System.out.println("\t1. YES\n\t2. NO");
            String sel = sc.next();
            if (sel.equals("1")) {
                save = Save.YES;
                break;
            } else if (sel.equals("2")) {
                save = Save.NO;
                break;
            }
        }

        System.out.println("Sending objects.");

        //Send one object at a time with a new socket connection
        for (Object o : toSend) {
            //Create server socket (we only expect one socket connection to be one object)
            Socket clientSocket;
            ServerSocket serverSocket;
            PrintWriter out;
            BufferedReader in;
            try {
                serverSocket = new ServerSocket(port);

                System.out.println("Wait for client.");
                //Accept on server socket to connect to client

                clientSocket = serverSocket.accept();
                System.out.println("Client found.");

                //Get printwriter on socket outputstream
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            String[] s = new String[2];
            if (choice == Choice.JSON) {
                System.out.println("Convert to JSON:\n");

                try {
                    s = stringifyJSON(o);
                    System.out.println(s[0] + "\n" );
                    if (save == Save.YES) {
                        try (PrintWriter fout = new PrintWriter("out_" + objType + ".json")) {
                            fout.println(s[1]);
                        }
                    }
                    System.out.println("Sending JSON " + o.getClass());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("Convert to XML");
                try {
                    s = stringifyXML(o);
                    System.out.println(s[0] + "\n" );
                    if (save == Save.YES) {
                        try (PrintWriter fout = new PrintWriter("out_" + objType + ".xml")) {
                            fout.println(s[1]);
                        }
                    }
                    System.out.println("Sending XML " + o.getClass());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            out.println(s[1]);  // Send the string
            out.flush();

            //println to printwriter to send String s (JSON string of object as one line)
            System.out.println("Send complete!");

            //Close socket (we only expect one socket connection to be one object)
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        }
        System.out.println("Server down.");
    }

    /**
     * A special method that handles the circular reference in the Person class
     * @param sc
     * @param clazz
     * @param spouse
     * @return The spouse field to some Person object that is under construction
     * @throws Exception
     */
    public static Object buildSpouse(Scanner sc, Class<?> clazz, Person spouse) throws Exception {
        Object object = clazz.getDeclaredConstructor().newInstance();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            Class<?> type = f.getType();

            if (type.equals(Person.class)) {
                f.set(object, spouse);
            } else {
                handleFieldType(sc, object, f, type);
            }
        }
        return object;
    }


    /**
     *
     * @param sc
     * @param clazz
     * @return
     * @throws Exception
     */
    public static Object buildObject(Scanner sc, Class<?> clazz) throws Exception {
        Object object = clazz.getDeclaredConstructor().newInstance();
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            int modifiers = f.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }

            Class<?> type = f.getType();

            if (clazz.equals(Person.class) && type.equals(Person.class)) {
                f.set(object, buildSpouse(sc, type, (Person) object));
            } else {
                handleFieldType(sc, object, f, type);
            }
        }
        return object;
    }

    public static Object buildArrayList(Scanner sc, Class<?> elementType) throws Exception {
        ArrayList<Object> list = new ArrayList<>();
        System.out.println("Specify array size:");
        int length = sc.nextInt();

        for (int i = 0; i < length; i++) {
            System.out.println("Building element [" + i + "] of type " + elementType.getSimpleName());
            Object elem = handleArrElemTypes(sc, elementType);;
            list.add(elem);
        }
        return list;
    }

    /**
     *
     * @param sc
     * @param elementType
     * @return
     * @throws Exception
     */
    public static Object buildArray(Scanner sc, Class<?> elementType) throws Exception {
        System.out.println("Specify array size:");
        int length = sc.nextInt();
        Object arr = Array.newInstance(elementType, length);
        System.out.println("Populate the Array of size "+ length + ":" );

        for (int i = 0; i < length; i++) {
            System.out.print("Element [" + i + "] of type " + elementType.getSimpleName() + ": ");
            Object elem = handleArrElemTypes(sc, elementType);
            Array.set(arr, i, elem);
        }
        return arr;
    }

    /**
     * Checks the type of the field and sets the scanner input value or calls the appropriate build method if an Object
     * @param sc
     * @param object
     * @param f
     * @param type
     * @throws Exception
     */
    private static void handleFieldType(Scanner sc, Object object, Field f, Class<?> type) throws Exception {
        System.out.println(type + " " + f.getName() + ":");

        if (type.equals(int.class) || type.equals(Integer.class)) {
            f.set(object, sc.nextInt());
        } else if (type.equals(String.class)) {
            f.set(object, sc.next());
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            f.set(object, sc.nextBoolean());
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            f.set(object, sc.nextDouble());
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            f.set(object, sc.nextFloat());
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            f.set(object, sc.nextLong());
        } else if (type.equals(char.class)) {
            f.set(object, sc.next().charAt(0));
        } else if (type.isArray()) {
            f.set(object, buildArray(sc, type.componentType()));
        } else if (List.class.isAssignableFrom(type)) {
            ParameterizedType listType = (ParameterizedType) f.getGenericType();
            Class<?> elementType = (Class<?>) listType.getActualTypeArguments()[0];
            f.set(object, buildArrayList(sc, elementType));
        } else {
            f.set(object, buildObject(sc, type));
        }
    }

    /**
     * Checks the type of an array and populates the elements from scanner input (probably inefficient to check every time)
     * @param sc
     * @param elementType
     * @return
     * @throws Exception
     */
    private static Object handleArrElemTypes(Scanner sc, Class<?> elementType) throws Exception {
        Object elem;

        if (elementType.equals(int.class) || elementType.equals(Integer.class)) {
            elem = sc.nextInt();
        } else if (elementType.equals(String.class)) {
            elem = sc.next();
        } else if (elementType.equals(boolean.class) || elementType.equals(Boolean.class)) {
            elem = sc.nextBoolean();
        } else if (elementType.equals(double.class) || elementType.equals(Double.class)) {
            elem = sc.nextDouble();
        } else if (elementType.equals(float.class) || elementType.equals(Float.class)) {
            elem = sc.nextFloat();
        } else if (elementType.equals(long.class) || elementType.equals(Long.class)) {
            elem = sc.nextLong();
        } else if (elementType.equals(char.class)) {
            elem = sc.next().charAt(0);
        } else {
            elem = buildObject(sc, elementType);
        }
        return elem;
    }

    /**
     *
     * @param o
     * @return
     * @throws Exception
     */
    public static String[] stringifyJSON(Object o) throws Exception {
        JsonObject jsonObject = Serializer.serializeObject(o);

        // Prepare a factory for a traditional multi-line JSON for printing to terminal
        Map<String, Object> prettyProps = Map.of(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory prettyFactory = Json.createWriterFactory(prettyProps);

        StringWriter prettyFmt = new StringWriter();
        try(JsonWriter jsonWriter = prettyFactory.createWriter(prettyFmt)) {
            jsonWriter.writeObject(jsonObject);
        }

        return new String[] { prettyFmt.toString(), jsonObject.toString() };
    }

    /**
     * @param o
     * @return
     * @throws Exception
     */
    public static String[] stringifyXML(Object o) throws Exception {
        Document xmlObject = SerializerXML.serializeObject(o);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        DOMSource sourceIndented = new DOMSource(xmlObject);
        StringWriter writerIndented = new StringWriter();
        StreamResult resultIndented = new StreamResult(writerIndented);
        Transformer transformerIndented = transformerFactory.newTransformer();
        transformerIndented.setOutputProperty(OutputKeys.INDENT, "yes");
        transformerIndented.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformerIndented.transform(sourceIndented, resultIndented);

        DOMSource sourceOneLine = new DOMSource(xmlObject);
        StringWriter writerOneLine = new StringWriter();
        StreamResult resultOneLine = new StreamResult(writerOneLine);
        Transformer transformerOneLine = transformerFactory.newTransformer();
        transformerOneLine.setOutputProperty(OutputKeys.INDENT, "no");
        transformerOneLine.transform(sourceOneLine, resultOneLine);

        return new String[] { writerIndented.getBuffer().toString(), writerOneLine.getBuffer().toString() };
    }

}
