package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.entities.instances.EntityPlayerGun;
import minecrafttransportsimulator.entities.instances.PartGun;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.mcinterface.AWrapperWorld;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packets.components.APacketEntity;
import minecrafttransportsimulator.systems.ConfigSystem;

/**
 * Packet used to send signals to guns.  This can be either to change the state of the gun,
 * or to re-load the gun with the specified bullets.  If we are doing state commands, then
 * this packet first gets sent to the server from the client who requested the command.  After this,
 * it is send to all players tracking the gun (if applicable).  If this packet is for re-loading bullets, then it will
 * only appear on clients after the server has verified the bullets can in fact be loaded.
 *
 * @author don_bruce
 */
public class PacketPartGun extends APacketEntity<PartGun> {
    private final Request stateRequest;
    private final ItemBullet bulletItem;
    private final int bulletQty;
    private final int fireModeIndex; // For SET_FIRE_MODE requests

    public PacketPartGun(PartGun gun, Request stateRequest) {
        super(gun);
        this.stateRequest = stateRequest;
        this.bulletItem = null;
        this.bulletQty = 0;
        this.fireModeIndex = -1;
    }

    public PacketPartGun(PartGun gun, ItemBullet bullet, int bulletQty) {
        super(gun);
        this.stateRequest = Request.RELOAD_ONCLIENT;
        this.bulletItem = bullet;
        this.bulletQty = bulletQty;
        this.fireModeIndex = -1;
    }

    public PacketPartGun(PartGun gun, Request stateRequest, ItemBullet lastBullet) {
        super(gun);
        this.stateRequest = stateRequest;
        this.bulletItem = lastBullet;
        this.bulletQty = 0;
        this.fireModeIndex = -1;
    }

    public PacketPartGun(PartGun gun, Request stateRequest, int fireModeIndex) {
        super(gun);
        this.stateRequest = stateRequest;
        this.bulletItem = null;
        this.bulletQty = 0;
        this.fireModeIndex = fireModeIndex;
    }

    public PacketPartGun(ByteBuf buf) {
        super(buf);
        this.stateRequest = Request.values()[buf.readByte()];
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            this.bulletItem = readItemFromBuffer(buf);
            this.bulletQty = buf.readInt();
            this.fireModeIndex = -1;
        } else if (stateRequest == Request.BULLETS_OUT) {
            this.bulletItem = readItemFromBuffer(buf);
            this.bulletQty = 0;
            this.fireModeIndex = -1;
        } else if (stateRequest == Request.SET_FIRE_MODE) {
            this.bulletItem = null;
            this.bulletQty = 0;
            this.fireModeIndex = buf.readInt();
        } else {
            this.bulletItem = null;
            this.bulletQty = 0;
            this.fireModeIndex = -1;
        }
    }

    @Override
    public void writeToBuffer(ByteBuf buf) {
        super.writeToBuffer(buf);
        buf.writeByte(stateRequest.ordinal());
        if (stateRequest == Request.RELOAD_ONCLIENT) {
            writeItemToBuffer(bulletItem, buf);
            buf.writeInt(bulletQty);
        } else if (stateRequest == Request.BULLETS_OUT) {
            writeItemToBuffer(bulletItem, buf);
        } else if (stateRequest == Request.SET_FIRE_MODE) {
            buf.writeInt(fireModeIndex);
        }
    }

    @Override
    public boolean handle(AWrapperWorld world, PartGun gun) {
        switch (stateRequest) {
            case CLEAR_ONCLIENT: {
                gun.clearBullets();
                break;
            }
            case RELOAD_ONCLIENT: {
                gun.setReloadVars(bulletItem, bulletQty);
                break;
            }
            case RELOAD_HAND: {
                gun.isHandHeldGunReloadRequested = true;
                break;
            }
            case TRIGGER_ON: {
                if (ConfigSystem.settings.general.devMode.value) {
                    InterfaceManager.coreInterface.logError("[GUN DEBUG] TRIGGER_ON packet received, isClient=" + world.isClient());
                }
                gun.playerHoldingTrigger = true;
                gun.playerPressedTrigger = true;
                break;
            }
            case TRIGGER_OFF: {
                if (ConfigSystem.settings.general.devMode.value) {
                    InterfaceManager.coreInterface.logError("[GUN DEBUG] TRIGGER_OFF packet received, isClient=" + world.isClient());
                }
                gun.playerHoldingTrigger = false;
                break;
            }
            case AIM_ON: {
                gun.isHandHeldGunAimed = true;
                break;
            }
            case AIM_OFF: {
                gun.isHandHeldGunAimed = false;
                break;
            }
            case BULLETS_OUT: {
                gun.bulletsPresentOnServer = false;
                if (bulletItem != null) {
                    gun.lastLoadedBullet = bulletItem;
                }
                break;
            }
            case BULLETS_PRESENT: {
                gun.bulletsPresentOnServer = true;
                break;
            }
            case HANDHELD_MOVEMENTS: {
                gun.performGunHandheldMovements();
                break;
            }
            case SET_FIRE_MODE: {
                gun.setFireModeIndex(fireModeIndex);
                // For handheld guns, save state immediately to persist fire mode
                if (gun.masterEntity instanceof EntityPlayerGun) {
                    ((EntityPlayerGun) gun.masterEntity).saveGunState();
                }
                break;
            }
        }
        return stateRequest.sendToClients;
    }

    public static enum Request {
        CLEAR_ONCLIENT(false),
        RELOAD_ONCLIENT(false),
        RELOAD_HAND(false),
        TRIGGER_ON(true),
        TRIGGER_OFF(true),
        AIM_ON(true),
        AIM_OFF(true),
        BULLETS_OUT(false),
        BULLETS_PRESENT(false),
        HANDHELD_MOVEMENTS(true),
        SET_FIRE_MODE(true);

        private final boolean sendToClients;

        private Request(boolean sendToClients) {
            this.sendToClients = sendToClients;
        }
    }
}
