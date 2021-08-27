package dev.simplix.protocolize.data.packets;

import dev.simplix.protocolize.api.ClickType;
import dev.simplix.protocolize.api.PacketDirection;
import dev.simplix.protocolize.api.mapping.AbstractProtocolMapping;
import dev.simplix.protocolize.api.mapping.ProtocolIdMapping;
import dev.simplix.protocolize.api.packet.AbstractPacket;
import dev.simplix.protocolize.api.util.ProtocolUtil;
import dev.simplix.protocolize.data.item.ItemStack;
import dev.simplix.protocolize.data.item.ItemStackSerializer;
import io.netty.buffer.ByteBuf;
import lombok.*;
import lombok.experimental.Accessors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.simplix.protocolize.api.util.ProtocolVersions.*;

/**
 * Date: 27.08.2021
 *
 * @author Exceptionflug
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Accessors(fluent = true)
public class ClickWindow extends AbstractPacket {

    public static final List<ProtocolIdMapping> MAPPINGS = Arrays.asList(
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_8, MINECRAFT_1_8, 0x0E),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_9, MINECRAFT_1_11_2, 0x07),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12, MINECRAFT_1_12, 0x08),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_12_1, MINECRAFT_1_12_2, 0x07),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_13, MINECRAFT_1_13_2, 0x08),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_14, MINECRAFT_1_16_4, 0x09),
            AbstractProtocolMapping.rangedIdMapping(MINECRAFT_1_17, MINECRAFT_1_17_1, 0x08)
    );

    private Map<Short, ItemStack> slotData = new HashMap<>();
    private int windowId;
    private int actionNumber;
    private short slot;
    private ClickType clickType;
    private ItemStack itemStack;

    /**
     * @since 1.7.1-SNAPSHOT protocol 756
     */
    private int stateId;

    @Override
    public void read(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        windowId = buf.readUnsignedByte();
        if (protocolVersion >= MINECRAFT_1_17_1) {
            stateId = ProtocolUtil.readVarInt(buf);
        }
        slot = buf.readShort();
        byte button = buf.readByte();
        if (protocolVersion < MINECRAFT_1_17) {
            actionNumber = buf.readShort();
        }
        int mode;
        if (protocolVersion == MINECRAFT_1_8) {
            mode = buf.readByte();
        } else {
            mode = ProtocolUtil.readVarInt(buf);
        }
        if (protocolVersion >= MINECRAFT_1_17) {
            int length = ProtocolUtil.readVarInt(buf);
            for (int i = 0; i < length; i++) {
                slotData.put(buf.readShort(), ItemStackSerializer.read(buf, protocolVersion));
            }
        }
        clickType = ClickType.getType(mode, button);
        itemStack = ItemStackSerializer.read(buf, protocolVersion);
    }

    @Override
    public void write(ByteBuf buf, PacketDirection packetDirection, int protocolVersion) {
        buf.writeByte(windowId & 0xFF);
        if (protocolVersion >= MINECRAFT_1_17_1) {
            ProtocolUtil.writeVarInt(buf, stateId);
        }
        buf.writeShort(slot);
        buf.writeByte(clickType.button());
        if (protocolVersion < MINECRAFT_1_17) {
            buf.writeShort(actionNumber);
        }
        if (protocolVersion == MINECRAFT_1_8) {
            buf.writeByte(clickType.mode());
        } else {
            ProtocolUtil.writeVarInt(buf, clickType.mode());
        }
        if (protocolVersion >= MINECRAFT_1_17) {
            ProtocolUtil.writeVarInt(buf, slotData.size());
            for (short slot : slotData.keySet()) {
                buf.writeShort(slot);
                ItemStackSerializer.write(buf, slotData.get(slot), protocolVersion);
            }
        }
        if (itemStack == null) {
            ItemStackSerializer.write(buf, ItemStack.NO_DATA, protocolVersion);
        } else {
            ItemStackSerializer.write(buf, itemStack, protocolVersion);
        }
    }

}
