package de.learnlib.algorithm.register.radt;

import de.learnlib.data.SMTUtil;
import de.learnlib.data.DataWords;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SDTLeaf;
import de.learnlib.data.SDTRelabeling;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueGenerator;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.data.UnrestrictedSuffixValue;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.GuardElement;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.Valuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class OptimizedSymbolicSuffixBuilder<I extends ParameterizedSymbol> {

    private final Constants consts;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    public OptimizedSymbolicSuffixBuilder(Constants consts, SymbolicSuffixRestrictionBuilder restrictionBuilder) {
        this.consts = consts;
        this.restrictionBuilder = restrictionBuilder;
    }

    /**
     * Extend suffix by prepending it with the last symbol of prefix. Any suffix value in the
     * new suffix which is not compared with a constant, a parameter in the prefix (excluding
     * the last symbol), a previous free suffix value or more than one symbolic data value
     * will be set to non-free. Any non-free parameter that is equal to a single non-free
     * suffix value will be optimized for equality with that suffix value.
     *
     * @param prefix (last symbol will be prepended to suffix)
     * @param sdt
     * @param suffix
     * @param registers - a list of registers that must be revealed by the suffix
     * @return a new suffix formed by prepending suffix with the last symbol of prefix
     */
    public SymbolicSuffix extendSuffix(Word<SymbolInstance<I>> prefix, SDT sdt, SymbolicSuffix suffix, DataValue... values) {
        Word<I> suffixActions = suffix.getActions();
        if (values.length > 0) {
            SymbolicSuffix s = extendSuffixRevealingRegisters(prefix, sdt, suffixActions, values);
            return s;
        }

        Set<List<SDTGuard<?>>> paths = sdt.getAllPaths(new ArrayList<>()).keySet();
        SymbolicSuffix coalesced = null;
        for (List<SDTGuard<?>> path : paths) {
            SymbolicSuffix extended = extendSuffix(prefix, path, suffixActions);
            if (coalesced == null) {
                coalesced = extended;
            } else {
                coalesced = coalesceSuffixes(coalesced, extended);
            }
        }
        return coalesced;
    }

    private SymbolicSuffix extendSuffixRevealingRegisters(Word<SymbolInstance<I>> prefix, SDT sdt, Word<I> suffixActions, DataValue[] registers) {
        SDT prunedSDT = pruneSDT(sdt, registers);
        Set<List<SDTGuard<?>>> paths = prunedSDT.getAllPaths(new ArrayList<>()).keySet();
        assert paths.size() > 0 : "All paths in SDT were pruned";
        SymbolicSuffix suffix = null;
        for (List<SDTGuard<?>> path : paths) {
            SymbolicSuffix extended = extendSuffix(prefix, path, suffixActions);
            if (suffix == null) {
                suffix = extended;
            } else {
                suffix = mergeSuffixes(extended, suffix);
            }
        }
        return suffix;
    }

    SymbolicSuffix extendSuffix(Word<SymbolInstance<I>> prefix, List<SDTGuard<?>> sdtPath, Word<I> suffixActions) {
        Word<SymbolInstance<I>> sub = prefix.prefix(prefix.length()-1);
        SymbolInstance<I> action = prefix.lastSymbol();
        I actionSymbol = action.getBaseSymbol();
        SymbolicSuffix<I> actionSuffix = new SymbolicSuffix<>(sub, prefix.suffix(1), restrictionBuilder);
        int actionArity = actionSymbol.getArity();
        int subArity = DataWords.paramValLength(sub);

        Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
        for (SuffixValue<?> sv : actionSuffix.getDataValues()) {
            restrictions.put(sv, actionSuffix.getRestriction(sv));
        }

        List<DataValue> subVals = Arrays.asList(DataWords.valsOf(prefix));
        SDTRelabeling renaming = new SDTRelabeling();
        sdtPath.stream()
                .map(SDTGuard::getRegisters)
                .flatMap(Set::stream)
                .filter(DataValue.class::isInstance)
                .map( x -> (DataValue<?>) x)
                .distinct().forEach( d -> {
                    int dPos = subVals.indexOf(d);
                    if (dPos >= subArity) {
                        SuffixValue sv = new SuffixValue(d.getDataType(), dPos+1-subArity);
                        renaming.put(d, sv);
                    }
                });

        for (SDTGuard guard : sdtPath) {
            SuffixValue oldSV = guard.getParameter();
            SuffixValue newSV = new SuffixValue(oldSV.getDataType(), oldSV.getId()+actionArity);
            renaming.put(oldSV, newSV);
            SDTGuard renamedGuard = guard.relabel(renaming);
            SuffixValueRestriction restr = restrictionBuilder.restrictSuffixValue(renamedGuard, restrictions);
            restrictions.put(newSV, restr);
        }

        Word<I> actions = suffixActions.prepend(actionSymbol);
        return new SymbolicSuffix(actions, restrictions);
    }

    SDT pruneSDT(SDT sdt, DataValue[] registers) {
        LabeledSDT lsdt = new LabeledSDT(0, sdt);
        LabeledSDT pruned = pruneSDTNode(lsdt, lsdt, registers);
        return pruned.toUnlabeled();
    }

    private LabeledSDT pruneSDTNode(LabeledSDT lsdt, LabeledSDT node, DataValue[] registers) {
        LabeledSDT pruned = lsdt;
        int nodeLabel = node.getLabel();
        for (int label : node.getChildIndices()) {
            if (pruned.getNode(nodeLabel).getChildren().size() < 2) {
                break;
            }
            pruned = pruneSDTBranch(pruned, label, registers);
        }
        for (int label : pruned.getNode(nodeLabel).getChildIndices()) {
            LabeledSDT parent = pruned.getNode(label);
            if (parent != null) {
                pruned = pruneSDTNode(pruned, parent, registers);
            }
        }
        return pruned;
    }

    private LabeledSDT pruneSDTBranch(LabeledSDT lsdt, int label, DataValue[] registers) {
        if (branchContainsRegister(lsdt.getNode(label), registers) ||
            guardOnRegisters(lsdt.getGuard(label), registers)) {
            return lsdt;
        }
        LabeledSDT pruned = LabeledSDT.pruneBranch(lsdt, label);
        SDT prunedSDT = pruned.toUnlabeled();
        int revealedRegisters = 0;
        for (DataValue r : registers) {
            if (guardsOnRegisterHaveBothOutcomes(prunedSDT, r)) {
                revealedRegisters++;
            }
        }
        if (revealedRegisters < registers.length) {
            return lsdt;
        }
        return pruned;
    }

    private boolean branchContainsRegister(LabeledSDT node, DataValue[] registers) {
        for (Map.Entry<SDTGuard, LabeledSDT> e : node.getChildren().entrySet()) {
            SDTGuard guard = e.getKey();
            LabeledSDT child = e.getValue();
            Set<GuardElement> comparands = guard.getComparands(guard.getParameter());
            for (DataValue sdv : registers) {
                if (comparands.contains(sdv)) {
                    return true;
                }
            }
            boolean childContainsRegister = branchContainsRegister(child, registers);
            if (childContainsRegister)
                return true;
        }
        return false;
    }

    private boolean guardOnRegisters(SDTGuard guard, DataValue[] registers) {
        SuffixValue sv = guard.getParameter();
        for (DataValue r : registers) {
            if (guard.getComparands(sv).contains(r)) {
            return true;
            }
        }
        return false;
    }

    private SymbolicSuffix mergeSuffixes(SymbolicSuffix<I> suffix1, SymbolicSuffix suffix2) {
        assert suffix1.getActions().equals(suffix2.getActions());

        Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
        for (SuffixValue sv : suffix1.getDataValues()) {
            SuffixValueRestriction restr1 = suffix1.getRestriction(sv);
            SuffixValueRestriction restr2 = suffix2.getRestriction(sv);
            if (restr1.equals(restr2)) {
                restrictions.put(sv, restr1);
            } else {
                restrictions.put(sv, new UnrestrictedSuffixValue(sv));
            }
        }
        return new SymbolicSuffix(suffix1.getActions(), restrictions);
    }

    public boolean sdtRevealsRegister(SDT sdt, SymbolicDataValue register) {
        if (sdt instanceof SDTLeaf) {
            return false;
        }

        Map<SDTGuard, SDT> children = sdt.getChildren();
        Set<SDTGuard> guards = new LinkedHashSet<>();
        for (Map.Entry<SDTGuard, SDT> branch : children.entrySet()) {
            SDTGuard guard = branch.getKey();
            SDT s = branch.getValue();
            if (guard.getComparands(guard.getParameter()).contains(register)) {
                guards.add(guard);
            } else {
                boolean revealed = sdtRevealsRegister(s, register);
                if (revealed) {
                    return true;
                }
            }
        }

        // cannot have both outcomes if not at least 2 branches
        if (guards.size() < 2) {
            return false;
        }

        // find a guard that can accept
        SDTGuard guardA = null;
        for (SDTGuard g : guards) {
            SDT s = children.get(g);
            if (!s.getPaths(true).isEmpty()) {
                guardA = g;
                break;
            }
        }
        if (guardA == null) {
            return false;
        }
        // exists other guard which can reject?
        for (SDTGuard g : guards) {
            if (g == guardA) {
                continue;
            }
            SDT s = children.get(g);
            if (!s.getPaths(false).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean guardsOnRegisterHaveBothOutcomes(SDT sdt, DataValue register) {
        if (sdt instanceof SDTLeaf)
            return true;

        Map<SDTGuard, SDT> childrenWithRegister = new LinkedHashMap<>();
        for (Map.Entry<SDTGuard, SDT> e : sdt.getChildren().entrySet()) {
            if (!e.getKey().getComparands(register).isEmpty()) {
            childrenWithRegister.put(e.getKey(), e.getValue());
            }
        }
        if (!childrenWithRegister.isEmpty()) {
            if (!guardHasBothOutcomes(childrenWithRegister)) {
                return false;
            }
        }
        boolean ret = true;
        for (SDT child : sdt.getChildren().values()) {
            ret = ret && guardsOnRegisterHaveBothOutcomes(child, register);
        }
        return ret;
    }

    private boolean guardHasBothOutcomes(Map<SDTGuard, SDT> children) {
        if (children.size() < 2)
            return false;
        Iterator<SDTGuard> guards = children.keySet().iterator();
        SDT firstSDT = children.get(guards.next());
        boolean hasAccepting = !firstSDT.getPaths(true).isEmpty();
        while(guards.hasNext()) {
            // if hasAccepting, find rejecting
            // else find accepting
            if (!children.get(guards.next()).getPaths(!hasAccepting).isEmpty()) {
                return true;
            }
        }
        if (hasAccepting) {
            // could not find rejecting, see if first sdt has rejecting
            return !firstSDT.getPaths(false).isEmpty();
        }
        return false;
    }

    /**
     * Provides a one-symbol extension of an (optimized) suffix for two non-empty prefixes leading
     * to inequivalent locations, based on the SDTs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix extendDistinguishingSuffix(Word<SymbolInstance<I>> prefix1, SDT sdt1,
            Word<SymbolInstance<I>> prefix2,  SDT sdt2,  SymbolicSuffix suffix) {
        assert !prefix1.isEmpty() && !prefix2.isEmpty() && prefix1.lastSymbol().getBaseSymbol().equals(prefix2.lastSymbol().getBaseSymbol());
        // prefix1 = subprefix1 + sym(d1); prefix2 = subprefix2 + sym(d2)
        // our new_suffix will be sym(s1) + suffix
        // we first determine if s1 is free (extended to all parameters in sym, if there are more)
        SymbolicSuffix suffix1 = extendSuffix(prefix1, sdt1, suffix);
        SymbolicSuffix suffix2 = extendSuffix(prefix2, sdt2, suffix);

        return coalesceSuffixes(suffix1, suffix2);
    }

    /**
     * Provides an optimized suffix to distinguish two inequivalent locations specified by prefixes,
     * based on the SDTs that revealed the source of the inequivalence.
     */
    public SymbolicSuffix distinguishingSuffixFromSDTs(Word<SymbolInstance<I>> prefix1, SDT sdt1,
            Word<SymbolInstance<I>> prefix2,  SDT sdt2,  Word<I> suffixActions, ConstraintSolver solver) {
        // we build valuations which we use to determine satisfiable paths
        Valuation<SymbolicDataValue<?>, DataValue<?>> valuationSdt1 = buildValuation(prefix1, consts);
        Valuation<SymbolicDataValue<?>, DataValue<?>> valuationSdt2 = buildValuation(prefix2, consts);
        Valuation<SymbolicDataValue<?>, DataValue<?>> combined = new Valuation<>();
        combined.putAll(valuationSdt1);
        combined.putAll(valuationSdt2);
        SymbolicSuffix suffix = distinguishingSuffixFromSDTs(prefix1, sdt1, prefix2, sdt2, combined, suffixActions, solver);
        return suffix;
    }

    private SymbolicSuffix distinguishingSuffixFromSDTs(Word<SymbolInstance<I>> prefix1, SDT sdt1,
                                                        Word<SymbolInstance<I>> prefix2, SDT sdt2,
                                                        Valuation<SymbolicDataValue<?>, DataValue<?>> valuation, Word<I> suffixActions, ConstraintSolver solver) {
        SymbolicSuffix best = null;
        for (boolean b : new boolean [] {true, false}) {
            // we check for paths
            List<List<SDTGuard<?>>> pathsSdt1 = sdt1.getPaths(b);
            List<List<SDTGuard<?>>> pathsSdt2 = sdt2.getPaths(!b);
            for (List<SDTGuard<?>> pathSdt1 : pathsSdt1) {
                Expression<Boolean>  expr1 = toGuardExpression(pathSdt1);
                for (List<SDTGuard<?>> pathSdt2 : pathsSdt2) {
                    Expression<Boolean>  expr2 = toGuardExpression(pathSdt2);
                    if (solver.isSatisfiable(SMTUtil.toExpression(ExpressionUtil.and(expr1, expr2), valuation)) == Result.SAT) {
                        SymbolicSuffix suffix = buildOptimizedSuffix(prefix1, pathSdt1, prefix2, pathSdt2, suffixActions);
                        best = pickBest(best, suffix);
                    }
                }
            }
        }

        return best;
    }

    private SymbolicSuffix buildOptimizedSuffix(Word<SymbolInstance<I>> prefix1, List<SDTGuard<?>> pathSdt1,
            Word<SymbolInstance<I>> prefix2, List<SDTGuard<?>> pathSdt2,
            Word<I> suffixActions) {
        SymbolicSuffix suffix1 = extendSuffix(prefix1, pathSdt1, suffixActions);
        SymbolicSuffix suffix2 = extendSuffix(prefix2, pathSdt2, suffixActions);

        return coalesceSuffixes(suffix1, suffix2);
    }

    SymbolicSuffix coalesceSuffixes(SymbolicSuffix suffix1, SymbolicSuffix suffix2) {
        assert suffix1.getActions().equals(suffix2.getActions());

        Map<SuffixValue, SuffixValueRestriction> restrictions = new LinkedHashMap<>();

        SuffixValueGenerator sgen = new SuffixValueGenerator();
        for (int i = 0; i < DataWords.paramLength(suffix1.getActions()); i++) {
            DataType type = suffix1.getDataValue(i + 1).getDataType();
            SuffixValue sv = sgen.next(type);
            SuffixValueRestriction restr1 = suffix1.getRestriction(sv);
            SuffixValueRestriction restr2 = suffix2.getRestriction(sv);
            SuffixValueRestriction restr = restr1.merge(restr2, restrictions);
            restrictions.put(sv, restr);
        }

        return new SymbolicSuffix(suffix1.getActions(), restrictions);
    }

    private SymbolicSuffix pickBest(SymbolicSuffix current, SymbolicSuffix next) {
        if (current == null) {
            return next;
        }
        if (score(next) < score(current)) {
            return next;
        }
        return current;
    }

    private int score(SymbolicSuffix suffix) {
        final int freeCost = 100000;
        final int distinctValueCost = 100;
        return suffix.getFreeValues().size() * freeCost + suffix.getValues().size() * distinctValueCost;
    }

    private Expression<Boolean> toGuardExpression(List<SDTGuard<?>> guards) {
        List<Expression<Boolean>> expr = new ArrayList<>();
        for (SDTGuard g : guards) {
            expr.add(g.toExpr());
        }
        Expression<Boolean> [] exprArr = new Expression[expr.size()];
        return ExpressionUtil.and(expr.toArray(exprArr));
    }

    private Valuation<SymbolicDataValue<?>, DataValue<?>> buildValuation(Word<SymbolInstance<I>> prefix, Constants constants) {
        Valuation<SymbolicDataValue<?>, DataValue<?>> valuation = new Valuation<>();
        DataValue[] values = DataWords.valsOf(prefix);
        constants.forEach((c, dv) -> valuation.put(c, dv));
        return valuation;
    }
}
