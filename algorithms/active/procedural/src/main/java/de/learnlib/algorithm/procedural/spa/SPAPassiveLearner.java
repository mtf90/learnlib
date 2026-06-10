/* Copyright (C) 2013-2026 TU Dortmund University
 * This file is part of LearnLib <https://learnlib.de>.
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
package de.learnlib.algorithm.procedural.spa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import de.learnlib.algorithm.PassiveLearningAlgorithm;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.procedural.SPA;
import net.automatalib.automaton.procedural.impl.StackSPA;
import net.automatalib.common.util.HashUtil;
import net.automatalib.word.Word;

/**
 * A variation of {@link SPALearner} for passive automata learning contexts.
 *
 * @param <I>
 *         input symbol type
 */
public class SPAPassiveLearner<I> implements PassiveLearningAlgorithm<SPA<?, I>, I, Boolean> {

    private final ProceduralInputAlphabet<I> alphabet;
    private final Function<Alphabet<I>, PassiveDFALearner<I>> learnerConstructor;

    private I initialProcedure;
    private final Map<I, Collection<Word<I>>> projectedTraces;

    public SPAPassiveLearner(ProceduralInputAlphabet<I> alphabet,
                             Function<Alphabet<I>, PassiveDFALearner<I>> learnerConstructor) {
        this.alphabet = alphabet;
        this.learnerConstructor = learnerConstructor;
        this.projectedTraces = new HashMap<>();
    }

    @Override
    public void addSamples(Collection<? extends DefaultQuery<I, Boolean>> samples) {
        for (DefaultQuery<I, Boolean> s : samples) {
            addSample(s);
        }
    }

    @Override
    public void addSample(DefaultQuery<I, Boolean> sample) {

        // input validation
        if (!sample.getOutput()) {
            throw new IllegalArgumentException("Only positive examples are allowed");
        }

        final Word<I> input = sample.getInput();

        if (!alphabet.isWellMatched(input)) {
            throw new IllegalArgumentException("Input is not well-matched");
        }
        if (initialProcedure == null) {
            this.initialProcedure = input.firstSymbol();
        } else if (!Objects.equals(this.initialProcedure, input.firstSymbol())) {
            throw new IllegalArgumentException("Inconsistent initial procedures");
        }

        // sub-trace extraction
        for (int i = 0; i < input.size(); i++) {
            final I sym = input.getSymbol(i);
            if (alphabet.isCallSymbol(sym)) {
                final int retIdx = alphabet.findReturnIndex(input, i + 1);
                final Word<I> subWord = input.subWord(i + 1, retIdx);
                final Word<I> projected = alphabet.project(subWord, 0);
                projectedTraces.computeIfAbsent(sym, k -> new ArrayList<>()).add(projected);
            }
        }
    }

    @Override
    public SPA<?, I> computeModel() {
        final Map<I, DFA<?, I>> procedures = new HashMap<>(HashUtil.capacity(projectedTraces.size()));

        for (Entry<I, Collection<Word<I>>> e : projectedTraces.entrySet()) {
            final I procedure = e.getKey();
            final PassiveDFALearner<I> learner = learnerConstructor.apply(alphabet.getProceduralAlphabet());

            learner.addSamples(Boolean.TRUE, e.getValue());

            final DFA<?, I> dfa = learner.computeModel();

            procedures.put(procedure, dfa);
        }

        return new StackSPA<>(alphabet, initialProcedure, procedures);
    }
}
