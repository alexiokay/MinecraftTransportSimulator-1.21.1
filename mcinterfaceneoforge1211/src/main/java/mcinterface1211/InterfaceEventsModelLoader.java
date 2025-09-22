package mcinterface1211;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import minecrafttransportsimulator.packloading.PackParser;
import minecrafttransportsimulator.systems.ConfigSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * Interface for handling events pertaining to loading models into MC.  This used to handle mainly item models, but
 * now it just re-directs texture calls for the main core mod to allow them to work in development with the referenced
 * core library files that MC doesn't see normally.
 *
 * @author don_bruce
 */
public class InterfaceEventsModelLoader {
    public static PackResourcePack packPack = new PackResourcePack();

    /**
     * Called to init the custom model loader.  Should be done before any other things.
     * This allows injecting our custom resource manager into MC's systems to have it use it.
     * We do this by registering it as a reload listener, as on a resource reload (and boot) MC will purge the list
     * of packs and will re-query from disk.  But we aren't on disk, and so we will need to be
     * ready when that call comes and will re-add ourselves.
     */
    public static void init() {
        packPack.domains.addAll(PackParser.getAllPackIDs());
        InterfaceManager.coreInterface.logError("MODEL LOADER INIT: Initialized with " + packPack.domains.size() + " domains");
    }

