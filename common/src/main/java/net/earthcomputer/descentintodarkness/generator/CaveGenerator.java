package net.earthcomputer.descentintodarkness.generator;

import net.earthcomputer.descentintodarkness.generator.room.SimpleRoom;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class CaveGenerator {
    public static final int STEP_CREATING_ROOMS = 1;
    public static final int STEP_APPLYING_ROOMS = 2;
    public static final int STEP_SMOOTHING_CALCULATING = 3;
    public static final int STEP_SMOOTHING_APPLYING = 4;
    public static final int STEP_CARVING = 5;
    public static final int STEP_PAINTING = 6;
    public static final int STEP_GENERATING_STRUCTURES = 7;
    public static final int TOTAL_STEPS = 8;

    private CaveGenerator() {
    }

    public static String generateCave(CaveGenContext ctx, int size) {
        ctx.listener().pushProgress(Component.translatable("descent_into_darkness.generating", ctx.styleHolder().unwrapKey().orElseThrow().location().toString()));
        Vec3 pos = new Vec3(0, ctx.style().startY(), 0);
        int length = ctx.style().length().sample(ctx.rand);
        Vec3 startingDir = new Vec3(1, 0, 0);
        if (ctx.style().randomRotation()) {
            startingDir = startingDir.yRot(ctx.rand.nextFloat() * Mth.TWO_PI);
        }
        String caveString = generateBranch(ctx, size, pos, length, 'C', true, startingDir);
        PostProcessor.postProcess(ctx);
        ctx.listener().popProgress();
        return caveString;
    }

    public static String generateBranch(CaveGenContext ctx, int size, Vec3 pos, int length, char startingSymbol, boolean moreBranches, Vec3 dir) {
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

        ModuleGenerator.read(ctx, layout, pos, dir, size);
        return layout.getValue();
    }
}
