package ml.nextuniverse.StaffUtilitiesBukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.kitteh.vanish.VanishManager;
import org.kitteh.vanish.event.VanishStatusChangeEvent;
import org.kitteh.vanish.staticaccess.VanishNoPacket;
import org.kitteh.vanish.staticaccess.VanishNotLoadedException;
import redis.clients.jedis.Jedis;

import java.util.HashMap;

public class Main extends JavaPlugin implements Listener {
    private HashMap<String, Location> spectate = new HashMap<>();
    private HashMap<String, GameMode> spectateGamemode = new HashMap<>();

    VanishManager vanish;
    @Override
    public void onEnable() {
        try {
            vanish = VanishNoPacket.getManager();
        }
        catch (VanishNotLoadedException e) {
            getLogger().warning("VanishNoPacket is not installed!");
        }
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onVanishStatus(VanishStatusChangeEvent e) {
        if (e.isVanishing()) {
            Jedis jedis = new Jedis("localhost");
            jedis.publish("StaffUtils", "VanishStart;" + e.getPlayer().getName());
        }
        else {
            Jedis jedis = new Jedis("localhost");
            jedis.publish("StaffUtils", "VanishEnd;" + e.getPlayer().getName());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("spectate")) {
            if (sender.hasPermission("staff.spectate") && sender instanceof Player) {
                if (args.length == 0) {
                    if (spectate.containsKey(sender.getName())) {
                        Location l = spectate.get(sender.getName());
                        ((Player) sender).teleport(l);
                        GameMode g = spectateGamemode.get(sender.getName());
                        ((Player) sender).setGameMode(g);
                        sender.sendMessage(ChatColor.AQUA + "You have stopped spectating and have been returned to your original location.");
                        Player s = (Player) sender;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.showPlayer(s);
                        }
                        Jedis jedis = new Jedis("localhost");
                        jedis.publish("StaffUtils", "SpectateEnd;" + sender.getName());
                        spectate.remove(sender.getName());
                        spectateGamemode.remove(sender.getName());
                    }
                    else {
                        if (!spectate.containsKey(sender.getName())) {
                            spectate.put(sender.getName(), ((Player) sender).getLocation());
                            spectateGamemode.put(sender.getName(), ((Player) sender).getGameMode());
                        }
                        ((Player) sender).setGameMode(GameMode.SPECTATOR);
                        Player s = (Player) sender;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.hidePlayer(s);
                        }
                        sender.sendMessage(ChatColor.AQUA + "You are now spectating, type " + ChatColor.WHITE + "/spectate [player]" + ChatColor.AQUA + " to spectate a player. Type " + ChatColor.WHITE + "/spectate" + ChatColor.AQUA + " to stop spectating.");
                        Jedis jedis = new Jedis("localhost");
                        jedis.publish("StaffUtils", "SpectateStart;" + sender.getName());

                    }
                }
                else if (args.length == 1) {
                    if (Bukkit.getPlayer(args[0]).isOnline()) {
                        Player target = Bukkit.getPlayer(args[0]);
                        if (!spectate.containsKey(sender.getName())) {
                            spectate.put(sender.getName(), ((Player) sender).getLocation());
                            spectateGamemode.put(sender.getName(), ((Player) sender).getGameMode());
                        }
                        ((Player) sender).teleport(target);
                        ((Player) sender).setGameMode(GameMode.SPECTATOR);
                        Player s = (Player) sender;
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.hidePlayer(s);
                        }
                        sender.sendMessage(ChatColor.AQUA + "You are now spectating. Type " + ChatColor.WHITE + "/spectate" + ChatColor.AQUA + " to stop spectating.");
                        Jedis jedis = new Jedis("localhost");
                        jedis.publish("StaffUtils", "SpectateStart;" + sender.getName());
                    }
                    else {
                        sender.sendMessage(ChatColor.RED + "Player " + ChatColor.RESET + args[0] + ChatColor.RED + " not found.");
                    }
                }
            }
            else {

                if (sender instanceof ConsoleCommandSender) sender.sendMessage(ChatColor.RED + "Nope.avi");
                else sender.sendMessage(ChatColor.RED + "You do not have permission to perform that command");
            }
            return true;
        }
        return false;
    }
    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        Player sender = e.getPlayer();
        if (spectate.containsKey(sender.getName())) {
            Location l = spectate.get(sender.getName());
            sender.teleport(l);
            GameMode g = spectateGamemode.get(sender.getName());
            sender.setGameMode(g);
            Player s = sender;
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(s);
            }
            Jedis jedis = new Jedis("localhost");
            jedis.publish("StaffUtils", "SpectateEnd;" + e.getPlayer().getName());
            spectate.remove(sender.getName());
            spectateGamemode.remove(sender.getName());
        }
    }
}
