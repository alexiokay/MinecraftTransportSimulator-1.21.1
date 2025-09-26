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
import org.lwjgl.PointerBuffer;
import org.lwjgl.fmod.FMOD;
import org.lwjgl.fmod.FMODStudio;
import org.lwjgl.fmod.FMOD_3D_ATTRIBUTES;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.system.MemoryUtil;

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
import org.lwjgl.system.MemoryStack;

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
     * Current FMOD status for real-time config display
     **/
    private static String currentFMODStatus = "Not initialized";
    private static String currentAudioSystem = "Pending";
    private static int currentFMODErrorCode = -1;

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
     * This gets incremented whenever we try to get a source and fail.  If we get to 10, the sound system
     * will stop attempting to play sounds.  Used for when mods take all the sources.
     **/
    private static byte sourceGetFailures = 0;
    private static boolean postedSoundWarning;

    private static long fmodSystem;
    private static final String RESET  = "\u001B[0m";
    private static final String GREEN  = "\u001B[32m";
    public static final String RED    = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static boolean pausedForMenu = false;

    public static void FMODSystemInit() {
        // Skip FMOD initialization on server side - servers don't need audio
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD initialization skipped on server side" + RESET);
            fmodSystem = 0;
            currentFMODStatus = "Skipped (server side)";
            currentAudioSystem = "None";
            currentFMODErrorCode = 0;
            ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pp = stack.mallocPointer(1);
            int result = FMODStudio.FMOD_Studio_System_Create(pp, FMOD.FMOD_VERSION);
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system create failed: error code=" + result + " - continuing anyway." + RESET);
                currentFMODStatus = "Create failed";
                currentAudioSystem = "OpenAL (fallback)";
                currentFMODErrorCode = result;
                ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
                return;
            }
            fmodSystem = pp.get(0);

            // Get low-level system to configure output type before initialization
            PointerBuffer lowLevelSystem = stack.mallocPointer(1);
            result = FMODStudio.FMOD_Studio_System_GetCoreSystem(fmodSystem, lowLevelSystem);
            if (result == FMOD.FMOD_OK) {
                long coreSystem = lowLevelSystem.get(0);
                // Set output type to WASAPI for better shared mode support
                result = FMOD.FMOD_System_SetOutput(coreSystem, FMOD.FMOD_OUTPUTTYPE_WASAPI);
                if (result != FMOD.FMOD_OK) {
                    InterfaceManager.coreInterface.logInfo(GREEN + "FMOD failed to set WASAPI output (code=" + result + "), using default output" + RESET);
                } else {
                    InterfaceManager.coreInterface.logInfo(GREEN + "FMOD using WASAPI output for shared mode compatibility" + RESET);
                }
            }

            int maxChannels = 128; // Increase channel limit to prevent exhaustion
            int studioFlags = FMODStudio.FMOD_STUDIO_INIT_NORMAL;
            // Use flags that allow hardware sharing with other audio systems
            int flags = FMOD.FMOD_INIT_NORMAL | FMOD.FMOD_INIT_MIX_FROM_UPDATE;
            result = FMODStudio.FMOD_Studio_System_Initialize(
                    fmodSystem, maxChannels, studioFlags, flags, 0);
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system initialization failed: error code=" + result + " - continuing anyway." + RESET);
                currentFMODStatus = "Initialize failed";
                currentAudioSystem = "OpenAL (fallback)";
                currentFMODErrorCode = result;
                ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
                fmodSystem = 0;
                return;
            }

            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system successfully created and initialized" + RESET);
            currentFMODStatus = "Successfully initialized";
            currentAudioSystem = "FMOD";
            currentFMODErrorCode = 0;
            ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);

            // Load bank files from direct file paths (original working method)
            FMODLoadBank("fmod/Master.bank");
            FMODLoadBank("fmod/Master.strings.bank");
            FMODLoadBank("fmod/Weapons.bank");

            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system ready with events: test_event, test_voice, alarm, explosion, m1919, tank_shot, vehicle_explosion, jet_flyby, ship_alarm, etc." + RESET);
        } catch (Exception e) {
            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system initialization failed with exception: " + e.getMessage() + " - continuing anyway." + RESET);
            currentFMODStatus = "Exception: " + e.getMessage();
            currentAudioSystem = "OpenAL (fallback)";
            currentFMODErrorCode = -1;
            ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
            fmodSystem = 0;
        }
    }

    public static void FMODUpdateListener() {
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();

        Point3D position = player.getPosition();
        Point3D forward = player.getLineOfSight(1.0).normalize();
        //Point3D up = player.getUpVector(1.0f).normalize();
        Point3D velocity = player.getVelocity();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FMOD_3D_ATTRIBUTES attrs = FMOD_3D_ATTRIBUTES.malloc(stack);
            attrs.position$().set((float) position.x, (float) position.y, (float) -position.z);
            attrs.velocity().set((float) velocity.x, (float) velocity.y, (float) -velocity.z);
            attrs.forward().set((float) forward.x, (float) forward.y, (float) -forward.z);
            attrs.up().set(0f, 1f, -0f);
            int result = FMODStudio.FMOD_Studio_System_SetListenerAttributes(fmodSystem, 0, attrs, null);
            if (result != FMOD.FMOD_OK) {
                System.err.println("SetListenerAttributes failed: result=" + result);
            }
        }
    }

    public static void FMODSystemShutdown() {
        // Check if FMOD system is initialized before trying to shut it down
        if (fmodSystem == 0) {
            return; // Nothing to shutdown
        }

        // Clean up all active FMOD instances before shutdown
        FMODCleanupAllInstances();

        int result = FMODStudio.FMOD_Studio_System_Release(fmodSystem);
        if (result != FMOD.FMOD_OK) {
            InterfaceManager.coreInterface.logErrorMain(RED + "FMOD system release failed: error code=" + result + RESET);
            currentFMODStatus = "Shutdown failed";
            currentAudioSystem = "Unknown";
            currentFMODErrorCode = result;
            ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
        } else {
            currentFMODStatus = "Shutdown";
            currentAudioSystem = "None";
            currentFMODErrorCode = 0;
            ConfigBridge.updateFMODStatus(currentFMODStatus, currentAudioSystem, currentFMODErrorCode);
        }
        fmodSystem = 0;
    }

    private static void FMODSystemUpdate() {
        if (fmodSystem == 0) return;
        IWrapperPlayer player = InterfaceManager.clientInterface.getClientPlayer();
        if (!InterfaceManager.clientInterface.isGamePaused() && player != null) {
            if (pausedForMenu) {
                FMODSetMasterPaused(false);
                pausedForMenu = false;
            }
            FMODUpdateListener();
        } else {
            if (!pausedForMenu) {
                FMODSetMasterPaused(true);
                pausedForMenu = true;
            }
        }

        // Clean up finished FMOD instances
        FMODCleanupFinishedInstances();

        int result = FMODStudio.FMOD_Studio_System_Update(fmodSystem);
        if (result != FMOD.FMOD_OK) {
            InterfaceManager.coreInterface.logErrorMain(RED + "FMOD system update failed: error code=" + result + RESET);
        }
    }

    private static void FMODLoadBank(String path) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer bankPtr = stack.mallocPointer(1);
            int result = FMODStudio.FMOD_Studio_System_LoadBankFile(
                    fmodSystem,
                    stack.UTF8(path, true),
                    FMODStudio.FMOD_STUDIO_LOAD_BANK_NORMAL,
                    bankPtr
            );
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD system failed to load bank: " + '"' + path + '"' + ":" + result + RESET);
                //System.err.println("Unable to load bank '" + path + "': " + result);
            } else {
                //System.out.println("Successfully loaded FMOD bank: " + '"' + path + '"');
                InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system successfully loaded bank: " + '"' + path + '"' + RESET);
            }
        }
    }

    private static void FMODSetMasterPaused(boolean pause) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer busPtr = stack.mallocPointer(1);
            int result = FMODStudio.FMOD_Studio_System_GetBus(
                    fmodSystem, stack.UTF8("bus:/", true), busPtr
            );
            if (result == FMOD.FMOD_OK) {
                long bus = busPtr.get(0);
                FMODStudio.FMOD_Studio_Bus_SetPaused(bus, pause ? 1 : 0);
            }
        }
    }

    private static void FMODLoadBankFromResource(String resourcePath) {
        try {
            // Try multiple strategies to load the bank file from resources
            InputStream bankStream = null;

            // Strategy 1: Try direct class loader
            bankStream = InterfaceSound.class.getResourceAsStream(resourcePath);

            // Strategy 2: Try context class loader if first failed
            if (bankStream == null) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                bankStream = contextClassLoader.getResourceAsStream(resourcePath.substring(1)); // Remove leading /
            }

            // Strategy 3: Try the core module's class loader
            if (bankStream == null) {
                bankStream = InterfaceManager.coreInterface.getClass().getResourceAsStream(resourcePath);
            }

            if (bankStream == null) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD bank resource not found: " + resourcePath + RESET);
                return;
            }

            // Read all bytes from the input stream
            byte[] bankData = bankStream.readAllBytes();
            bankStream.close();

            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD loaded bank data from resource: " + resourcePath + ", size=" + bankData.length + " bytes" + RESET);

            // Extract to temporary file and use LoadBankFile method
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            java.io.File tempFile = new java.io.File(System.getProperty("java.io.tmpdir"), "fmod_" + fileName);

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(bankData);
            }

            InterfaceManager.coreInterface.logInfo(GREEN + "FMOD extracted bank to temp file: " + tempFile.getAbsolutePath() + RESET);

            // Use LoadBankFile method like the original working implementation
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer bankPtr = stack.mallocPointer(1);
                int result = FMODStudio.FMOD_Studio_System_LoadBankFile(
                        fmodSystem,
                        stack.UTF8(tempFile.getAbsolutePath(), true),
                        FMODStudio.FMOD_STUDIO_LOAD_BANK_NORMAL,
                        bankPtr
                );

                if (result == FMOD.FMOD_OK) {
                    InterfaceManager.coreInterface.logInfo(GREEN + "FMOD system successfully loaded bank from resource: " + resourcePath + " via temp file" + RESET);
                } else {
                    InterfaceManager.coreInterface.logErrorMain(RED + "FMOD system failed to load bank from resource: " + resourcePath + ", error code=" + result + RESET);
                }
            }

            // Clean up temp file
            tempFile.deleteOnExit();

        } catch (Exception e) {
            InterfaceManager.coreInterface.logErrorMain(RED + "Exception loading FMOD bank from resource: " + resourcePath + ", error: " + e.getMessage() + RESET);
        }
    }



    public void FMODPlaySoundEvent(SoundInstance sound) {
        // Check if FMOD system is initialized - if not, fallback to OpenAL
        if (fmodSystem == 0) {
            InterfaceManager.coreInterface.logInfo("FMOD system not available, falling back to OpenAL for sound: " + (sound.soundDef != null ? sound.soundDef.name : sound.soundName));
            playQuickSound(sound);
            return;
        }

        // Enforce maximum instance limit to prevent resource exhaustion
        if (activeFMODInstances.size() >= MAX_FMOD_INSTANCES) {
            InterfaceManager.coreInterface.logErrorMain(RED + "Maximum FMOD instances (" + MAX_FMOD_INSTANCES + ") reached, cleaning up oldest instances" + RESET);
            FMODCleanupFinishedInstances();

            // If still at limit, remove oldest instance
            if (activeFMODInstances.size() >= MAX_FMOD_INSTANCES && !activeFMODInstances.isEmpty()) {
                String oldestKey = activeFMODInstances.keySet().iterator().next();
                long oldestInstance = activeFMODInstances.remove(oldestKey);
                FMODStudio.FMOD_Studio_EventInstance_Stop(oldestInstance, FMODStudio.FMOD_STUDIO_STOP_IMMEDIATE);
                FMODStudio.FMOD_Studio_EventInstance_Release(oldestInstance);
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Use the FMOD eventName from JSON if available, otherwise fall back to soundPlayingName
            String fmodEventName = (sound.soundDef != null && sound.soundDef.eventName != null)
                    ? sound.soundDef.eventName
                    : sound.soundPlayingName;

            // Create a unique key that allows multiple sounds per entity but prevents exact duplicates
            String instanceKey = fmodEventName + "_" + sound.entity.uniqueUUID + "_" + System.nanoTime();

            // For looping sounds, use a simpler key to prevent stacking unless forced
            if (sound.soundDef != null && sound.soundDef.looping && !sound.soundDef.forceSound) {
                String loopingKey = fmodEventName + "_" + sound.entity.uniqueUUID + "_looping";
                if (activeFMODInstances.containsKey(loopingKey)) {
                    return; // Already playing this looping sound
                }
                instanceKey = loopingKey;
            }

            PointerBuffer descPtr = stack.mallocPointer(1);
            ByteBuffer eventName = stack.UTF8("event:/" + fmodEventName);
            int result = FMODStudio.FMOD_Studio_System_GetEvent(
                    fmodSystem,
                    eventName,
                    descPtr
            );
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD system failed to find event: " + '"' + fmodEventName + '"' + ", error code=" + result + ", falling back to OpenAL" + RESET);
                // Fallback to original MTS sound system - exactly like before FMOD
                playQuickSound(sound);
                return;
            }
            long description = descPtr.get(0);

            // Create an instance of the event.
            PointerBuffer instancePtr = stack.mallocPointer(1);
            result = FMODStudio.FMOD_Studio_EventDescription_CreateInstance(description, instancePtr);
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD failed to create instance for '" + fmodEventName + "': " + result + RESET);

                // If we're out of memory or channels, try cleanup and retry once
                if (result == FMOD.FMOD_ERR_MEMORY || result == FMOD.FMOD_ERR_CHANNEL_STOLEN) {
                    FMODCleanupFinishedInstances();
                    result = FMODStudio.FMOD_Studio_EventDescription_CreateInstance(description, instancePtr);
                    if (result != FMOD.FMOD_OK) {
                        InterfaceManager.coreInterface.logErrorMain(RED + "FMOD failed to create instance after cleanup for '" + fmodEventName + "': " + result + RESET);
                        return;
                    }
                } else {
                    return;
                }
            }
            long instance = instancePtr.get(0);

            // Track this instance
            activeFMODInstances.put(instanceKey, instance);

            FMOD_3D_ATTRIBUTES attributes = FMOD_3D_ATTRIBUTES.calloc(stack);
            attributes.position$().set((float) sound.entity.position.x, (float) sound.entity.position.y, (float) -sound.entity.position.z);
            attributes.velocity().set(0f, 0f, -0f);
            attributes.forward().set(0f, 0f, -1f);
            attributes.up().set(0f, 1f, -0f);
            result = FMODStudio.FMOD_Studio_EventInstance_Set3DAttributes(instance, attributes);
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD failed to set 3D attributes for '" + fmodEventName + "': " + result + RESET);
            }

            // Start the event.  This begins playback.
            result = FMODStudio.FMOD_Studio_EventInstance_Start(instance);
            if (result != FMOD.FMOD_OK) {
                InterfaceManager.coreInterface.logErrorMain(RED + "FMOD failed to start event '" + fmodEventName + "': " + result + RESET);
                // Remove from tracking if start failed
                activeFMODInstances.remove(instanceKey);
            } else {
                if (FMODStudio.FMOD_Studio_EventInstance_Get3DAttributes(instance, attributes) == FMOD.FMOD_OK) {
                    InterfaceManager.coreInterface.logInfo(GREEN + "Playing sound event " + '"' + fmodEventName + '"' + " at: " + attributes.position$().x() + ", " + attributes.position$().y() + ", " + attributes.position$().z() + RESET);
                }
            }
        }
    }

    /**
     * Cleans up finished FMOD event instances to prevent memory leaks.
     */
    private static void FMODCleanupFinishedInstances() {
        if (fmodSystem == 0 || activeFMODInstances.isEmpty()) return;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer playbackState = stack.mallocInt(1);
            Iterator<Map.Entry<String, Long>> iterator = activeFMODInstances.entrySet().iterator();
            int cleanedCount = 0;

            while (iterator.hasNext()) {
                Map.Entry<String, Long> entry = iterator.next();
                long instance = entry.getValue();

                int result = FMODStudio.FMOD_Studio_EventInstance_GetPlaybackState(instance, playbackState);
                if (result == FMOD.FMOD_OK) {
                    int state = playbackState.get(0);
                    // Clean up stopped, stopping, or fading instances
                    if (state == FMODStudio.FMOD_STUDIO_PLAYBACK_STOPPED ||
                        state == FMODStudio.FMOD_STUDIO_PLAYBACK_STOPPING) {
                        FMODStudio.FMOD_Studio_EventInstance_Release(instance);
                        iterator.remove();
                        cleanedCount++;
                    }
                } else {
                    // If we can't get playback state, assume it's invalid and clean it up
                    FMODStudio.FMOD_Studio_EventInstance_Release(instance);
                    iterator.remove();
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                InterfaceManager.coreInterface.logInfo(GREEN + "Cleaned up " + cleanedCount + " finished FMOD instances" + RESET);
            }
        }
    }

    /**
     * Cleans up all active FMOD event instances.
     */
    private static void FMODCleanupAllInstances() {
        if (fmodSystem == 0 || activeFMODInstances.isEmpty()) return;

        for (long instance : activeFMODInstances.values()) {
            // Stop the instance first, then release it
            FMODStudio.FMOD_Studio_EventInstance_Stop(instance, FMODStudio.FMOD_STUDIO_STOP_IMMEDIATE);
            FMODStudio.FMOD_Studio_EventInstance_Release(instance);
        }
        activeFMODInstances.clear();
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
            sourceGetFailures = 0;
        }
    }

    @Override
    public void playQuickSound(SoundInstance sound) {
        if (ALC.getFunctionProvider() != null && sourceGetFailures < 10) {
            //First get the IntBuffer pointer to where this sound data is stored.
            Integer dataBufferPointer;
            try {
                dataBufferPointer = loadOGGJarSound(sound.soundPlayingName);
            } catch (Exception e) {
                if (++sourceGetFailures == 10) {
                    InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSYSTEM);
                }
                dataBufferPointer = null;
            }
            if (dataBufferPointer != null) {
                //Set the sound's source buffer index.
                IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
                AL10.alGetError();
                AL10.alGenSources(sourceBuffer);
                if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                    AL10.alDeleteBuffers(dataBufferPointer);
                    if (++sourceGetFailures == 10) {
                        if (!postedSoundWarning) {
                            InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                            postedSoundWarning = true;
                        }
                        ///Kill off the sound that's furthest from the player to make room if we have a sound we can remove.
                        //This keeps the sounds going, even with limited slots.
                        if (!playingSounds.isEmpty()) {
                            SoundInstance furthestSound = null;
                            Point3D playerPosition = InterfaceManager.clientInterface.getClientPlayer().getPosition();
                            for (SoundInstance testSound : playingSounds) {
                                if (furthestSound == null || playerPosition.isFirstCloserThanSecond(testSound.position, furthestSound.position)) {
                                    furthestSound = testSound;
                                }
                            }
                            sourceGetFailures = 0;
                            //Manually stop sound and remove from iterator.
                            //This makes the source entity think that it's still playing and won't re-add it.
                            AL10.alSourcei(furthestSound.sourceIndex, AL10.AL_BUFFER, AL10.AL_NONE);
                            sourceBuffer = BufferUtils.createIntBuffer(1);
                            sourceBuffer.put(furthestSound.sourceIndex).flip();
                            AL10.alDeleteSources(sourceBuffer);
                            playingSounds.remove(furthestSound);
                        }
                    }
                    return;
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
        if (ALC.getFunctionProvider() != null && sourceGetFailures < 10) {
            //Set the sound's source buffer index.
            IntBuffer sourceBuffer = BufferUtils.createIntBuffer(1);
            AL10.alGetError();
            AL10.alGenSources(sourceBuffer);
            if (AL10.alGetError() != AL10.AL_NO_ERROR) {
                if (++sourceGetFailures == 10) {
                    if (!postedSoundWarning) {
                        InterfaceManager.clientInterface.getClientPlayer().displayChatMessage(LanguageSystem.SYSTEM_SOUNDSLOT);
                        postedSoundWarning = true;
                    }
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
            String soundDomain = soundName.substring(0, soundName.indexOf(':'));
            String soundPath = soundName.substring(soundDomain.length() + 1);
            InputStream soundStream = InterfaceManager.coreInterface.getPackResource("/assets/" + soundDomain + "/sounds/" + soundPath + ".ogg");
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
        return currentFMODStatus;
    }

    /**
     * Gets the current audio system for real-time config display.
     */
    public static String getCurrentAudioSystem() {
        return currentAudioSystem;
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
        return currentFMODErrorCode;
    }
}