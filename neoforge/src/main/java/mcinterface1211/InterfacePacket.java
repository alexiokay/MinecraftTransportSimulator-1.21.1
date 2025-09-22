package mcinterface1211;

import java.util.function.Supplier;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.IInterfacePacket;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

class InterfacePacket implements IInterfacePacket {
    private static final String PROTOCOL_VERSION = "1";
    private static final BiMap<Byte, Class<? extends APacketBase>> packetMappings = HashBiMap.create();
    private static PayloadRegistrar registrar;
    private static boolean initialized = false;

    /**
     * Called to init this network.  Needs to be done after networking is ready.
     * Packets should be registered at this point in this constructor.
     */
    public static void init(PayloadRegistrar payloadRegistrar) {
        // Use synchronized block to prevent race conditions
        synchronized (InterfacePacket.class) {
            if (initialized) {
                InterfaceManager.coreInterface.logError("PACKET WARNING: init() already completed! Ignoring duplicate call to prevent re-registration errors.");
                return;
            }

            // Set initialized IMMEDIATELY to prevent any race conditions
            initialized = true;
            InterfaceManager.coreInterface.logError("PACKET DEBUG: Initializing packet system for first time");
            registrar = payloadRegistrar;

        // Register the main wrapper packet payload for bidirectional communication
        // In NeoForge 1.21.1, we use playBidirectional for two-way communication
        try {
            registrar.playBidirectional(
                WrapperPacket.TYPE,
                WrapperPacket.STREAM_CODEC,
                WrapperPacket::handle
            );
            InterfaceManager.coreInterface.logError("PACKET DEBUG: Successfully registered bidirectional packet");
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("PACKET ERROR: Failed to register bidirectional packet: " + e.getMessage());
            e.printStackTrace();
        }

        //Register internal packets, then external.
        byte packetIndex = 0;
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeClient.class);
        InterfaceManager.packetInterface.registerPacket(packetIndex++, PacketEntityCSHandshakeServer.class);
        APacketBase.initPackets(packetIndex);
        } // End synchronized block
    }

    @Override
    public void registerPacket(byte packetIndex, Class<? extends APacketBase> packetClass) {
        packetMappings.put(packetIndex, packetClass);
    }

    @Override
    public byte getPacketIndex(APacketBase packet) {
        return packetMappings.inverse().get(packet.getClass());
    }

    @Override
    public void sendToServer(APacketBase packet) {
        PacketDistributor.sendToServer(new WrapperPacket(packet));
    }

    @Override
    public void sendToAllClients(APacketBase packet) {
        PacketDistributor.sendToAllPlayers(new WrapperPacket(packet));
    }

    @Override
    public void sendToPlayer(APacketBase packet, IWrapperPlayer player) {
        PacketDistributor.sendToPlayer((ServerPlayer) ((WrapperPlayer) player).player, new WrapperPacket(packet));
    }

    /**
     * Gets the world this packet was sent from based on its context.
     * Used for handling packets arriving on the server.
     */
    private static AWrapperWorld getServerWorld(IPayloadContext ctx) {
        return WrapperWorld.getWrapperFor(ctx.player().level());
    }

    @Override
    public void writeDataToBuffer(IWrapperNBT data, ByteBuf buf) {
        //We know this will be a PacketBuffer, so we can cast rather than wrap.
        ((FriendlyByteBuf) buf).writeNbt(((WrapperNBT) data).tag);
    }

    @Override
    public WrapperNBT readDataFromBuffer(ByteBuf buf) {
        return new WrapperNBT(((FriendlyByteBuf) buf).readNbt());
    }

    /**
     * Custom class for packets.  Allows for a common packet to be used for all MC versions,
     * as well as less boilerplate code due to thread operations.  Note that when this packet
     * arrives on the other side of the pipeline, MC won't know what class to construct.
     * That's up to us to handle via the packet's first byte.  Also note that this class
     * must be public, as if it is private MC won't be able to construct it due to access violations.
     */
    public static class WrapperPacket implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<WrapperPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(InterfaceLoader.MODID, "wrapper"));

        public static final StreamCodec<FriendlyByteBuf, WrapperPacket> STREAM_CODEC = StreamCodec.<FriendlyByteBuf, WrapperPacket>of(
            (buf, message) -> WrapperPacket.toBytes(message, buf),
            (buf) -> WrapperPacket.fromBytes(buf)
        );

        private APacketBase packet;

        /**
         * Do NOT call!  Required to keep NeoForge from crashing.
         **/
        public WrapperPacket() {
        }

        public WrapperPacket(APacketBase packet) {
            this.packet = packet;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static WrapperPacket fromBytes(FriendlyByteBuf buf) {
            try {
                byte packetIndex = buf.readByte();
                InterfaceManager.coreInterface.logError("PACKET DEBUG: Decoding packet with index: " + packetIndex);

                Class<? extends APacketBase> packetClass = packetMappings.get(packetIndex);
                if (packetClass == null) {
                    throw new IllegalStateException("No packet class registered for index " + packetIndex + ". Registered indices: " + packetMappings.keySet());
                }

                InterfaceManager.coreInterface.logError("PACKET DEBUG: Creating packet of class: " + packetClass.getSimpleName());
                return new WrapperPacket(packetClass.getConstructor(ByteBuf.class).newInstance(buf));
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("PACKET ERROR: Failed to decode packet! Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Packet decoding failed", e);
            }
        }

        public static void toBytes(WrapperPacket message, FriendlyByteBuf buf) {
            try {
                // Don't write packet index here - writeToBuffer() already does that!
                byte packetIndex = InterfaceManager.packetInterface.getPacketIndex(message.packet);
                InterfaceManager.coreInterface.logError("PACKET DEBUG: Encoding packet " + message.packet.getClass().getSimpleName() + " with index: " + packetIndex);

                message.packet.writeToBuffer(buf);
            } catch (Exception e) {
                InterfaceManager.coreInterface.logError("PACKET ERROR: Failed to encode packet! Exception: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Packet encoding failed", e);
            }
        }

        public static void handle(WrapperPacket message, IPayloadContext ctx) {
            if (message.packet.runOnMainThread()) {
                //Need to put this in a runnable to not run it on the network thread and get a CME.
                ctx.enqueueWork(() -> {
                    //We need to use side-specific getters here to avoid side-specific classes from trying to be loaded
                    //by the JVM when this method is created.  Failure to do this will result in network faults.
                    //For this, we use abstract methods that are extended in our sub-classes.
                    AWrapperWorld world;
                    if (ctx.flow().isServerbound()) {
                        world = getServerWorld(ctx);
                    } else {
                        world = InterfaceManager.clientInterface.getClientWorld();
                    }
                    if (world != null) {
                        message.packet.handle(world);
                    }
                });
            } else {
                if (ctx.flow().isServerbound()) {
                    message.packet.handle(getServerWorld(ctx));
                } else {
                    message.packet.handle(InterfaceManager.clientInterface.getClientWorld());
                }
            }
        }
    }
}
