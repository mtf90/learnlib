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
package de.learnlib.algorithm.register.rastar;

import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.logging.Category;
import de.learnlib.oracle.TreeOracle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An observation table.
 *
 * @author falk
 */
public class ObservationTable<I extends ParameterizedSymbol> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservationTable.class);

    private final Alphabet<I> inputs;
    private final TreeOracle<I> oracle;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final List<SymbolicSuffix<I>> suffixes;
    private final Map<Word<SymbolInstance<I>>, Component<I>> components;
    private final Deque<SymbolicSuffix<I>> newSuffixes;
    private final Deque<Word<SymbolInstance<I>>> newPrefixes;
    private final Deque<Component<I>> newComponents;


    public ObservationTable(Alphabet<I> inputs, TreeOracle<I> oracle) {
        this.inputs = inputs;
        this.oracle = oracle;
        this.restrictionBuilder = oracle.getRestrictionBuilder();

        suffixes = new ArrayList<>();
        components = new LinkedHashMap<>();
        newSuffixes = new ArrayDeque<>();
        newPrefixes = new ArrayDeque<>();
        newComponents = new ArrayDeque<>();
    }

    void addComponent(Component<I> c) {
        LOGGER.info(Category.EVENT, "Queueing component for obs: {}", c);
        newComponents.add(c);
    }

    void addSuffix(SymbolicSuffix<I> suffix) {
        LOGGER.info(Category.EVENT, "Queueing suffix for obs: {}", suffix);
        newSuffixes.add(suffix);
    }

    void addPrefix(Word<SymbolInstance<I>> prefix) {
        LOGGER.info(Category.EVENT, "Queueing prefix for obs: {}", prefix);
        newPrefixes.add(prefix);
    }

    boolean complete() {
        if (!newComponents.isEmpty()) {
            processNewComponent();
            return false;
        }

        if (!newPrefixes.isEmpty()) {
            processNewPrefix();
            return false;
        }

        if (!newSuffixes.isEmpty()) {
            processNewSuffix();
            checkBranchingCompleteness();
            return false;
        }

        //AutomatonBuilder ab = new AutomatonBuilder(getComponents(), new Constants());
        //Hypothesis hyp = ab.toRegisterAutomaton();
        //FIXME: the default logging appender cannot log models and data structures
        //System.out.println(hyp.toString());
        return checkVariableConsistency();
    }

    private boolean checkBranchingCompleteness() {
        LOGGER.info(Category.PHASE, "Checking Branching Completeness");
        boolean ret = true;
        for (Component<I> c : components.values()) {
            boolean ub = c.updateBranching(oracle);
            ret = ret && ub;
        }
        return ret;
    }

    private boolean checkVariableConsistency() {
        LOGGER.info(Category.PHASE, "Checking Variable Consistency");
        for (Component<I> c : components.values()) {
            if (!c.checkVariableConsistency()) {
                return false;
            }
        }
        return true;
    }

    private void processNewSuffix() {
        SymbolicSuffix<I> suffix = newSuffixes.poll();
        LOGGER.info(Category.EVENT, "Adding suffix to obs: {}", suffix);
        //System.out.println("Adding suffix to obs: " + suffix);
        suffixes.add(suffix);
        for (Component<I> c : components.values()) {
            c.addSuffix(suffix, oracle);
        }
    }

    private void processNewPrefix() {
        Word<SymbolInstance<I>> prefix = newPrefixes.poll();
        LOGGER.info(Category.EVENT, "Adding prefix to obs: {}", prefix);
        Row<I> r = Row.computeRow(oracle, prefix, suffixes);
        for (Component<I> c : components.values()) {
            if (c.addRow(r)) {
                return;
            }
        }
        Component<I> c = new Component<>(r, this, restrictionBuilder);
        addComponent(c);
    }

    private void processNewComponent() {
        Component<I> c = newComponents.poll();
        //System.out.println("Adding component to obs: " + c);
        components.put(c.getAccessSequence(), c);
        c.start(oracle, inputs);
    }

    Map<Word<SymbolInstance<I>>, Component<I>> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OBS *******************************************************************\n");
        for (Component<I> c : getComponents().values()) {
            c.toString(sb);
        }
        sb.append("***********************************************************************\n");
        return sb.toString();
    }

}
