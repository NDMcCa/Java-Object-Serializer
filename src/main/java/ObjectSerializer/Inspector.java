package ObjectSerializer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

/**
 * CPSC 501
 * Inspector starter class
 *
 * @author Jonathan Hudson
 */
public class Inspector {
    private static Map<Integer, Object> idToObject;

    public static void inspect(Object obj, boolean recursive) throws IllegalAccessException {
        idToObject = new HashMap<>();
        Class<?> c = obj.getClass();
        inspectClass(c, obj, recursive, 0);
    }

    private static void inspectClass(Class<?> c, Object obj, boolean recursive, int depth) throws IllegalAccessException {
        int hash = obj.hashCode();
        if (idToObject.containsKey(hash)) {
            System.out.println("Circular reference detected; terminated inspection of this object");
            return;
        }

        idToObject.put(hash, obj);

        // Current class---------------------------------------------------------------------------------
        String tab = new String(new char[depth]).replace("\0", "\t");
        System.out.println(tab + "ClASS");
        System.out.println(tab + "Class: " + c.getName());


        // Fields -----------------------------------------------------------------------------------
        inspectFields(c, obj, recursive, depth, tab);

    }

    /**
     * Inspects fields for type, modifier, and value(s). If the recursive flag is set it will call inspect() on any field that is an instantiated instance of Object
     * @param c
     * @param obj
     * @param recursive
     * @param depth
     * @param tab
     * @throws IllegalAccessException
     */
    private static void inspectFields(Class<?> c, Object obj, boolean recursive, int depth, String tab) throws IllegalAccessException {
        System.out.println(tab + "FIELDS (" + c.getName() + ")");
        if (c.getDeclaredFields().length > 0) {
            System.out.println(tab + "Fields ->");
            for (Field f : c.getDeclaredFields()) {
                f.setAccessible(true);
                System.out.println(tab + " FIELD");
                System.out.println(tab +  "  Name: " + f.getName());
                if (f.getType().isPrimitive()) {
                    System.out.println(tab + "  Type: " + f.getType());
                    System.out.println(tab + "  Modifiers: " + Modifier.toString(f.getModifiers()));
                    System.out.println(tab + "  Current Value: " + f.get(obj));
                } else if (f.getType().isArray()){
                    System.out.println(tab + "  Type: " + f.getType());
                    System.out.println(tab + "  Modifiers: " + Modifier.toString(f.getModifiers()));
                    System.out.println(tab + "  Component Type: " + f.getType().getComponentType().getName());
                    Object val = f.get(obj);
                    int len = Array.getLength(val);
                    System.out.println(tab + "  Length: " + len);
                    System.out.println(tab + "  Entries ->");
                    for (int i = 0; i < len; i++) {
                        System.out.println(tab + "   Value: " + Array.get(val, i));
                    }
                } else {
                    System.out.println(tab + "  Type: " + f.getType());
                    System.out.println(tab + "  Modifiers: " + Modifier.toString(f.getModifiers()));
                    System.out.println(tab + "  Current Value: " + f.get(obj));
                    if (recursive && f.get(obj) != null) {
                        System.out.println(tab + "\t-> Recursively inspect");
                        inspectClass(f.getType(), f.get(obj), recursive, depth + 1);
                    }
                }
            }
        } else {
            System.out.println(tab + "Fields -> None");
        }
    }
}
