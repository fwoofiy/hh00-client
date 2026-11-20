package dev.hh00.command;

import java.util.List;

public abstract class Command {

    private final String name;
    private final String description;
    private final List<String> aliases;

    protected Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = List.of(aliases);
    }

    public abstract void execute(String[] args);

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public boolean matches(String input) {
        if (name.equalsIgnoreCase(input)) return true;
        return aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(input));
    }

    public String getUsage() {
        return "." + name;
    }
}
