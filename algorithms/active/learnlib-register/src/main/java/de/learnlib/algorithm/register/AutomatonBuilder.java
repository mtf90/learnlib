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
package de.learnlib.algorithm.register;

import de.learnlib.algorithm.register.radt.DT;
import de.learnlib.algorithm.register.radt.DTHyp;
import de.learnlib.algorithm.register.rastar.Hypothesis;
import de.learnlib.data.Bijection;
import de.learnlib.data.Branching;
import de.learnlib.data.DataWords;
import de.learnlib.data.RegisterAssignment;
import de.learnlib.data.ReplacingValuesVisitor;
import de.learnlib.logging.Category;
import gov.nasa.jpf.constraints.api.Expression;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.Util;
import net.automatalib.automaton.ra.impl.CompactRATransition;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Constructs Register Automata from observation tables
 *
 * @author falk
 */
public class AutomatonBuilder<I extends ParameterizedSymbol> {

    private final Map<Word<SymbolInstance<I>>, LocationComponent<I>> components;

    private final Map<Word<SymbolInstance<I>>, Integer> locations = new LinkedHashMap<>();

    private final Hypothesis<I> automaton;

    protected final Constants consts;

    private static final Logger LOGGER = LoggerFactory.getLogger(AutomatonBuilder.class);

    public AutomatonBuilder(Alphabet<I> alphabet, Map<Word<SymbolInstance<I>>, LocationComponent<I>> components, Constants consts) {
        this.consts = consts;
        this.components = components;
        this.automaton = new Hypothesis<>(alphabet, consts);
    }

    public AutomatonBuilder(Alphabet<I> alphabet, Map<Word<SymbolInstance<I>>, LocationComponent<I>> components, Constants consts, DT<I> dt) {
        this.consts = consts;
        this.components = components;
        this.automaton = new DTHyp<>(alphabet, dt, consts);
    }

    public Hypothesis<I> toRegisterAutomaton() {
        LOGGER.debug(Category.EVENT, "computing hypothesis");
        computeLocations();
        computeTransitions();
        return this.automaton;
    }

    private void computeLocations() {
        LocationComponent<I> c = components.get(Word.epsilon());
        LOGGER.debug(Category.EVENT, "{0}", c);
        Integer loc = this.automaton.addInitialState(c.isAccepting());
        this.locations.put(Word.epsilon(), loc);
        this.automaton.setAccessSequence(loc, Word.epsilon());

        for (Entry<Word<SymbolInstance<I>>, LocationComponent<I>> e : this.components.entrySet()) {
            if (!e.getKey().equals(Word.epsilon())) {
                LOGGER.debug(Category.EVENT, "{0}", e.getValue());
                loc = this.automaton.addState(e.getValue().isAccepting());
                this.locations.put(e.getKey(), loc);
                this.automaton.setAccessSequence(loc, e.getKey());
            }
        }
    }

    private void computeTransitions() {
        for (LocationComponent<I> c : components.values()) {
            computeTransition(c, c.getPrimePrefix());
            for (PrefixContainer<I> r : c.getOtherPrefixes()) {
                computeTransition(c, r);
            }
        }
    }

    private static <I extends ParameterizedSymbol> boolean otherPrefixesContain(LocationComponent<I> c, Word<SymbolInstance<I>> src_id) {
        for (PrefixContainer<I> p : c.getOtherPrefixes()) {
            if (p.getPrefix().equals(src_id)) {
                return true;
            }
        }
        return false;
    }

