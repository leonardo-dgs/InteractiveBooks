package net.leonardo_dgs.interactivebooks.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.lucko.helper.reflect.MinecraftVersion;
import me.lucko.helper.reflect.NmsVersion;
import me.lucko.helper.reflect.ServerReflection;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PlayerUtil {

    private static final NmsVersion NMS_VERSION = ServerReflection.getNmsVersion();
    private static final boolean OPENBOOK_NATIVE_SUPPORT = MinecraftVersion.getRuntimeVersion().isAfterOrEq(MinecraftVersion.of(1, 14, 2));

    private static final Enum<?> ENUM_HAND;
    private static final Constructor<?> PACKET_CONSTRUCTOR;
    private static final Object MINECRAFT_KEY;
    private static final Constructor<?> PACKETDATASERIALIZER_CONSTRUCTOR;
    private static final Method PACKETDATASERIALIZER_ENUM;

    static {
        Enum<?> enumHand = null;
        Constructor<?> packetConstructor = null;
        Object minecraftKey = null;
        Constructor<?> packetDataSerializerConstructor = null;
        Method packetDataSerializerEnum = null;
        try {
            switch (ServerReflection.getNmsVersion()) {
                case v1_14_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutOpenBook").getConstructor(ServerReflection.nmsClass("EnumHand"));
                    break;

                case v1_13_R2:
                case v1_13_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(ServerReflection.nmsClass("MinecraftKey"), ServerReflection.nmsClass("PacketDataSerializer"));
                    minecraftKey = ServerReflection.nmsClass("MinecraftKey").getMethod("a", String.class).invoke(null, "minecraft:book_open");
                    packetDataSerializerConstructor = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class);
                    packetDataSerializerEnum = ServerReflection.nmsClass("PacketDataSerializer").getMethod("a", Enum.class);
                    break;

                case v1_12_R1:
                case v1_11_R1:
                case v1_10_R1:
                case v1_9_R2:
                case v1_9_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(String.class, ServerReflection.nmsClass("PacketDataSerializer"));
                    packetDataSerializerConstructor = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class);
                    packetDataSerializerEnum = ServerReflection.nmsClass("PacketDataSerializer").getMethod("a", Enum.class);
                    break;

                case v1_8_R3:
                case v1_8_R2:
                case v1_8_R1:
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(String.class, ServerReflection.nmsClass("PacketDataSerializer"));
                    packetDataSerializerConstructor = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class);
                    break;
            }
        } catch (NoSuchFieldException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        ENUM_HAND = enumHand;
        PACKET_CONSTRUCTOR = packetConstructor;
        MINECRAFT_KEY = minecraftKey;
        PACKETDATASERIALIZER_CONSTRUCTOR = packetDataSerializerConstructor;
        PACKETDATASERIALIZER_ENUM = packetDataSerializerEnum;
    }

    /**
     * Opens a book to a set of players.
     *
     * @param book the book to open
     * @param players the players to whom open the book
     */
    public static void openBook(ItemStack book, Player... players) {
        if (!book.getType().equals(Material.WRITTEN_BOOK)) {
            return;
        }
        if (OPENBOOK_NATIVE_SUPPORT) {
            for (Player player : players)
                player.openBook(book);
        }
        else {
            Object openBookPacket = null;
            try {
                Object packetDataSerializer;
                Object packetDataSerializerArg;
                switch (NMS_VERSION) {
                    case v1_14_R1:
                        openBookPacket = PACKET_CONSTRUCTOR.newInstance(ENUM_HAND);
                        break;

                    case v1_13_R2:
                    case v1_13_R1:
                        packetDataSerializerArg = PACKETDATASERIALIZER_CONSTRUCTOR.newInstance(Unpooled.buffer());
                        packetDataSerializer = PACKETDATASERIALIZER_ENUM.invoke(packetDataSerializerArg, ENUM_HAND);
                        openBookPacket = PACKET_CONSTRUCTOR.newInstance(MINECRAFT_KEY, packetDataSerializer);
                        break;

                    case v1_12_R1:
                    case v1_11_R1:
                    case v1_10_R1:
                    case v1_9_R2:
                    case v1_9_R1:
                        packetDataSerializerArg = PACKETDATASERIALIZER_CONSTRUCTOR.newInstance(Unpooled.buffer());
                        packetDataSerializer = PACKETDATASERIALIZER_ENUM.invoke(packetDataSerializerArg, ENUM_HAND);
                        openBookPacket = PACKET_CONSTRUCTOR.newInstance("MC|BOpen", packetDataSerializer);
                        break;

                    case v1_8_R3:
                    case v1_8_R2:
                    case v1_8_R1:
                        packetDataSerializer = PACKETDATASERIALIZER_CONSTRUCTOR.newInstance(Unpooled.buffer());
                        openBookPacket = PACKET_CONSTRUCTOR.newInstance("MC|BOpen", packetDataSerializer);
                        break;
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }

            for (Player player : players) {
                int slot = player.getInventory().getHeldItemSlot();
                ItemStack old = player.getInventory().getItem(slot);
                player.getInventory().setItem(slot, book);
                ReflectionUtil.sendPacket(openBookPacket, player);
                player.getInventory().setItem(slot, old);
            }
        }
    }

}
