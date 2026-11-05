package cc.rewound.modapi.fabric.payload;

import io.netty.buffer.ByteBuf;
import cc.rewound.modapi.RewoundModAPI;
import cc.rewound.modapi.error.ErrorReason;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

public class ClientboundRewoundPayload implements CustomPacketPayload {
    private final Type<ClientboundRewoundPayload> type;

    private ClientboundRewoundPacket packet;
    private ErrorReason errorReason;

    private ClientboundRewoundPayload(Type<ClientboundRewoundPayload> type, ByteBuf buf) {
        this.type = type;

        PacketSerializer serializer = new PacketSerializer(buf);
        boolean success = serializer.readBoolean();
        if (!success) {
            this.errorReason = ErrorReason.getById(serializer.readVarInt());
            return;
        }

        this.packet = RewoundModAPI.getInstance().getRegistry().createClientboundPacket(type.id().toString(), serializer);
    }

    @Override
    public @NonNull Type<? extends CustomPacketPayload> type() {
        return type;
    }

    public boolean isSuccess() {
        return packet != null;
    }

    public ClientboundRewoundPacket getPacket() {
        return packet;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public static StreamCodec<ByteBuf, ClientboundRewoundPayload> buildCodec(Type<ClientboundRewoundPayload> id) {
        return CustomPacketPayload.codec((value, buf) -> {
            throw new UnsupportedOperationException("Cannot write ClientboundRewoundPayload");
        }, buf -> new ClientboundRewoundPayload(id, buf));
    }
}
