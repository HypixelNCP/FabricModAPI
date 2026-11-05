package cc.rewound.modapi.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import cc.rewound.modapi.error.ErrorReason;

/**
 * Callback for when a Rewound Mod API error reason is received.
 */
public interface RewoundModAPIErrorCallback {

    Event<RewoundModAPIErrorCallback> EVENT = EventFactory.createArrayBacked(RewoundModAPIErrorCallback.class, callbacks -> (identifier, reason) -> {
        for (RewoundModAPIErrorCallback callback : callbacks) {
            callback.onError(identifier, reason);
        }
    });

    void onError(String identifier, ErrorReason reason);

}
