package mcinterface1211;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.player.AbstractClientPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

/**
 * Simple test to see if we can hook into player rendering events
 */
@EventBusSubscriber(Dist.CLIENT)
public class TestPlayerAnimation {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPlayerAnimation.class);

    @SubscribeEvent
    public static void onPlayerRender(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
            LOGGER.error("TEST: Player render event triggered for: {}", player.getName().getString());
        }
    }
}