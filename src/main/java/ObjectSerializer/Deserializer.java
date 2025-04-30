package ObjectSerializer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

public class Deserializer {

    public static Object deserializeObject(JsonObject jsonObject) throws Exception {
        assert jsonObject != null;
        JsonArray objectsArray = jsonObject.getJsonArray("objects");
        Map<String, Object> idToObject = new HashMap<>();

        for (JsonValue object : objectsArray) {
            JsonObject objectJson = object.asJsonObject();
            String id = objectJson.getString("id");
            String type = objectJson.getString("type");
            String className = objectJson.getString("class");
            Class<?> clazz = Class.forName(className);
            Object instance;
            if (type.equals("object")) {
                instance = clazz.getConstructor().newInstance();
            } else if (type.equals("array")) {
                int len = objectJson.getInt("length");
                instance = Array.newInstance(clazz.getComponentType(), len);
            } else {
                throw new RuntimeException("Unknown object type: " + type);
            }
            idToObject.put(id, instance);
        }
        for (JsonValue object : objectsArray) {
            JsonObject objectJson = object.asJsonObject();
            String id = objectJson.getString("id");
            Object instance = idToObject.get(id);

            if (objectJson.getString("type").equals("object")) {
                JsonArray fieldsArray = objectJson.getJsonArray("fields");

                for (JsonValue field : fieldsArray) {
                    JsonObject fieldJson = field.asJsonObject();
                    String fieldName = fieldJson.getString("name");
                    String declaringClass = fieldJson.getString("declaringclass");
                    Class<?> deClazz = Class.forName(declaringClass);

                    Field declaredField = deClazz.getDeclaredField(fieldName);
                    int modifiers = declaredField.getModifiers();
                    if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers)) {
                        continue;
                    }

                    Class<?> fieldType = declaredField.getType();
                    declaredField.setAccessible(true);

                    if (fieldType.isArray()) {
                        int length = fieldJson.getInt("length");
                        JsonArray entriesArray = fieldJson.getJsonArray("entries");
                        Object arrayInstance = Array.newInstance(fieldType.getComponentType(), length);

                        handleArray(entriesArray, fieldType, idToObject, arrayInstance);
                        declaredField.set(instance, arrayInstance);
                    } else {
                        if (fieldJson.containsKey("value")) {
                            String value = fieldJson.getString("value");
                            if (fieldType == int.class || fieldType == Integer.class) {
                                declaredField.set(instance, Integer.parseInt(value));
                            } else if (fieldType == double.class || fieldType == Double.class) {
                                declaredField.set(instance, Double.parseDouble(value));
                            } else if (fieldType == boolean.class || fieldType == Boolean.class) {
                                declaredField.set(instance, Boolean.parseBoolean(value));
                            } else if (fieldType == long.class || fieldType == Long.class) {
                                declaredField.set(instance, Long.parseLong(value));
                            } else if (fieldType == float.class || fieldType == Float.class) {
                                declaredField.set(instance, Float.parseFloat(value));
                            } else if (fieldType == short.class || fieldType == Short.class) {
                                declaredField.set(instance, Short.parseShort(value));
                            } else if (fieldType == byte.class || fieldType == Byte.class) {
                                declaredField.set(instance, Byte.parseByte(value));
                            } else if (fieldType == char.class || fieldType == Character.class) {
                                declaredField.set(instance, value.charAt(0));
                            } else if (fieldType == String.class) {
                                declaredField.set(instance, value);
                            } else {
                                throw new RuntimeException("Unsupported primitive type: " + fieldType);
                            }
                        } else {
                            String referenceId = fieldJson.getString("reference");
                            if (referenceId.equals("null")) {
                                declaredField.set(instance, null);
                            } else {
                                Object referencedObject = idToObject.get(referenceId);
                                declaredField.set(instance, referencedObject);
                            }
                        }
                    }
                }
            } else {
                JsonArray entriesArray = objectJson.getJsonArray("entries");
                Class<?> componentType = instance.getClass().getComponentType();

                handleArray(entriesArray, componentType, idToObject, entriesArray);
            }
        }
        return idToObject.get("0");
    }

    /**
     * Handles the deserialization of elements in an array object
     * @param entriesArray
     * @param fieldType
     * @param idToObject
     * @param arrayInstance
     */
    private static void handleArray(JsonArray entriesArray, Class<?> fieldType, Map<String, Object> idToObject, Object arrayInstance) {
        for (int i = 0; i < entriesArray.size(); i++) {
            JsonObject elem = entriesArray.getJsonObject(i);
            String valStr = elem.containsKey("value") ? elem.getString("value") : null;
            String refStr = elem.containsKey("reference") ? elem.getString("reference") : null;
            Object elementValue = null;

            if (valStr != null) {
                if (fieldType.getComponentType() == int.class || fieldType.getComponentType() == Integer.class) {
                    elementValue = Integer.parseInt(valStr);
                } else if (fieldType.getComponentType() == double.class || fieldType.getComponentType() == Double.class) {
                    elementValue = Double.parseDouble(valStr);
                } else if (fieldType.getComponentType() == boolean.class || fieldType.getComponentType() == Boolean.class) {
                    elementValue = Boolean.parseBoolean(valStr);
                } else if (fieldType.getComponentType() == long.class || fieldType.getComponentType() == Long.class) {
                    elementValue = Long.parseLong(valStr);
                } else if (fieldType.getComponentType() == float.class || fieldType.getComponentType() == Float.class) {
                    elementValue = Float.parseFloat(valStr);
                } else if (fieldType.getComponentType() == short.class || fieldType.getComponentType() == Short.class) {
                    elementValue = Short.parseShort(valStr);
                } else if (fieldType.getComponentType() == byte.class || fieldType.getComponentType() == Byte.class) {
                    elementValue = Byte.parseByte(valStr);
                } else if (fieldType.getComponentType() == char.class || fieldType.getComponentType() == Character.class) {
                    elementValue = valStr.charAt(0);
                } else if (fieldType.getComponentType() == String.class) {
                    elementValue = valStr;
                } else {
                    throw new RuntimeException("Unsupported array element type: " + fieldType.getComponentType());
                }
            } else if (refStr != null) {
                if (refStr.equals("null")) {
                    elementValue = null;
                } else {
                    elementValue = idToObject.get(refStr);
                }
            }
            Array.set(arrayInstance, i, elementValue);
        }
    }
}
