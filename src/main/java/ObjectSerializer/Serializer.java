package ObjectSerializer;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

public class Serializer {
    public static IdentityHashMap<Object, Integer> objectIds = new IdentityHashMap<>();
    public static List<JsonObject> serializedObjects = new ArrayList<>();
    public static int idCounter = 0;

    /**
     *
     * @param object
     * @return
     * @throws Exception
     */
    public static JsonObject serializeObject(Object object) throws Exception {
        objectIds.clear();
        serializedObjects.clear();
        idCounter = 0;

        serializeInner(object);

        JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
        for (JsonObject jsonObj : serializedObjects) {
            arrBuilder.add(jsonObj);
        }

        return Json.createObjectBuilder().add("objects", arrBuilder).build();
    }

    /**
     *
     * @param object
     * @return
     * @throws Exception
     */
    private static JsonObject SerializeVR(Object object) throws Exception {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        if (object == null) {
            builder.add("reference", "null");
        } else if (isPrimitiveOrWrapper(object.getClass())) {
            builder.add("value", String.valueOf(object));
        } else {
            if (!objectIds.containsKey(object)) {
                serializeInner(object);
            }
            builder.add("reference", String.valueOf(objectIds.get(object)));
        }
        return builder.build();
    }

    /**
     *
     * @param object
     * @throws Exception
     */
    private static void serializeInner(Object object) throws Exception {
        if (object == null || objectIds.containsKey(object)) return;

        int id = idCounter++;
        objectIds.put(object, id);

        Class<?> clazz = object.getClass();
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("class", clazz.getName());
        builder.add("id", String.valueOf(id));

        if (isPrimitiveOrWrapper(object)) {
            builder.add("type", "value");
            builder.add("value", String.valueOf(object));
        } else if (clazz.isArray()) {
            builder.add("type", "array");
            builder.add("length", Array.getLength(object));

            JsonArrayBuilder arrBuilder = Json.createArrayBuilder();
            for (int i = 0; i < Array.getLength(object); i++) {
                Object element = Array.get(object, i);
                if (object.getClass().getComponentType().isPrimitive()) {
                    arrBuilder.add(String.valueOf(element));
                } else {
                    arrBuilder.add(SerializeVR(element));
                }
            }

            builder.add("entries", arrBuilder);
        } else {
            builder.add("type", "object");
            JsonArrayBuilder arrBuilder = Json.createArrayBuilder();

            Class<?> currentClazz = clazz;
            while (currentClazz != null) {
                for (Field field : currentClazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object val = field.get(object);
                    JsonObjectBuilder fieldJson = Json.createObjectBuilder()
                            .add("name", field.getName())
                            .add("declaringclass", field.getDeclaringClass().getName());

                    if (val == null) {
                        fieldJson.add("reference", "null");

                    } else if (val.getClass().isArray()) {
                        fieldJson.add("type", "array");
                        fieldJson.add("length", Array.getLength(val));

                        JsonArrayBuilder entriesBuilder = Json.createArrayBuilder();
                        for (int i = 0; i < Array.getLength(val); i++) {
                            Object element = Array.get(val, i);
                            if (val.getClass().getComponentType().isPrimitive()) {
                                JsonObjectBuilder valueWrapper = Json.createObjectBuilder();
                                valueWrapper.add("value", String.valueOf(element));
                                entriesBuilder.add(valueWrapper);
                            } else {
                                entriesBuilder.add(SerializeVR(element));
                            }
                        }

                        fieldJson.add("entries", entriesBuilder);

                    } else if (isPrimitiveOrWrapper(val)) {

                        fieldJson.add("value", String.valueOf(val));

                    } else {
                        // normal object
                        if (!objectIds.containsKey(val)) {
                            serializeInner(val);
                        }
                        fieldJson.add("reference", String.valueOf(objectIds.get(val)));
                    }
                    arrBuilder.add(fieldJson);
                }
                currentClazz = currentClazz.getSuperclass();
            }
            builder.add("fields", arrBuilder);
        }
        JsonObject jsonObj = builder.build();
        serializedObjects.add(jsonObj);
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
