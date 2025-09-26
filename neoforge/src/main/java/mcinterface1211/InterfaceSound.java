package mcinterface1211;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;

// FMOD API imports
import com.fmodapi.FMODAPI;

import minecrafttransportsimulator.baseclasses.Point3D;
import minecrafttransportsimulator.entities.instances.EntityRadio;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.mcinterface.IInterfaceSound;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.sound.IStreamDecoder;
import minecrafttransportsimulator.sound.OGGDecoder;
import minecrafttransportsimulator.sound.RadioStation;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.LanguageSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Interface for the sound system.  This is responsible for playing sound from vehicles/interactions.
 * As well as from the internal radio.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Dist.CLIENT)
public class InterfaceSound implements IInterfaceSound {
    /**
     * Flag for game paused state.  Gets set when the game is paused.
     **/
    private static boolean isSystemPaused;

    /**
     * Map to track FMOD API sound instances
     **/
    private static final Map<SoundInstance, String> fmodInstances = new HashMap<>();

    /**
     * Map of String-based file-names to Integer pointers to buffer locations.  Used for loading sounds into
     * memory to prevent the need to load them every time they are played.
     **/
    private static final Map<String, Integer> dataSourceBuffers = new HashMap<>();

    /**
     * List of sounds currently playing.  Queued for updates every tick.
     **/
    private static final Set<SoundInstance> playingSounds = new HashSet<>();

    /**
     * List of playing {@link RadioStation} objects.
     **/
    private static final List<RadioStation> playingStations = new ArrayList<>();

    /**
     * List of sounds to start playing next update.  Split from playing sounds to avoid CMEs and odd states.
     **/
    private static final List<SoundInstance> queuedSounds = new ArrayList<>();
    /**
     * List of radios paused.  Needs to be separate from normal paused sound this those get re-added to the sound set.
     **/
    private static final List<SoundInstance> pausedRadioSounds = new ArrayList<>();

    /**
     * Map of active FMOD event instances to track and manage them properly.
     **/
    private static final Map<String, Long> activeFMODInstances = new HashMap<>();

    /**
     * Maximum number of concurrent FMOD instances to prevent resource exhaustion.
     **/
    private static final int MAX_FMOD_INSTANCES = 64;

    /**
     * Flag to prevent spam of sound slot warnings when audio sources are full.
     **/
    private static boolean postedSoundWarning;

    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static boolean pausedForMenu = false;

    /**
     * Initialize FMOD using the API mod - no direct initialization needed
     */
    public static void FMODSystemInit() {
        // FMOD initialization is now handled by the FMOD API mod
        // Just log the status and load our banks
        if (FMODAPI.isAvailable()) {
            var status = FMODAPI.getStatus();
            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD API available: " + status.status + RESET);

            // Load MTS banks from our JAR resources
            FMODLoadBankFromResource("/assets/mts/sounds/fmod/Master.strings.bank");
            FMODLoadBankFromResource("/assets/mts/sounds/fmod/Master.bank");
            FMODLoadBankFromResource("/assets/mts/sounds/fmod/Weapons.bank");
        } else {
            InterfaceManager.coreInterface.logInfo(YELLOW + "FMOD API not available, using OpenAL fallback" + RESET);
        }
    }

    public static void FMODUpdateListener() {
        if (!FMODAPI.isAvailable()) {
            return;
        }

        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (player == null) {
            return;
        }

        Point3D position = player.getPosition();
        Point3D forward = player.getLineOfSight(1.0).normalize();
        Point3D velocity = player.getVelocity();

        // Update listener position using FMOD API
        FMODAPI.setListenerPosition(
            position.x, position.y, position.z,
            forward.x, forward.y, forward.z,
            velocity.x, velocity.y, velocity.z
        );
    }

    public static void FMODSystemShutdown() {
        // Clean up MTS-specific FMOD instances
        if (FMODAPI.isAvailable()) {
            // Stop all our tracked sounds
            for (String instanceId : fmodInstances.values()) {
                FMODAPI.stopEvent(instanceId, false);
            }
            fmodInstances.clear();
        }
    }

    private static void FMODSystemUpdate() {
        if (!FMODAPI.isAvailable()) {
            return;
        }

        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (!InterfaceManager.clientInterface.isGamePaused() && player != null) {
            FMODUpdateListener();
        }

        // Clean up finished sound instances
        FMODCleanupFinishedInstances();
    }

