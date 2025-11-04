package dev.auto.trims;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class PluginBootDriver implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY.newHandler(event -> {
            context.getLogger().info("The following datapacks were found: {}",
                String.join(", ", event.registrar().getDiscoveredPacks().keySet())
            );
        }));
    }
}
