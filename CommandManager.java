package dev.hh00.command;

import dev.hh00.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public final class CommandManager {

    private final List<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public void register(Command command) {
        commands.add(command);
    }

    public void execute(String raw) {
        String line = raw.trim();
        if (line.isEmpty()) return;

        String[] parts = line.split("\\s+");
        String name = parts[0];
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        for (Command command : commands) {
            if (command.matches(name)) {
                try {
                    command.execute(args);
                } catch (Exception e) {
                    ChatUtil.send("\u00a7cError: " + e.getMessage());
                }
                return;
            }
        }

        ChatUtil.send("\u00a7cUnknown command \u00a7f" + name + "\u00a7c. Try \u00a7f" + prefix + "help");
    }

    public Collection<Command> getCommands() {
        return commands;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