    private static void FMODLoadBank(String path) {
        if (FMODAPI.loadBank(path)) {
            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD API successfully loaded bank: " + '"' + path + '"' + RESET);
        } else {
            InterfaceManager.coreInterface.logErrorMain(RED + "FMOD API failed to load bank: " + '"' + path + '"' + RESET);
        }
    }


    private static void FMODLoadBankFromResource(String resourcePath) {
        // Use the new FMOD API method for loading banks from resources
        if (FMODAPI.loadBankFromResource(InterfaceSound.class, resourcePath)) {
            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD API successfully loaded bank from resource: " + resourcePath + RESET);
        } else {
            InterfaceManager.coreInterface.logErrorMain(RED + "FMOD API failed to load bank from resource: " + resourcePath + RESET);
        }
    }



    public void FMODPlaySoundEvent(SoundInstance sound) {
        // Check if FMOD system is available through API
        if (!FMODAPI.isAvailable()) {
            InterfaceManager.coreInterface.logInfo("FMOD system not available, falling back to OpenAL for sound: " + (sound.soundDef != null ? sound.soundDef.name : sound.soundName));
            playQuickSound(sound);
            return;
        }

        // Use the FMOD eventName from JSON if available, otherwise fall back to soundPlayingName
        String fmodEventName = (sound.soundDef != null && sound.soundDef.eventName != null)
                ? sound.soundDef.eventName
                : sound.soundPlayingName;

        // Get position coordinates for 3D audio
        double posX = sound.entity.position.x;
        double posY = sound.entity.position.y;
        double posZ = sound.entity.position.z;

        // Use default volume and pitch (MTS handles complex volume/pitch calculations elsewhere)
        float volume = 1.0f;
        float pitch = 1.0f;

        // For looping sounds, check if already playing unless forced
        if (sound.soundDef != null && sound.soundDef.looping && !sound.soundDef.forceSound) {
            String loopingKey = fmodEventName + "_" + sound.entity.uniqueUUID + "_looping";
            if (activeFMODInstances.containsKey(loopingKey)) {
                return; // Already playing this looping sound
            }
        }

        // Play the sound through FMOD API
        String instanceId = FMODAPI.playEventAt(fmodEventName, posX, posY, posZ, volume, pitch);

        if (instanceId != null) {
            // Track this instance for cleanup
            String instanceKey = fmodEventName + "_" + sound.entity.uniqueUUID + "_" + System.nanoTime();
            if (sound.soundDef != null && sound.soundDef.looping && !sound.soundDef.forceSound) {
                instanceKey = fmodEventName + "_" + sound.entity.uniqueUUID + "_looping";
            }

            // Store the instance ID for tracking (we'll need to modify the storage type)
            activeFMODInstances.put(instanceKey, Long.parseLong(instanceId));

            InterfaceManager.coreInterface.logInfo(GREEN + "Playing sound event " + '"' + fmodEventName + '"' + " at: " + posX + ", " + posY + ", " + posZ + RESET);
        } else {
            InterfaceManager.coreInterface.logInfo(YELLOW + "FMOD API failed to play event: " + fmodEventName + ", falling back to OpenAL using: " + sound.soundPlayingName + RESET);
            // The fallback correctly uses sound.soundPlayingName which is the OGG file
            playQuickSound(sound);
        }
    }

    /**
     * Cleans up finished FMOD event instances to prevent memory leaks.
     * The FMOD API handles most cleanup automatically, so this is simplified.
     */
    private static void FMODCleanupFinishedInstances() {
        if (!FMODAPI.isAvailable() || activeFMODInstances.isEmpty()) return;

        // The FMOD API handles internal cleanup automatically
        // For now, we'll let the API manage cleanup internally
        // This method is kept for compatibility but simplified

        InterfaceManager.coreInterface.logInfo("FMOD cleanup handled by API, active instances: " + activeFMODInstances.size());
    }

    /**
     * Cleans up all active FMOD event instances.
     */
    private static void FMODCleanupAllInstances() {
        if (!FMODAPI.isAvailable() || activeFMODInstances.isEmpty()) return;

        // Stop all sounds through the FMOD API
        FMODAPI.stopAllSounds();
        activeFMODInstances.clear();

        InterfaceManager.coreInterface.logInfo("All FMOD instances stopped and cleared");
    }

