package me.untouchedodin0.privatemines.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Split;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.tools.javac.Main;
import me.untouchedodin0.kotlin.menu.Menu;
import me.untouchedodin0.kotlin.mine.data.MineData;
import me.untouchedodin0.kotlin.mine.storage.MineStorage;
import me.untouchedodin0.kotlin.mine.type.MineType;
import me.untouchedodin0.kotlin.utils.AudienceUtils;
import me.untouchedodin0.privatemines.PrivateMines;
import me.untouchedodin0.privatemines.config.MenuConfig;
import me.untouchedodin0.privatemines.config.MessagesConfig;
import me.untouchedodin0.privatemines.factory.MineFactory;
import me.untouchedodin0.privatemines.mine.Mine;
import me.untouchedodin0.privatemines.mine.MineTypeManager;
import me.untouchedodin0.privatemines.storage.sql.SQLUtils;
import me.untouchedodin0.privatemines.utils.QueueUtils;
import me.untouchedodin0.privatemines.utils.Utils;
import me.untouchedodin0.privatemines.utils.world.MineWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import redempt.redlib.misc.Task;
import redempt.redlib.region.CuboidRegion;

@CommandAlias("privatemine|privatemines|pmine")
public class PrivateMinesCommand extends BaseCommand {

  PrivateMines privateMines = PrivateMines.getPrivateMines();
  MineStorage mineStorage = privateMines.getMineStorage();
  MineTypeManager mineTypeManager = privateMines.getMineTypeManager();
  AudienceUtils audienceUtils = new AudienceUtils();

  final String prefix = ChatColor.translateAlternateColorCodes('&', this.privateMines.getConfig().getString("Prefix"));

  private Cache<UUID, Long> resetcooldown = CacheBuilder.newBuilder().expireAfterWrite(300, TimeUnit.SECONDS).build();


  @Default
  public void defaultCommand(Player player) {
    Menu mainMenu = MenuConfig.getMenus().get("mainMenu");
    mainMenu.open(player);
  }

  @Subcommand("version")
  @CommandPermission("privatemines.version")
  public void version(Player player) {
    String localVersion = privateMines.getDescription().getVersion();
    String gitVersion = Utils.getGit();

    audienceUtils.sendMessage(player,
        String.format("<green>Private Mines is running v%s, latest commit <gray>(%s)", localVersion,
            gitVersion));
  }

  @Subcommand("give")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.give")
  @Syntax("<target>")
  public void give(CommandSender sender, OfflinePlayer target) {
    MineFactory mineFactory = new MineFactory();
    MineWorldManager mineWorldManager = privateMines.getMineWorldManager();
    Location location = mineWorldManager.getNextFreeLocation();
    mineWorldManager.setCurrentLocation(location);

    MineType defaultMineType = mineTypeManager.getDefaultMineType();

    if (target.getPlayer() != null) {
      if (mineStorage.hasMine(target.getUniqueId())) {
        if (sender instanceof Player player) {
          audienceUtils.sendMessage(player, MessagesConfig.playerAlreadyOwnsAMine);
        } else {
          sender.sendMessage(prefix + ChatColor.RED + "Player has a mine!");
        }
      } else {
        sender.sendMessage(prefix + ChatColor.GREEN + "Giving " + target.getName() + " a mine!");
        mineFactory.create(target.getPlayer(), location, defaultMineType, true);

        if (sender instanceof Player player) {
          audienceUtils.sendMessage(player, target, MessagesConfig.gavePlayerMine.replace("{name}",
              Objects.requireNonNull(target.getName())));
        }
      }
    }
  }

  @Subcommand("delete")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.delete")
  @Syntax("<target>")
  public void delete(CommandSender sender, OfflinePlayer target) {
    if (!mineStorage.hasMine(target.getUniqueId())) {
      if (sender instanceof Player player) {
        audienceUtils.sendMessage(player, MessagesConfig.playerDoesntOwnMine);
      }
    } else {
      Mine mine = mineStorage.get(target.getUniqueId());
      if (mine != null) {
        mine.delete(true);
        SQLUtils.delete(mine);
        if (sender instanceof Player player) {
          audienceUtils.sendMessage(player, target, MessagesConfig.deletedPlayersMine);
        }
      }
    }
  }