    private void computeTransition(LocationComponent<I> dest_c, PrefixContainer<I> r) {
        if (r.getPrefix().isEmpty()) {
            return;
        }

        LOGGER.debug(Category.EVENT, "computing transition: {1} to {0}", new Object[] {dest_c, r});

        Word<SymbolInstance<I>> dest_id = dest_c.getAccessSequence();
        Word<SymbolInstance<I>> src_id = r.getPrefix().prefix(r.getPrefix().length() - 1);

        assert src_id != null;
        LocationComponent<I> src_c = null;
        for (LocationComponent<I> c : this.components.values()) {
            if (c.getPrimePrefix().getPrefix().equals(src_id) || otherPrefixesContain(c, src_id)) {
                src_id = c.getAccessSequence();
                src_c = c;
                break;
            }
        }
        //this.components.get(src_id);
        assert src_c != null;

        //        if (src_c == null && automaton instanceof DTHyp)
        //        	return;

        // locations
        Integer src_loc = this.locations.get(src_id);
        Integer dest_loc = this.locations.get(dest_id);

        // action
        I action = r.getPrefix().lastSymbol().getBaseSymbol();

        // guard
        Branching<I> b = src_c.getBranching(action);
        //System.out.println("b.getBranches is  " + b.getBranches().toString());
        //System.out.println("getting guard for  " + r.getPrefix().toString());
        Expression<Boolean> guard = b.getBranches().get(r.getPrefix());
        //System.out.println("assignment: " + src_c.getPrimePrefix().getAssignment());
        if (guard == null) {
            guard = findMatchingGuard(dest_id, b.getBranches(), consts);
        }

        ReplacingValuesVisitor rvv = new ReplacingValuesVisitor();
        guard = rvv.apply(guard, src_c.getPrimePrefix().getAssignment());

        // TODO: better solution
        // guard is null because r is transition from a short prefix
        if (automaton instanceof DTHyp && guard == null) {return;}

        assert true;
        assert guard != null;

        // assignment
        RegisterAssignment srcAssign = src_c.getPrimePrefix().getAssignment();
        RegisterAssignment destAssign = dest_c.getPrimePrefix().getAssignment();
        Bijection<DataValue<?>> remapping = dest_c.getRemapping(r);
        Assignment assign = computeAssignment(r.getPrefix(), srcAssign, destAssign, remapping);

        //System.out.println(assign);

        // create transition
        CompactRATransition t = createTransition(guard, dest_loc, assign);
        if (t != null) {
            LOGGER.debug(Category.EVENT, "computed transition {0}", t);
            this.automaton.addTransition(src_loc, action, t);
            this.automaton.setTransitionSequence(t, r.getPrefix());
        }
    }

    protected CompactRATransition createTransition(Expression<Boolean> guard, Integer dest_loc,
                                                   Assignment assign) {
        return new CompactRATransition(guard, assign, dest_loc);
    }

    public static <I extends ParameterizedSymbol> Expression<Boolean> findMatchingGuard(Word<SymbolInstance<I>> dw, Map<Word<SymbolInstance<I>>, Expression<Boolean>> branches,
                                                        Constants consts) {
        //System.out.println("findMatchingGuard: " + div);
        ParameterValuation pars = new ParameterValuation(dw);
        //RegisterValuation vars = div.registerValuation();
        for (Expression<Boolean> g : branches.values()) {
            if (g.evaluateSMT(Util.compose(pars, consts))) {
                return g;
            }
        }
        return null;
    }

    public static <I extends ParameterizedSymbol> Assignment computeAssignment(Word<SymbolInstance<I>> prefix,
                                        RegisterAssignment srcAssign,
                                        RegisterAssignment destAssign,
                                        Bijection<DataValue<?>> remapping) {

        VarMapping<Register<?>, SymbolicDataValue<?>> assignments = new VarMapping<>();
        ParameterizedSymbol action = prefix.lastSymbol().getBaseSymbol();
        DataValue[] pvals = DataWords.valsOf(prefix);
        for (Entry<DataValue<?>, DataValue<?>> e : remapping.entrySet()) {
            Register rNew = destAssign.get(e.getValue());
            assert rNew != null;
            if (srcAssign.containsKey(e.getKey())) {
                // has been stored in a register before => copy register to register
                Register rOld = srcAssign.get(e.getKey());
                assert rOld != null;
                assignments.put(rNew, rOld);
            } else {
                // has not been stored before => copy parameter to register
                int id = 0;
                for (int i = 0; i < action.getArity(); i++) {
                    if (pvals[pvals.length - action.getArity() + i].equals(e.getKey())) {
                        id = i;
                        break;
                    }
                }
                Parameter<?> pNew = new Parameter<>(e.getKey().getDataType(), id + 1);
                assert pNew.getId() > 0;
                assignments.put(rNew, pNew);
            }
        }
        return new Assignment(assignments);
    }

}
