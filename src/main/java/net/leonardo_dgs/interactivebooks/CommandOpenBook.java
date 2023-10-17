package net.leonardo_dgs.interactivebooks;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;

@CommandAlias("%openbook")
@CommandPermission("%interactivebooks.open")
final class CommandOpenBook extends BaseCommand {
    private final IBook book;

    CommandOpenBook(IBook book) {
        super();
        this.book = book;
    }

    @Default
    @CatchUnknown
    void onCommand(Player player) {
        book.open(player);
    }
}
