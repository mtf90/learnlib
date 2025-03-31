/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
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
package de.learnlib.theory;

import de.learnlib.data.DataWords;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SDTLeaf;
import de.learnlib.data.SDTRelabeling;
import de.learnlib.data.SuffixValuation;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.theory.guard.AndGuard;
import de.learnlib.theory.guard.DisequalityGuard;
import de.learnlib.theory.guard.EqualityGuard;
import de.learnlib.theory.guard.TrueGuard;
import de.learnlib.theory.restriction.GenericSuffixRestrictionBuilderImpl;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.GuardElement;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author falk and sofia
 */
public class IntegerEqualityTheory implements Theory<Integer> {

    private final DataType<Integer> dataType;

    private final boolean useNonFreeOptimization;

//    protected boolean freshValues = false;
//
//    protected IOOracle ioOracle;

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegerEqualityTheory.class);

    public IntegerEqualityTheory(DataType<Integer> dataType) {
        this.dataType = dataType;
        this.useNonFreeOptimization = false;
    }

    public IntegerEqualityTheory(DataType<Integer> dataType, boolean useNonFreeOptimization) {
        this.dataType = dataType;
        this.useNonFreeOptimization = useNonFreeOptimization;
    }

    public List<DataValue<Integer>> getPotential(List<DataValue<Integer>> vals) {
        return vals;
    }

    // given a map from guards to SDTs, merge guards based on whether they can
    // use another SDT. Base case: always add the 'else' guard first.
    private Map<SDTGuard, SDT> mergeGuards(Map<EqualityGuard<Integer>, SDT> eqs, AndGuard deqGuard, SDT deqSdt) {
        Map<SDTGuard, SDT> retMap = new LinkedHashMap<>();
        List<SDTGuard> deqList = new ArrayList<>();
        List<EqualityGuard> eqList = new ArrayList<>();
        for (Map.Entry<EqualityGuard<Integer>, SDT> e : eqs.entrySet()) {
            SDT eqSdt = e.getValue();
            EqualityGuard eqGuard = e.getKey();
            LOGGER.trace("comparing guards: " + eqGuard.toString() + " to " + deqGuard.toString()
                    + "\nSDT    : " + eqSdt.toString() + "\nto SDT : " + deqSdt.toString());
            List<EqualityGuard<Integer>> ds = new ArrayList<>();
            ds.add(eqGuard);
            LOGGER.trace("remapping: " + ds);
            if (!isEquivalentUnder(eqSdt, deqSdt, ds)) {
                LOGGER.trace("--> not eq.");
                deqList.add(new DisequalityGuard(eqGuard.getParameter(), eqGuard.getRegister()));
                eqList.add(eqGuard);
            } else {
                LOGGER.trace("--> equivalent");
            }

        }
        if (eqList.isEmpty()) {
            retMap.put(new TrueGuard(deqGuard.getParameter()), deqSdt);
        } else if (eqList.size() == 1) {
            EqualityGuard q = eqList.get(0);
            retMap.put(q, eqs.get(q));
            retMap.put(new DisequalityGuard(q.getParameter(), q.getRegister()), deqSdt);
        } else if (eqList.size() > 1) {
            for (EqualityGuard q : eqList) {
                retMap.put(q, eqs.get(q));
            }
            retMap.put(new AndGuard(deqGuard.getParameter(), deqList), deqSdt);
        }
        assert !retMap.isEmpty();

        return retMap;
    }

    public boolean isEquivalentUnder(SDT eqSDT, SDT deqSDT, List<EqualityGuard<Integer>> ds) {
        //        throw new IllegalStateException();
        if (deqSDT instanceof SDTLeaf) {
            if (eqSDT instanceof SDTLeaf) {
                return (eqSDT.isAccepting() == deqSDT.isAccepting());
            }
            return false;
        }
        SDTRelabeling eqRenaming = new SDTRelabeling();
        for (EqualityGuard<?> d : ds) {
            eqRenaming.put(d.getParameter(), d.getRegister());
        }

        SDT otherRelabeled =  deqSDT.relabel(eqRenaming);
        return SDT.equivalentUnderId(eqSDT, otherRelabeled);
    }

    // process a tree query
    @Override
    public <I extends ParameterizedSymbol> SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix, List<DataValue<?>> values,
                                                         Constants constants, SuffixValuation suffixValues, TreeOracle<I> oracle) {

        int pId = values.size() + 1;

        SuffixValue<?> currentParam = suffix.getSuffixValue(pId);
        assert currentParam.getDataType().equals(this.dataType);
        DataType<Integer> type = (DataType<Integer>) currentParam.getDataType();

        Map<EqualityGuard<Integer>, SDT> tempKids = new LinkedHashMap<>();

        Collection<DataValue<Integer>> potSet = new HashSet<>();
        potSet.addAll(constants.values(type));
        potSet.addAll(DataWords.valSet(prefix, type));
        potSet.addAll(suffixValues.values(type));

        List<DataValue<Integer>> potList = new ArrayList<>(potSet);
        List<DataValue<Integer>> potential = getPotential(potList);

        DataValue<Integer> fresh = getFreshValue(potential);

        List<DataValue> equivClasses = new ArrayList<>(potSet);
        equivClasses.add(fresh);
        //System.out.println(" prefix: " + prefix);
        //System.out.println(" potential: " + potential);
        //System.out.println(" eqs " + Arrays.toString(equivClasses.toArray()));
        EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useNonFreeOptimization);
        List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(currentParam), prefix, suffix.getActions(), values);
        assert filteredEquivClasses.size() > 0;

        // TODO: integrate fresh-value optimization with restrictions
        // special case: fresh values in outputs
