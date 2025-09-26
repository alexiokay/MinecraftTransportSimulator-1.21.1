package mcinterface1211;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Client-only configuration screen registration.
 * This class is only loaded on the client side to avoid server-side issues.
 */
@OnlyIn(Dist.CLIENT)
public class ClientConfigRegistration {

    /**
     * Registers the config screen factory for the mod.
     * This method is called via reflection from InterfaceLoader.
     */
    public static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}