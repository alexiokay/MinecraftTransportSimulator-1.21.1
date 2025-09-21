package mcinterface1211;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import minecrafttransportsimulator.mcinterface.InterfaceManager;

/**
 * Custom ResourceProvider that makes content pack JAR resources available to NeoForge's ResourceManager.
 * This bridges the gap between MTS content pack system and NeoForge 1.21.1 resource loading.
 *
 * @author don_bruce
 */
public class ContentPackResourceProvider extends AbstractPackResources {

    private final Map<ResourceLocation, JarResourceInfo> availableResources = new HashMap<>();
    private final Set<String> availableNamespaces = new HashSet<>();

    private static class JarResourceInfo {
        final File jarFile;
        final String entryPath;

        JarResourceInfo(File jarFile, String entryPath) {
            this.jarFile = jarFile;
            this.entryPath = entryPath;
        }
    }

    public ContentPackResourceProvider() {
        super(new PackLocationInfo("mts_content_packs",
                                  net.minecraft.network.chat.Component.literal("MTS Content Packs"),
                                  net.minecraft.server.packs.repository.PackSource.DEFAULT,
                                  java.util.Optional.empty()));
        scanContentPacks();
    }

    private void scanContentPacks() {
        try {
            File modsDir = new File("run/mods");
            if (!modsDir.exists()) {
                modsDir = new File("mods");
            }

            if (modsDir.exists() && modsDir.isDirectory()) {
                InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Scanning content pack resources");

                for (File file : modsDir.listFiles()) {
                    if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
                        if (isContentPack(file.getName())) {
                            scanJarResources(file);
                        }
                    }
                }

                InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Found " + availableResources.size() + " resources in " + availableNamespaces.size() + " namespaces");
                for (String namespace : availableNamespaces) {
                    InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Namespace: " + namespace);
                }
            }
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Failed to scan content packs: " + e.getMessage());
        }
    }

    private boolean isContentPack(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.contains("pack") ||
               lowerName.contains("content") ||
               lowerName.contains("mts") ||
               lowerName.contains("official") ||
               lowerName.contains("transport") ||
               lowerName.contains("vehicle");
    }

    private void scanJarResources(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            var entries = jar.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (entryName.startsWith("assets/") && !entry.isDirectory()) {
                    // Parse the resource location from the entry path
                    String resourcePath = entryName.substring("assets/".length());
                    int firstSlash = resourcePath.indexOf('/');

                    if (firstSlash > 0) {
                        String namespace = resourcePath.substring(0, firstSlash);
                        String path = resourcePath.substring(firstSlash + 1);

                        ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath(namespace, path);
                        availableResources.put(resourceLocation, new JarResourceInfo(jarFile, entryName));
                        availableNamespaces.add(namespace);

                        InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Registered " + resourceLocation);
                    }
                }
            }
        } catch (Exception e) {
            InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Failed to scan JAR " + jarFile.getName() + ": " + e.getMessage());
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        // Check if requesting pack.mcmeta
        if (paths.length == 1 && "pack.mcmeta".equals(paths[0])) {
            return () -> {
                // Return a basic pack.mcmeta for our content pack provider
                String packMeta = "{\n" +
                    "  \"pack\": {\n" +
                    "    \"pack_format\": 26,\n" +
                    "    \"description\": \"MTS Content Pack Resources\"\n" +
                    "  }\n" +
                    "}";
                return new java.io.ByteArrayInputStream(packMeta.getBytes());
            };
        }
        return null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType packType, ResourceLocation location) {
        if (packType != PackType.CLIENT_RESOURCES) {
            return null;
        }

        JarResourceInfo info = availableResources.get(location);
        if (info != null) {
            InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Providing resource stream for: " + location);
            return () -> {
                try {
                    JarFile jar = new JarFile(info.jarFile);
                    ZipEntry entry = jar.getEntry(info.entryPath);
                    if (entry != null) {
                        // Read the entire content into memory to avoid jar file handle issues
                        InputStream entryStream = jar.getInputStream(entry);
                        byte[] content = entryStream.readAllBytes();
                        entryStream.close();
                        jar.close();
                        InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Successfully read " + content.length + " bytes for: " + location);
                        return new java.io.ByteArrayInputStream(content);
                    } else {
                        jar.close();
                        InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Entry not found in JAR: " + info.entryPath);
                    }
                } catch (IOException e) {
                    InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Failed to get resource " + location + ": " + e.getMessage());
                }
                return null;
            };
        }

        return null;
    }

    @Override
    public void listResources(PackType packType, String namespace, String path, ResourceOutput resourceOutput) {
        if (packType != PackType.CLIENT_RESOURCES) {
            return;
        }

        for (Map.Entry<ResourceLocation, JarResourceInfo> entry : availableResources.entrySet()) {
            ResourceLocation resourceLocation = entry.getKey();
            if (resourceLocation.getNamespace().equals(namespace) && resourceLocation.getPath().startsWith(path)) {
                JarResourceInfo info = entry.getValue();
                resourceOutput.accept(resourceLocation, () -> {
                    try {
                        JarFile jar = new JarFile(info.jarFile);
                        ZipEntry zipEntry = jar.getEntry(info.entryPath);
                        if (zipEntry != null) {
                            // Read the entire content into memory to avoid jar file handle issues
                            InputStream entryStream = jar.getInputStream(zipEntry);
                            byte[] content = entryStream.readAllBytes();
                            entryStream.close();
                            jar.close();
                            return new java.io.ByteArrayInputStream(content);
                        } else {
                            jar.close();
                        }
                    } catch (IOException e) {
                        InterfaceManager.coreInterface.logError("RESOURCE PROVIDER: Failed to list resource " + resourceLocation + ": " + e.getMessage());
                    }
                    return null;
                });
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType packType) {
        if (packType == PackType.CLIENT_RESOURCES) {
            return new HashSet<>(availableNamespaces);
        }
        return Set.of();
    }

    @Override
    public void close() {
        // Resources are closed when streams are closed
    }

    public PackLocationInfo location() {
        return new PackLocationInfo("mts_content_packs",
                                  net.minecraft.network.chat.Component.literal("MTS Content Packs"),
                                  net.minecraft.server.packs.repository.PackSource.DEFAULT,
                                  java.util.Optional.empty());
    }
}