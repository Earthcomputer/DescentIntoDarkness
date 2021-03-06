package com.gmail.sharpcastle33.did.listeners;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.config.MobSpawnEntry;
import com.gmail.sharpcastle33.did.instancing.CaveTracker;
import io.lumine.xikage.mythicmobs.MythicMobs;
import io.lumine.xikage.mythicmobs.adapters.bukkit.BukkitAdapter;
import io.lumine.xikage.mythicmobs.api.bukkit.events.MythicMobSpawnEvent;
import io.lumine.xikage.mythicmobs.mobs.ActiveMob;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MobSpawnManager implements Runnable, Listener {
	private final Random rand = new Random();
	private boolean spawningMob = false;

	private void spawnMobs(CaveTracker cave) {
		if (cave.getPlayers().isEmpty()) {
			return;
		}

		if (cave.getSpawnCooldown() > 0) {
			cave.setSpawnCooldown(cave.getSpawnCooldown() - 1);
			return;
		}

		// Prevent lack of mob spawning due to idleness
		if (rand.nextFloat() < cave.getStyle().getNaturalPollutionIncrease()) {
			MobSpawnEntry mobType = getRandomSpawnEntry(cave);
			if (mobType != null) {
				Player victim = getRandomPlayer(cave, mobType, Integer.MIN_VALUE);
				if (victim != null) {
					cave.addPlayerMobPollution(victim.getUniqueId(), mobType, victim.isSprinting() ? cave.getStyle().getSprintingPenalty() : 1);
				}
			}
		}

		for (int i = 0; i < cave.getStyle().getSpawnAttemptsPerTick(); i++) {
			if (spawnMob(cave)) {
				break;
			}
		}
	}

	private boolean spawnMob(CaveTracker cave) {
		// pick random type of mob to spawn and check if the cave has enough total pollution to spawn it
		MobSpawnEntry spawnEntry = getRandomSpawnEntry(cave);
		if (spawnEntry == null) {
			return false;
		}
		CaveTracker.MobEntry mobEntry = cave.getMobEntry(spawnEntry);
		if (!mobEntry.isSpawningPack() && mobEntry.getPackSpawnThreshold() > mobEntry.getTotalPollution()) {
			return false;
		}
		boolean spawningPack = mobEntry.getTotalPollution() >= spawnEntry.getSingleMobCost();
		mobEntry.setSpawningPack(spawningPack);
		if (!spawningPack) {
			mobEntry.setPackSpawnThreshold(spawnEntry.getMinPackCost() + rand.nextInt(spawnEntry.getMaxPackCost() - spawnEntry.getMinPackCost() + 1));
		}

		// try to spawn that mob next to a player with enough pollution to afford it
		Player chosenPlayer = getRandomPlayer(cave, spawnEntry, spawnEntry.getSingleMobCost());
		if (chosenPlayer == null) {
			chosenPlayer = getRandomPlayer(cave, spawnEntry, 1);
			if (chosenPlayer == null) {
				return false;
			}
		}


		// Pick a random distance between minDistance and maxDistance, then a random point on the sphere with that radius.
		// Makes distances uniformly likely, as opposed to
		Vector spawnLocation = new Vector(rand.nextGaussian(), rand.nextGaussian(), rand.nextGaussian())
				.normalize()
				.multiply(spawnEntry.getMinDistance() + rand.nextDouble() * (spawnEntry.getMaxDistance() - spawnEntry.getMinDistance()))
				.add(chosenPlayer.getLocation().toVector());
		spawnLocation.setY(Math.floor(spawnLocation.getY()));

		// quick exit for the blocks the mob will definitely intersect
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY(), spawnLocation.getBlockZ()).isPassable()) {
			return false;
		}
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY() + 1, spawnLocation.getBlockZ()).isPassable()) {
			return false;
		}
		if (!cave.getWorld().getBlockAt(spawnLocation.getBlockX(), spawnLocation.getBlockY() - 1, spawnLocation.getBlockZ()).getType().isSolid()) {
			return false;
		}

		if (!doSpawn(cave, spawnEntry, spawnLocation)) {
			return false;
		}

		// deduct pollution
		cave.addPlayerMobPollution(chosenPlayer.getUniqueId(), spawnEntry, -spawnEntry.getSingleMobCost());
		cave.setSpawnCooldown(cave.getSpawnCooldown() + spawnEntry.getCooldown());

		return true;
	}

	private boolean doSpawn(CaveTracker cave, MobSpawnEntry spawnEntry, Vector spawnLocation) {
		boolean isMythicMob = MythicMobs.inst().getMobManager().getMythicMob(spawnEntry.getMob()) != null;
		Location loc = spawnLocation.toLocation(cave.getWorld());

		// spawn mob and check its hitbox doesn't intersect anything
		// the event cancels the mob spawning in this case
		Entity mob;
		if (isMythicMob) {
			spawningMob = true;
			ActiveMob activeMob;
			try {
				activeMob = MythicMobs.inst().getMobManager().spawnMob(spawnEntry.getMob(), loc);
			} finally {
				spawningMob = false;
			}
			if (activeMob == null) {
				return false;
			}
			mob = BukkitAdapter.adapt(activeMob.getEntity());
		} else {
			com.sk89q.worldedit.world.entity.EntityType entityType =
					com.sk89q.worldedit.world.entity.EntityType.REGISTRY.get(spawnEntry.getMob().toLowerCase(Locale.ROOT));
			if (entityType == null) {
				Bukkit.getLogger().log(Level.SEVERE, "Could not spawn mob: " + spawnEntry.getMob());
				return false;
			}
			EntityType bukkitEntity = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(entityType);
			mob = cave.getWorld().spawnEntity(loc, bukkitEntity);
			if (!canSpawnMob(cave.getWorld(), mob)) {
				mob.remove();
				return false;
			}
		}

		mob.setRotation(rand.nextFloat() * 360, 0);

		return true;
	}

	@Nullable
	private Player getRandomPlayer(CaveTracker cave, MobSpawnEntry spawnEntry, int minPollution) {
		CaveTracker.MobEntry mobEntry = cave.getMobEntry(spawnEntry);
		int totalOnlinePollution = cave.getPlayers().stream()
				.filter(this::isPlayerOnline)
				.mapToInt(mobEntry::getPlayerPollution)
				.filter(pollution -> pollution >= minPollution)
				.sum();

		if (totalOnlinePollution <= 0) {
			List<UUID> players = cave.getPlayers().stream().filter(this::isPlayerOnline).filter(player -> mobEntry.getPlayerPollution(player) >= minPollution).collect(Collectors.toList());
			if (players.isEmpty()) {
				return null;
			}
			return Bukkit.getPlayer(players.get(rand.nextInt(players.size())));
		}

		int index = rand.nextInt(totalOnlinePollution);
		Player chosenPlayer = null;
		for (UUID player : cave.getPlayers()) {
			if (!isPlayerOnline(player)) {
				continue;
			}
			int pollution = mobEntry.getPlayerPollution(player);
			if (pollution < minPollution) {
				continue;
			}
			index -= pollution;
			if (index <= 0) {
				chosenPlayer = Bukkit.getPlayer(player);
				break;
			}
		}
		return chosenPlayer;
	}

	@Nullable
	public MobSpawnEntry getRandomSpawnEntry(CaveTracker cave) {
		List<MobSpawnEntry> spawnEntries = cave.getStyle().getSpawnEntries();
		int totalWeight = spawnEntries.stream().mapToInt(MobSpawnEntry::getWeight).sum();
		int index = rand.nextInt(totalWeight);
		for (MobSpawnEntry entry : spawnEntries) {
			index -= entry.getWeight();
			if (index <= 0) {
				return entry;
			}
		}
		return null;
	}

	private boolean isPlayerOnline(UUID player) {
		OfflinePlayer p = Bukkit.getOfflinePlayer(player);
		//noinspection ConstantConditions - Bukkit derp
		return p != null && p.isOnline();
	}

	@Override
	public void run() {
		for (CaveTracker cave : DescentIntoDarkness.plugin.getCaveTrackerManager().getCaves()) {
			spawnMobs(cave);
		}
	}

	@EventHandler
	public void onMythicMobSpawn(MythicMobSpawnEvent event) {
		if (!spawningMob) {
			return;
		}
		World world = event.getLocation().getWorld();
		if (world == null) {
			return;
		}
		if (!canSpawnMob(world, event.getEntity())) {
			event.setCancelled();
		}
	}

	private boolean canSpawnMob(World world, Entity mob) {
		for (int x = (int)Math.floor(mob.getBoundingBox().getMinX()); x <= (int)Math.ceil(mob.getBoundingBox().getMaxX()); x++) {
			for (int y = (int)Math.floor(mob.getBoundingBox().getMinY()); y <= (int)Math.ceil(mob.getBoundingBox().getMaxY()); y++) {
				for (int z = (int)Math.floor(mob.getBoundingBox().getMinZ()); z <= (int)Math.ceil(mob.getBoundingBox().getMaxZ()); z++) {
					if (!world.getBlockAt(x, y, z).isPassable()) {
						return false;
					}
				}
			}
		}
		return true;
	}
}
