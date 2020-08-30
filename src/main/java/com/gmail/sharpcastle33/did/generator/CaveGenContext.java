package com.gmail.sharpcastle33.did.generator;

import com.gmail.sharpcastle33.did.DescentIntoDarkness;
import com.gmail.sharpcastle33.did.Util;
import com.gmail.sharpcastle33.did.config.CaveStyle;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.NullExtent;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CaveGenContext implements AutoCloseable {
	private final World world;
	public final CaveStyle style;
	public final Random rand;
	private boolean debug;
	private final Map<BlockVector3, BlockState> blockStorage = new HashMap<>();
	private final Set<BlockVector2> accessedChunks = new HashSet<>();
	private Region limit = null;

	private CaveGenContext(World world, CaveStyle style, Random rand) {
		this.world = world;
		this.style = style;
		this.rand = rand;
	}

	public CaveGenContext setDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	public boolean isDebug() {
		return debug;
	}

	public CaveGenContext limit(Region limit) {
		this.limit = limit;
		return this;
	}

	public static CaveGenContext create(World world, CaveStyle style, Random rand) {
		if (Bukkit.isPrimaryThread()) {
			throw new IllegalStateException("Cannot create a CaveGenContext on the main thread");
		}
		return new CaveGenContext(world, style, rand);
	}

	private void markChunkAccessed(BlockVector3 blockPos) {
		BlockVector2 chunkPos = BlockVector2.at(blockPos.getX() >> 4, blockPos.getZ() >> 4);
		accessedChunks.add(chunkPos);
	}

	public boolean setBlock(BlockVector3 pos, BlockStateHolder<?> block) {
		if (pos.getBlockY() <= 0 || pos.getBlockY() >= 255) {
			return false;
		}
		if (limit != null && !limit.contains(pos)) {
			return false;
		}
		BlockState oldState = getBlock(pos); // marks chunk accessed
		blockStorage.put(pos, block.toImmutableState());
		return !block.equalsFuzzy(oldState);
	}

	public BlockState getBlock(BlockVector3 pos) {
		if (pos.getBlockY() < 0 || pos.getBlockY() > 255) {
			return Util.requireDefaultState(BlockTypes.AIR);
		}
		if (pos.getBlockY() == 0 || pos.getBlockY() == 255) {
			return Util.requireDefaultState(BlockTypes.BEDROCK);
		}
		if (limit != null && !limit.contains(pos)) {
			return style.getBaseBlock().toImmutableState();
		}
		markChunkAccessed(pos);
		return blockStorage.getOrDefault(pos, style.getBaseBlock().toImmutableState());
	}

	public Extent asExtent() {
		return new NullExtent() {
			@Override
			public BlockState getBlock(BlockVector3 position) {
				return CaveGenContext.this.getBlock(position);
			}

			@Override
			public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 location, T block) {
				return CaveGenContext.this.setBlock(location, block);
			}
		};
	}

	@Override
	public void close() {
		// fill accessed chunks and their neighbors
		List<BlockVector2> filledChunks = this.accessedChunks.stream()
				.flatMap(chunk -> IntStream.rangeClosed(-1, 1).mapToObj(dx -> chunk.add(dx, 0)))
				.flatMap(chunk -> IntStream.rangeClosed(-1, 1).mapToObj(dz -> chunk.add(0, dz)))
				.distinct()
				.collect(Collectors.toList());
		runAsyncEdits(
				filledChunks.stream()
						.flatMap(chunk -> IntStream.range(0, 16).mapToObj(sectionY -> BlockVector3.at(chunk.getX(), sectionY, chunk.getZ())))
						.<Edit>map(section -> session -> fillChunkSection(session, section))
						.collect(Collectors.toList()),
				progress -> Bukkit.getLogger().log(Level.INFO, String.format("Filling base chunks... %d%%", (int) (progress * 100)))
		);

		List<Edit> blockEdits = blockStorage.entrySet().stream()
				.collect(Collectors.groupingBy(entry -> BlockVector3.at(entry.getKey().getX() >> 4, entry.getKey().getY() >> 4, entry.getKey().getZ() >> 4)))
				.entrySet().stream()
				.sorted(Comparator.<Map.Entry<BlockVector3, List<Map.Entry<BlockVector3, BlockState>>>>comparingInt(entry -> entry.getKey().getY()).thenComparingInt(entry -> entry.getKey().getX()).thenComparingInt(entry -> entry.getKey().getZ()))
				.map(Map.Entry::getValue)
				.<Edit>map(entries -> session -> {
					for (Map.Entry<BlockVector3, BlockState> entry : entries) {
						session.setBlock(entry.getKey(), entry.getValue());
					}
				})
				.collect(Collectors.toList());
		runAsyncEdits(blockEdits, progress -> Bukkit.getLogger().log(Level.INFO, String.format("Filling blocks... %d%%", (int) (progress * 100))));

		// bedrock wall around all generated chunks
		List<Edit> bedrockWallEdits = filledChunks.stream().flatMap((BlockVector2 filledChunk) -> {
			List<Edit> editsThisChunk = new ArrayList<>(4);
			BlockVector2 north = filledChunk.add(0, -1);
			if (!filledChunks.contains(north)) {
				editsThisChunk.add(session -> fill(session, new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16, 1, filledChunk.getZ() * 16 - 1),
						BlockVector3.at(filledChunk.getX() * 16 + 15, 254, filledChunk.getZ() * 16 - 1)
				), Util.requireDefaultState(BlockTypes.BEDROCK)));
			}
			BlockVector2 east = filledChunk.add(1, 0);
			if (!filledChunks.contains(east)) {
				editsThisChunk.add(session -> fill(session, new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16 + 16, 1, filledChunk.getZ() * 16),
						BlockVector3.at(filledChunk.getX() * 16 + 16, 254, filledChunk.getZ() * 16 + 15)
				), Util.requireDefaultState(BlockTypes.BEDROCK)));
			}
			BlockVector2 south = filledChunk.add(0, 1);
			if (!filledChunks.contains(south)) {
				editsThisChunk.add(session -> fill(session, new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16, 1, filledChunk.getZ() * 16 + 16),
						BlockVector3.at(filledChunk.getX() * 16 + 15, 254, filledChunk.getZ() * 16 + 16)
				), Util.requireDefaultState(BlockTypes.BEDROCK)));
			}
			BlockVector2 west = filledChunk.add(-1, 0);
			if (!filledChunks.contains(west)) {
				editsThisChunk.add(session -> fill(session, new CuboidRegion(
						BlockVector3.at(filledChunk.getX() * 16 - 1, 1, filledChunk.getZ() * 16),
						BlockVector3.at(filledChunk.getX() * 16 - 1, 254, filledChunk.getZ() * 16 + 15)
				), Util.requireDefaultState(BlockTypes.BEDROCK)));
			}
			return editsThisChunk.stream();
		}).collect(Collectors.toList());
		runAsyncEdits(bedrockWallEdits, progress -> Bukkit.getLogger().log(Level.INFO, String.format("Creating bedrock walls... %d%%", (int) (progress * 100))));

		Bukkit.getLogger().log(Level.INFO, "Cave finished generating");
	}

	private void fillChunkSection(EditSession session, BlockVector3 sectionPos) throws WorldEditException {
		BlockVector3 from = BlockVector3.at(sectionPos.getX() * 16, sectionPos.getY() * 16, sectionPos.getZ() * 16);
		BlockVector3 to = from.add(15, 15, 15);
		if (from.getY() == 0) from = from.withY(1);
		else if (to.getY() == 255) to = to.withY(254);
		fill(session, new CuboidRegion(from, to), style.getBaseBlock());
	}

	private void fill(EditSession session, Region region, BlockStateHolder<?> block) throws WorldEditException {
		// for some reason, setBlocks is too slow here, so we use a loop
		for (BlockVector3 pos : region) {
			session.setBlock(pos, block);
		}
	}

	/**
	 * Runs the given list of edits on the main thread. Blocks until done.
	 */
	private void runAsyncEdits(List<Edit> editList, DoubleConsumer progressIndicator) {
		int editCount = editList.size();
		if (editCount == 0) {
			return;
		}
		Iterator<Edit> edits = editList.iterator();
		final long MAX_TIME = 5000000; // 10ms, 1/5 of a tick
		AtomicInteger tickCounter = new AtomicInteger(0);
		AtomicInteger editCounter = new AtomicInteger(0);
		AtomicInteger taskId = new AtomicInteger();
		CompletableFuture<Void> task = new CompletableFuture<>();
		taskId.set(Bukkit.getScheduler().scheduleSyncRepeatingTask(DescentIntoDarkness.plugin, () -> {
			if (editCounter.get() == 0) {
				progressIndicator.accept(0);
			}
			long beginTick = System.nanoTime();
			do {
				try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder().world(world).build()) {
					edits.next().edit(session);
				} catch (WorldEditException e) {
					Bukkit.getLogger().log(Level.SEVERE, "An error occurred while editing world", e);
					Bukkit.getScheduler().cancelTask(taskId.get());
					return;
				}
				editCounter.incrementAndGet();
			} while (edits.hasNext() && System.nanoTime() - beginTick < MAX_TIME);
			if (!edits.hasNext()) {
				Bukkit.getScheduler().cancelTask(taskId.get());
				progressIndicator.accept(1);
				task.complete(null);
			} else {
				if (tickCounter.incrementAndGet() % 20 == 0) {
					progressIndicator.accept((double)editCounter.get() / editCount);
				}
			}
		}, 0, 2));
		try {
			task.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException("Thread interrupted", e);
		}
	}

	@FunctionalInterface
	private interface Edit {
		void edit(EditSession session) throws WorldEditException;
	}
}
