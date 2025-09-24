package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.components.AEntityA_Base;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;

/**
 * Packet used to remove entities from the world.  Sent from server to clients
 * when an entity is removed on the server to ensure clients also remove their copy.
 *
 * @author don_bruce
 */
public class PacketEntityRemove extends APacketEntity<AEntityA_Base> {

    public PacketEntityRemove(AEntityA_Base entity) {
        super(entity);
    }

    public PacketEntityRemove(ByteBuf buf) {
        super(buf);
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
    }

    @Override
    public boolean handle(AWrapperWorld world, AEntityA_Base entity) {
        InterfaceManager.coreInterface.logError("PACKET ENTITY REMOVE: Received entity removal packet for " + entity.getClass().getSimpleName() + " UUID " + entity.uniqueUUID + " on " + (world.isClient() ? "CLIENT" : "SERVER"));

        // Only process on client side
        if (world.isClient()) {
            InterfaceManager.coreInterface.logError("PACKET ENTITY REMOVE: Removing entity on client side: " + entity.getClass().getSimpleName() + " UUID " + entity.uniqueUUID);
            entity.remove();
        }

        // Return false to prevent server from re-sending this packet to all clients
        return false;
    }
}