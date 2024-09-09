package net.earthcomputer.descentintodarkness;

import net.earthcomputer.descentintodarkness.generator.Centroid;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;

public final class DIDUtil {
    private DIDUtil() {
    }

    public static void ensureConnected(List<Centroid> centroidsInOut, int connectingCentroidRadius, Function<Vec3, Centroid> centroidSupplier) {
        // find the minimum spanning tree of the centroids using Kruskal's algorithm, to ensure they are connected
        List<Pair<Centroid, Centroid>> edges = new ArrayList<>();
        for (int i = 0; i < centroidsInOut.size() - 1; i++) {
            for (int j = i + 1; j < centroidsInOut.size(); j++) {
                edges.add(Pair.of(centroidsInOut.get(i), centroidsInOut.get(j)));
            }
        }
        edges.sort(Comparator.comparingDouble(edge -> edge.getLeft().pos.distanceTo(edge.getRight().pos) - edge.getLeft().size - edge.getRight().size));
        Map<Centroid, Integer> nodeGroups = new HashMap<>();
        int groupCount = 0;
        ListIterator<Pair<Centroid, Centroid>> edgesItr = edges.listIterator();
        while (edgesItr.hasNext()) {
            Pair<Centroid, Centroid> edge = edgesItr.next();
            Integer groupA = nodeGroups.get(edge.getLeft());
            Integer groupB = nodeGroups.get(edge.getRight());
            if (groupA != null && groupA.equals(groupB)) {
                edgesItr.remove();
            } else {
                if (groupA != null && groupB != null) {
                    nodeGroups.replaceAll((key, val) -> val.equals(groupB) ? groupA : val);
                } else {
                    Integer group = groupA == null ? groupB == null ? groupCount++ : groupB : groupA;
                    nodeGroups.put(edge.getLeft(), group);
                    nodeGroups.put(edge.getRight(), group);
                }
            }
        }

        for (Pair<Centroid, Centroid> edge : edges) {
            double distance = edge.getLeft().pos.distanceTo(edge.getRight().pos);
            double actualDistance = distance - edge.getLeft().size - edge.getRight().size;
            if (actualDistance < 0) {
                continue;
            }
            actualDistance = Math.max(1, actualDistance);
            Vec3 dir = edge.getRight().pos.subtract(edge.getLeft().pos).scale(1.0 / distance);

            int numSegments = (int) Math.ceil(actualDistance / connectingCentroidRadius) + 1;
            double segmentLength = actualDistance / numSegments;

            Vec3 startPos = edge.getLeft().pos.add(dir.scale(edge.getLeft().size));
            Vec3 segment = dir.scale(segmentLength);
            for (int i = 1; i < numSegments; i++) {
                centroidsInOut.add(centroidSupplier.apply(startPos.add(segment.scale(i))));
            }
        }
    }
}
