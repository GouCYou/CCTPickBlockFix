package cn.cctstudio.velocity.pickblockfix.packet;

import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromBlock;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPickItemFromEntity;

import java.util.Optional;

/**
 * Recognizes the modern frontend pick packets added after 1.21.1.
 */
public final class PickPacketRecognizer {

    public Optional<RecognizedPickPacket> recognize(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM_FROM_BLOCK) {
            WrapperPlayClientPickItemFromBlock wrapper = new WrapperPlayClientPickItemFromBlock(event);
            return Optional.of(RecognizedPickPacket.block(packetName(event), event.getPacketId(), wrapper.getBlockPos(), wrapper.isIncludeData()));
        }
        if (event.getPacketType() == PacketType.Play.Client.PICK_ITEM_FROM_ENTITY) {
            WrapperPlayClientPickItemFromEntity wrapper = new WrapperPlayClientPickItemFromEntity(event);
            return Optional.of(RecognizedPickPacket.entity(packetName(event), event.getPacketId(), wrapper.getEntityId(), wrapper.isIncludeData()));
        }
        return Optional.empty();
    }

    public boolean looksLikePickPacket(PacketReceiveEvent event) {
        String packetName = packetName(event);
        return packetName.contains("PICK_ITEM");
    }

    public String packetName(PacketReceiveEvent event) {
        return packetName(event.getPacketType());
    }

    public String packetName(Object packetType) {
        if (packetType instanceof Enum<?> packetEnum) {
            return packetEnum.name();
        }
        return String.valueOf(packetType);
    }

    public enum Kind {
        BLOCK,
        ENTITY
    }

    public record RecognizedPickPacket(
            Kind kind,
            String packetName,
            int packetId,
            Vector3i blockPos,
            Integer entityId,
            boolean includeData
    ) {
        public static RecognizedPickPacket block(String packetName, int packetId, Vector3i blockPos, boolean includeData) {
            return new RecognizedPickPacket(Kind.BLOCK, packetName, packetId, blockPos, null, includeData);
        }

        public static RecognizedPickPacket entity(String packetName, int packetId, int entityId, boolean includeData) {
            return new RecognizedPickPacket(Kind.ENTITY, packetName, packetId, null, entityId, includeData);
        }
    }
}
