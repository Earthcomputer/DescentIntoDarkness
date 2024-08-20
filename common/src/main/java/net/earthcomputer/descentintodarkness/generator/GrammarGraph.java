package net.earthcomputer.descentintodarkness.generator;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class GrammarGraph {
    public static final Codec<GrammarGraph> CODEC = Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, ExtraCodecs.POSITIVE_INT))
        .comapFlatMap(GrammarGraph::deserialize, GrammarGraph::serialize);

    private final Map<Character, RuleSet> ruleSets;

    public GrammarGraph(Map<Character, RuleSet> ruleSets) {
        this.ruleSets = ruleSets;
    }

    public boolean hasRuleSet(char symbol) {
        return ruleSets.containsKey(symbol);
    }

    public RuleSet getRuleSet(char symbol) {
        return ruleSets.get(symbol);
    }

    private Map<String, Map<String, Integer>> serialize() {
        ImmutableMap.Builder<String, Map<String, Integer>> map = ImmutableMap.builder();
        ruleSets.forEach((symbol, ruleSet) -> {
            StringBuilder key = new StringBuilder(String.valueOf(symbol));
            for (String tag : ruleSet.getTags()) {
                key.append(" ").append(tag);
            }
            ImmutableMap.Builder<String, Integer> ruleSection = ImmutableMap.builder();
            for (Pair<Integer, String> entry : ruleSet.getEntries()) {
                ruleSection.put(entry.getRight(), entry.getLeft());
            }
            map.put(key.toString(), ruleSection.build());
        });
        return map.build();
    }

    private static DataResult<GrammarGraph> deserialize(Map<String, Map<String, Integer>> map) {
        Map<Character, RuleSet> ruleSets = new HashMap<>();

        for (var entry : map.entrySet()) {
            String key = entry.getKey();
            Map<String, Integer> ruleSection = entry.getValue();

            String[] parts = key.split(" ");
            String symbolStr = parts[0];
            if (symbolStr.length() != 1) {
                return DataResult.error(() -> "Symbol must be a single character: " + symbolStr);
            }
            char symbol = symbolStr.charAt(0);
            List<Pair<Integer, String>> entries = new ArrayList<>();
            for (var sectionEntry : ruleSection.entrySet()) {
                entries.add(Pair.of(sectionEntry.getValue(), sectionEntry.getKey()));
            }
            if (entries.isEmpty()) {
                return DataResult.error(() -> "Rule has no substitutions");
            }
            List<String> tags = Arrays.stream(parts).skip(1).collect(Collectors.toCollection(ArrayList::new));
            ruleSets.put(symbol, new RuleSet(entries, tags));
        }

        return DataResult.success(new GrammarGraph(ruleSets));
    }

    public DataResult<GrammarGraph> validate(Iterable<Character> startingSymbols, Set<Character> roomSymbols) {
        if (roomSymbols.stream().anyMatch(ruleSets::containsKey)) {
            return DataResult.error(() -> "Room cannot use the same symbol as a grammar rule set");
        }
        if (ruleSets.isEmpty()) {
            for (Character startingSymbol : startingSymbols) {
                if (!roomSymbols.contains(startingSymbol)) {
                    return DataResult.error(() -> "Could not find starting/branch '" + startingSymbol + "' symbol");
                }
            }
            return DataResult.success(this);
        }

        for (Character startingSymbol : startingSymbols) {
            if (!ruleSets.containsKey(startingSymbol)) {
                return DataResult.error(() -> "Could not find starting/branch '" + startingSymbol + "' symbol");
            }
        }

        for (RuleSet ruleSet : ruleSets.values()) {
            for (Pair<Integer, String> entry : ruleSet.getEntries()) {
                String substitution = entry.getRight();
                for (int i = 0; i < substitution.length(); i++) {
                    char target = substitution.charAt(i);
                    if (!ruleSets.containsKey(target) && !roomSymbols.contains(target)) {
                        return DataResult.error(() -> "Substitution contains symbol that is not a rule or a room");
                    }
                }
            }
        }

        for (Character startingSymbol : startingSymbols) {
            DataResult<GrammarGraph> result = checkIllegalRecursion(startingSymbol, new HashSet<>(), new HashSet<>());
            if (result.isError()) {
                return result;
            }
        }

        return DataResult.success(this);
    }

    // Checks for all recursion in the grammar and complains about it, except in the special case where a rule directly
    // references itself at the *end* of a substitution. Grammars where this rule is enforced, along with the other
    // trivial checks performed by this class, guarantee that the cave will terminate.
    private DataResult<GrammarGraph> checkIllegalRecursion(char fromSymbol, Set<Character> recursionStack, Set<Character> visitedSymbols) {
        if (recursionStack.contains(fromSymbol)) {
            return DataResult.error(() -> "Complex recursion detected in the cave grammar");
        }

        if (!visitedSymbols.add(fromSymbol)) {
            return DataResult.success(this);
        }

        recursionStack.add(fromSymbol);

        RuleSet ruleSet = ruleSets.get(fromSymbol);
        boolean foundNonSelfReferential = false;
        for (Pair<Integer, String> entry : ruleSet.getEntries()) {
            String substitution = entry.getRight();
            for (int i = 0; i < substitution.length(); i++) {
                char target = substitution.charAt(i);
                if (target == fromSymbol) {
                    if (i != substitution.length() - 1) {
                        return DataResult.error(() -> "Self-referential rule detected in cave grammar. Self-reference is only allowed at the end of a substitution");
                    }
                } else {
                    if (ruleSets.containsKey(target)) {
                        DataResult<GrammarGraph> result = checkIllegalRecursion(target, recursionStack, visitedSymbols);
                        if (result.isError()) {
                            return result;
                        }
                    }
                }
            }
            foundNonSelfReferential |= substitution.isEmpty() || substitution.charAt(substitution.length() - 1) != fromSymbol;
        }
        if (!foundNonSelfReferential) {
            return DataResult.error(() -> "Rule is made up of entirely self-referential substitutions");
        }

        recursionStack.remove(fromSymbol);

        return DataResult.success(this);
    }

    public static class RuleSet {
        // A list of strings that this character may be replaced with, each with a weight attached
        private final List<Pair<Integer, String>> entries;
        private final List<String> tags;
        private final int totalWeight;

        public RuleSet(List<Pair<Integer, String>> entries, List<String> tags) {
            this.entries = entries;
            this.tags = tags;
            int totalWeight = 0;
            for (Pair<Integer, String> entry : entries) {
                totalWeight += entry.getLeft();
            }
            this.totalWeight = totalWeight;
        }

        public List<Pair<Integer, String>> getEntries() {
            return entries;
        }

        public List<String> getTags() {
            return tags;
        }

        public String getRandomSubstitution(RandomSource rand) {
            int randVal = rand.nextInt(totalWeight);
            for (Pair<Integer, String> entry : entries) {
                randVal -= entry.getLeft();
                if (randVal < 0) {
                    return entry.getRight();
                }
            }
            throw new AssertionError("The total weight of the entries is greater than totalWeight?");
        }
    }
}
