package dev.hh00.command.commands;

import dev.hh00.Client;
import dev.hh00.command.Command;
import dev.hh00.core.KeyBind;
import dev.hh00.core.Module;
import dev.hh00.util.ChatUtil;
import dev.hh00.util.KeyUtil;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Bind a module to a key", "b");
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            ChatUtil.send("\u00a7cUsage: " + getUsage());
            return;
        }

        Module module = Client.get().getModuleManager().get(args[0]);
        if (module == null) {
            ChatUtil.send("\u00a7cModule \u00a7f" + args[0] + "\u00a7c not found");
            return;
        }

        int key = KeyUtil.parse(args[1]);
        if (key < 0) {
            ChatUtil.send("\u00a7cUnknown key \u00a7f" + args[1]);
            return;
        }

        module.getKeyBind().setKey(key);
        Client.get().getConfigManager().save();

        ChatUtil.send("\u00a7aBound \u00a7f" + module.getName() + "\u00a7a to \u00a7f"
                + (key == KeyBind.NONE ? "NONE" : KeyUtil.getName(key)));
    }

    @Override
    public String getUsage() {
        return ".bind <module> <key>";
    }
}