    /**
     * Register additional models for content pack items.
     * This is the modern NeoForge 1.21.1 approach to registering custom item models.
     */
    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        try {
            // Register models for all pack items
            for (AItemPack<?> packItem : PackParser.getAllPackItems()) {
                if (packItem != null) {
                    // Use the item's pack ID to determine the correct namespace
                    String itemPackID = packItem.definition.packID;
                    String registrationName = packItem.getRegistrationName();

                    String namespace, itemName;

                    if (itemPackID.equals(InterfaceLoader.MODID)) {
                        // MTS core items - register in mts namespace
                        namespace = InterfaceLoader.MODID;
                        itemName = registrationName;
                    } else {
                        // Content pack items - register in pack namespace
                        namespace = itemPackID;
                        // Remove pack prefix from registration name if present
                        if (registrationName.startsWith(itemPackID + ".")) {
                            itemName = registrationName.substring(itemPackID.length() + 1);
                        } else {
                            itemName = registrationName;
                        }
                    }

                    // Register the standalone model for this item in the correct namespace
                    ModelResourceLocation modelLocation = ModelResourceLocation.standalone(
                        ResourceLocation.fromNamespaceAndPath(namespace, itemName)
                    );

                    event.register(modelLocation);

                    if (ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("MODEL EVENT: Registered model for pack item: " + modelLocation);
                    }
                }
            }
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("Failed to register pack item models: " + e.getMessage());
        }
    }

    /**
     * Custom ResourcePack class for auto-generating item JSONs.
     */
    public static class PackResourcePack implements PackResources {
        private final Set<String> domains;
        private final Set<String> fakeDomains;
        private final Map<String, String> generatedModels;

        private PackResourcePack() {
            super();
            domains = new HashSet<>();
            fakeDomains = new HashSet<>();
            generatedModels = new HashMap<>();
            fakeDomains.add(InterfaceLoader.MODID);
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            // Add debug logging to see what's being requested
            InterfaceManager.coreInterface.logError("PACK RESOURCE REQUEST: " + type + " -> " + location);

            //First check if we should even process this resource.
            // Handle both pack namespaces and the MTS namespace for JSON models
            boolean shouldProcess = (domains.contains(location.getNamespace()) || domains.contains(getPackID(location.getPath()))) && (location.getPath().endsWith(".png") || location.getPath().endsWith(".json"));

            // Also handle MTS namespace JSON model requests specifically
            if (!shouldProcess && location.getNamespace().equals(InterfaceLoader.MODID) && location.getPath().contains("models/item/") && location.getPath().endsWith(".json")) {
                shouldProcess = true;
            }

            if (shouldProcess) {
                String path = location.getPath();

                // Handle PNG textures (existing logic)
                if (path.endsWith(".png")) {
                    //Create stream return variable and get raw data.
                    InputStream stream;
                    String domain = !location.getNamespace().equals(InterfaceLoader.MODID) ? location.getNamespace() : getPackID(location.getPath());
                    String rawPackInfo = location.getPath();
                    String streamLocation = "/assets/" + domain + "/" + rawPackInfo;
                    stream = InterfaceManager.coreInterface.getPackResource(streamLocation);
                    if (stream == null && !streamLocation.contains("/assets/mts/textures/mcfont")) {
                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("Couldn't find requested PNG: " + streamLocation);
                        }
                    }
                    //Return whichever stream we found.
                    final InputStream streamForSupplier = stream;
                    return () -> streamForSupplier;
                }

                // Handle JSON models - redirect to correct content pack namespace
                else if (path.endsWith(".json") && path.contains("models/item/")) {
                    return handleItemModelRequest(location);
                }
            }
            return null;
        }

        /**
         * Handles item model requests by redirecting to the correct content pack namespace
         * or generating fallback models if none exist.
         */
        private IoSupplier<InputStream> handleItemModelRequest(ResourceLocation location) {
            String path = location.getPath();
            String namespace = location.getNamespace();

            // For content pack namespaces, look for models directly using the item name
            if (!namespace.equals(InterfaceLoader.MODID) && domains.contains(namespace)) {
                // Extract item name from path like "models/item/enginequad.json"
                if (path.contains("models/item/") && path.endsWith(".json")) {
                    String itemPath = path.substring(path.indexOf("models/item/") + 12); // Remove "models/item/"
                    String itemName = itemPath.substring(0, itemPath.length() - 5); // Remove ".json"

                    if (ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("CONTENT PACK MODEL REQUEST: " + namespace + ":" + itemName);
                    }

                    // Try to find existing model in various locations within the content pack
                    String[] possiblePaths = {
                        "/assets/" + namespace + "/models/item/" + itemName + ".json",
                        "/assets/" + namespace + "/models/item/parts/" + itemName + ".json",
                        "/assets/" + namespace + "/models/item/items/" + itemName + ".json",
                        "/assets/" + namespace + "/models/item/vehicles/" + itemName + ".json",
                        "/assets/" + namespace + "/models/item/decors/" + itemName + ".json"
                    };

                    for (String possiblePath : possiblePaths) {
                        InputStream stream = InterfaceManager.coreInterface.getPackResource(possiblePath);
                        if (stream != null) {
                            if (ConfigSystem.settings.general.devMode.value) {
                                InterfaceManager.coreInterface.logError("CONTENT PACK MODEL SUCCESS: Found model at " + possiblePath);
                            }
                            final InputStream streamForSupplier = stream;
                            return () -> streamForSupplier;
                        }
                    }

                    // If no model found, generate a fallback
                    if (ConfigSystem.settings.general.devMode.value) {
                        InterfaceManager.coreInterface.logError("CONTENT PACK MODEL: No existing model found, generating fallback for " + namespace + ":" + itemName);
                    }
                    return generateItemModel(location, namespace, itemName);
                }
            }

            // Handle legacy MTS namespace requests (old format like "mtsofficialpack.itemname")
            if (path.contains("models/item/") && path.contains(".")) {
                String itemPath = path.substring(path.indexOf("models/item/") + 12); // Remove "models/item/"
                if (itemPath.endsWith(".json")) {
                    itemPath = itemPath.substring(0, itemPath.length() - 5); // Remove ".json"

                    if (itemPath.contains(".")) {
                        String[] parts = itemPath.split("\\.", 2);
                        String packID = parts[0];
                        String itemName = parts[1];

                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("LEGACY MODEL REDIRECT: Looking for " + packID + ":" + itemName);
                        }

                        // First try to find existing model in the content pack namespace
                        String redirectedPath = "/assets/" + packID + "/models/item/" + itemName + ".json";
                        InputStream stream = InterfaceManager.coreInterface.getPackResource(redirectedPath);

                        if (stream != null) {
                            if (ConfigSystem.settings.general.devMode.value) {
                                InterfaceManager.coreInterface.logError("LEGACY MODEL REDIRECT SUCCESS: Found model at " + redirectedPath);
                            }
                            final InputStream streamForSupplier = stream;
                            return () -> streamForSupplier;
                        }

                        // If no existing model, try other common locations
                        String[] possiblePaths = {
                            "/assets/" + packID + "/models/item/items/" + itemName + ".json",
                            "/assets/" + packID + "/models/item/parts/" + itemName + ".json",
                            "/assets/" + packID + "/models/item/vehicles/" + itemName + ".json",
                            "/assets/" + packID + "/models/item/decors/" + itemName + ".json"
                        };

                        for (String possiblePath : possiblePaths) {
                            stream = InterfaceManager.coreInterface.getPackResource(possiblePath);
                            if (stream != null) {
                                if (ConfigSystem.settings.general.devMode.value) {
                                    InterfaceManager.coreInterface.logError("LEGACY MODEL REDIRECT SUCCESS: Found model at " + possiblePath);
                                }
                                final InputStream streamForSupplier = stream;
                                return () -> streamForSupplier;
                            }
                        }

                        // If no model found, generate a fallback
                        if (ConfigSystem.settings.general.devMode.value) {
                            InterfaceManager.coreInterface.logError("LEGACY MODEL REDIRECT: No existing model found, generating fallback for " + packID + ":" + itemName);
                        }
                        return generateItemModel(location, packID, itemName);
                    }
                }
            }

            // Fallback to original generation method
            return generateItemModel(location);
        }

        /**
         * Generates a basic item model JSON for content pack items.
         * Creates a simple generated model that references the item's texture.
         */
        private IoSupplier<InputStream> generateItemModel(ResourceLocation location) {
            return generateItemModel(location, null, null);
        }

        private IoSupplier<InputStream> generateItemModel(ResourceLocation location, String packID, String itemName) {
            String cacheKey = location.toString();

            // Check cache first
            if (generatedModels.containsKey(cacheKey)) {
                final String cachedModel = generatedModels.get(cacheKey);
                return () -> new ByteArrayInputStream(cachedModel.getBytes());
            }

            try {
                // Extract item information from path
                // Expected path format: models/item/{packID.itemName}.json
                String path = location.getPath();
                String namespace = location.getNamespace();

                // Remove models/item/ prefix and .json suffix
                String itemPath = path.substring("models/item/".length(), path.length() - 5);

                // Use provided pack ID and item name if available, otherwise parse from path
                String actualPackID, actualItemName;
                if (packID != null && itemName != null) {
                    actualPackID = packID;
                    actualItemName = itemName;
                } else {
                    // Parse pack ID and item name from the registration name format (packID.itemName)
                    if (itemPath.contains(".")) {
                        int dotIndex = itemPath.indexOf('.');
                        actualPackID = itemPath.substring(0, dotIndex);
                        actualItemName = itemPath.substring(dotIndex + 1);
                    } else {
                        actualPackID = namespace;
                        actualItemName = itemPath;
                    }
                }

                // Generate texture path based on item path
                String texturePath = resolveTexturePath(actualPackID, actualItemName);

                // Create basic item model JSON
                String modelJson = generateBasicItemModel(texturePath);

                // Cache the generated model
                generatedModels.put(cacheKey, modelJson);

                if (ConfigSystem.settings.general.devMode.value) {
                    InterfaceManager.coreInterface.logError("Generated item model for: " + location + " -> texture: " + texturePath);
                }

                final String finalModelJson = modelJson;
                return () -> new ByteArrayInputStream(finalModelJson.getBytes());

            } catch (Exception e) {
                if (ConfigSystem.settings.general.devMode.value) {
                    InterfaceManager.coreInterface.logError("Failed to generate item model for: " + location + " - " + e.getMessage());
                }

                // Return fallback model on error
                String fallbackModel = generateFallbackItemModel();
                final String finalFallbackModel = fallbackModel;
                return () -> new ByteArrayInputStream(finalFallbackModel.getBytes());
            }
        }

        /**
         * Resolves the texture path for an item based on content pack structure conventions.
         */
        private String resolveTexturePath(String packID, String itemName) {
            // Use the same format as existing models: "packid:item/itemname"
            return packID + ":item/" + itemName;
        }

        /**
         * Generates a basic item model JSON with the specified texture.
         */
        private String generateBasicItemModel(String texturePath) {
            return "{\n" +
                   "  \"parent\": \"item/generated\",\n" +
                   "  \"textures\": {\n" +
                   "    \"layer0\": \"" + texturePath + "\"\n" +
                   "  }\n" +
                   "}";
        }

        /**
         * Generates a fallback item model for when texture cannot be determined.
         */
        private String generateFallbackItemModel() {
            return "{\n" +
                   "  \"parent\": \"item/generated\",\n" +
                   "  \"textures\": {\n" +
                   "    \"layer0\": \"minecraft:item/barrier\"\n" +
                   "  }\n" +
                   "}";
        }

        @Override
        public Set<String> getNamespaces(PackType pType) {
            // Return both fake domains and all pack domains to ensure we handle all requests
            Set<String> allNamespaces = new HashSet<>();
            allNamespaces.addAll(fakeDomains);
            allNamespaces.addAll(domains);
            return allNamespaces;
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> pDeserializer) {
            return null;
        }

        @Override
        public String packId() {
            return InterfaceLoader.MODID + "_packs";
        }

        @Override
        public void close() {
        }

        @Override
        public IoSupplier<InputStream> getRootResource(String... pElements) {
            String pFileName = String.join("/", pElements);
            if (!pFileName.contains("/") && !pFileName.contains("\\")) {
                return this.getResource(PackType.CLIENT_RESOURCES, ResourceLocation.withDefaultNamespace(pFileName));
            } else {
                throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
            }
        }

        @Override
        public net.minecraft.server.packs.PackLocationInfo location() {
            return new net.minecraft.server.packs.PackLocationInfo("mts_generated", net.minecraft.network.chat.Component.literal("MTS Generated Resources"), net.minecraft.server.packs.repository.PackSource.DEFAULT, java.util.Optional.empty());
        }

        @Override
        public void listResources(PackType pType, String pNamespace, String pPath, PackResources.ResourceOutput pResourceOutput) {
            //Don't list resources.  Ours are on-demand and we don't handle the cached items/models.
        }

        private static String getPackID(String path) {
            int distanceToFirstDot = path.indexOf(".");
            int distanceToSlashBefore = path.lastIndexOf("/", distanceToFirstDot);
            if (distanceToSlashBefore != -1) {
                String packID = path.substring(distanceToSlashBefore + 1, distanceToFirstDot);
                if (PackParser.getAllPackIDs().contains(packID)) {
                    return packID;
                }
            }
            //Not an actual pack resource, must be from core.
            return InterfaceLoader.MODID;
        }
    }
}
