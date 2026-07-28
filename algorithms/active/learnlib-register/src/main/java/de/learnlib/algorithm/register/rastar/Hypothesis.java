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
package de.learnlib.algorithm.register.rastar;

import de.learnlib.AccessSequenceTransformer;
import de.learnlib.TransitionSequenceTransformer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.State;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.automaton.ra.impl.CompactRATransition;
import net.automatalib.common.util.Pair;
import net.automatalib.data.Constants;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * @author falk
 */
public class Hypothesis<I extends ParameterizedSymbol> extends CompactRA<I>
        implements AccessSequenceTransformer<SymbolInstance<I>>, TransitionSequenceTransformer<SymbolInstance<I>> {

    private final Map<Integer, Word<SymbolInstance<I>>> accessSequences = new LinkedHashMap<>();

    private final Map<CompactRATransition, Word<SymbolInstance<I>>> transitionSequences =
            new LinkedHashMap<>();

    public Hypothesis(Alphabet<I> alphabet, Constants consts) {
        super(alphabet, new RegisterValuation(), consts);
    }

    public void setAccessSequence(Integer loc, Word<SymbolInstance<I>> as) {
        accessSequences.put(loc, as);
    }

    public void setTransitionSequence(CompactRATransition t, Word<SymbolInstance<I>> as) {
        transitionSequences.put(t, as);
    }

    public Map<Integer, Word<SymbolInstance<I>>> getAccessSequences() {
        return accessSequences;
    }

    public Map<CompactRATransition, Word<SymbolInstance<I>>> getTransitionSequences() {
        return transitionSequences;
    }

    @Override
    public Word<SymbolInstance<I>> transformAccessSequence(Word<SymbolInstance<I>> word) {
        Integer loc = getSemantics().getState(word).getLocation();
        return accessSequences.get(loc);
    }

    public Set<Word<SymbolInstance<I>>> possibleAccessSequences(Word<SymbolInstance<I>> word) {
        Set<Word<SymbolInstance<I>>> ret = new LinkedHashSet<Word<SymbolInstance<I>>>();
        ret.add(transformAccessSequence(word));
        return ret;
    }

    @Override
    public boolean isAccessSequence(Word<SymbolInstance<I>> word) {
        return accessSequences.containsValue(word);
    }

    @Override
    public Word<SymbolInstance<I>> transformTransitionSequence(Word<SymbolInstance<I>> word) {
        State<Integer> state = getSemantics().getState(word.prefix(-1));

        if (state == null) {
            return null;
        }

        RegisterValuation valuation = state.getValuation();
        SymbolInstance<I> sym = word.lastSymbol();

        for (CompactRATransition t : getTransitions(state.getLocation(), sym.getBaseSymbol())) {
            if (t.isEnabled(valuation, new ParameterValuation(sym), getConstants())) {
                return transitionSequences.get(t);
            }
        }

        return null;
    }

    public Word<SymbolInstance<I>> transformTransitionSequence(Word<SymbolInstance<I>> word,
                                                               Word<SymbolInstance<I>> loc) {
        return this.transformTransitionSequence(word);
    }
//
//    public Word<SymbolInstance<I>> branchWithSameGuard(Word<SymbolInstance<I>> word, Branching<I> branching) {
//        ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();
//
//        List<Pair<CompactRATransition, RegisterValuation>> tvseq = getTransitionsAndValuations(word);
//        RegisterValuation vars = tvseq.get(tvseq.size() - 1).getSecond();
//        ParameterValuation pval = new ParameterValuation(word.lastSymbol());
//
//        for (Map.Entry<Word<SymbolInstance<I>>, Expression<Boolean>> e : branching.getBranches().entrySet()) {
//            if (e.getKey().lastSymbol().getBaseSymbol().equals(ps)) {
//                Word<SymbolInstance<I>> prefix = e.getKey().prefix(e.getKey().size() - 1);
//                RegisterValuation varsRef =
//                        getTransitionsAndValuations(prefix).get(getTransitionsAndValuations(prefix).size() - 1)
//                                                           .getSecond();
//                // System.out.println(varsRef);
//                RegisterAssignment ra = new RegisterAssignment();
//                varsRef.forEach((key, value) -> ra.put(value, key));
//                Expression<Boolean> guard = SMTUtil.valsToRegisters(e.getValue(), ra);
//                if (guard.evaluateSMT(SMTUtil.compose(vars, pval, constants))) {
//                    return e.getKey();
//                }
//            }
//        }
//        return null;
//    }
//
//    public VarMapping<Register<?>, ? extends SymbolicDataValue> getLastTransitionAssignment(Word<SymbolInstance<I>> word) {
//        List<CompactRATransition> tseq = getTransitions(word);
//        return tseq.get(tseq.size() - 1).getAssignment().getAssignment();
//    }
}
