package net.leonardo_dgs.interactivebooks.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.lucko.helper.reflect.MinecraftVersion;
import me.lucko.helper.reflect.ServerReflection;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public final class PlayerUtil {

    private static final Object OPENBOOK_PACKET;

    static {
        Object openBookPacket = null;
        try {
            Constructor<?> packetConstructor;
            Enum<?> enumHand;
            Object packetDataSerializer;
            Object packetDataSerializerArg;
            Object minecraftKey;
            switch (ServerReflection.getNmsVersion()) {
                case v1_14_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutOpenBook").getConstructor(ServerReflection.nmsClass("EnumHand"));
                    openBookPacket = packetConstructor.newInstance(enumHand);
                    break;

                case v1_13_R2:
                case v1_13_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    minecraftKey = ServerReflection.nmsClass("MinecraftKey").getMethod("a", String.class).invoke(null, "minecraft:book_open");
                    packetDataSerializerArg = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetDataSerializer = ServerReflection.nmsClass("PacketDataSerializer").getMethod("a", Enum.class).invoke(packetDataSerializerArg, enumHand);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(ServerReflection.nmsClass("MinecraftKey"), ServerReflection.nmsClass("PacketDataSerializer"));
                    openBookPacket = packetConstructor.newInstance(minecraftKey, packetDataSerializer);
                    break;

                case v1_12_R1:
                case v1_11_R1:
                case v1_10_R1:
                case v1_9_R2:
                case v1_9_R1:
                    enumHand = (Enum<?>) ServerReflection.nmsClass("EnumHand").getField("MAIN_HAND").get(null);
                    packetDataSerializerArg = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetDataSerializer = ServerReflection.nmsClass("PacketDataSerializer").getMethod("a", Enum.class).invoke(packetDataSerializerArg, enumHand);
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(String.class, ServerReflection.nmsClass("PacketDataSerializer"));
                    openBookPacket = packetConstructor.newInstance("MC|BOpen", packetDataSerializer);
                    break;

                case v1_8_R3:
                case v1_8_R2:
                case v1_8_R1:
                    packetDataSerializer = ServerReflection.nmsClass("PacketDataSerializer").getConstructor(ByteBuf.class).newInstance(Unpooled.buffer());
                    packetConstructor = ServerReflection.nmsClass("PacketPlayOutCustomPayload").getConstructor(String.class, ServerReflection.nmsClass("PacketDataSerializer"));
                    openBookPacket = packetConstructor.newInstance("MC|BOpen", packetDataSerializer);
                    break;
            }
        } catch (NoSuchFieldException | InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        OPENBOOK_PACKET = openBookPacket;
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
        if (MinecraftVersion.getRuntimeVersion().isAfterOrEq(MinecraftVersion.of(1, 14, 2))) {
            for (Player player : players) {
                player.openBook(book);
            }
            return;
        }
        for (Player player : players) {
            int slot = player.getInventory().getHeldItemSlot();
            ItemStack old = player.getInventory().getItem(slot);
            player.getInventory().setItem(slot, book);
            ReflectionUtil.sendPacket(OPENBOOK_PACKET, player);
            player.getInventory().setItem(slot, old);
        }
    }

}
