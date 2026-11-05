package cc.rewound.modapi.fabric;

import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import cc.rewound.modapi.RewoundModAPI;
import cc.rewound.modapi.RewoundModAPIImplementation;
import cc.rewound.modapi.fabric.event.RewoundModAPICallback;
import cc.rewound.modapi.fabric.event.RewoundModAPIErrorCallback;
import cc.rewound.modapi.fabric.payload.ClientboundRewoundPayload;
import cc.rewound.modapi.fabric.payload.ServerboundRewoundPayload;
import cc.rewound.modapi.packet.EventPacket;
import cc.rewound.modapi.packet.RewoundPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundGameFinishPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundGameRejoinPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundGameStartPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

public class FabricModAPI implements ClientModInitializer, RewoundModAPIImplementation {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG_MODE = FabricLoader.getInstance().isDevelopmentEnvironment() || Boolean.getBoolean("cc.rewound.modapi.debug");

    private boolean onRewound = false;

    @GameTest
    @Override
    public void onInitializeClient() {
        RewoundModAPI.getInstance().setModImplementation(this);
    }

    @Override
    public void onInit() {
        RewoundModAPI.getInstance().createHandler(ClientboundHelloPacket.class, packet -> onRewound = true);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onRewound = false);

        if (DEBUG_MODE) {
            LOGGER.info("Debug mode is enabled!");
            registerDebug();
        }

        reloadRegistrations();
    }

    @Override
    public boolean sendPacket(RewoundPacket packet) {
        if (!isConnectedToRewound()) {
            return false;
        }

        ServerboundRewoundPayload rewoundPayload = new ServerboundRewoundPayload(packet);

        if (Minecraft.getInstance().getConnection() != null) {
            ClientPlayNetworking.send(rewoundPayload);
            return true;
        }

        try {
            ClientConfigurationNetworking.send(rewoundPayload);
            return true;
        } catch (IllegalStateException ignored) {
            LOGGER.warn("Failed to send a packet as the client is not connected to a server '{}'", packet);
            return false;
        }
    }

    @Override
    public boolean isConnectedToRewound() {
        return onRewound;
    }

    /**
     * Reloads the identifiers that are registered in the Rewound Mod API and makes sure that the packets are registered.
     * <p>
     * This method is available for internal use to add new packets externally, and is not intended for use by other developers.
     */
    @ApiStatus.Internal
    public static void reloadRegistrations() {
        for (String identifier : RewoundModAPI.getInstance().getRegistry().getClientboundIdentifiers()) {
            try {
                registerClientbound(identifier);
                LOGGER.info("Registered clientbound packet with identifier '{}'", identifier);
            } catch (Exception e) {
                LOGGER.error("Failed to register clientbound packet with identifier '{}'", identifier, e);
            }
        }

        for (String identifier : RewoundModAPI.getInstance().getRegistry().getServerboundIdentifiers()) {
            try {
                registerServerbound(identifier);
                LOGGER.info("Registered serverbound packet with identifier '{}'", identifier);
            } catch (Exception e) {
                LOGGER.error("Failed to register serverbound packet with identifier '{}'", identifier, e);
            }
        }
    }

    private static void registerClientbound(String identifier) {
        try {
            CustomPacketPayload.Type<ClientboundRewoundPayload> clientboundId = new CustomPacketPayload.Type<>(
                    Identifier.parse(identifier)
            );
            StreamCodec<ByteBuf, ClientboundRewoundPayload> codec = ClientboundRewoundPayload.buildCodec(clientboundId);
            PayloadTypeRegistry.clientboundPlay().register(clientboundId, codec);
            PayloadTypeRegistry.clientboundConfiguration().register(clientboundId, codec);

            // Also register the global receiver for handling incoming packets during PLAY and CONFIGURATION
            ClientPlayNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
                LOGGER.debug("Received packet with identifier '{}', during PLAY", identifier);
                handleIncomingPayload(identifier, payload);

            });
            ClientConfigurationNetworking.registerGlobalReceiver(clientboundId, (payload, context) -> {
                LOGGER.debug("Received packet with identifier '{}', during CONFIGURATION", identifier);
                handleIncomingPayload(identifier, payload);
            });
        } catch (IllegalArgumentException ignored) {
            // Ignored as this is fired when we reload the registrations and the packet is already registered
        }
    }

    private static void handleIncomingPayload(String identifier, ClientboundRewoundPayload payload) {
        if (!payload.isSuccess()) {
            LOGGER.warn("Received an error response for packet {}: {}", identifier, payload.getErrorReason());
            try {
                RewoundModAPI.getInstance().handleError(identifier, payload.getErrorReason());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling error response for packet {}", identifier, e);
            }

            try {
                RewoundModAPIErrorCallback.EVENT.invoker().onError(identifier, payload.getErrorReason());
            } catch (Exception e) {
                LOGGER.error("An error occurred while handling error response for packet {}", identifier, e);
            }
            return;
        }

        try {
            RewoundModAPI.getInstance().handle(payload.getPacket());
        } catch (Exception e) {
            LOGGER.error("An error occurred while handling packet {}", identifier, e);
        }

        try {
            RewoundModAPICallback.EVENT.invoker().onPacketReceived(payload.getPacket());
        } catch (Exception e) {
            LOGGER.error("An error occurred while handling packet {}", identifier, e);
        }
    }

    private static void registerServerbound(String identifier) {
        try {
            CustomPacketPayload.Type<ServerboundRewoundPayload> serverboundId = new CustomPacketPayload.Type<>(
                    Identifier.parse(identifier)
            );
            StreamCodec<ByteBuf, ServerboundRewoundPayload> codec = ServerboundRewoundPayload.buildCodec();
            PayloadTypeRegistry.serverboundPlay().register(serverboundId, codec);
            PayloadTypeRegistry.serverboundConfiguration().register(serverboundId, codec);
        } catch (IllegalArgumentException ignored) {
            // Ignored as this is fired when we reload the registrations and the packet is already registered
        }
    }

    private static void registerDebug() {
        // Register events
        subscribeDebug(ClientboundLocationPacket.class);
        subscribeDebug(ClientboundGameStartPacket.class);
        subscribeDebug(ClientboundGameFinishPacket.class);
        subscribeDebug(ClientboundGameRejoinPacket.class);

        RewoundModAPICallback.EVENT.register(packet -> LOGGER.info("Received packet {}", packet));
        RewoundModAPIErrorCallback.EVENT.register((identifier, error) -> LOGGER.error("Received error response for packet {}: {}", identifier, error));
    }

    private static <T extends EventPacket> void subscribeDebug(Class<T> event) {
        RewoundModAPI.getInstance().subscribeToEventPacket(event);
        RewoundModAPI.getInstance().createHandler(event, packet -> LOGGER.info("Received event {}", packet))
                .onError(error -> LOGGER.error("Received error response for event {}: {}", event.getSimpleName(), error));
    }
}
