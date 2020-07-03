package net.leonardo_dgs.interactivebooks.util;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ReflectionUtil {

    @Getter
    private static final String nmsVersion = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

    public static Class<?> nmsClass(String className) throws ClassNotFoundException {
        return Class.forName("net.minecraft.server." + getNmsVersion() + "." + className);
    }

    public static Class<?> obcClass(String className) throws ClassNotFoundException {
        return Class.forName("org.bukkit.craftbukkit." + getNmsVersion() + "." + className);
    }

    /**
     * Gets a {@link Field} of a given {@link Class}.
     * This method wraps the {@link NoSuchFieldException} and the {@link SecurityException} into a {@link RuntimeException}.
     *
     * @param clazz the class to which the field belongs
     * @param name  the name of the field
     * @return the {@link Field} with the supplied name of the supplied class
     * @see Class#getField(String)
     */
    public static Field getField(Class<?> clazz, String name) {
        try {
            return clazz.getField(name);
        } catch (NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a declared {@link Field} of a given {@link Class}.
     * This method wraps the {@link NoSuchFieldException} and the {@link SecurityException} into a {@link RuntimeException},
     * and sets accessible the {@link Field}.
     *
     * @param clazz the class to which the field belongs
     * @param name  the name of the field
     * @return the {@link Field} with the supplied name of the supplied class
     * @see Class#getDeclaredField(String)
     */
    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the value of a {@link Field} of the supplied {@link Object}.
     * This method wraps the {@link NoSuchFieldException} and the {@link IllegalAccessException} into a {@link RuntimeException}.
     *
     * @param from      the class to which the field belongs
     * @param obj       the object to which set the field
     * @param fieldName the name of the field
     * @param newValue  the new value to set
     * @param <T>       the type of the class parameters
     * @see Field#set(Object, Object)
     */
    public static <T> void setField(Class<T> from, Object obj, String fieldName, Object newValue) {
        try {
            Field f = from.getDeclaredField(fieldName);
            boolean accessible = f.isAccessible();
            f.setAccessible(true);
            f.set(obj, newValue);
            f.setAccessible(accessible);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a {@link Method} of a given {@link Class}.
     * This method wraps the {@link NoSuchMethodException} into a {@link RuntimeException}.
     *
     * @param clazz the class to which the method belongs
     * @param name  the name of the method
     * @param args  the {@link Class} list representing the arguments of the method
     * @return the {@link Method} with the specified properties
     * @see Class#getMethod(String, Class[])
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... args) {
        try {
            return clazz.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets a declared {@link Method} of a given {@link Class}.
     * This method wraps the {@link NoSuchMethodException} into a {@link RuntimeException},
     * and sets accessible the returned {@link Method}.
     *
     * @param clazz the class to which the method belongs
     * @param name  the name of the method
     * @param args  the {@link Class} list representing the arguments of the method
     * @return the {@link Method} with the specified properties
     * @see Class#getMethod(String, Class[])
     */
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... args) {
        try {
            Method method = clazz.getDeclaredMethod(name, args);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the returned value of the <em>getHandle()</em> method of a given {@link Object}.
     * This can be useful for NMS and OBC code.
     *
     * @param obj the {@link Object} from which to get the returned value
     * @return the returned value of the getHandle() method of the given {@link Object}
     */
    public static Object getHandle(Object obj) {
        try {
            return getDeclaredMethod(obj.getClass(), "getHandle", new Class[0]).invoke(obj);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the value of the field <em>playerConnection</em>
     * of the instance of the {@link Player} supplied.
     * This can be useful for NMS and OBC code.
     *
     * @param player the {@link Player} from which to get the <em>playerConnection</em> field value
     * @return the value of the field <em>playerConnection</em> of the supplied player's instance
     */
    public static Object getConnection(Player player) {
        try {
            Object craftPlayer = getHandle(player);
            return getField(craftPlayer.getClass(), "playerConnection").get(craftPlayer);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sends a Minecraft packet to a list of players.
     * This can be useful for NMS and OBC code.
     *
     * @param packet  the packet to send
     * @param players the players to whom send the packet
     */
    public static void sendPacket(Object packet, Player... players) {
        try {
            for (Player player : players) {
                Object connection = getConnection(player);
                getDeclaredMethod(connection.getClass(), "sendPacket", nmsClass("Packet")).invoke(connection, packet);
            }
        } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
