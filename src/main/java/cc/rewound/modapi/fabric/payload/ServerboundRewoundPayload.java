package cc.rewound.modapi.fabric.payload;

import io.netty.buffer.ByteBuf;
import cc.rewound.modapi.packet.RewoundPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public class ServerboundRewoundPayload implements CustomPacketPayload {
    private final Type<ServerboundRewoundPayload> type;
    private final RewoundPacket packet;

    public ServerboundRewoundPayload(RewoundPacket packet) {
        this.type = new CustomPacketPayload.Type<>(Identifier.parse(packet.getIdentifier()));
        this.packet = packet;
    }

    private void write(ByteBuf buf) {
        PacketSerializer serializer = new PacketSerializer(buf);
        packet.write(serializer);
    }


    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public static StreamCodec<ByteBuf, ServerboundRewoundPayload> buildCodec() {
        return CustomPacketPayload.codec(ServerboundRewoundPayload::write, _ -> {
            throw new UnsupportedOperationException("Cannot read ServerboundRewoundPayload");
        });
    }
}