  @Subcommand("upgrade")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.upgrade")
  public void upgrade(CommandSender sender) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(prefix + ChatColor.RED + "Only players can use this command!");
    } else {
      if (!mineStorage.hasMine(player)) {
        player.sendMessage(prefix + ChatColor.RED + "You don't own a mine!");
      } else {
        Mine mine = mineStorage.get(player);
        if (mine != null) {
          MineData mineData = mine.getMineData();
          MineType currentType = mineData.getMineType();
          MineType nextType = mineTypeManager.getNextMineType(currentType);
          double cost = nextType.getUpgradeCost();
          double bal = PrivateMines.getEconomy().getBalance(player);

          if (bal >= cost) {
            // player has enough money, upgrade the mine
            mine.upgrade(false);
          } else {
            // player does not have enough money
            player.sendMessage(prefix + ChatColor.RED +
                String.format("You need %.2f to upgrade the mine. You currently have %.2f.", cost,
                    bal));
          }
        }
      }
    }
  }

  @Subcommand("forceupgrade")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.forceupgrade")
  @Syntax("<target>")
  public void forceUpgrade(CommandSender sender, OfflinePlayer target) {
    if (!mineStorage.hasMine(target.getUniqueId())) {
      if (sender instanceof Player player) {
        audienceUtils.sendMessage(player, MessagesConfig.playerDoesntOwnMine);
      }
    } else {
      Mine mine = mineStorage.get(target.getUniqueId());
      if (mine != null) {
        SQLUtils.delete(mine);
        mine.upgrade(true);

        List<Player> players = new ArrayList<>();

        MineData mineData = mine.getMineData();
        Location minFull = mineData.getMinimumFullRegion();
        Location maxFull = mineData.getMaximumFullRegion();
        CuboidRegion cuboidRegion = new CuboidRegion(minFull, maxFull);

        for (Player player : Bukkit.getOnlinePlayers()) {
          if (cuboidRegion.contains(player.getLocation())) {
            if (player.getUniqueId().equals(mineData.getMineOwner())) {
              return;
            }
            players.add(player);
          }
        }

        for (Player toTeleport : players) {
          Bukkit.getServer().dispatchCommand(toTeleport, "spawn");
        }

        Task.syncDelayed(() -> {
          for (Player player : players) {
            mine.teleport(player);
          }
          players.clear();
        }, 20L);
      }
    }
  }

  @Subcommand("reset")
  @CommandPermission("privatemines.reset")
  public void reset(Player player) {
    if (!mineStorage.hasMine(player)) {
      player.sendMessage(prefix + ChatColor.RED + "You don't own a mine!");
    } else if (!resetcooldown.asMap().containsKey(player.getUniqueId())) {
      Mine mine = mineStorage.get(player);
      if (mine != null) {
        mine.handleReset();
        resetcooldown.asMap().put(player.getUniqueId(), System.currentTimeMillis() + 300000);
      }
    } else {
      long distance = resetcooldown.asMap().get(player.getUniqueId()) - System.currentTimeMillis();
      player.sendMessage(ChatColor.RED + "You must wait " + TimeUnit.MILLISECONDS.toSeconds(distance) + " seconds to use this again!");
    }
  }

  @Subcommand("teleport")
  @CommandPermission("privatemines.teleport")
  public void teleport(Player player) {
    if (!mineStorage.hasMine(player)) {
      player.sendMessage(prefix + ChatColor.RED + "You don't own a mine!");
    } else {
      Mine mine = mineStorage.get(player);
      if (mine != null) {
        mine.teleport(player);
      }
    }
  }

  @Subcommand("go")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.go")
  @Syntax("<target>")
  public void go(Player player, OfflinePlayer target) {
    if (target.getPlayer() != null) {
      Player targetPlayer = target.getPlayer();
      Mine mine = mineStorage.get(targetPlayer);
      if (mine != null) {
        MineData mineData = mine.getMineData();
        if (mineData != null) {
          if (mineData.isOpen()) {
            mine.teleport(player);
            player.sendMessage(prefix + ChatColor.GREEN + "You have been teleported to "+ targetPlayer.getName() + "'s mine!");
          } else {
            player.sendMessage(prefix + ChatColor.RED + targetPlayer.getName() + "'s mine is closed!");
          }
        }
      }
    }
  }

  @Subcommand("expand")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.expand")
  @Syntax("<target> <amount>")
  public void expand(CommandSender commandSender, OfflinePlayer target, int amount) {
    if (!mineStorage.hasMine(Objects.requireNonNull(target.getPlayer()))) {
      return;
    }
    Mine mine = mineStorage.get(target.getPlayer());
    if (mine != null) {
      if (mine.canExpand(amount)) {
        for (int i = 0; i < amount; i++) {
          mine.expand();
        }
        commandSender.sendMessage(prefix + ChatColor.GREEN + "Successfully expanded " + target.getName() + "'s mine!");
      }
    }
  }

  @Subcommand("open")
  @CommandPermission("privatemines.open")
  public void open(Player player) {
    Mine mine = mineStorage.get(player);
    MineData mineData;
    mineData = mine.getMineData();
    if (mineData.isOpen()) {
      player.sendMessage(prefix + ChatColor.GREEN + "Your mine is already open!");
      return;
    }
    if (mine != null) {
      mineData.setOpen(true);
      mine.setMineData(mineData);
      mineStorage.replaceMineNoLog(player, mine);
      player.sendMessage(prefix + ChatColor.GREEN + "Your mine has been opened!");
    }
  }

  @Subcommand("close")
  @CommandPermission("privatemines.close")
  public void close(Player player) {
    Mine mine = mineStorage.get(player);
    MineData mineData;
    mineData = mine.getMineData();
    if (!mineData.isOpen()) {
      player.sendMessage(prefix + ChatColor.RED + "Your mine is already closed!");
      return;
    }
    if (mine != null) {
      mineData.setOpen(false);
      mine.setMineData(mineData);
      mineStorage.replaceMineNoLog(player, mine);
      player.sendMessage(prefix + ChatColor.RED + "Your mine has been closed!");
    }
  }

  @Subcommand("ban")
  @CommandPermission("privatemines.ban")
  @Syntax("<target>")
  public void ban(Player player, Player target) {
    Mine mine = mineStorage.get(player);
    if (mine != null) {
      mine.ban(target);
      mineStorage.replaceMineNoLog(player, mine);
    }
  }

  @Subcommand("unban")
  @CommandPermission("privatemines.unban")
  @Syntax("<target>")
  public void unban(Player player, Player target) {
    Mine mine = mineStorage.get(player);
    if (mine != null) {
      mine.unban(target);
      mineStorage.replaceMineNoLog(player, mine);
    }
  }

  @Subcommand("tax")
  @CommandPermission("privatemines.tax")
  @Syntax("<amount>")
  public void tax(Player player, double tax) {
    Mine mine = mineStorage.get(player);
    if (mine != null) {
      MineData mineData = mine.getMineData();
      mineData.setTax(tax);
      mine.setMineData(mineData);
      mineStorage.replaceMineNoLog(player, mine);
      player.sendMessage(prefix + ChatColor.GREEN + "Mine tax has been set to " + tax + "%");
    }
  }

  @Subcommand("claim")
  @CommandPermission("privatemines.claim")
  public void claim(Player player) {
    QueueUtils queueUtils = privateMines.getQueueUtils();
    if (queueUtils.isInQueue(player.getUniqueId())) {
      player.sendMessage(prefix + ChatColor.RED + "You're already in the queue!");
      return;
    }
    queueUtils.claim(player);
  }

  @Subcommand("setblocks")
  @CommandCompletion("@players")
  @CommandPermission("privatemines.setblocks")
  @Syntax("<target> <materials> (DIRT, STONE)")
  public void setBlocks(CommandSender sender, OfflinePlayer target,
      @Split(",") String[] materials) {
    Map<Material, Double> map = new HashMap<>();

    for (String s : materials) {
      if (Material.getMaterial(s.toUpperCase()) == null) {
        sender.sendMessage(prefix + ChatColor.RED + "Failed to find Material: " + s);
        return;
      }
      Material material = Material.valueOf(s.toUpperCase());
      map.put(material, 1.0);
    }

    if (target != null) {
      Mine mine = mineStorage.get(Objects.requireNonNull(target.getPlayer()));
      if (mine != null) {
        MineData mineData = mine.getMineData();
        mineData.setMaterials(map);
        mine.setMineData(mineData);
        mineStorage.replaceMineNoLog(target.getPlayer(), mine);
        mine.handleReset();
        Task.asyncDelayed(() -> {
          SQLUtils.updateMaterials(mine);
        });
      }
    }
  }
}
