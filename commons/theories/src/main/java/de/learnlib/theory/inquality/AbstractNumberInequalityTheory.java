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
package de.learnlib.theory.inquality;

import de.learnlib.data.Bijection;
import de.learnlib.data.DataWords;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SuffixValuation;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.theory.Theory;
import de.learnlib.data.UnrestrictedSuffixValue;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.theory.EquivalenceClassFilter;
import de.learnlib.theory.guard.DisequalityGuard;
import de.learnlib.theory.guard.EqualityGuard;
import de.learnlib.theory.guard.IntervalGuard;
import de.learnlib.theory.guard.TrueGuard;
import de.learnlib.theory.restriction.FreshSuffixValue;
import de.learnlib.theory.restriction.GenericSuffixRestrictionBuilderImpl;
import de.learnlib.theory.restriction.GreaterSuffixValue;
import de.learnlib.theory.restriction.LesserSuffixValue;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.ConstraintSolver.Result;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Valuation;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.expressions.Constant;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.GuardElement;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sofia Cassel
 * @author Fredrik Tåquist
 */
public abstract class AbstractNumberInequalityTheory<N extends Number & Comparable<N>> implements Theory<N> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNumberInequalityTheory.class);

	private final ConstraintSolver solver;

	private final DataType<N> type;

	boolean useSuffixOpt = false;

	public AbstractNumberInequalityTheory(DataType<N> t, ConstraintSolver solver) {
        this.type = t;
		this.solver = solver;
	}

	public AbstractNumberInequalityTheory(DataType<N> t, ConstraintSolver solver, boolean useSuffixOpt) {
        this.type = t;
		this.solver = solver;
		this.useSuffixOpt = useSuffixOpt;
	}

    /**
     * Given a prefix and a potential, generate data values for each equivalence class.
     *
     * @return A mapping from data values to their corresponding SDT guards
     */
    private <I extends ParameterizedSymbol> Map<DataValue<N>, SDTGuard> generateEquivClasses(SuffixValue<?> suffixValue,
																							 Map<DataValue<N>, GuardElement> potValuation,
																							 Constants consts) {

	Map<DataValue<N>, GuardElement> filteredPotValuation = new LinkedHashMap<>(potValuation);
	for (DataValue<N> d : potValuation.keySet()) {
		if (!d.getDataType().equals(suffixValue.getDataType())) {
			filteredPotValuation.remove(d);
		}
	}
	potValuation = filteredPotValuation;

    	Map<DataValue<N>, SDTGuard> valueGuards = new LinkedHashMap<>();

    	if (potValuation.isEmpty()) {
    		DataValue<N> fresh = getFreshValue(new ArrayList<>());
    		valueGuards.put(fresh, new TrueGuard<>(suffixValue));
    		return valueGuards;
    	}
        int usedVals = potValuation.size();
        List<DataValue<N>> sortedPot = new ArrayList<>(potValuation.keySet());
        sortedPot.sort(getComparator());

        Valuation vals = new Valuation();
        for (Entry<DataValue<N>, GuardElement> pot : potValuation.entrySet()) {
			GuardElement r = pot.getValue();
            DataValue<N> dv = pot.getKey();
            // TODO: fix unchecked invocation
	    if (!(r instanceof DataValue)) {
		vals.setValue((Variable<N>) r, dv.getValue());
	    }
        }

        // smallest
        DataValue dl = sortedPot.get(0);
        GuardElement rl = potValuation.get(dl);
        IntervalGuard sg = new IntervalGuard(suffixValue, null, rl);
        DataValue smallest = instantiate(sg, vals, consts, sortedPot);
        assert smallest != null;
        valueGuards.put(smallest, sg);

        for (int i = 1; i < usedVals; i++) {
            // equality
            EqualityGuard eg = new EqualityGuard(suffixValue, rl);
            valueGuards.put(dl, eg);

            // interval
            DataValue dr = sortedPot.get(i);
            GuardElement rr = potValuation.get(dr);
            IntervalGuard ig = new IntervalGuard(suffixValue, rl, rr);
            DataValue di = instantiate(ig, vals, consts, sortedPot);
            assert di != null;
            valueGuards.put(di, ig);

            dl = dr;
            rl = rr;
        }
        EqualityGuard eg = new EqualityGuard(suffixValue, rl);
        valueGuards.put(dl, eg);

        // greatest
        IntervalGuard gg = new IntervalGuard(suffixValue, rl, null);
        DataValue dg = instantiate(gg, vals, consts, sortedPot);
        assert dg != null;
        valueGuards.put(dg, gg);

        return valueGuards;
    }

    /**
     * Filter out equivalence classes that are to be removed through suffix optimization.
     *
     * @param valueGuards - a mapping between data values and corresponding SDT guards
     * @param prefix - the prefix
     * @param suffix - the suffix
     * @param suffixValue - the suffix value for which to apply optimizations
     * @param values - word valuation
     * @return valueGuards without data values that are filtered out due to optimizations
     */
    private <I extends ParameterizedSymbol> Map<DataValue, SDTGuard> filterEquivClasses(Map<DataValue<N>, SDTGuard> valueGuards,
			Word<SymbolInstance<I>> prefix,
			SymbolicSuffix suffix,
			SuffixValue suffixValue, List<DataValue<?>> values) {
		List<DataValue> equivClasses = new ArrayList<>();
		equivClasses.addAll(valueGuards.keySet());
		EquivalenceClassFilter eqcFilter = new EquivalenceClassFilter(equivClasses, useSuffixOpt);
		List<DataValue> filteredEquivClasses = eqcFilter.toList(suffix.getRestriction(suffixValue), prefix, suffix.getActions(), values);

		Map<DataValue, SDTGuard> ret = new LinkedHashMap<>();
		for (Entry<DataValue<N>, SDTGuard> e : valueGuards.entrySet()) {
			DataValue ec = e.getKey();
			if (filteredEquivClasses.contains(ec)) {
				ret.put(ec, e.getValue());
			}
		}
		return ret;
    }

	/**
	 * Merge SDT guards corresponding to data values representing equivalence classes from left (corresponding
	 * to lower data values) to right. Guards will be be merged if their respective sub-SDTs are equivalent,
	 * taking into account cases where two SDTs can be made equivalent by imposing equality. Any guards
	 * corresponding to equivalence classes that have been removed through suffix optimization will not be
	 * merged, but will instead be removed.
	 * If the merging results in only a single guard, it will be replaced by a True guard.
	 *
	 * @param sdts - a mapping from SDT guards to their corresponding sub-SDTs
	 * @param equivClasses - a mapping from data values to corresponding SDT guards
	 * @param filteredOut - data values removed through suffix optimization
	 * @return a mapping from merged SDT guards to their respective sub-trees
	 */
	protected Map<SDTGuard, SDT> mergeGuards(Map<SDTGuard, SDT> sdts,
											 Map<DataValue<N>, SDTGuard> equivClasses,
											 Collection<DataValue<N>> filteredOut) {
		Map<SDTGuard, SDT> merged = new LinkedHashMap<>();

		List<DataValue<N>> ecValuesSorted = new ArrayList<>(equivClasses.keySet());
		ecValuesSorted.sort(getComparator());

		// merge guards from left (lesser values) to right (greater values)
		// stop merging when reaching
		// i) a value that is filtered out, or
		// ii) an inequivalent sub-tree
		SDT currSdt = null;
		SDTGuard currGuard = null;
		SDTGuard currMerged = null;
		SDTGuard prevGuard = null;
		SDT prevSdt = null;
		for (DataValue nextDv : ecValuesSorted) {
			if (filteredOut.contains(nextDv)) {
				// stop merging if next guard was filtered out
				if (currGuard != null) {
					assert currMerged != null;
					assert currSdt != null;
					merged.put(currMerged, currSdt);
				}
				currSdt = null;
				currGuard = null;
				currMerged = null;
				prevGuard = null;
				prevSdt = null;
				continue;
			} else {
				boolean keepMerging = true;
				SDTGuard nextGuard = equivClasses.get(nextDv);
				SDT nextSdt = sdts.get(nextGuard);
				if (currSdt == null) {
					// this is the first guard of the run
					currGuard = nextGuard;
					currMerged = nextGuard;
					currSdt = nextSdt;
					continue;
				} else {
					if (equivalentWithRenaming(currSdt, currGuard, nextSdt, nextGuard)) {
						// if left guard is equality, check for equality with previous guard
						if (currGuard instanceof EqualityGuard && prevGuard != null &&
								!equivalentWithRenaming(prevSdt, prevGuard, nextSdt, nextGuard)) {
							keepMerging = false;
						}
					} else {
						keepMerging = false;
					}
				}

				if (keepMerging) {
					currMerged = mergeIntervals(currMerged, nextGuard);
					prevGuard = currGuard;
					prevSdt = currSdt;
				} else {
					assert currMerged != null;
					assert currSdt != null;
					merged.put(currMerged, currSdt);
					currMerged = nextGuard;
					currSdt = nextSdt;
					prevGuard = null;
					prevSdt = null;
				}
				currGuard = nextGuard;
			}
		}
		if (currMerged != null) {
			merged.put(currMerged, currSdt);
		}

		merged = checkForDisequality(merged);

		// if only one guard, replace with true guard
		if (merged.size() == 1) {
			Entry<SDTGuard, SDT> entry = merged.entrySet().iterator().next();
			SDTGuard g = entry.getKey();
			if (g instanceof DisequalityGuard || (g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard())) {
				merged = new LinkedHashMap<>();
				merged.put(new TrueGuard(g.getParameter()), entry.getValue());
			}
		}

		assert !merged.isEmpty();

		return merged;
	}

	/**
	 * Check whether two SDTs are equivalent. If one of the SDT guards is an equality guard, check whether
	 * the SDT corresponding to the other guard is equivalent to the SDT corresponding to the equality guard
	 * under the restriction of that equality.
	 *
	 * @param sdt1
	 * @param guard1
	 * @param sdt2
	 * @param guard2
	 * @return true if sdt1 is equivalent to sdt2, or can be under equality guard2, or vice versa
	 */
	private boolean equivalentWithRenaming(SDT sdt1, SDTGuard guard1, SDT sdt2, SDTGuard guard2) {
		if (guard1 != null && guard1 instanceof EqualityGuard) {
			Expression<Boolean> renaming = guard1.toExpr();
			return sdt1.isEquivalentUnderCondition(sdt2, renaming);
		} else if (guard2 != null && guard2 instanceof EqualityGuard) {
			Expression<Boolean> renaming = guard2.toExpr();
			return sdt2.isEquivalentUnderCondition(sdt1, renaming);
		}
		return sdt1.isEquivalent(sdt2, new Bijection<>());
	}

	/**
	 * Merge two SDT guards from left to right. Exactly one of the guards must include an equality such that
	 * there is either an equality on the right register of left guard or an equality on the left register
	 * of the right guard. In addition, the right register of the left guard must match the left register
	 * of the right guard. If these conditions are met, the guards will be merged. For example, a guard
	 * (r1 < s1 <= r2) can be merged with (r2 < s1 < r3), producing the merged guard (r1 < s1 < r3).
	 * Similarly, (s1 < r1) can be merged with (r1 == s1), producing the merged guard (s1 <= r1).
	 *
	 * @param leftGuard
	 * @param rightGuard
	 * @return leftGuard merged with rightGuard
	 */
	private SDTGuard mergeIntervals(SDTGuard leftGuard, SDTGuard rightGuard) {
		SuffixValue suffixValue = leftGuard.getParameter();
		if (leftGuard instanceof EqualityGuard) {
			EqualityGuard egLeft = (EqualityGuard) leftGuard;
			GuardElement rl = egLeft.getRegister();
			if (rightGuard instanceof IntervalGuard) {
				IntervalGuard igRight = (IntervalGuard) rightGuard;
				if (!igRight.isSmallerGuard() && igRight.getSmallerElement().equals(rl)) {
					if (igRight.isBiggerGuard()) {
						return IntervalGuard.greaterOrEqualGuard(suffixValue, rl);
					} else {
						return new IntervalGuard(suffixValue, rl, igRight.getGreaterElement(), true, false);
					}
				}
			}
		} else if (leftGuard instanceof IntervalGuard && !((IntervalGuard) leftGuard).isBiggerGuard()) {
			IntervalGuard igLeft = (IntervalGuard) leftGuard;
			GuardElement rr = igLeft.getGreaterElement();
			if (igLeft.isSmallerGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return IntervalGuard.lessOrEqualGuard(suffixValue, rr);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getSmallerElement().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isIntervalGuard()) {
						return IntervalGuard.lessGuard(suffixValue, igRight.getGreaterElement());
					} else {
						return new TrueGuard(suffixValue);
					}
				}
			} else if (igLeft.isIntervalGuard()) {
				if (rightGuard instanceof EqualityGuard && ((EqualityGuard) rightGuard).getRegister().equals(rr)) {
					return new IntervalGuard(suffixValue, igLeft.getSmallerElement(), rr, igLeft.isLeftClosed(), true);
				} else if (rightGuard instanceof IntervalGuard &&
						!((IntervalGuard) rightGuard).isSmallerGuard() &&
						((IntervalGuard) rightGuard).getSmallerElement().equals(rr)) {
					IntervalGuard igRight = (IntervalGuard) rightGuard;
					if (igRight.isBiggerGuard()) {
						return new IntervalGuard(suffixValue, igLeft.getSmallerElement(), null, igLeft.isLeftClosed(), false);
					} else {
						return new IntervalGuard(suffixValue, igLeft.getSmallerElement(), igRight.getGreaterElement(), igLeft.isLeftClosed(), igRight.isRightClosed());
					}
				}
			}
		}
		throw new IllegalArgumentException("Guards are not compatible for merging");
	}

	/**
	 * Check whether two interval guards can be transformed into a disequality guard. This is possible if
	 * the guards are of the form (r < s), (s == r), (s < r) such that the sub-SDTs of the guards (r < s)
	 * and (s < r) are equivalent. If so, the guards (r < s) and (s < r) are replaced by a guard (s != r).
	 *
	 * @param guards - a mapping from SDT guards to their corresponding sub-SDTs
	 * @return a new mapping from SDT guards to corresponding sub-SDTs, with guards transformed as described above
	 */
	private Map<SDTGuard, SDT> checkForDisequality(Map<SDTGuard, SDT> guards) {
		int size = guards.size();
		if (size < 1 || size > 3)
			return guards;

		Optional<SDTGuard> less = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isSmallerGuard()).findAny();
		Optional<SDTGuard> greater = guards.keySet().stream().filter(g -> g instanceof IntervalGuard && ((IntervalGuard) g).isBiggerGuard()).findAny();
		if (less.isPresent() && greater.isPresent()) {
			IntervalGuard lg = (IntervalGuard) less.get();
			IntervalGuard gg = (IntervalGuard) greater.get();
			SDT ls = guards.get(lg);
			SDT gs = guards.get(gg);
			GuardElement rr = lg.getGreaterElement();
			GuardElement rl = gg.getSmallerElement();
			if (rr.equals(rl) && ls.isEquivalent(gs, new Bijection<>())) {
				Map<SDTGuard, SDT> diseq = new LinkedHashMap<>();
				diseq.put(new DisequalityGuard(lg.getParameter(), rr), guards.get(lg));
				Optional<SDTGuard> equal = guards.keySet().stream().filter(g -> g instanceof EqualityGuard).findAny();
				if (equal.isPresent()) {
					EqualityGuard eg = (EqualityGuard) equal.get();
					assert eg.getRegister().equals(rr);
					diseq.put(eg, guards.get(eg));
				}
				return diseq;
			}
		}
		return guards;
	}

    @Override
    public <I extends ParameterizedSymbol> SDT treeQuery(Word<SymbolInstance<I>> prefix,
														 SymbolicSuffix<I> suffix,
														 List<DataValue<?>> values,
														 Constants consts,
														 SuffixValuation suffixValues,
														 TreeOracle<I> oracle) {

    	int pId = values.size() + 1;
    	SuffixValue<?> currentParam = suffix.getSuffixValue(pId);
    	Map<DataValue<N>, GuardElement> pot = getPotential(prefix, suffixValues, consts);

        Map<DataValue<N>, SDTGuard> equivClasses = generateEquivClasses(currentParam, pot, consts);
        Map<DataValue, SDTGuard> filteredEquivClasses = filterEquivClasses(equivClasses, prefix, suffix, currentParam,
																		   values);

        Map<SDTGuard, SDT> children = new LinkedHashMap<>();
        for (Entry<DataValue, SDTGuard> ec : filteredEquivClasses.entrySet()) {
        	SuffixValuation nextSuffixVals = new SuffixValuation();
			List<DataValue<?>> nextVals = new ArrayList<>();
        	nextVals.addAll(values);
        	nextVals.add(ec.getKey());
        	nextSuffixVals.putAll(suffixValues);
        	nextSuffixVals.put(currentParam, ec.getKey());
        	SDT sdt = oracle.treeQuery(prefix, suffix, nextVals, consts, nextSuffixVals);
        	children.put(ec.getValue(), sdt);
        }

        Collection<DataValue<N>> filteredOut = new ArrayList<>();
        filteredOut.addAll(equivClasses.keySet());
        filteredOut.removeAll(filteredEquivClasses.keySet());
        Map<SDTGuard, SDT> merged = mergeGuards(children, equivClasses, filteredOut);

        Map<SDTGuard, SDT> reversed = new LinkedHashMap<>();
        List<SDTGuard> keys = new ArrayList<>(merged.keySet());
        Collections.reverse(keys);
        for (SDTGuard g : keys) {
        	reversed.put(g, merged.get(g));
        }

        return new SDT(reversed);
    }

    private <I extends ParameterizedSymbol> Map<DataValue<N>, GuardElement> getPotential(Word<SymbolInstance<I>> prefix,
    		SuffixValuation suffixValues,
    		Constants consts) {
    	Map<DataValue<N>, GuardElement> pot = new LinkedHashMap<>();
    	//RegisterGenerator rgen = new RegisterGenerator();

    	List<DataValue> seen = new ArrayList<>();
    	for (SymbolInstance<I> psi : prefix) {
    		DataValue dvs[] = psi.getParameterValues();
    		DataType dts[] = psi.getBaseSymbol().getPtypes();
    		for (int i = 0; i < dvs.length; i++) {
    			//Register r = rgen.next(dts[i]);
    			DataValue dv = safeCast(dvs[i]);
    			if (dv != null && !seen.contains(dv)) {
    				pot.put(dv, dv);
    				seen.add(dv);
    			}
    		}
    	}

    	for (Entry<SuffixValue<?>, DataValue<?>> e : suffixValues.entrySet()) {
    		SuffixValue sv = e.getKey();
    		DataValue dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, sv);
    		}
    	}

    	for (Entry<SymbolicDataValue.Constant<?>, DataValue<?>> e : consts.entrySet()) {
    		SymbolicDataValue.Constant c = e.getKey();
    		DataValue dv = safeCast(e.getValue());
    		if (dv != null) {
    			pot.put(dv, c);
    		}
    	}

    	return pot;
    }

    private DataValue<?> getRegisterValue(GuardElement r, Constants constants,
            SuffixValuation pval) {
        if (r instanceof DataValue<?>) {
            return (DataValue<?>) r;
        } else if (r instanceof SuffixValue<?>) {
            return pval.get( (SuffixValue<?>) r);
        } else if (r instanceof SymbolicDataValue.Constant<?>) {
            return constants.get((SymbolicDataValue.Constant<?>) r);
        } else {
            throw new IllegalStateException("this can't possibly happen");
        }
    }

    @Override
    public <I extends ParameterizedSymbol> DataValue instantiate(
            Word<SymbolInstance<I>> prefix,
            I ps,
            SuffixValuation pval,
            Constants constants,
            SDTGuard guard,
            SuffixValue<N> param,
            Set<DataValue<N>> oldDvs) {

        DataType type = param.getDataType();
        DataValue returnThis = null;
        List<DataValue> prefixValues = Arrays.asList(DataWords.valsOf(prefix));

        if (guard instanceof EqualityGuard) {
            EqualityGuard eqGuard = (EqualityGuard) guard;

            GuardElement ereg = eqGuard.getRegister();
            if (ereg instanceof DataValue<?>) {
                returnThis = (DataValue<?>) ereg;
            } else if (ereg instanceof SuffixValue<?>) {
                returnThis = pval.get( (SuffixValue<?>) ereg);
            } else if (ereg instanceof SymbolicDataValue.Constant<?>) {
                returnThis = constants.get((SymbolicDataValue.Constant<?>) ereg);
            }
            assert returnThis != null;
        } else if (guard instanceof TrueGuard || guard instanceof DisequalityGuard) {

            Collection<DataValue> potSet = DataWords.joinValsToSet(
                    constants.values(type),
                    DataWords.valSet(prefix, type),
                    pval.values(type));

            returnThis = this.getFreshValue(new ArrayList(potSet));
        } else {
            Collection<DataValue<N>> alreadyUsedValues
                    = DataWords.joinValsToSet(
                            constants.values(type),
                            DataWords.valSet(prefix, type),
                            pval.values(type));
            Valuation val = new Valuation();
            if (guard instanceof IntervalGuard) {
                IntervalGuard iGuard = (IntervalGuard) guard;
                if (!iGuard.isBiggerGuard()) {
                    GuardElement r = iGuard.getGreaterElement();
                    if (r instanceof SuffixValue<?> || r instanceof SymbolicDataValue.Constant<?>) {
                        DataValue regVal = getRegisterValue(r, constants, pval);
                        val.setValue( (Variable) r, regVal.getValue());
                    }
                }
                if (!iGuard.isSmallerGuard()) {
                    GuardElement l =  iGuard.getSmallerElement();
                    if (l instanceof SuffixValue<?> || l instanceof SymbolicDataValue.Constant<?>) {
                        DataValue regVal = getRegisterValue(l, constants, pval);
                        val.setValue( (Variable) l, regVal.getValue());
                    }
                }
            /*} else if (guard instanceof SDTGuard.SDTIfGuard) {
                SymbolicDataValue r = ((SDTIfGuard) guard).getRegister();
                DataValue regVal = getRegisterValue(r,
                        prefixValues, constants, pval);
                val.setValue(r, regVal.getValue());

            } else if (guard instanceof SDTGuard.SDTOrGuard) {
                SDTGuard iGuard = ((SDTGuard.SDTOrGuard) guard).disjuncts().get(0);

                returnThis = instantiate(iGuard, val, constants, alreadyUsedValues);
            } else if (guard instanceof SDTGuard.SDTAndGuard) {
                assert ((SDTGuard.SDTAndGuard) guard).conjuncts().stream().allMatch(g -> g instanceof SDTGuard.DisequalityGuard);
                SDTGuard aGuard = ((SDTGuard.SDTAndGuard) guard).conjuncts().get(0);

                returnThis = instantiate(aGuard, val, constants, alreadyUsedValues);
            */
            } else {
                throw new IllegalStateException("only =, != or interval allowed. Got " + guard);
            }

            if (!(oldDvs.isEmpty())) {
                for (DataValue oldDv : oldDvs) {
                    Valuation newVal = new Valuation();
                    newVal.putAll(val);
                    newVal.setValue( new SuffixValue(param.getDataType(), param.getId()) , oldDv.getValue());
                    DataValue inst = instantiate(guard, newVal, constants, alreadyUsedValues);
                    if (inst != null) {
                        return inst;
                    }
                }
            }
            returnThis = instantiate(guard, val, constants, alreadyUsedValues);

        }
        return returnThis;
    }

    public void useSuffixOptimization(boolean useSuffixOpt) {
    	this.useSuffixOpt = useSuffixOpt;
    }

    @Override
    public <I extends ParameterizedSymbol>  SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<? extends SymbolInstance<I>> prefix, Word<? extends SymbolInstance<I>> suffix, Constants consts) {
    	int firstActionArity = suffix.size() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;
    	if (suffixValue.getId() <= firstActionArity) {
    	    return new UnrestrictedSuffixValue(suffixValue);
    	}

    	DataValue prefixVals[] = DataWords.valsOf(prefix);
    	DataValue suffixVals[] = DataWords.valsOf(suffix);
    	DataValue constVals[] = new DataValue[consts.size()];
    	constVals = consts.values().toArray(constVals);
    	DataValue priorVals[] = new DataValue[prefixVals.length + constVals.length + suffixValue.getId() - 1];
    	DataType svType = suffixValue.getDataType();
    	DataValue svDataValue = safeCast(suffixVals[suffixValue.getId()-1]);
    	assert svDataValue != null;

    	System.arraycopy(prefixVals, 0, priorVals, 0, prefixVals.length);
    	System.arraycopy(constVals, 0, priorVals, prefixVals.length, constVals.length);
    	System.arraycopy(suffixVals, 0, priorVals, prefixVals.length + constVals.length, suffixValue.getId() - 1);

    	// is suffix value greater than all prior or smaller than all prior?
    	boolean greater = false;
    	boolean lesser = false;
    	boolean foundFirst = false;
    	for (int i = 0; i < priorVals.length; i++) {
    		if (priorVals[i].getDataType().equals(svType)) {
    			DataValue dv = safeCast(priorVals[i]);
    			assert dv != null;
    			int comparison = getComparator().compare(svDataValue, dv);

    			if (foundFirst) {
    				if ((greater && comparison < 0) ||
    						(lesser && comparison > 0) ||
    						comparison == 0) {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    			} else {
    				if (comparison > 0) {
    					greater = true;
    				} else if (comparison < 0) {
    					lesser = true;
    				} else {
    					return new UnrestrictedSuffixValue(suffixValue);
    				}
    				foundFirst = true;
    			}
    		}
    	}

    	if (!foundFirst) {
    		return new GreaterSuffixValue(suffixValue);
    	}

    	assert (greater && !lesser) || (!greater && lesser);
    	return greater ? new GreaterSuffixValue(suffixValue) : new LesserSuffixValue(suffixValue);
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
    	SuffixValue sv = guard.getParameter();

    	if (guard instanceof IntervalGuard) {
    		IntervalGuard ig = (IntervalGuard) guard;
    		if (ig.isBiggerGuard()) {
    			return new GreaterSuffixValue(sv);
    		} else if (ig.isSmallerGuard()) {
    			return new LesserSuffixValue(sv);
    		}
    	}
    	SuffixValueRestriction restr = GenericSuffixRestrictionBuilderImpl.genericRestriction(guard, prior);
    	if (restr instanceof FreshSuffixValue) {
    		restr = new GreaterSuffixValue(sv);
    	}
    	return restr;
    }
//
//    @Override
//    public boolean guardRevealsRegister(SDTGuard guard, SymbolicDataValue register) {
//    	if (guard instanceof SDTGuard.EqualityGuard && ((SDTGuard.EqualityGuard) guard).register().equals(register)) {
//    		return true;
//    	} else if (guard instanceof SDTGuard.DisequalityGuard && ((SDTGuard.DisequalityGuard)guard).register().equals(register)) {
//    		return true;
//    	} else if (guard instanceof SDTGuard.IntervalGuard) {
//    		SDTGuard.IntervalGuard ig = (SDTGuard.IntervalGuard) guard;
//    		if (ig.smallerElement().equals(register) || ig.greaterElement().equals(register)) {
//    			return true;
//    		}
//    	} else if (guard instanceof SDTGuard.SDTOrGuard) {
//    		boolean revealsGuard = false;
//    		for (SDTGuard g : ((SDTGuard.SDTOrGuard)guard).disjuncts()) {
//    			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
//    		}
//    		return revealsGuard;
//    	} else if (guard instanceof SDTGuard.SDTAndGuard) {
//		boolean revealsGuard = false;
//		for (SDTGuard g : ((SDTGuard.SDTAndGuard)guard).conjuncts()) {
//			revealsGuard = revealsGuard || this.guardRevealsRegister(g, register);
//		}
//		return revealsGuard;
//	}
//	return false;
//    }


	public List<DataValue<N>> getPotential(Collection<DataValue<N>> dvs) {
		//assume we can just sort the list and get the values
		List<DataValue<N>> sortedList = new ArrayList<>();
		for (DataValue<N> d : dvs) {
			//                    if (d.getId() instanceof Integer) {
			//                        sortedList.add(new DataValue(d.getType(), ((Integer) d.getId()).NValue()));
			//                    } else if (d.getId() instanceof N) {
			sortedList.add(d);
			//                    } else {
			//                        throw new IllegalStateException("not supposed to happen");
			//                    }
		}

		//sortedList.addAll(dvs);
		Collections.sort(sortedList, getComparator());

		//System.out.println("I'm sorted!  " + sortedList.toString());
		return sortedList;
	}

	private List<Expression<Boolean>> instantiateGuard(SDTGuard g, Valuation val) {
		List<Expression<Boolean>> eList = new ArrayList<Expression<Boolean>>();
		if (g instanceof EqualityGuard) {
			EqualityGuard equalityGuard = (EqualityGuard) g;
			// pick up the register
			SymbolicDataValue si = (SymbolicDataValue) equalityGuard.getRegister();
			// get the register value from the valuation
			DataValue sdi = new DataValue(type, val.getValue(si));
			// add the register value as a constant
			Constant wm = new Constant(
					type.getType(), (sdi.getValue()));
			// add the constant equivalence expression to the list
			eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, si));

		} else if (g instanceof DisequalityGuard) {
			DisequalityGuard disequalityGuard = (DisequalityGuard) g;
			// pick up the register
			SymbolicDataValue si = (SymbolicDataValue) disequalityGuard.getRegisters();
			// get the register value from the valuation
			DataValue sdi = new DataValue(type, val.getValue(si));
			// add the register value as a constant
			Constant wm = new Constant(type.getType(), (sdi.getValue()));
			// add the constant equivalence expression to the list
			eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, si));
			throw new RuntimeException("this seems to be wrong ...");

		} else if (g instanceof IntervalGuard) {
			IntervalGuard iGuard = (IntervalGuard) g;
			if (!iGuard.isBiggerGuard()) {
				GuardElement r =  iGuard.getGreaterElement();
				assert r != null;
				DataValue ri = (r instanceof DataValue) ? (DataValue) r :
						new DataValue(type, val.getValue( (Variable) r));
				Constant wm = new Constant(type.getType(), (ri.getValue()));
				// add the constant equivalence expression to the list
				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, r.asExpression()));
			}
			if (!iGuard.isSmallerGuard()) {
				GuardElement l = iGuard.getSmallerElement();
				assert l != null;
				DataValue li = (l instanceof DataValue) ? (DataValue) l :
						new DataValue(type, val.getValue( (Variable) l));
				Constant wm = new Constant(type.getType(), (li.getValue()));
				// add the constant equivalence expression to the list
				eList.add(new NumericBooleanExpression(wm, NumericComparator.EQ, l.asExpression()));
			}
		}
		return eList;
	}

	public DataValue instantiate(SDTGuard g, Valuation val, Constants c, Collection<DataValue<N>> alreadyUsedValues) {
		//System.out.println("INSTANTIATING: " + g.toString());
		SuffixValue sp = g.getParameter();
		Valuation newVal = new Valuation();
		newVal.putAll(val);
		Expression<Boolean> x = g.toExpr();
		Result res;
		if (g instanceof EqualityGuard) {
			//System.out.println("SOLVING: " + x);
			res = solver.solve(x, newVal);
		} else {
			List<Expression<Boolean>> eList = new ArrayList<>();
			// add the guard
			eList.add(g.toExpr());
			eList.addAll(instantiateGuard(g, val));
//			if (g instanceof OrGuard og) {
//				// for all registers, pick them up
//				for (SDTGuard subg : og.disjuncts()) {
//					if (!(subg instanceof EqualityGuard)) {
//						eList.addAll(instantiateGuard(subg, val));
//					}
//				}
//			}

			// add disequalities
			for (DataValue au : alreadyUsedValues) {
				Constant w = new Constant(type.getType(), (au.getValue()));
				Expression<Boolean> auExpr = new NumericBooleanExpression(w, NumericComparator.NE, sp);
				eList.add(auExpr);
			}

			if (newVal.containsValueFor(sp)) {
				DataValue spN = new DataValue(type, newVal.getValue(sp));
				Constant spw = new Constant(type.getType(), (spN.getValue()));
				Expression<Boolean> spExpr = new NumericBooleanExpression(spw, NumericComparator.EQ, sp);
				eList.add(spExpr);
			}

			Expression<Boolean> _x = ExpressionUtil.and(eList);
			//System.out.println("SOLVING: " + _x + " with " + newVal);
			res = solver.solve(_x, newVal);
			//                    System.out.println("SOLVING:: " + res + "  " + eList + "  " + newVal);
		}
		//                System.out.println("VAL: " + newVal);
		//                System.out.println("g toExpr is: " + g.toExpr(c).toString() + " and vals " + newVal.toString() + " and param-variable " + sp.toVariable().toString());
		//                System.out.println("x is " + x.toString());
		if (res == Result.SAT) {
			//                    System.out.println("SAT!!");
			//                    System.out.println(newVal.getValue(sp.toVariable()) + "   " + newVal.getValue(sp.toVariable()).getClass());
			DataValue d = new DataValue(type, newVal.getValue(sp));
			//System.out.println("return d: " + d.toString());
			return d;//new DataValue<N>(NType, d);
		} else {
			//                    System.out.println("UNSAT: " + _x + " with " + newVal);
			return null;
		}
	}

	protected Comparator<DataValue<N>> getComparator() {
		return Comparator.comparing(Constant::getValue);
	}

	protected DataValue<N> safeCast(DataValue<?> dv) {
		if (dv.getValue() instanceof Number) {
			return new DataValue<>(((DataType<N>) dv.getDataType()), (N) dv.getValue());
		}
		return null;
	}

	@Override
	public DataType<N> getDataType() {
		return type;
	}
}
