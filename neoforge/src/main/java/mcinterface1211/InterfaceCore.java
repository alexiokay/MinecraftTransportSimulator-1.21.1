package mcinterface1211;
import net.minecraft.core.registries.Registries;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.mcinterface.IInterfaceCore;
import minecrafttransportsimulator.mcinterface.IWrapperItemStack;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.InterfaceManager;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

class InterfaceCore implements IInterfaceCore {
    protected static final Map<String, List<BuilderItem>> taggedItems = new HashMap<>();

    @Override
    public boolean isGameFlattened() {
        return true;
    }

    @Override
    public boolean isModPresent(String modID) {
        return ModList.get().isLoaded(modID);
    }

    @Override
    public boolean isFluidValid(String fluidID) {
        for (ResourceLocation location : BuiltInRegistries.FLUID.keySet()) {
            if (location.getPath().equals(fluidID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getModName(String modID) {
        return ModList.get().getModContainerById(modID).get().getModInfo().getDisplayName();
    }
    
    @Override
    public InputStream getPackResource(String resource) {
        int assetsIndexEnd = resource.indexOf("assets/") + "assets/".length();
        int modIDEnd = resource.indexOf("/", assetsIndexEnd + 1);
        String modID = resource.substring(assetsIndexEnd, modIDEnd);

        // First try using NeoForge 1.21.1 ResourceManager for proper resource loading
        try {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                // On client side, use Minecraft's resource manager for proper asset loading
                String resourcePath = resource.substring(resource.indexOf("assets/") + "assets/".length());
                String namespace = resourcePath.substring(0, resourcePath.indexOf("/"));
                String path = resourcePath.substring(resourcePath.indexOf("/") + 1);

                ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, path);
                var resourceManager = Minecraft.getInstance().getResourceManager();
                var resourceOptional = resourceManager.getResource(location);
                if (resourceOptional.isPresent()) {
                    return resourceOptional.get().open();
                }
            }
        } catch (Exception e) {
            // Fall through to legacy loading methods
        }

        Optional<? extends ModContainer> optional = ModList.get().getModContainerById(modID);
        if (optional.isPresent()) {
            // In NeoForge 1.21.1, use the ModContainer's classloader directly for resource access
            ModContainer container = optional.get();

            // For content packs, try loading directly through the ModContainer's classloader
            if (!modID.equals(InterfaceLoader.MODID)) {
                try {
                    // Use the ModContainer's class loader to load resources
                    // This should work for content pack mods registered with NeoForge
                    InputStream stream = container.getClass().getClassLoader().getResourceAsStream(resource);
                    if (stream != null) {
                        InterfaceManager.coreInterface.logError("RESOURCE DEBUG: Loaded resource from mod container: " + modID + " - " + resource);
                        return stream;
                    }
                } catch (Exception e) {
                    InterfaceManager.coreInterface.logError("RESOURCE DEBUG: Failed to load from mod container: " + modID + " - " + e.getMessage());
                }
            }

            // Original loading methods as fallback
            try {
                // First try loading through the container's mod instance if available
                Class<?> modClass = Class.forName(container.getModInfo().getModId() + "." + container.getModInfo().getDisplayName().replaceAll("\\s+", ""));
                InputStream stream = modClass.getResourceAsStream(resource);
                if (stream != null) {
                    return stream;
                }
            } catch (ClassNotFoundException e) {
                // Fall through to other methods
            }

            if (modID.equals(InterfaceLoader.MODID)) {
                //For dev builds, the core files aren't in the main jar yet and are in their own compiled one.
                //This requires us to check a class of that jar vs the mod jar for the resource.
                return InterfaceManager.class.getResourceAsStream(resource);
            }
        } else {
            // Check for external content packs in the mods directory
            InputStream packStream = loadResourceFromContentPacks(resource, modID);
            if (packStream != null) {
                return packStream;
            }
        }
        //Try to get a Minecraft texture, we use the classloader of the block class, since it's common to servers and clients.
        return Blocks.AIR.getClass().getResourceAsStream(resource);
    }

    /**
     * Loads resources from external content pack JARs.
     * This handles content packs that are not registered as mods but placed in the mods directory.
     */
    private InputStream loadResourceFromContentPacks(String resource, String modID) {
        try {
            // For development environment, check the MTSOfficialPack directory directly
            // This is needed because during early mod loading, the pack isn't available as a JAR yet
            if ("mtsofficialpack".equals(modID)) {
                File packDir = new File("../MTSOfficialPack-1.21.1/src/main/resources");
                if (!packDir.exists()) {
                    // Try alternative path
                    packDir = new File("C:/Users/alexispace/Desktop/webdev/minecraft/MTSOfficialPack-1.21.1/src/main/resources");
                }

                if (packDir.exists()) {
                    // Remove leading slash if present
                    String resourcePath = resource.startsWith("/") ? resource.substring(1) : resource;
                    // If resource starts with "assets/", remove it since it's already in the resources folder
                    if (resourcePath.startsWith("assets/")) {
                        resourcePath = resourcePath.substring("assets/".length());
                        // Now add "assets/" back as part of the directory structure
                        resourcePath = "assets/" + resourcePath;
                    }

                    File resourceFile = new File(packDir, resourcePath);
                    if (resourceFile.exists()) {
                        try {
                            InterfaceLoader.LOGGER.info("MTS: Loading resource from development pack directory: {}", resource);
                            return new FileInputStream(resourceFile);
                        } catch (IOException e) {
                            InterfaceLoader.LOGGER.warn("MTS: Failed to read from development pack: {}", e.getMessage());
                        }
                    }
                }
            }

            // Get the mods directory - this works for both dev and production environments
            File modsDir = new File("run/mods");
            if (!modsDir.exists()) {
                // Fallback for different directory structures
                modsDir = new File("mods");
            }

            if (modsDir.exists() && modsDir.isDirectory()) {
                // Look for JAR files that might contain the modID
                for (File file : modsDir.listFiles()) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                        try (JarFile jarFile = new JarFile(file)) {
                            // Check if this JAR contains resources for our modID
                            ZipEntry entry = jarFile.getEntry(resource);
                            if (entry != null) {
                                InterfaceLoader.LOGGER.info("MTS: Found resource {} in content pack {}", resource, file.getName());
                                // Read the data into memory to avoid closed JarFile issues
                                try (InputStream entryStream = jarFile.getInputStream(entry)) {
                                    byte[] data = entryStream.readAllBytes();
                                    return new ByteArrayInputStream(data);
                                }
                            }
                        } catch (IOException e) {
                            InterfaceLoader.LOGGER.warn("MTS: Failed to read content pack {}: {}", file.getName(), e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            InterfaceLoader.LOGGER.error("MTS: Error loading resource {} from content packs: {}", resource, e.getMessage());
        }
        return null;
    }

    @Override
    public void logError(String message) {
        InterfaceLoader.LOGGER.error("MTSERROR: " + message);
    }

    @Override
    public IWrapperNBT getNewNBTWrapper() {
        return new WrapperNBT();
    }

    @Override
    public IWrapperItemStack getAutoGeneratedStack(AItemBase item, IWrapperNBT data) {
        WrapperItemStack newStack = new WrapperItemStack(new ItemStack(BuilderItem.itemMap.get(item)));
        newStack.setData(data);
        return newStack;
    }

    @Override
    public IWrapperItemStack getStackForProperties(String name, int meta, int qty) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(name));
        if (item != null) {
            return new WrapperItemStack(new ItemStack(item, qty));
        } else {
            return new WrapperItemStack(ItemStack.EMPTY.copy());
        }
    }

    @Override
    public String getStackItemName(IWrapperItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(((WrapperItemStack) stack).stack.getItem()).toString();
    }

    @Override
    public boolean isOredictMatch(IWrapperItemStack stackA, IWrapperItemStack stackB) {
        return !((WrapperItemStack) stackA).stack.isEmpty() && ((WrapperItemStack) stackA).stack.is(((WrapperItemStack) stackB).stack.getItem());
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<IWrapperItemStack> getOredictMaterials(String oreName, int stackSize) {
        //Convert to lowercase in case we are camelCase from oreDict systems.
        //Also do a bunch of stupid stream crap, cause hashmaps are clearly not made to lookup things...
        String lowerCaseOre = oreName.toLowerCase(Locale.ROOT);
        List<IWrapperItemStack> stacks = new ArrayList<>();
        Stream<TagKey<Item>> tagStream = BuiltInRegistries.ITEM.getTagNames().filter(tagKey -> tagKey.location().getPath().equals(lowerCaseOre));
        tagStream.forEach(tagKey -> {
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tagKey)) {
                stacks.add(new WrapperItemStack(new ItemStack(holder.value(), stackSize)));
            }
        });
        //Couldn't find normal OreDict, check our internal stuff.
        if (stacks.isEmpty()) {
            List<BuilderItem> items = taggedItems.get(lowerCaseOre);
            if (items != null) {
                items.forEach(item -> stacks.add(new WrapperItemStack(new ItemStack(item, stackSize))));
            }
        }

        return stacks;
    }
}
