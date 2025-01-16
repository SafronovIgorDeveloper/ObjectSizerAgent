package ru.util;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

public class ObjectSizerAgent {
    private static Instrumentation instrumentation;

    public static void premain(String args, Instrumentation inst) {
        instrumentation = inst;
    }

    public static long getObjectSize(Object obj) {
        if (instrumentation == null) {
            throw new IllegalStateException("Instrumentation API не инициализировано!");
        }
        return instrumentation.getObjectSize(obj);
    }

    public static long getFullObjectSize(Object root) {
        Set<Object> visited = new HashSet<>();
        return getFullObjectSize(root, visited);
    }

    private static long getFullObjectSize(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) {
            return 0;
        }
        visited.add(obj);

        long size = getObjectSize(obj);

        Class<?> clazz = obj.getClass();

        if (clazz.isArray()) {
            if (!clazz.getComponentType().isPrimitive()) {
                for (Object element : (Object[]) obj) {
                    size += getFullObjectSize(element, visited);
                }
            }
            return size;
        }

        while (clazz != null) {
            Field[] fields = clazz.getDeclaredFields();
            AccessibleObject.setAccessible(fields, true);

            for (Field field : fields) {
                if (!field.getType().isPrimitive()) {
                    try {
                        Object fieldValue = field.get(obj);
                        size += getFullObjectSize(fieldValue, visited);
                    } catch (IllegalAccessException ignored) {
                        // Если доступ невозможен, пропускаем
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return size;
    }
}