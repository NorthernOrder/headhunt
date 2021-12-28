package xyz.nrth.headhunt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Headhunt extends JavaPlugin implements Listener {

    private static final String killsConstant = "Kills";
    private static Headhunt headhunt;
    private final Map<UUID, List<UUID>> killLists = new HashMap<>();

    @Override
    public void onEnable() {
        headhunt = this;
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("startgame").setExecutor(new StartCommand());
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player killed = e.getEntity();
        Player killer = killed.getKiller();
        if (killer == null) return;
        List<UUID> list = killLists.get(killer.getUniqueId());
        if (killed.equals(killer) || list.contains(killed.getUniqueId())) return;

        list.add(killed.getUniqueId());

        if (list.size() != Bukkit.getOnlinePlayers().size()) return;

        sendCommand("gamemode spectator @a");
        Bukkit.broadcast(Component.text(killer.getName()).append(Component.text(" has won the game!")));
    }

    public void sendCommand(String command) {
        getServer().dispatchCommand(getServer().getConsoleSender(), command);
    }

    public void startGame() {
        sendCommand("effect give @a minecraft:slow_falling 20 1");
        sendCommand("setworldspawn 0 200 0");
        sendCommand("gamerule spawnRadius 100");
        sendCommand("difficulty normal");
        sendCommand("gamerule doDaylightCycle true");
        sendCommand("gamerule doWeatherCycle false");
        sendCommand("gamerule keepInventory true");
        sendCommand("worldborder center 0 0");
        sendCommand("worldborder set 512");
        sendCommand("gamemode survival @a");
        sendCommand("clear @a");
        sendCommand("tp @a 0 200 0");
        sendCommand("time set 0");
        sendCommand("weather clear");
        Bukkit.getOnlinePlayers().forEach((player) -> {
            ItemStack item = new ItemStack(Material.BOOK);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(killsConstant));
            meta.lore(Collections.singletonList(Component.text(killsConstant)));
            item.setItemMeta(meta);
            player.getInventory().setItem(EquipmentSlot.HAND, item);
        });
        Bukkit.getScheduler().runTaskLater(this, () -> {
            sendCommand("worldborder set 100 600");
        }, 20 * 10 * 60);
    }

    public static Headhunt getPlugin() {
        return headhunt;
    }

    @EventHandler
    public void onUseItem(PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (item == null || item.getType() != Material.BOOK) return;

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta.displayName() == null || itemMeta.lore() == null || itemMeta.lore().isEmpty()) return;

        PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText()
        String name = serializer.serialize(itemMeta.displayName());
        String lore = serializer.serialize(itemMeta.lore().get(0));
        if (!name.equals(killsConstant) || !lore.equals(killsConstant)) return;

        Inventory inv = Bukkit.createInventory(null, 56, Component.text(killsConstant));

        for (UUID killed : killLists.get(e.getPlayer().getUniqueId())) {
            ItemStack skullItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
            Player killedPlayer = Bukkit.getPlayer(killed);
            skullMeta.setOwningPlayer(killedPlayer);
            skullMeta.displayName(Component.text(killedPlayer.getName()));
            skullItem.setItemMeta(skullMeta);
            inv.addItem(skullItem);
        }

        e.getPlayer().openInventory(inv);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (killLists.containsKey(e.getPlayer().getUniqueId())) return;

        killLists.put(e.getPlayer().getUniqueId(), new ArrayList<>());
    }
}
