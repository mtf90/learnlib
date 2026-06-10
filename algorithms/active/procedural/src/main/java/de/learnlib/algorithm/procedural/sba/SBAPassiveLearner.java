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
package de.learnlib.algorithm.procedural.sba;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import de.learnlib.algorithm.PassiveLearningAlgorithm;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.automaton.procedural.SBA;
import net.automatalib.automaton.procedural.impl.StackSBA;
import net.automatalib.common.util.HashUtil;
import net.automatalib.word.Word;

/**
 * A variation of {@link SBALearner} for passive automata learning contexts.
 *
 * @param <I>
 *         input symbol type
 */
public class SBAPassiveLearner<I> implements PassiveLearningAlgorithm<SBA<?, I>, I, Boolean> {

    private final ProceduralInputAlphabet<I> alphabet;
    private final Function<Alphabet<I>, PassiveDFALearner<I>> learnerConstructor;

    private I initialProcedure;
    private final Map<I, Collection<Word<I>>> projectedTraces;
    private final Set<I> terminatedProcedures;

    public SBAPassiveLearner(ProceduralInputAlphabet<I> alphabet,
                             Function<Alphabet<I>, PassiveDFALearner<I>> learnerConstructor) {
        this.alphabet = alphabet;
        this.learnerConstructor = learnerConstructor;

        final int capacity = HashUtil.capacity(alphabet.getNumCalls());

        this.projectedTraces = new HashMap<>(capacity);
        this.terminatedProcedures = new HashSet<>(capacity);

        for (I i : alphabet.getCallAlphabet()) {
            this.projectedTraces.put(i, new HashSet<>());
        }
    }

    @Override
    public void addSamples(Collection<? extends DefaultQuery<I, Boolean>> samples) {
        for (DefaultQuery<I, Boolean> s : samples) {
            addSample(s);
        }
    }

    @Override
    @SuppressWarnings("PMD.AvoidReassigningLoopVariables") // we want to skip ahead here
    public void addSample(DefaultQuery<I, Boolean> sample) {

        // input validation
        if (!sample.getOutput()) {
            throw new IllegalArgumentException("Only positive examples are allowed");
        }

        final Word<I> input = sample.getInput();

        if (!alphabet.isReturnMatched(input)) {
            throw new IllegalArgumentException("Input is not return-matched");
        }
        if (initialProcedure == null) {
            this.initialProcedure = input.firstSymbol();
        } else if (!Objects.equals(this.initialProcedure, input.firstSymbol())) {
            throw new IllegalArgumentException("Inconsistent initial procedures");
        }

        // activate procedures that are just accessed
        if (alphabet.isCallSymbol(input.lastSymbol())) {
            this.projectedTraces.get(input.lastSymbol()).add(Word.epsilon());
        }

        // sub-trace extraction
        // backward analysis over unmatched call symbols
        for (int i = input.size() - 1; i > 0;) {
            final int callIdx = alphabet.findCallIndex(input, i);
            final I callSym = input.getSymbol(callIdx);
            final Word<I> subWord = input.subWord(callIdx + 1, i + 1);
            final Word<I> projected = alphabet.project(subWord, 0);

            this.projectedTraces.get(callSym).add(projected);

            if (alphabet.isReturnSymbol(input.getSymbol(i))) {
                terminatedProcedures.add(callSym);
            }

            // normal forward analysis over well-matched subwords
            addSampleInternal(subWord);
            i = callIdx;
        }
    }

    private void addSampleInternal(Word<I> input) {
        for (int i = 0; i < input.size() - 1; i++) {
            final I sym = input.getSymbol(i);
            if (alphabet.isCallSymbol(sym)) {
                terminatedProcedures.add(sym);

                final int retIdx = alphabet.findReturnIndex(input, i + 1);
                final Word<I> subWord = input.subWord(i + 1, retIdx);
                final Word<I> projected = alphabet.project(subWord, 0);

                projectedTraces.get(sym).add(projected);
            }
        }
    }

    @Override
    public SBA<?, I> computeModel() {
        final Map<I, DFA<?, I>> procedures = new HashMap<>(HashUtil.capacity(projectedTraces.size()));

        for (Entry<I, Collection<Word<I>>> e : projectedTraces.entrySet()) {
            final I procedure = e.getKey();
            final PassiveDFALearner<I> learner = learnerConstructor.apply(alphabet);

            for (Word<I> in : e.getValue()) {

                // ensure call, return, and error closure
                if (!in.isEmpty() && (Objects.equals(in.lastSymbol(), alphabet.getReturnSymbol()) ||
                                      alphabet.isCallSymbol(in.lastSymbol()) &&
                                      !terminatedProcedures.contains(in.lastSymbol()))) {
                    // do two-step futures to prevent simple back loops when merging
                    for (I i : alphabet) {
                        for (I i2 : alphabet) {
                            learner.addSample(in.append(i).append(i2), Boolean.FALSE);
                        }
                    }
                }

                learner.addSample(in, Boolean.TRUE);
            }

            final DFA<?, I> dfa = learner.computeModel();

            procedures.put(procedure, dfa);
        }

        return new StackSBA<>(alphabet, initialProcedure, procedures);
    }
}
