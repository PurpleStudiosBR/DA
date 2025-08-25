package br.com.purplemc.purpleesconde.commands;

import br.com.purplemc.purpleesconde.PurpleEsconde;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CosmeticCommand implements CommandExecutor {

    private final PurpleEsconde plugin;

    public CosmeticCommand(PurpleEsconde plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cApenas jogadores podem usar este comando!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            plugin.getCosmeticManager().openCosmeticGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("shop") || args[0].equalsIgnoreCase("loja")) {
            plugin.getCosmeticManager().openCosmeticGUI(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("coins") || args[0].equalsIgnoreCase("money")) {
            double coins = plugin.getCosmeticManager().getPlayerCoins(player);
            player.sendMessage("§6Você tem " + (int)coins + " coins.");
            return true;
        }

        String cosmeticId = args[0].toLowerCase();
        if (plugin.getCosmeticManager().hasCosmetic(player, cosmeticId)) {
            if (plugin.getCosmeticManager().setPlayerCosmetic(player, cosmeticId)) {
                player.sendMessage("§aCosmético equipado com sucesso!");
            } else {
                player.sendMessage("§cErro ao equipar cosmético!");
            }
        } else {
            player.sendMessage("§cVocê não possui este cosmético! Use §e/cosmetic §cpara abrir a loja.");
        }

        return true;
    }
}