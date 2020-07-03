package net.leonardo_dgs.interactivebooks.util;

enum BookEventActionType {

    SHOW_TEXT("show text", "tooltip"),
    SHOW_ITEM("show item", "item"),
    SHOW_ENTITY("show entity", "entity"),

    RUN_COMMAND("run command", "command", "cmd"),
    SUGGEST_COMMAND("suggest command", "suggest cmd", "suggest"),
    OPEN_URL("open url", "url", "link"),
    CHANGE_PAGE("change page");

    static BookEventActionType parse(String type) {
        for (BookEventActionType actionType : values())
            for (String identifier : actionType.getAliases())
                if (type.equalsIgnoreCase(identifier))
                    return actionType;
        return null;
    }

    private final String[] aliases;

    BookEventActionType(String... aliases) {
        this.aliases = aliases;
    }

    public String[] getAliases() {
        return aliases;
    }

}
