package dev.hh00.command.commands;

import dev.hh00.Client;
import dev.hh00.command.Command;
import dev.hh00.util.ChatUtil;

public class HelpCommand extends Command {

    public HelpCommand() {
        super("help", "List every command", "h", "?");
    }

    @Override
    public void execute(String[] args) {
        ChatUtil.send("\u00a77Commands:");
        for (Command command : Client.get().getCommandManager().getCommands()) {
            ChatUtil.send("\u00a7b" + command.getUsage() + " \u00a77- " + command.getDescription());
        }
    }
}
