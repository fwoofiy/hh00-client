package dev.hh00.command.commands;

import dev.hh00.Client;
import dev.hh00.command.Command;
import dev.hh00.core.Module;
import dev.hh00.util.ChatUtil;

public class ToggleCommand extends Command {

    public ToggleCommand() {
        super("toggle", "Toggle a module on or off", "t");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtil.send("\u00a7cUsage: " + getUsage() + " <module>");
            return;
        }

        Module module = Client.get().getModuleManager().get(args[0]);
        if (module == null) {
            ChatUtil.send("\u00a7cModule \u00a7f" + args[0] + "\u00a7c not found");
            return;
        }

        module.toggle();
        ChatUtil.send((module.isEnabled() ? "\u00a7aEnabled " : "\u00a7cDisabled ") + module.getName());
    }

    @Override
    public String getUsage() {
        return ".toggle <module>";
    }
}
