package ObjectSerializer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DeserializerXML {
    public static Object deserialize(Document document) throws Exception {
        assert document != null;
        NodeList objectNodes = document.getElementsByTagName("object");
        Map<String, Object> idToObject = new HashMap<>();

        for (int i = 0; i < objectNodes.getLength(); i++) {
            Element objectElement = (Element) objectNodes.item(i);
            String id = objectElement.getAttribute("id");
            String className = objectElement.getAttribute("class");
            Class<?> clazz = Class.forName(className);
            Object instance;
            if (className.startsWith("[")) {
                int len = Integer.parseInt(objectElement.getAttribute("length"));
                instance = Array.newInstance(clazz.getComponentType(), len);
            } else {
                instance = clazz.getConstructor().newInstance();
            }
            idToObject.put(id, instance);
        }

        for (int i = 0; i < objectNodes.getLength(); i++) {
            Element objectElement = (Element) objectNodes.item(i);
            String id = objectElement.getAttribute("id");
            String className = objectElement.getAttribute("class");
            Object instance = idToObject.get(id);

            if (className.startsWith("[")) {
                NodeList elements = objectElement.getChildNodes();
                int k = 0;
                for (int j = 0; j < elements.getLength(); j++) {
                    Node node = elements.item(j);
                    if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                    Element element = (Element) node;

                    // Very explicit handling of primitives because of the dumb Object wrappers Java uses
                    switch (element.getTagName()) {
                        case "null" -> Array.set(instance, k++, null);
                        case "reference" -> {
                            String refId = element.getTextContent().trim();
                            Array.set(instance, k++, idToObject.get(refId));
                        }
                        case "value" -> {
                            String value = element.getTextContent().trim();

                            // Handle char array specifically because apparently Java can't figure this out otherwise
                            if (instance.getClass().getComponentType() == char.class) {
                                Array.set(instance, k++, value.charAt(0));  // Handle as char
                            }
                        }
                    }
                }
            } else {
                NodeList fields = objectElement.getElementsByTagName("field");
                for (int j = 0; j < fields.getLength(); j++) {
                    Element fieldElement = (Element) fields.item(j);
                    String fieldName = fieldElement.getAttribute("name");
                    String declaringClass = fieldElement.getAttribute("declaringclass");
                    Class<?> clazz = Class.forName(declaringClass);

                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Element valueElement = (Element) fieldElement.getFirstChild();

                    while (valueElement != null && valueElement.getNodeType() != Node.ELEMENT_NODE) {
                        valueElement = (Element) valueElement.getNextSibling();
                    }

                    if (valueElement.getTagName().equals("null")) {
                        field.set(instance, null);
                    } else if (valueElement.getTagName().equals("reference")) {
                        String referenceId = valueElement.getTextContent();
                        field.set(instance, idToObject.get(referenceId));
                    } else if (valueElement.getTagName().equals("value")) {
                        String value = valueElement.getTextContent();
                        Class<?> fieldType = field.getType();

                        if (fieldType == int.class || fieldType == Integer.class) {
                            field.set(instance, Integer.parseInt(value));
                        } else if (fieldType == double.class || fieldType == Double.class) {
                            field.set(instance, Double.parseDouble(value));
                        } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                            field.set(instance, Boolean.parseBoolean(value));
                        } else if (fieldType == long.class || fieldType == Long.class) {
                            field.set(instance, Long.parseLong(value));
                        } else if (fieldType == float.class || fieldType == Float.class) {
                            field.set(instance, Float.parseFloat(value));
                        } else if (fieldType == short.class || fieldType == Short.class) {
                            field.set(instance, Short.parseShort(value));
                        } else if (fieldType == byte.class || fieldType == Byte.class) {
                            field.set(instance, Byte.parseByte(value));
                        } else if (fieldType == char.class || fieldType == Character.class) {
                            field.set(instance, value.charAt(0));
                        } else if (fieldType == String.class) {
                            field.set(instance, value);
                        } else {
                            throw new RuntimeException("Unsupported primitive type: " + fieldType);
                        }
                    }
                }
            }

        }
        return idToObject.get("0");
    }
}
