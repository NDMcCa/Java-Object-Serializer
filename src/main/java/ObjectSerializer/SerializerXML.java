package ObjectSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class SerializerXML {
    private static IdentityHashMap<Object, Integer> objectIds = new IdentityHashMap<>();
    private static int idCounter = 0;
    public static List<Element> serializedObjects = new ArrayList<>();

    public static Document serializeObject(Object object) throws Exception {
        objectIds.clear();
        serializedObjects.clear();
        idCounter = 0;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();


        Element root = doc.createElement("serialized");
        doc.appendChild(root);

        serializeInner(object, doc);

        for (Element elem : serializedObjects) {
            root.appendChild(elem);
        }
        return doc;
    }

    /**
     *
     * @param object
     * @return
     * @throws Exception
     */
    public static Node serializeVR(Object object, Document doc) throws Exception {
        if (object == null) {
            return doc.createElement("null");
        } else if (isPrimitiveOrWrapper(object) || object instanceof String) {
            Element valElem = doc.createElement("value");
            valElem.setTextContent(object.toString());
            return valElem;
        } else {
            if (!objectIds.containsKey(object)) {
                serializeInner(object, doc);
            }
            Element ref = doc.createElement("reference");
            ref.setTextContent(String.valueOf(objectIds.get(object)));
            return ref;
        }
    }

    /**
     *
     * @param object
     * @throws Exception
     */
    private static void serializeInner(Object object, Document doc) throws Exception {
        if (object == null || objectIds.containsKey(object)) return;

        int id = idCounter++;
        objectIds.put(object, id);
        Class<?> clazz = object.getClass();
        Element objectElement = doc.createElement("object");
        objectElement.setAttribute("class", clazz.getName());
        objectElement.setAttribute("id", String.valueOf(id));

        if (clazz.isArray()) {
            objectElement.setAttribute("length", String.valueOf(Array.getLength(object)));
            for (int i = 0; i < Array.getLength(object); i++) {
                Object element = Array.get(object, i);
                objectElement.appendChild(serializeVR(element, doc));
            }
        } else {
            for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
                for (Field field : c.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    field.setAccessible(true);

                    Element fieldElement = doc.createElement("field");
                    fieldElement.setAttribute("name", field.getName());
                    fieldElement.setAttribute("declaringclass", field.getDeclaringClass().getName());
                    fieldElement.appendChild(serializeVR(field.get(object), doc));
                    objectElement.appendChild(fieldElement);
                }
            }
        }
        serializedObjects.add(objectElement);
    }


    /**
     *
     * @param object
     * @return
     */
    private static boolean isPrimitiveOrWrapper(Object object) {
        return object.getClass().isPrimitive() ||
                object.getClass() == Integer.class || object.getClass() == Double.class || object.getClass() == Boolean.class ||
                object.getClass() == Long.class || object.getClass() == Float.class || object.getClass() == Short.class ||
                object.getClass() == Byte.class || object.getClass() == Character.class || object instanceof String;
    }
}