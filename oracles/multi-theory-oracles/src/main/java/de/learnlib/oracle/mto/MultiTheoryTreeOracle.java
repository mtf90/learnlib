/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.oracle.mto;

import de.learnlib.data.Branching;
import de.learnlib.data.DataWords;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SDTLeaf;
import de.learnlib.data.SuffixValuation;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.logging.Category;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.oracle.mto.MultiTheoryBranching.Node;
import de.learnlib.query.DefaultQuery;
import de.learnlib.theory.Theories;
import de.learnlib.theory.Theory;
import de.learnlib.theory.guard.AndGuard;
import de.learnlib.theory.guard.TrueGuard;
import de.learnlib.theory.restriction.SymbolicSuffixRestrictionBuilderImpl;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.automatalib.common.util.Pair;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.Mapping;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author falk
 */
public class MultiTheoryTreeOracle<I extends ParameterizedSymbol> implements TreeOracle<I> {

    private final MembershipOracle<SymbolInstance<I>, Boolean> oracle;

    private final Constants constants;

    private final Theories teachers;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final ConstraintSolver solver;

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiTheoryTreeOracle.class);

    public MultiTheoryTreeOracle(MembershipOracle<SymbolInstance<I>, Boolean> oracle, Theories teachers, Constants constants,
                                 ConstraintSolver solver) {
        this(oracle, teachers, constants, solver, new SymbolicSuffixRestrictionBuilderImpl(constants, teachers));
    }

    public MultiTheoryTreeOracle(MembershipOracle<SymbolInstance<I>, Boolean> oracle, Theories teachers, Constants constants,
                                 ConstraintSolver solver, SymbolicSuffixRestrictionBuilder builder) {
        this.oracle = oracle;
        this.teachers = teachers;
        this.constants = constants;
        this.solver = solver;
        this.restrictionBuilder = builder;
    }

    @Override
    public SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix) {
        SDT sdt = treeQuery(prefix, suffix, new ArrayList<>(), constants, new SuffixValuation());
        //System.out.println(sdt);
        return sdt;
    }

    @Override
    public SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix, List<DataValue<?>> values,
            Constants constants, SuffixValuation suffixValues) {

//        System.out.println("prefix = " + prefix + "   suffix = " + suffix + "    values = " + values);

        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<SymbolInstance<I>> concSuffix = DataWords.instantiate(suffix.getActions(), values);

//            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<SymbolInstance<I>, Boolean> query = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

//            System.out.println("Trace = " + trace.toString() + " >>> "
//                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);

        Theory teach = teachers.get(sd.getDataType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, constants, suffixValues, this);
    }

    public <T> SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix, List<DataValue<?>> values,
                         SuffixValue<T> value, Constants constants, SuffixValuation suffixValues) {

        //        System.out.println("prefix = " + prefix + "   suffix = " + suffix + "    values = " + values);

        if (values.size() == DataWords.paramLength(suffix.getActions())) {
            Word<SymbolInstance<I>> concSuffix = DataWords.instantiate(suffix.getActions(), values);

            //            Word<PSymbolInstance> trace = prefix.concat(concSuffix);
            DefaultQuery<SymbolInstance<I>, Boolean> query = new DefaultQuery<>(prefix, concSuffix);
            oracle.processQueries(Collections.singletonList(query));
            boolean qOut = query.getOutput();

            //            System.out.println("Trace = " + trace.toString() + " >>> "
            //                    + (qOut ? "ACCEPT (+)" : "REJECT (-)"));
            return qOut ? SDTLeaf.ACCEPTING : SDTLeaf.REJECTING;

            // return accept / reject as a leaf
        }

        // OTHERWISE get the first noninstantiated data value in the suffix and its type
        SymbolicDataValue sd = suffix.getDataValue(values.size() + 1);

        Theory teach = teachers.get(sd.getDataType());

        // make a new tree query for prefix, suffix, prefix valuation, ...
        // to the correct teacher (given by type of first DV in suffix)
        return teach.treeQuery(prefix, suffix, values, constants, suffixValues, this);
    }

    /**
     * This method computes the initial branching for an SDT. It reuses existing
     * valuations where possible.
     *
     */
    @Override
    public Branching<I> getInitialBranching(Word<SymbolInstance<I>> prefix, I ps, SDT... sdts) {

        LOGGER.info(Category.QUERY, "computing initial branching for {0} after {1}", new Object[] { ps, prefix });

        MultiTheoryBranching mtb;
        Node n;

        if (sdts.length == 0) {
            n = createFreshNode(1, prefix, ps, new SuffixValuation());
            mtb = new MultiTheoryBranching(prefix, ps, n, constants, teachers, solver, sdts);
        } else {
            n = createNode(1, prefix, ps, new SuffixValuation(), new LinkedHashMap<>(), constants, teachers, solver, sdts);
            MultiTheoryBranching fluff = new MultiTheoryBranching(prefix, ps, n, constants, teachers, solver, sdts);
            mtb = fluff;
        }

        LOGGER.trace(Category.QUERY, mtb.toString());

        return mtb;
    }

    private Node createFreshNode(int i, Word<SymbolInstance<I>> prefix, I ps, SuffixValuation pval) {

        if (i == ps.getArity() + 1) {
            return new Node();
        } else {
            Map<DataValue<?>, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            DataType type = ps.getPtypes()[i - 1];
            LOGGER.trace(Category.QUERY, "current type: " + type.getName());
            SuffixValue p = new SuffixValue(type, i);
            SDTGuard guard = new TrueGuard(new SuffixValue(type, i));
            Theory teach = teachers.get(type);
            DataValue dvi = teach.instantiate(prefix, ps, pval, constants, guard, p, new LinkedHashSet<>());
            pval.put(p, dvi);

            nextMap.put(dvi, createFreshNode(i + 1, prefix, ps, pval));

            guardMap.put(dvi, guard);
            return new Node(p, nextMap, guardMap);
        }
    }

    private static <I extends ParameterizedSymbol> Node createNode(int i, Word<SymbolInstance<I>> prefix, I ps, SuffixValuation pval, Constants constants,  Theories teachers, ConstraintSolver solver,
                                                                   SDT... sdts) {
        Node n = createNode(i, prefix, ps, pval, new LinkedHashMap<>(), constants, teachers, solver, sdts);
        return n;
    }

    public static <I extends ParameterizedSymbol> Node createNode(int i, Word<? extends SymbolInstance<I>> prefix, I ps, SuffixValuation pval,
                                                                  Map<SuffixValue, Set<DataValue<?>>> oldDvMap, Constants constants,  Theories teachers, ConstraintSolver solver, SDT... sdts) {

        if (i == ps.getArity() + 1) {
            return new Node();
        } else {
            // obtain the data type, teacher, parameter
            DataType type = ps.getPtypes()[i - 1];
            Theory teach = teachers.get(type);
            SuffixValue p = new SuffixValue(type, i);

            // valuation
            Mapping<SymbolicDataValue<?>, DataValue<?>> valuation = buildValuation(pval, prefix, constants);

            // the map may contain no old values for p, in which case we use an empty set
            // (to avoid potential NPE when instantiating guards)
            Set<DataValue<?>> oldDvs = oldDvMap.getOrDefault(p, Collections.emptySet());

            // initialize maps for next nodes
            Map<DataValue<?>, Node> nextMap = new LinkedHashMap<>();
            Map<DataValue, SDTGuard> guardMap = new LinkedHashMap<>();

            MultiTheorySDTLogicOracle mlo = new MultiTheorySDTLogicOracle(constants, solver);
            // get merged guards mapped to the set of old guards from which they are
            // generated
            Map<SDTGuard, Set<SDTGuard>> mergedGuards = getNewRefinedInitialGuards(sdts, mlo, valuation);
            // get old guards mapped to the child SDT they connect to
            Map<SDTGuard, List<SDT>> nextSDTs = getChildren(sdts);

            for (Map.Entry<SDTGuard, Set<SDTGuard>> mergedGuardEntry : mergedGuards.entrySet()) {
                SDTGuard guard = mergedGuardEntry.getKey();
                Set<SDTGuard> oldGuards = mergedGuardEntry.getValue();

                // first solve using a constraint solver
                DataValue dvi = teach.instantiate(prefix, ps, pval, constants, guard, p, oldDvs);
                // if merging of guards is done properly, there should be no case where the
                // guard cannot be instantiated.
                assert (dvi != null);

                SDT[] nextLevelSDTs = oldGuards.stream().map(g -> nextSDTs.get(g)).flatMap(g -> g.stream()) // stream
                                                                                                            // with of
                                                                                                            // sdt lists
                                                                                                            // for old
                                                                                                            // guards
                        .distinct().toArray(SDT[]::new); // merge and pick distinct elements

                SuffixValuation otherPval = new SuffixValuation();
                otherPval.putAll(pval);
                otherPval.put(p, dvi);

                nextMap.put(dvi, createNode(i + 1, prefix, ps, otherPval, oldDvMap, constants, teachers, solver, nextLevelSDTs));
                if (guardMap.containsKey(dvi)) {
                    throw new IllegalStateException(
                            "Guard instantiated using a dvi that was already used to instantiate a prior guard.");
                }
                guardMap.put(dvi, guard);
            }

            LOGGER.trace(Category.QUERY, "guardMap: " + guardMap);
            LOGGER.trace(Category.QUERY, "nextMap: " + nextMap);
            assert !nextMap.isEmpty();
            assert !guardMap.isEmpty();
            return new Node(p, nextMap, guardMap);
        }
    }

    // conjoins the initial guards of the SDTs producing a map from new refined
    // (initial) guards to the set of guards they originated from
    private static Map<SDTGuard, Set<SDTGuard>> getNewRefinedInitialGuards(SDT[] sdts, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue<?>, DataValue<?>> valuation) {
        Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
        for (SDT sdt : sdts) {
            Set<SDTGuard> nextGuardGroup = sdt.getChildren().keySet();
            mergedGroup = combineGroups(mergedGroup, nextGuardGroup, mlo, valuation);
        }
        return mergedGroup;
    }

    // merges the next set of initial guards to the map of new initial guards,
    // producing an new map of refined guards
    // merging involves:
    // 1. conjoining each initial guard with each refined guard in the map where
    // this is possible in the sense that the guards are not mutually exclusive
    // 2. updating the map
    private static Map<SDTGuard, Set<SDTGuard>> combineGroups(Map<SDTGuard, Set<SDTGuard>> mergedHead, Set<SDTGuard> nextGroup,
            MultiTheorySDTLogicOracle mlo, Mapping<SymbolicDataValue<?>, DataValue<?>> valuation) {
        Map<SDTGuard, Set<SDTGuard>> mergedGroup = new LinkedHashMap<>();
        if (mergedHead.isEmpty()) {
            nextGroup.forEach(next -> {
                mergedGroup.put(next, Collections.singleton(next));
            });
            return mergedGroup;
        }

        // pairs of guards that have been conjoined
        Set<Pair<SDTGuard, SDTGuard>> headNextPairs = new HashSet<>();

        for (Map.Entry<SDTGuard, Set<SDTGuard>> entry : mergedHead.entrySet()) {
            SDTGuard head = entry.getKey();
            Set<SDTGuard> oldGuards = entry.getValue();

            // we filter out pairs already covered
            SDTGuard[] notCoveredPairs = nextGroup.stream()
                    .filter(next -> !headNextPairs.contains(Pair.of(next, head))).toArray(SDTGuard[]::new);

            // we then select only the next guards which can be conjoined with the head
            // guard, i.e.
            SDTGuard[] compatibleNextGuards = Stream.of(notCoveredPairs)
                    .filter(next -> canBeMerged(head, next, mlo, valuation)).toArray(SDTGuard[]::new);

            for (SDTGuard next : compatibleNextGuards) {
                SDTGuard refinedGuard = null;
                if (head.equals(next))
                    refinedGuard = next;
                else if (refines(next, head, mlo, valuation))
                    refinedGuard = next;
                else if (refines(head, next, mlo, valuation))
                    refinedGuard = head;
                else
                    refinedGuard = conjoin(head, next);

                // we compute the old guard set, that is the guards over which conjunction was
                // applied to form the refined guard
                LinkedHashSet<SDTGuard> newOldGuards = new LinkedHashSet<>(oldGuards);
                newOldGuards.add(next);
                mergedGroup.put(refinedGuard, newOldGuards);
                headNextPairs.add(Pair.of(head, next));
            }
        }
        return mergedGroup;
    }

    public static SDTGuard conjoin(SDTGuard guard1, SDTGuard guard2) {
        assert guard1.getParameter().equals(guard2.getParameter());
        if (guard1.equals(guard2))
            return guard1;

        if (guard1 instanceof TrueGuard) {
            return guard2;
        }

        if (guard2 instanceof TrueGuard) {
            return guard1;
        }

        if (guard1 instanceof AndGuard && guard2 instanceof AndGuard) {
            List<SDTGuard> guards = new ArrayList<SDTGuard>(((AndGuard) guard1).conjuncts());
            guards.addAll(((AndGuard) guard2).conjuncts());
            return new AndGuard(guard1.getParameter(), guards);
        }

        if (guard1 instanceof AndGuard || guard2 instanceof AndGuard) {
            AndGuard andGuard = guard1 instanceof AndGuard ?
                    (AndGuard) guard1 : (AndGuard) guard2;
            SDTGuard otherGuard = guard2 instanceof AndGuard ? guard1 : guard2;
            List<SDTGuard> conjuncts = andGuard.conjuncts();
            conjuncts.add(otherGuard);
            return new AndGuard(guard1.getParameter(), conjuncts);
        }
        return new AndGuard(guard1.getParameter(), Arrays.asList(guard1, guard2));
    }

    private static boolean canBeMerged(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue<?>, DataValue<?>> valuation) {
//        if (a.equals(b) || a instanceof TrueGuard || b instanceof TrueGuard)
//            return true;

        // two equality guards cannot be merged as this could
        // violate history independence (it could entail the
        // equality of two registers)
//        if (a instanceof EqualityGuard && b instanceof EqualityGuard)
//            return false;

        // some quick answers, implemented for compatibility with older theories.
//        if (a instanceof EqualityGuard)
//            if (b.equals( a.invert() ))
//                return false;
//        if (b instanceof EqualityGuard)
//            if (a.equals( b.invert() ))
//                return false;
        return !mlo.areMutuallyExclusive(a.toExpr(), b.toExpr(), valuation);
    }

    private static boolean refines(SDTGuard a, SDTGuard b, MultiTheorySDTLogicOracle mlo,
            Mapping<SymbolicDataValue<?>, DataValue<?>> valuation) {
        if (b instanceof TrueGuard)
            return true;
        boolean ref1 = mlo.doesRefine(a.toExpr(), b.toExpr(), valuation);
        return ref1;
    }

    // Produces a mapping from top level SDT guards to the next level SDTs.
    // Since the same guard can appear in multiple SDTs, the guard maps to a list of SDTs.
    private static Map<SDTGuard, List<SDT>> getChildren(SDT[] sdts) {
        List<Map<SDTGuard, SDT>> sdtChildren = Stream.of(sdts).map(sdt -> sdt.getChildren())
                .collect(Collectors.toList());
        Map<SDTGuard, List<SDT>> children = new LinkedHashMap<>();
        for (Map<SDTGuard, SDT> child : sdtChildren) {
            child.forEach((guard, nextSdt) -> {
                children.putIfAbsent(guard, new ArrayList<>());
                children.get(guard).add(nextSdt);
            });
        }

        return children;
    }

    private static <I extends ParameterizedSymbol> Mapping<SymbolicDataValue<?>, DataValue<?>> buildValuation(SuffixValuation suffixValuation,
            Word<? extends SymbolInstance<I>> prefix, Constants constants) {
        Mapping<SymbolicDataValue<?>, DataValue<?>> valuation = new Mapping<>();
        valuation.putAll(suffixValuation);
        valuation.putAll(constants);
        return valuation;
    }

    @Override
    public Map<Word<SymbolInstance<I>>, Boolean> instantiate(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix,
            SDT sdt) {

        Map<Word<SymbolInstance<I>>, Boolean> words = new LinkedHashMap<>();
        instantiate(words, prefix, suffix, sdt, 0, 0,
                    new SuffixValuation(), new ParameterGenerator(), new SuffixValuation(), new ParameterGenerator());
        return words;
    }

    private void instantiate(Map<Word<SymbolInstance<I>>, Boolean> words, Word<SymbolInstance<I>> prefix,
            SymbolicSuffix<I> suffix, SDT sdt, int aidx, int pidx,
                             SuffixValuation pval, ParameterGenerator pgen, SuffixValuation gpval, ParameterGenerator gpgen) {
        if (aidx == suffix.getActions().length()) {
            words.put(prefix, sdt.isAccepting());
        } else {
            I ps = suffix.getActions().getSymbol(aidx);
            if (ps.getArity() == pidx) {
                DataValue[] vals = pval.values().toArray(new DataValue [] {});
                SymbolInstance<I> psi = new SymbolInstance<>(ps, vals);
                Word<SymbolInstance<I>> newPrefix = prefix.append(psi);
                instantiate(words, newPrefix, suffix, sdt, aidx+1, 0, new SuffixValuation(), new ParameterGenerator(), gpval, gpgen);
            } else {
                SuffixValue p = new SuffixValue(ps.getPtypes()[pidx], pgen.next(ps.getPtypes()[pidx]).getId());
                SuffixValue gp = new SuffixValue( ps.getPtypes()[pidx], gpgen.next(ps.getPtypes()[pidx]).getId() );
                Theory t = teachers.get(ps.getPtypes()[pidx]);
                for (Map.Entry<SDTGuard, SDT> entry : sdt.getChildren().entrySet()) {
                    DataValue val = t.instantiate(prefix, ps, gpval, constants, entry.getKey(), p, Collections.emptySet());
                    SuffixValuation newPval = new SuffixValuation();
                    newPval.putAll(pval);
                    newPval.put(p, val);
                    SuffixValuation newGpval = new SuffixValuation();
                    newGpval.putAll(gpval);
                    newGpval.put(gp, val);
                    ParameterGenerator newPgen = new ParameterGenerator();
                    newPgen.set(pgen);
                    ParameterGenerator newGpgen = new ParameterGenerator();
                    newGpgen.set(gpgen);
                    instantiate(words, prefix, suffix, entry.getValue(), aidx, pidx+1, newPval, newPgen, newGpval, newGpgen);
                }
            }
        }
    }

    public Theories getTeachers() {
    	return teachers;
    }

    @Override
    public SymbolicSuffixRestrictionBuilder getRestrictionBuilder() {
    	return restrictionBuilder;
    }
}
