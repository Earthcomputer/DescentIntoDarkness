package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.generator.room.SimpleRoom;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CaveGenerator {
    public static final int STEP_CREATING_ROOMS = 1;
    public static final int STEP_CARVING_CENTROIDS = 2;
    public static final int STEP_SMOOTHING = 3;
    public static final int STEP_PAINTING = 4;
    public static final int STEP_GENERATING_STRUCTURES = 5;
    public static final int TOTAL_STEPS = 6;

    private CaveGenerator() {
    }

    public static String generateCave(CaveGenContext ctx, int size) {
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating", ctx.styleHolder().unwrapKey().orElseThrow().location().toString()));
        Vec3 pos = new Vec3(0, ctx.style().startY(), 0);
        List<List<Vec3>> roomLocations = new ArrayList<>();
        int length = ctx.style().length().sample(ctx.rand);
        Vec3 startingDir = new Vec3(1, 0, 0);
        if (ctx.style().randomRotation()) {
            startingDir = startingDir.yRot(ctx.rand.nextFloat() * Mth.TWO_PI);
        }
        String caveString = generateBranch(ctx, size, pos, length, 'C', true, startingDir, roomLocations);
        PostProcessor.postProcess(ctx, roomLocations);
        ctx.listener().popProgress();
        return caveString;
    }

    public static String generateBranch(CaveGenContext ctx, int size, Vec3 pos, int length, char startingSymbol, boolean moreBranches, Vec3 dir, List<List<Vec3>> roomLocations) {
        LayoutGenerator.Layout layout = LayoutGenerator.generateCave(ctx, length, startingSymbol);

        if (!moreBranches) {
            Character simpleRoomSymbol = ctx.style().rooms().entrySet().stream().filter(room -> room.getValue() instanceof SimpleRoom).map(Map.Entry::getKey).findFirst().orElse(null);
            String branchReplacement = simpleRoomSymbol == null ? "" : simpleRoomSymbol.toString();
            for (var roomWithSymbol : ctx.style().rooms().entrySet()) {
                if (roomWithSymbol.getValue().isBranch()) {
                    layout.setValue(layout.getValue().replace(roomWithSymbol.getKey().toString(), branchReplacement));
                }
            }
        }

        ModuleGenerator.read(ctx, layout, pos, dir, size, roomLocations);
        return layout.getValue();
    }
}