    /**
     * Main update loop.  Call every tick to update playing sounds,
     * as well as queue up sounds that aren't playing yet but need to.
     */
    public static void update() {
        if (ALC.getFunctionProvider() == null) {
            //Don't go any further if OpenAL isn't ready.
            return;
        }

        //Handle pause state logic.
        if (InterfaceManager.clientInterface.isGamePaused()) {
            if (!isSystemPaused) {
                for (SoundInstance sound : playingSounds) {
                    AL10.alSourcePause(sound.sourceIndex);
                }
                isSystemPaused = true;
            } else {
                for (SoundInstance sound : playingSounds) {
                    //Stop playing sounds when paused as they can get corrupted.
                    if (sound.radio != null) {
                        pausedRadioSounds.add(sound);
                        sound.radio.currentStation.removeRadio(sound.radio);
                    } else {
                        sound.stopSound = true;
                    }
                }
                playingSounds.removeAll(pausedRadioSounds);
            }
            return;
        } else if (isSystemPaused) {
            for (SoundInstance sound : playingSounds) {
                AL10.alSourcePlay(sound.sourceIndex);
            }
            for (SoundInstance sound : pausedRadioSounds) {
                sound.radio.currentStation.addRadio(sound.radio);
            }
            pausedRadioSounds.clear();
            isSystemPaused = false;
        }

        //Get the player for further calculations.
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        //If the client world is null, or we don't have a player we need to stop all sounds.
        if (InterfaceManager.clientInterface.getClientWorld() == null || player == null) {
            queuedSounds.clear();
            for (SoundInstance sound : playingSounds) {
                sound.stopSound = true;
            }
        }

        //Start playing all queued sounds.
        if (!queuedSounds.isEmpty()) {
            for (SoundInstance sound : queuedSounds) {
                AL10.alSourcePlay(sound.sourceIndex);
                playingSounds.add(sound);
            }
            queuedSounds.clear();
        }

        //Update playing sounds.
        boolean soundSystemReset = false;
        Iterator<SoundInstance> iterator = playingSounds.iterator();
        while (iterator.hasNext()) {
            SoundInstance sound = iterator.next();
            AL10.alGetError();
            int state = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_SOURCE_STATE);
            //If we are an invalid name, it means the sound system was reset.
            if (AL10.alGetError() == AL10.AL_INVALID_NAME) {
                soundSystemReset = true;
                break;
            }

            if (state == AL10.AL_PLAYING) {
                if (sound.stopSound) {
                    AL10.alSourceStop(sound.sourceIndex);
                } else {
                    //Update position and volume, and block rolloff.
                    sound.updatePosition();
                    AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.position.x, (float) sound.position.y, (float) sound.position.z);
                    if (sound.radio == null) {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume * ConfigSystem.client.controlSettings.soundVolume.value);
                    } else {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_GAIN, sound.volume * ConfigSystem.client.controlSettings.radioVolume.value);
                    }
                    AL10.alSourcef(sound.sourceIndex, AL10.AL_ROLLOFF_FACTOR, 0);

