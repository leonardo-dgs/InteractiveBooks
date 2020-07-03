package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("%openbook")
@CommandPermission("%interactivebooks.open")
public final class CommandOpenBook extends BaseCommand {

    private final IBook book;

    public CommandOpenBook(IBook book) {
        super();
        this.book = book;
    }

    @Default
    @CatchUnknown
    public void onCommand(Player player) {
        book.open(player);
    }

}
