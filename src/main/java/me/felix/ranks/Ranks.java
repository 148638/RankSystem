package me.felix.ranks;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class Ranks extends JavaPlugin implements Listener {

    FileConfiguration config = getConfig();

    @Override
    public void onEnable() {
        // Plugin startup logic
        config.options().copyDefaults(true);
        saveConfig();
        // Register events
        Bukkit.getServer().getPluginManager().registerEvents(this, this);
        // Check to see if default rank exists
        if (config.get("ranks.default") == null) {
            Bukkit.getLogger().info("Default rank does not exist, creating...");
            config.set("ranks.default", "default");
            config.set("ranks.default.prefix", "[Default]");
            config.set("ranks.default.color", "§f");
            config.set("ranks.default.permissions", "ranks.default");
            saveConfig();
            Bukkit.getLogger().info("Default rank created!");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    // Tab Completer
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender.hasPermission("ranks.use")) && !(sender.isOp())) return null;
        if (command.getName().equalsIgnoreCase("rank")) {
            if (args.length == 1) {
                completions.add("add");
                completions.add("delete");
                completions.add("list");
                completions.add("set");
                completions.add("edit");
                completions.add("reload");
                completions.add("help");
                completions.add("color");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("edit")) {
                    completions.addAll(config.getConfigurationSection("ranks").getKeys(false));
                } else if (args[0].equalsIgnoreCase("set")) {
                    // Add the usernames of all online players to completions
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                if (args[0].equalsIgnoreCase("set")) {
                    completions.addAll(config.getConfigurationSection("ranks").getKeys(false));
                } else if (args[0].equalsIgnoreCase("edit")) {
                    completions.add("prefix");
                    completions.add("color");
                    completions.add("permissions");
                }
            } else if (args.length == 4) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("permissions")) {
                        completions.add("add");
                        completions.add("delete");
                        completions.add("list");
                    }
                }
            } else if (args.length == 5) {
                if (args[0].equalsIgnoreCase("edit")) {
                    if (args[2].equalsIgnoreCase("permissions")) {
                        if (args[3].equalsIgnoreCase("delete")) {
                            completions.addAll(config.getStringList("ranks." + args[1] + ".permissions"));
                        }
                    }
                }
            }
        }
        Collections.sort(completions);
        return completions;
    }

    String[] alliases = {"rank", "ranks:rank", "ranks:r", "r"};

    // Command
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("ranks.use")) && !(sender.isOp())) {
            sender.sendMessage(ChatColor.RED + "You do not have permission!");
            return true;
        }
        if (Arrays.stream(alliases).anyMatch(label::equalsIgnoreCase)) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "Usage: /rank <add|delete|list|set>");
                return true;
            }
            //      /rank add <name> <prefix> <permissions> <color>
            if (args[0].equalsIgnoreCase("add")) {
                if (args.length == 5) {
                    // Check if name exists in config.yml file
                    if (config.get("ranks." + args[1]) != null) {
                        sender.sendMessage(ChatColor.RED + "Rank " + args[1] + " already exists! Use /rank edit <name> to edit it.");
                        return true;
                    }
                    String name = args[1];
                    String prefix = args[2];
                    String permissions = args[3];
                    String color = args[4];
                    config.set("ranks." + name + ".prefix", prefix);
                    // Add permissions to list
                    List<String> permissionsList = new ArrayList<>();
                    permissionsList.add(permissions);
                    config.set("ranks." + name + ".permissions", permissionsList);
                    config.set("ranks." + name + ".color", "§" + color);
                    saveConfig();
                    sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " added!");
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /rank add <name> <prefix> <permissions> <color>");
                    return true;
                }
                //      /rank edit <name> <prefix|permission|color> <value>
            } else if (args[0].equalsIgnoreCase("edit")) {
                if (args[2].equalsIgnoreCase("prefix")) {
                    if (args.length == 4) {
                        String name = args[1];
                        String prefix = args[3];
                        config.set("ranks." + name + ".prefix", prefix);
                        saveConfig();
                        sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " prefix set to " + prefix);
                        // Set the prefix of all online players with the rank to the new prefix
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            String rank = config.getString("players." + player.getName() + ".rank");
                            if (rank.toString().equalsIgnoreCase(name.toString())) {
                                updatePlayer(player);
                            }
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> prefix <prefix>");
                        return true;
                    }
                } else if (args[2].equalsIgnoreCase("permissions")) {
                    String name = args[1];
                    // Add permissions to list
                    List<String> permissionsList = new ArrayList<>();
                    // Get old permissions
                    permissionsList.addAll(config.getStringList("ranks." + name + ".permissions"));
                    if (args.length == 5) {
                        String permissions = args[4];
                        if (args[3].equalsIgnoreCase("add")) {
                            permissionsList.add(permissions);
                            config.set("ranks." + name + ".permissions", permissionsList);
                            saveConfig();
                            sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " permissions set to " + permissionsList);
                        } else if (args[3].equalsIgnoreCase("delete")) {
                            permissionsList.remove(permissions);
                            config.set("ranks." + name + ".permissions", permissionsList);
                            saveConfig();
                            sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " permissions set to " + permissionsList);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> permissions <add|delete|list> <?permission>");
                            return true;
                        }
                        // Set the prefix of all online players with the rank to the new prefix
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            String rank = config.getString("players." + player.getName() + ".rank");
                            if (rank.toString().equalsIgnoreCase(name.toString())) {
                                updatePlayer(player);
                            }
                        }
                        return true;
                    } else if (args.length == 4) {
                        if (args[3].equalsIgnoreCase("list")) {
                            sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " permissions: " + permissionsList);
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> permissions <add|delete|list> <?permission>");
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> permissions <add|delete|list> <?permission>");
                        return true;
                    }
                } else if (args[2].equalsIgnoreCase("color")) {
                    if (args.length == 4) {
                        String name = args[1];
                        String color = args[3];
                        config.set("ranks." + name + ".color", "§" + color);
                        saveConfig();
                        sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " color set to " + color);
                        // Set the prefix of all online players with the rank to the new prefix
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            String rank = config.getString("players." + player.getName() + ".rank");
                            if (rank.toString().equalsIgnoreCase(name.toString())) {
                                updatePlayer(player.getPlayer());
                            }
                        }
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> color <color>");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /rank edit <name> <prefix|permissions|color>");
                    return true;
                }
            }
            //      /rank delete <name>
            else if (args[0].equalsIgnoreCase("delete")) {
                if (args.length == 2) {
                    String name = args[1];
                    if (config.get("ranks." + name) == null) {
                        sender.sendMessage(ChatColor.RED + "Rank " + name + " does not exist!");
                        return true;
                    } else {
                        config.set("ranks." + name, null);
                        sender.sendMessage(ChatColor.YELLOW + "Rank " + name + " removed!");
                        saveConfig();

                        // Set the prefix of all online players with the rank to the new prefix
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            String rank = config.getString("players." + player.getName() + ".rank");
                            if (rank.toString().equalsIgnoreCase(name.toString())) {
                                config.set("players." + player.getName() + ".rank", "default");
                                updatePlayer(player);
                                saveConfig();
                            }
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /rank delete <name>");
                    return true;
                }
            }
            //      /Rank list & /Rank list <name>
            else if (args[0].equalsIgnoreCase("list")) {
                if (args.length == 1) {
                    breakLine(sender);
                    sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Ranks:");
                    for (String rank : config.getConfigurationSection("ranks").getKeys(false)) {
                        // Get color
                        String color = config.getString("ranks." + rank + ".color");
                        // Clickable message
                        TextComponent message = new TextComponent(ChatColor.translateAlternateColorCodes('&', "- " + color + rank));
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rank list " + rank));
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.YELLOW + "Click to view rank info").create()));
                        sender.spigot().sendMessage(message);
                    }
                    breakLine(sender);
                    return true;
                } else if (args.length == 2) {
                    String name = args[1];
                    // Check if rank exists in config.yml file
                    if (config.contains("ranks." + name)) {
                        String color = config.getString("ranks." + name + ".color");
                        breakLine(sender);
                        sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + name + ":");
                        sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Prefix: " + ChatColor.RESET + "" + color + config.getString("ranks." + name + ".prefix"));
                        sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Permissions: " + ChatColor.RESET + "" + config.getString("ranks." + name + ".permissions"));
                        // Display all players in rank
                        sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "Players: " + ChatColor.RESET + "" + getPlayersInRank(name));
                        breakLine(sender);
                        return true;
                    } else {
                        sender.sendMessage(ChatColor.RED + "Rank " + name + " does not exist!");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /rank list <name>");
                    return true;
                }
            }
            //      /rank set <player> <rank>
            else if (args[0].equalsIgnoreCase("set")) {
                if (args.length == 3) {
                    String player = args[1];
                    String rank = args[2];
                    // Check if rank exists in config.yml file
                    if (config.getConfigurationSection("ranks").getKeys(false).contains(rank)) {
                        // Check if player is online
                        if (Bukkit.getPlayer(player) != null) {
                            // Set player rank and oldrank
                            config.set("players." + player + ".oldrank", config.getString("players." + player + ".rank"));
                            config.set("players." + player + ".rank", rank);
                            saveConfig();
                            // Get color
                            String color = config.getString("ranks." + rank + ".color");
                            sender.sendMessage(ChatColor.YELLOW + "Player " + player + " rank set to " + color + rank);
                            Bukkit.getPlayer(player).sendMessage(ChatColor.YELLOW + "Your rank has been set to " + color + rank);
                            // Set player prefix
                            Player target = Bukkit.getPlayer(player);
                            updatePlayer(target);
                            return true;
                        } else {
                            sender.sendMessage(ChatColor.RED + "Player " + player + " is not online!");
                            return true;
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Rank " + rank + " does not exist!");
                        return true;
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /rank set <player> <rank>");
                    return true;
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("Config reloaded!");
                // Set the prefix of all online players with the rank to the new prefix
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updatePlayer(player);
                }
                return true;
            } else if (args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Rank Help:");
                sender.sendMessage(ChatColor.YELLOW + "/rank add <name> <prefix> <permission> <color>");
                sender.sendMessage(ChatColor.YELLOW + "/rank edit <name> <prefix|permissions|color> <?new value>");
                sender.sendMessage(ChatColor.YELLOW + "/rank set <player> <rank>");
                sender.sendMessage(ChatColor.YELLOW + "/rank delete <name>");
                sender.sendMessage(ChatColor.YELLOW + "/rank list <?name>");
                // Clickable message for color showing you all the minecraft color codes in chat
                TextComponent message = new TextComponent(ChatColor.YELLOW + "/rank color");
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rank color"));
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.YELLOW + "Click to view all the color codes").create()));
                sender.spigot().sendMessage(message);
                sender.sendMessage(ChatColor.YELLOW + "/rank reload");
                return true;
            } else if (args[0].equalsIgnoreCase("color")) {
                breakLine(sender);
                sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Color Codes:");
                breakLine(sender);
                // Loop through all the color codes
                for (ChatColor color : ChatColor.values()) {
                    // Check if the color is a color
                    if (color.isColor()) {
                        // Clickable message for color showing you all the minecraft color codes in chat
                        // Make sure the colon is always in the same place for each color
                        TextComponent message = new TextComponent(ChatColor.YELLOW + color.name() + " : " + color + color.getChar());
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "" + color.getChar()));
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.YELLOW + "Click to copy color code for color " + color + color.name()).create()));
                        sender.spigot().sendMessage(message);
                    }
                }
                breakLine(sender);
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /rank <add|edit|set|delete|list|reload|help|color>");
                return true;
            }
            return true;
        }
        return true;
    }

    private void breakLine(CommandSender sender) {
        sender.sendMessage(ChatColor.WHITE + "-----------------------");
    }

    private List<String> getPlayersInRank(String name) {
        List<String> players = new ArrayList<String>();
        for (String player : config.getConfigurationSection("players").getKeys(false)) {
            if (config.getString("players." + player + ".rank").equalsIgnoreCase(name)) {
                players.add(player);
            }
        }
        return players;
    }


    private void updatePlayer(Player player) {
        // Get primary color
        String color = config.getString("ranks." + config.getString("players." + player.getName() + ".rank") + ".color");
        // Set name & tab name
        player.setDisplayName(color + config.getString("ranks." + config.getString("players." + player.getName() + ".rank") + ".prefix") + " " + ChatColor.RESET + player.getName());
        player.setPlayerListName(color + config.getString("ranks." + config.getString("players." + player.getName() + ".rank") + ".prefix") + " " + ChatColor.RESET + player.getName());
        // Get the player rank
        String rank = config.getString("players." + player.getName() + ".rank");
        // Get the player permissions
        List<String> permissions = config.getStringList("ranks." + rank + ".permissions");
        // Old perms
        String oldRank = config.getString("players." + player.getName() + ".oldrank");
        List<String> oldPerms = config.getStringList("ranks." + oldRank + ".permissions");
        for (String perm : oldPerms) {
            // Remove the attachment from the player using string perm
            player.addAttachment(this, perm, false);
        }
        // Assign the player permissions
        for (String permission : permissions) {
            player.addAttachment(this, permission, true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Check if player is in config.yml file
        if (config.contains("players." + player.getName())) {
            updatePlayer(player);
        } else {
            // Set default rank
            config.set("players." + player.getName() + ".rank", "default");
            saveConfig();
            updatePlayer(player);
        }
    }

    @EventHandler
    public void onMessageSent(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        // Get primary color
        String color = config.getString("ranks." + config.getString("players." + player.getName() + ".rank") + ".color");
        event.setFormat(color + config.getString("ranks." + config.getString("players." + player.getName() + ".rank") + ".prefix") + " " + ChatColor.RESET + player.getName() + ": " + event.getMessage());
    }
}