                    //If the sound is looping, and the player isn't riding the source, calculate doppler pitch effect.
                    //Otherwise, set pitch as normal.
                    if (sound.soundDef != null && sound.soundDef.looping && !sound.soundDef.blockDoppler && !sound.entity.equals(player.getEntityRiding())) {
                        Point3D playerVelocity = player.getVelocity();
                        playerVelocity.y = 0;
                        double initalDelta = player.getPosition().subtract(sound.entity.position).length();
                        double finalDelta = player.getPosition().add(playerVelocity).subtract(sound.entity.position).add(-sound.entity.motion.x, 0D, -sound.entity.motion.z).length();
                        float dopplerFactor = (float) (initalDelta > finalDelta ? 1 + 0.25 * (initalDelta - finalDelta) / initalDelta : 1 - 0.25 * (finalDelta - initalDelta) / finalDelta);
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch * dopplerFactor);
                    } else {
                        AL10.alSourcef(sound.sourceIndex, AL10.AL_PITCH, sound.pitch);
                    }
                }
            } else {
                //We are a stopped sound.  Un-bind and delete any sources and buffers we are using.
                if (sound.radio == null) {
                    //Normal sound. Un-bind buffer and make sure we're flagged as stopped.
                    //We could have just reached the end of the sound.
                    AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
                    sound.stopSound = true;
                } else if (sound.stopSound) {
                    //Radio with stop command.  Un-bind all radio buffers.
                    int boundBuffers = AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED);
                    if (boundBuffers > 0) {
                        IntBuffer buffers = BufferUtils.createIntBuffer(boundBuffers);
                        AL10.alSourceUnqueueBuffers(sound.sourceIndex, buffers);
                    }
                }
                if (sound.stopSound) {
                    //Sound was commanded to be stopped.  Delete sound source to free up slot.
                    IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
                    sourceBuffer.put(sound.sourceIndex).flip();
                    AL10.alDeleteSources(sourceBuffer);

                    //Delete from playing list, and entity that has this sound.
                    iterator.remove();
                    sound.entity.sounds.remove(sound);
                }
            }
        }

        //Now update radio stations.
        for (RadioStation station : playingStations) {
            station.update();
        }

        //If the sound system was reset, blow out all saved data points.
        if (soundSystemReset) {
            InterfaceManager.coreInterface.logError("Had an invalid sound name.  Was the sound system reset?  Clearing all sounds, playing or not!");
            dataSourceBuffers.clear();
            for (SoundInstance sound : playingSounds) {
                sound.entity.sounds.remove(sound);
            }
            playingSounds.clear();
        }
    }

    @Override
    public void playQuickSound(SoundInstance sound) {
        if (ALC.getFunctionProvider() != null) {
            //First get the IntBuffer pointer to where this sound data is stored.
            Integer dataBufferPointer;
            try {
                dataBufferPointer = loadOGGJarSound(sound.soundPlayingName);
            } catch (Exception e) {
                // Log the error but don't disable the entire sound system
                System.err.println("[MTS Sound] Failed to load sound: " + sound.soundPlayingName + " - " + e.getMessage());
                dataBufferPointer = null;
            }
            if (dataBufferPointer != null) {
                //Set the sound's source buffer index.
                IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
                AL10.alGetError();
                AL10.alGenSources(sourceBuffer);
                if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                    AL10.alDeleteBuffers(dataBufferPointer);

                    // Try to free up a sound slot by removing the furthest sound from the player
                    if (!playingSounds.isEmpty()) {
                        if (!postedSoundWarning) {
                            InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                            postedSoundWarning = true;
                        }

                        SoundInstance furthestSound = null;
                        Point3D playerPosition = InterfaceManager.clientInterface.getClientPlayer().getPosition();
                        for (SoundInstance testSound : playingSounds) {
                            if (furthestSound == null || playerPosition.isFirstCloserThanSecond(testSound.position, furthestSound.position)) {
                                furthestSound = testSound;
                            }
                        }

                        if (furthestSound != null) {
                            //Manually stop sound and remove from iterator.
                            AL10.alSourcei(furthestSound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
                            sourceBuffer = BufferUtils.createIntBuffer(1);
                            sourceBuffer.put(furthestSound.sourceIndex).flip();
                            AL10.alDeleteSources(sourceBuffer);
                            playingSounds.remove(furthestSound);

                            // Try again with the freed slot
                            AL10.alGenSources(sourceBuffer);
                            if (AL10.alGetError() == AL10.AL_NO_ERROR) {
                                // Successfully got a source, continue with setup
                            } else {
                                // Still can't get a source, give up on this sound
                                return;
                            }
                        }
                    } else {
                        // No sounds to remove, give up
                        return;
                    }
                }
                sound.sourceIndex = sourceBuffer.get(0);

                //Set properties and bind data buffer to source.
                AL10.alGetError();
                AL10.alSourcei(sound.sourceIndex, AL10.AL_LOOPING, sound.soundDef != null && sound.soundDef.looping ? AL10.AL_TRUE : AL10.AL_FALSE);
                AL10.alSource3f(sound.sourceIndex, AL10.AL_POSITION, (float) sound.entity.position.x, (float) sound.entity.position.y, (float) sound.entity.position.z);
                AL10.alSourcei(sound.sourceIndex, AL10.AL_BUFFER, dataBufferPointer);

                //Done setting up buffer.  Queue sound to start playing.
                queuedSounds.add(sound);
                sound.entity.sounds.add(sound);

            }
        }
    }

    @Override
    public void addRadioStation(RadioStation station) {
        playingStations.add(station);
    }

    @Override
    public void addRadioSound(SoundInstance sound, Collection<Integer> buffers) {
        if (ALC.getFunctionProvider() != null) {
            //Set the sound's source buffer index.
            IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
            AL10.alGetError();
            AL10.alGenSources(sourceBuffer);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                if (!postedSoundWarning) {
                    InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                    postedSoundWarning = true;
                }
                return;
            }
            sound.sourceIndex = sourceBuffer.get(0);

            //Queue up the buffer sources to the source itself.
            for (int bufferIndex : buffers) {
                bindBuffer(sound, bufferIndex);
            }
            queuedSounds.add(sound);
        }
    }

    @Override
    public int createBuffer(ByteBuffer buffer, IStreamDecoder decoder) {
        IntBuffer newDataBuffer = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(newDataBuffer);
        AL10.alBufferData(newDataBuffer.get(0), AL10.AL_FORMAT_MONO16, buffer, decoder.getSampleRate());
        return newDataBuffer.get(0);
    }

    @Override
    public void deleteBuffer(int bufferIndex) {
        AL10.alDeleteBuffers(bufferIndex);
    }

    @Override
    public void bindBuffer(SoundInstance sound, int bufferIndex) {
        AL10.alSourceQueueBuffers(sound.sourceIndex, bufferIndex);
    }

    @Override
    public int getFreeStationBuffer(Collection<EntityRadio> playingRadios) {
        boolean freeBuffer = true;
        EntityRadio badRadio = null;
        AL10.alGetError();
        for (EntityRadio radio : playingRadios) {
            SoundInstance sound = radio.getPlayingSound();
            if (AL10.alGetSourcei(sound.sourceIndex, AL10.AL_BUFFERS_PROCESSED) == 0) {
                freeBuffer = false;
                break;
            }
            if (AL10.alGetError() == AL10.AL_INVALID_NAME) {
                badRadio = radio;
            }
        }
        if (badRadio != null) {
            badRadio.stop();
            return 0;
        } else if (freeBuffer) {
            //First get the old buffer index.
            int freeBufferIndex = 0;
            IntBuffer oldDataBuffer = BufferUtils.createIntBuffer(1);
            for (EntityRadio radio : playingRadios) {
                SoundInstance sound = radio.getPlayingSound();
                AL10.alSourceUnqueueBuffers(sound.sourceIndex, oldDataBuffer);
                if (freeBufferIndex == 0) {
                    freeBufferIndex = oldDataBuffer.get(0);
                } else if (freeBufferIndex != oldDataBuffer.get(0)) {
                    badRadio = radio;
                    break;
                }
            }
            if (badRadio != null) {
                badRadio.stop();
                return 0;
            } else {
                return freeBufferIndex;
            }
        } else {
            return 0;
        }
    }

    /**
     * Loads an OGG file in its entirety using the {@link InterfaceOGGDecoder}.
     * The sound is then stored in a dataBuffer keyed by soundName located in {@link #dataSourceBuffers}.
     * The pointer to the dataBuffer is returned for convenience as it allows for transparent sound caching.
     * If a sound with the same name is passed-in at a later time, it is assumed to be the same and rather
     * than re-parse the sound the system will simply return the same pointer index to be bound.
     */
    private static Integer loadOGGJarSound(String soundName) {
        if (dataSourceBuffers.containsKey(soundName)) {
            //Already parsed the data.  Return the buffer.
            return dataSourceBuffers.get(soundName);
        } else {
            //Need to parse the data.  Do so now.
            String soundDomain;
            String soundPath;

            // Handle cases where soundName doesn't contain domain (FMOD event names)
            int colonIndex = soundName.indexOf(':');
            if (colonIndex == -1) {
                // No domain specified, assume it's an MTS sound
                soundDomain = "mts";
                soundPath = soundName;
            } else {
                soundDomain = soundName.substring(0, colonIndex);
                soundPath = soundName.substring(colonIndex + 1);
            }

            String soundResourcePath = "/assets/" + soundDomain + "/sounds/" + soundPath + ".ogg";
            InputStream soundStream = InterfaceManager.coreInterface.getPackResource(soundResourcePath);
            if (soundStream != null) {
                //Create decoder and decode whole file.
                OGGDecoder decoder = new OGGDecoder(soundStream);
                ByteBuffer decodedData = ByteBuffer.allocateDirect(0);
                ByteBuffer blockRead;
                while ((blockRead = decoder.readBlock()) != null) {
                    decodedData = ByteBuffer.allocateDirect(decodedData.capacity() + blockRead.limit()).put(decodedData).put(blockRead);
                    decodedData.rewind();
                }

                //Generate an IntBuffer to store a pointer to the data buffer.
                IntBuffer dataBufferPointers = BufferUtils.createIntBuffer(1);
                AL10.alGenBuffers(dataBufferPointers);

                //Bind the decoder output buffer to the data buffer pointer.
                AL10.alBufferData(dataBufferPointers.get(0), AL10.AL_FORMAT_MONO16, decodedData, decoder.getSampleRate());

                //Done parsing.  Map the dataBuffer(s) to the soundName and return the index.
                dataSourceBuffers.put(soundName, dataBufferPointers.get(0));
                return dataSourceBuffers.get(soundName);
            } else {
                // Log the issue for debugging
                InterfaceManager.coreInterface.logErrorMain("Failed to find sound file: " + soundResourcePath + " (original soundName: " + soundName + ")");
                return null;
            }
        }
    }

    public static void stopAllSounds() {
        queuedSounds.clear();
        for (SoundInstance sound : playingSounds) {
            if (sound.radio != null) {
                sound.radio.stop();
            } else {
                sound.stopSound = true;
            }
        }

        //Mark world as un-paused and update sounds to stop the ones that were just removed.
        isSystemPaused = false;
        update();
    }

    /**
     * Update all sounds every client tick.
     */
    @SubscribeEvent
    public static void onIVClientTick(ClientTickEvent.Post event) {
        //We put this into a try block as sound system reloads can cause the thread to get stopped mid-execution.
        try {
            update();
            FMODSystemUpdate();
        } catch (Exception e) {
            e.printStackTrace();
            //Do nothing.  We only get exceptions here if OpenAL isn't ready.
        }
    }

    /**
     * Stop all sounds when the world is unloaded.
     */
    @SubscribeEvent
    public static void onIVWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            queuedSounds.removeIf(soundInstance -> event.getLevel() == ((WrapperWorld) soundInstance.entity.world).world);
            for (SoundInstance sound : playingSounds) {
                if (event.getLevel() == ((WrapperWorld) sound.entity.world).world) {
                    if (sound.radio != null) {
                        sound.radio.stop();
                    } else {
                        sound.stopSound = true;
                    }
                }
            }

            // Clean up FMOD instances related to this world
            FMODCleanupAllInstances();

            // Only shutdown FMOD completely if needed
            if (playingSounds.isEmpty()) {
                FMODSystemShutdown();
            }

            //Mark world as un-paused and update sounds to stop the ones that were just removed.
            isSystemPaused = false;
            update();
        }
    }

    /**
     * Gets the current FMOD status for real-time config display.
     */
    public static String getCurrentFMODStatus() {
        if (FMODAPI.isAvailable()) {
            var status = FMODAPI.getStatus();
            return status.status;
        }
        return "Not Available";
    }

    /**
     * Gets the current audio system for real-time config display.
     */
    public static String getCurrentAudioSystem() {
        if (FMODAPI.isAvailable()) {
            var status = FMODAPI.getStatus();
            return status.audioSystem;
        }
        return "OpenAL";
    }

    /**
     * Smart sound playing method that tries FMOD first, automatically falls back to original MTS sounds.
     * If eventName is provided, attempts FMOD. FMODPlaySoundEvent() already handles fallback to OpenAL.
     * If no eventName, uses original MTS sound system.
     *
     * @param sound The sound instance to play
     * @param eventName The FMOD event name (can be null for non-FMOD sounds)
     * @return true if FMOD was attempted, false if original system was used
     */
    public boolean playSmartSound(SoundInstance sound, String eventName) {
        // If we have an eventName, try FMOD (it will auto-fallback to OpenAL if FMOD unavailable)
        if (eventName != null && !eventName.isEmpty()) {
            JSONSound fmodSoundDef = new JSONSound();
            fmodSoundDef.eventName = eventName;
            SoundInstance fmodSound = new SoundInstance(sound.entity, fmodSoundDef);

            FMODPlaySoundEvent(fmodSound); // This method already handles fallback!
            return true; // Attempted FMOD (may have fallen back internally)
        } else {
            // No eventName provided - use original MTS sound system
            playQuickSound(sound);
            return false; // Used original system
        }
    }

    /**
     * Gets the current FMOD error code for real-time config display.
     */
    public static int getCurrentFMODErrorCode() {
        if (FMODAPI.isAvailable()) {
            var status = FMODAPI.getStatus();
            return status.errorCode;
        }
        return -1; // Not available
    }
}