//        if (freshValues) {
//
//            ParameterizedSymbol ps = computeSymbol(suffix, pId);
//
//            if (ps instanceof OutputSymbol && ps.getArity() > 0) {
//
//                int idx = computeLocalIndex(suffix, pId);
//                Word<PSymbolInstance> query = buildQuery(prefix, suffix, values);
//                Word<PSymbolInstance> trace = ioOracle.trace(query);
//
//                if (!trace.isEmpty() && trace.lastSymbol().getBaseSymbol().equals(ps)) {
//
//                    DataValue d = trace.lastSymbol().getParameterValues()[idx];
//
//                    if (d instanceof FreshValue) {
//                        d = getFreshValue(potential);
//                        values.put(pId, d);
//                        WordValuation trueValues = new WordValuation();
//                        trueValues.putAll(values);
//                        SuffixValuation trueSuffixValues = new SuffixValuation();
//                        trueSuffixValues.putAll(suffixValues);
//                        trueSuffixValues.put(currentParam, d);
//                        SDT sdt = oracle.treeQuery(prefix, suffix, trueValues, constants, trueSuffixValues);
//
//                        LOGGER.trace(" single deq SDT : " + sdt.toString());
//
//                        Map<SDTGuard, SDT> merged = mergeGuards(tempKids, new AndGuard(currentParam, List.of()), sdt);
//
//                        LOGGER.trace("temporary guards = " + tempKids.keySet());
//                        LOGGER.trace("merged guards = " + merged.keySet());
//
//                        return new SDT(merged);
//                    }
//                } else {
//                    int maxSufIndex = DataWords.paramLength(suffix.getActions()) + 1;
//                    SDT rejSdt = makeRejectingBranch(currentParam.getId() + 1, maxSufIndex, type);
//                    TrueGuard trueGuard = new TrueGuard(currentParam);
//                    Map<SDTGuard, SDT> merged = new LinkedHashMap<>();
//                    merged.put(trueGuard, rejSdt);
//                    return new SDT(merged);
//                }
//            }
//        }

        LOGGER.trace("potential " + potential.toString());

        // process each 'if' case
        // prepare by picking up the prefix values
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        LOGGER.trace("prefix list    " + prefixValues);

        List<SDTGuard> diseqList = new ArrayList<>();
        for (DataValue newDv : potential) {
        	if (filteredEquivClasses.contains(newDv)) {
	            LOGGER.trace(newDv.toString());

	            // this is the valuation of the suffixvalues in the suffix
	            SuffixValuation ifSuffixValues = new SuffixValuation();
	            ifSuffixValues.putAll(suffixValues); // copy the suffix valuation

	            EqualityGuard eqGuard = pickupDataValue(newDv, prefixValues, currentParam, values, constants);
	            LOGGER.trace("eqGuard is: " + eqGuard);
	            diseqList.add(new DisequalityGuard(eqGuard.getParameter(), eqGuard.getRegister()));
	            // construct the equality guard
	            // find the data value in the prefix
	            // this is the valuation of the positions in the suffix
	            List<DataValue<?>> ifValues = new ArrayList<>();
	            ifValues.addAll(values);
	            ifValues.add(newDv);
	            SDT eqOracleSdt = oracle.treeQuery(prefix, suffix, ifValues, constants, ifSuffixValues);

	            tempKids.put(eqGuard, eqOracleSdt);
        	}
        }

        Map<SDTGuard, SDT> merged;

        // process the 'else' case
        if (filteredEquivClasses.contains(fresh)) {
        	// this is the valuation of the positions in the suffix
	        List<DataValue<?>> elseValues = new ArrayList<>();
	        elseValues.addAll(values);
	        elseValues.add(fresh);

	        // this is the valuation of the suffixvalues in the suffix
	        SuffixValuation elseSuffixValues = new SuffixValuation();
	        elseSuffixValues.putAll(suffixValues);
	        elseSuffixValues.put(currentParam, fresh);

	        SDT elseOracleSdt = oracle.treeQuery(prefix, suffix, elseValues, constants, elseSuffixValues);

	        AndGuard deqGuard = new AndGuard(currentParam, diseqList);
	        LOGGER.trace("diseq guard = " + deqGuard);

	        // merge the guards
	        merged = mergeGuards(tempKids, deqGuard, elseOracleSdt);
        } else {
        	// if no else case, we can only have a true guard
        	// TODO: add  support for multiple equalities with same outcome
        	assert tempKids.size() == 1;

        	Iterator<Map.Entry<EqualityGuard<Integer>, SDT>> it = tempKids.entrySet().iterator();
        	Map.Entry<EqualityGuard<Integer>, SDT> e = it.next();
        	merged = new LinkedHashMap<>();
        	merged.put(e.getKey(), e.getValue());
        }

        // only keep registers that are referenced by the merged guards
        //pir.putAll(keepMem(merged));

        LOGGER.trace("temporary guards = " + tempKids.keySet());
        LOGGER.trace("merged guards = " + merged.keySet());

        // clear the temporary map of children
        tempKids.clear();

        for (SDTGuard g : merged.keySet()) {
            assert !(g == null);
        }

        SDT returnSDT = new SDT(merged);
        return returnSDT;

    }

    // construct equality guard by picking up a data value from the prefix
    private EqualityGuard pickupDataValue(DataValue newDv, List<DataValue> prefixValues, SuffixValue currentParam,
            List<DataValue<?>> ifValues, Constants constants) {
        DataType type = currentParam.getDataType();
        int newDv_i;
        for (Map.Entry <Constant<?>, DataValue<?>> entry : constants.entrySet()) {
            if (entry.getValue().equals(newDv)) {
                return new EqualityGuard(currentParam, entry.getKey());
            }
        }
        if (prefixValues.contains(newDv)) {
            // first index of the data value in the prefixvalues list
            newDv_i = prefixValues.indexOf(newDv) + 1;
            Register newDv_r = new Register(type, newDv_i);
            LOGGER.trace("current param = " + currentParam);
            LOGGER.trace("New register = " + newDv_r);
            return new EqualityGuard(currentParam, newDv);

        } // if the data value isn't in the prefix,
            // it is somewhere earlier in the suffix
        else {
            for (int i = 0; i < ifValues.size(); i++) {
                if (ifValues.get(i).equals(newDv)) {
                    return new EqualityGuard(currentParam, new SuffixValue<>(type, i + 1));
                }
            }
            throw new IllegalStateException();
        }
    }

    @Override
    // instantiate a parameter with a data value
    public <I extends ParameterizedSymbol> DataValue instantiate(Word<SymbolInstance<I>> prefix, I ps, SuffixValuation pval,
            Constants constants, SDTGuard guard, SuffixValue<Integer> param, Set<DataValue<Integer>> oldDvs) {

        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));
        LOGGER.trace("prefix values : " + prefixValues);
        DataType type = param.getDataType();
        Deque<SDTGuard> guards = new LinkedList<>();
        guards.add(guard);

        while(!guards.isEmpty()) {
            SDTGuard current = guards.remove();
            if (current instanceof EqualityGuard) {
                EqualityGuard<?> eqGuard = (EqualityGuard) current;
                LOGGER.trace("equality guard " + current);
                GuardElement ereg = eqGuard.getRegister();
                if (ereg instanceof DataValue<?>) {
                    DataValue<?> dv = (DataValue<?>) ereg;

                    Parameter p = new Parameter(dv.getDataType(), prefixValues.indexOf(dv) + 1);
                    LOGGER.trace("p: " + p.toString());
                    int idx = p.getId();
                    return prefixValues.get(idx - 1);
                } else if (ereg instanceof SuffixValue<?>) {
                    return pval.get( (SuffixValue) ereg);
                } else if (ereg instanceof Constant<?>) {
                    return constants.get((Constant) ereg);
                }
            } else if (current instanceof AndGuard) {
                guards.addAll(((AndGuard) current).conjuncts());
            }
            // todo: this only works under the assumption that disjunctions only contain disequality guards
        }

        Collection<DataValue> potSet = DataWords.joinValsToSet(constants.values(type), DataWords.valSet(prefix, type),
                pval.values(type));

        if (!potSet.isEmpty()) {
            LOGGER.trace("potSet = " + potSet);
        } else {
            LOGGER.trace("potSet is empty");
        }
        DataValue fresh = this.getFreshValue(new ArrayList(potSet));
        LOGGER.trace("fresh = " + fresh.toString());
        return fresh;

    }

    private ParameterizedSymbol computeSymbol(SymbolicSuffix<?> suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return a;
            }
        }
        return suffix.getActions().size() > 0 ? suffix.getActions().firstSymbol() : null;
    }

    private int computeLocalIndex(SymbolicSuffix<?> suffix, int pId) {
        int idx = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            idx += a.getArity();
            if (idx >= pId) {
                return pId - (idx - a.getArity()) - 1;
            }
        }
        return pId - 1;
    }

    private Word<SymbolInstance<?>> buildQuery(Word<SymbolInstance<?>> prefix, SymbolicSuffix<?> suffix,
            List<DataValue<?>> values) {

        Word<SymbolInstance<?>> query = prefix;
        int base = 0;
        for (ParameterizedSymbol a : suffix.getActions()) {
            if (base + a.getArity() > values.size()) {
                break;
            }
            DataValue[] vals = new DataValue[a.getArity()];
            for (int i = 0; i < a.getArity(); i++) {
                vals[i] = values.get(base + i + 1);
            }
            query = query.append(new SymbolInstance<>(a, vals));
            base += a.getArity();
        }
        return query;
    }

    /*
     * Creates a "unary tree" of depth maxIndex - nextSufIndex which leads to a
     * rejecting Leaf. Edges are of type {@link SDTTrueGuard}. Used to shortcut
     * output processing.
     */
    private SDT makeRejectingBranch(int nextSufIndex, int maxIndex, DataType type) {
        if (nextSufIndex == maxIndex) {
            // map.put(guard, SDTLeaf.REJECTING);
            return SDTLeaf.REJECTING;
        } else {
            Map<SDTGuard, SDT> map = new LinkedHashMap<>();
            TrueGuard trueGuard = new TrueGuard(new SuffixValue(type, nextSufIndex));
            map.put(trueGuard, makeRejectingBranch(nextSufIndex + 1, maxIndex, type));
            SDT sdt = new SDT(map);
            return sdt;
        }
    }

    @Override
    public <I extends ParameterizedSymbol> SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<? extends SymbolInstance<I>> prefix, Word<? extends SymbolInstance<I>> suffix, Constants consts) {
    	// for now, use generic restrictions with equality theory
    	return GenericSuffixRestrictionBuilderImpl.genericRestriction(suffixValue, prefix, suffix, consts);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	// for now, use generic restrictions with equality theory
    	return GenericSuffixRestrictionBuilderImpl.genericRestriction(guard, prior);
    }

    @Override
    public DataValue<Integer> getFreshValue(Collection<DataValue<Integer>> vals) {
        int dv = -1;;
        for (DataValue<Integer> d : vals) {
            dv = Math.max(dv, d.getValue());
        }

        return new DataValue<>(dataType, dv + 1);
    }

    @Override
    public Collection<DataValue<Integer>> getAllNextValues(List<DataValue<Integer>> vals) {
        ArrayList<DataValue<Integer>> ret = new ArrayList<>(vals.size() + 1);
        ret.addAll(vals);
        ret.add(getFreshValue(vals));
        return ret;
    }

    @Override
    public DataType<?> getDataType() {
        return this.dataType;
    }
}
