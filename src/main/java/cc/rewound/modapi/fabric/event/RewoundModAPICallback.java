package cc.rewound.modapi.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;

/**
 * Callback for when a Rewound Mod API packet is received.
 */
public interface RewoundModAPICallback {

    Event<RewoundModAPICallback> EVENT = EventFactory.createArrayBacked(RewoundModAPICallback.class, callbacks -> packet -> {
        for (RewoundModAPICallback callback : callbacks) {
            callback.onPacketReceived(packet);
        }
    });

    void onPacketReceived(ClientboundRewoundPacket packet);

}
