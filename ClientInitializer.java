package dev.hh00;

import dev.hh00.event.events.Render2DEvent;
import dev.hh00.event.events.TickEvent;
import dev.hh00.gui.HH00Screen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

/**
 * Fabric entry point. Wires vanilla game callbacks into the client event bus.
 */
public class ClientInitializer implements ClientModInitializer {

    private static KeyBinding openMenu;

    @Override
    public void onInitializeClient() {
        Client client = Client.get();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.hh00.menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("hh00", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            while (openMenu.wasPressed()) {
                if (minecraft.currentScreen == null) {
                    minecraft.setScreen(new HH00Screen());
                }
            }
            client.getEventBus().post(new TickEvent());
        });

        HudRenderCallback.EVENT.register((context, tickCounter) ->
                client.getEventBus().post(new Render2DEvent(context, tickCounter)));

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            String prefix = client.getCommandManager().getPrefix();
            if (message.startsWith(prefix)) {
                client.getCommandManager().execute(message.substring(prefix.length()));
                return false;
            }
            return true;
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(minecraft -> client.shutdown());

        client.init();
    }
}
