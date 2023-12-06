package ch.jmcommand.mahtimer;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;

public final class MAH_Timer extends JavaPlugin implements Listener {
    private FileConfiguration config;
    private BukkitTask currentTask;
    private BossBar bossBar;
    private int timeLeft;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getLogger().info("\u001B[33m+----------------+");
        getLogger().info("\u001B[33m| \u001B[32mMAH-TIMER \u001B[33m|");
        getLogger().info("\u001B[33m+----------------+");
        getLogger().info("\u001B[32mMAH-TIMER a été démarré avec succès!");

        // Enregistrement des écouteurs d'événements
        getServer().getPluginManager().registerEvents(this, this);

        this.getCommand("mahtimer").setExecutor(this);
        Permission stopPermission = new Permission("mahtimer.stop");
        getServer().getPluginManager().addPermission(stopPermission);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Vérifie si la commande est 'mahtimer'
        if (cmd.getName().equalsIgnoreCase("mahtimer") && args.length > 0) {
            if (args[0].equalsIgnoreCase("stop")) {
                // Vérifie si le joueur a la permission
                if (sender.hasPermission("mahtimer.stop")) {
                    stopAllTimers();
                    sender.sendMessage(ChatColor.GREEN + "Le timer MAH a été arrêté.");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission de faire cela.");
                    return true;
                }
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        String escapeGameMap = config.getString("escapeGameMap", "NomDeVotreMapEscapeGame");
        String finalMap = config.getString("escapeGameMapFinal", "NomDeVotreMapEscapeGameFinal");

        if (event.getTo().getWorld().getName().equals(escapeGameMap)) {
            stopAllTimers();
            startTimer(player, escapeGameMap);
        } else if (event.getTo().getWorld().getName().equals(finalMap)) {
            stopTimer();
        }
    }

    private void teleportPlayerOnTimerEnd(Player player) {
        String loseMap = config.getString("escapeGameMapLose", "NomDeVotreMapLose");
        World loseWorld = Bukkit.getWorld(loseMap);
        if (loseWorld != null) {
            player.teleport(loseWorld.getSpawnLocation());
        } else {
            getLogger().warning("Le monde '" + loseMap + "' n'existe pas. Impossible de téléporter le joueur.");
        }
    }

    private void startTimer(Player player, String escapeGameMap) {
        int timerDuration = config.getInt("timerDuration", 1800);
        String finalMap = config.getString("escapeGameMapFinal", "NomDeVotreMapEscapeGameFinal");
        String loseMap = config.getString("escapeGameMapLose", "NomDeVotreMapLose");
        timeLeft = timerDuration;

        if (currentTask != null) {
            currentTask.cancel();
        }

        bossBar = Bukkit.createBossBar("", BarColor.GREEN, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setVisible(true);

        currentTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    teleportPlayerOnTimerEnd(player); // Téléporte le joueur
                    bossBar.setProgress(1.0);
                    bossBar.setTitle(ChatColor.RED + config.getString("bossBarLose", "Temps écoulé! Merci d'avoir joué / visité le musée!"));
                    bossBar.setColor(BarColor.RED);
                    this.cancel();
                    Bukkit.getScheduler().runTaskLater(MAH_Timer.this, () -> bossBar.setVisible(false), 200L);
                    return;
                }

                bossBar.setTitle(formatTime(timeLeft));
                bossBar.setProgress((double) timeLeft / timerDuration);
                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private String formatTime(int timeInSeconds) {
        int hours = timeInSeconds / 3600;
        int minutes = (timeInSeconds % 3600) / 60;
        int seconds = timeInSeconds % 60;

        return ChatColor.GOLD + String.format("%02d:%02d:%02d", hours, minutes, seconds) + " restants";
    }

    private void stopTimer() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        if (bossBar != null) {
            bossBar.setProgress(1.0);
            bossBar.setTitle(ChatColor.GREEN + config.getString("bossBarWin", "Bravo! Vous avez réussi / visité le musée!"));
            bossBar.setColor(BarColor.GREEN);
            Bukkit.getScheduler().runTaskLater(this, () -> bossBar.setVisible(false), 200L);
        }
    }

    private void stopAllTimers() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
    }

    @Override
    public void onDisable() {
        stopAllTimers();
        getLogger().info("\u001B[31mMAH-TIMER a été désactivé avec succès!");
    }
}