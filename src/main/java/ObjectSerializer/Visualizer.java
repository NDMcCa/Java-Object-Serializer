package ObjectSerializer;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;

public class Visualizer {

    public static void main(String[] args) {
        boolean firsttime = true;
        System.out.println("Client up.");
        String host = "localhost";
        int port = 5000;
        Socket clientSocket = null;

        while (clientSocket == null) {
            try {

                clientSocket = new Socket(host, port);

            } catch (IOException e) {

                System.out.println("No connection. Retrying in 3 seconds...");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }

            }
        }

        if (clientSocket != null) {
            try {
                firsttime = false;
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Object object = read(in);

                if (object == null) {
                    System.out.println("No object sent.");
                    return;
                }

                System.out.println("Inspecting " + object.getClass());

                Inspector.inspect(object, true);
                // Close socket (we only expect one socket connection to be one object)
                in.close();
                clientSocket.close();
            } catch (Exception e) {
                if (firsttime) e.printStackTrace();
            }
        }

        System.out.println("No objects, Client down.");
    }

    public static Object read(BufferedReader in) throws IOException {
        StringBuilder buffer = new StringBuilder();

        //Get bufferedreader on socket inputstream
        System.out.println("Reading object");

        //Read a line from bufferedreader (this should be your one object) and not null
        String line;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
        }

        String dataString = buffer.toString();

        if (dataString.equals("Error: Failed to create ordered object")) {
            System.out.println(dataString);
            return null;
        }

        System.out.println("New object arrived.");
        String trim = dataString.trim();
        if (trim.startsWith("<?xml") || trim.startsWith("<")) {
            // An unfortunately nested try/catch to suppress the exception in the unit tests
            try {
                //Turn string into XML Document
                // Inspired by https://www.baeldung.com/java-convert-string-xml-dom
                System.out.println("Parsing XML");
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                InputSource is = new InputSource(new StringReader(dataString));
                Document xmlDocument;
                try {
                    xmlDocument = builder.parse(is);
                } catch (SAXException | IOException e) {
                    throw new RuntimeException(e);
                }

                try {
                    System.out.println("Deserializing XML");
                    return DeserializerXML.deserialize(xmlDocument);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            }
        } else {
            //Turn string into JSON Object
            System.out.println("Parsing JSON");
            JsonObject jsonObject = Json.createReader(new StringReader(dataString)).readObject();

            //Change JSON object into my object
            try {
                System.out.println("Deserializing JSON");
                return Deserializer.deserializeObject(jsonObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}