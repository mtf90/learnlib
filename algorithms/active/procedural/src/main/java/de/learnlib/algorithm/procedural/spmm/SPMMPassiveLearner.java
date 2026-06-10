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
package de.learnlib.algorithm.procedural.spmm;

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
import net.automatalib.automaton.procedural.SPMM;
import net.automatalib.automaton.procedural.impl.StackSPMM;
import net.automatalib.automaton.transducer.MealyMachine;
import net.automatalib.common.util.HashUtil;
import net.automatalib.common.util.Pair;
import net.automatalib.word.Word;

/**
 * A variation of {@link SPMMLearner} for passive automata learning contexts.
 *
 * @param <I>
 *         input symbol type
 * @param <O>
 *         output symbol type
 */
public class SPMMPassiveLearner<I, O> implements PassiveLearningAlgorithm<SPMM<?, I, ?, O>, I, Word<O>> {

    private final ProceduralInputAlphabet<I> alphabet;
    private final O errorSymbol;
    private final Function<Alphabet<I>, PassiveMealyLearner<I, O>> learnerConstructor;

    private I initialProcedure;
    private O initialOutput;
    private final Map<I, Collection<Pair<Word<I>, Word<O>>>> projectedTraces;
    private final Set<I> terminatedProcedures;

    public SPMMPassiveLearner(ProceduralInputAlphabet<I> alphabet,
                              O errorSymbol,
                              Function<Alphabet<I>, PassiveMealyLearner<I, O>> learnerConstructor) {
        this.alphabet = alphabet;
        this.errorSymbol = errorSymbol;
        this.learnerConstructor = learnerConstructor;

        final int capacity = HashUtil.capacity(alphabet.getNumCalls());

        this.projectedTraces = new HashMap<>(capacity);
        this.terminatedProcedures = new HashSet<>(capacity);

        for (I i : alphabet.getCallAlphabet()) {
            this.projectedTraces.put(i, new HashSet<>());
        }
    }

    @Override
    public void addSamples(Collection<? extends DefaultQuery<I, Word<O>>> samples) {
        for (DefaultQuery<I, Word<O>> s : samples) {
            addSample(s);
        }
    }

    @Override
    @SuppressWarnings("PMD.AvoidReassigningLoopVariables") // we want to skip ahead here
    public void addSample(DefaultQuery<I, Word<O>> in) {

        final DefaultQuery<I, Word<O>> sample = normalizeSample(in);
        final Word<I> input = sample.getInput();
        final Word<O> output = sample.getOutput();

        // input validation
        if (sample.length() != output.length()) {
            throw new IllegalArgumentException("Number of input symbols do not match number of output symbols");
        }
        if (!alphabet.isReturnMatched(input)) {
            throw new IllegalArgumentException("Input is not return-matched");
        }
        if (initialProcedure == null) {
            this.initialProcedure = input.firstSymbol();
        } else if (!Objects.equals(this.initialProcedure, input.firstSymbol())) {
            throw new IllegalArgumentException("Inconsistent initial procedures");
        }
        if (initialOutput == null) {
            this.initialOutput = output.firstSymbol();
        } else if (!Objects.equals(this.initialOutput, output.firstSymbol())) {
            throw new IllegalArgumentException("Inconsistent initial outputs");
        }

        // activate procedures that are just accessed
        if (alphabet.isCallSymbol(input.lastSymbol())) {
            this.projectedTraces.get(input.lastSymbol()).add(Pair.of(Word.epsilon(), Word.epsilon()));
        }

        // sub-trace extraction
        // backward analysis over unmatched call symbols
        for (int i = input.size() - 1; i > 0;) {
            final int callIdx = alphabet.findCallIndex(input, i);
            final I callSym = input.getSymbol(callIdx);

            final Word<I> subInput = input.subWord(callIdx + 1, i + 1);
            final Word<O> subOutput = output.subWord(callIdx + 1, i + 1);
            final Pair<Word<I>, Word<O>> projected = alphabet.project(subInput, subOutput, 0);

            this.projectedTraces.get(callSym).add(projected);

            if (alphabet.isReturnSymbol(input.getSymbol(i))) {
                terminatedProcedures.add(callSym);
            }

            // normal forward analysis over well-matched subwords
            addSampleInternal(subInput, subOutput);
            i = callIdx;
        }
    }

    private void addSampleInternal(Word<I> input, Word<O> output) {
        for (int i = 0; i < input.size() - 1; i++) {
            final I sym = input.getSymbol(i);
            if (alphabet.isCallSymbol(sym)) {
                terminatedProcedures.add(sym);

                final int retIdx = alphabet.findReturnIndex(input, i + 1);
                final Word<I> subInput = input.subWord(i + 1, retIdx);
                final Word<O> subOutput = output.subWord(i + 1, retIdx);
                final Pair<Word<I>, Word<O>> projected = alphabet.project(subInput, subOutput, 0);

                projectedTraces.get(sym).add(projected);
            }
        }
    }

    private DefaultQuery<I, Word<O>> normalizeSample(DefaultQuery<I, Word<O>> sample) {
        final Word<O> output = sample.getOutput();
        int idx = output.length() - 1;

        while (idx > 0) {
            if (!Objects.equals(output.getSymbol(idx), errorSymbol)) {
                break;
            }
            idx--;
        }

        int len = Math.min(idx + 2, output.length());

        if (len == output.length()) {
            return sample;
        } else {
            return new DefaultQuery<>(Word.epsilon(), sample.getInput().prefix(len), output.prefix(len));
        }
    }

    @Override
    public SPMM<?, I, ?, O> computeModel() {
        final Map<I, MealyMachine<?, I, ?, O>> procedures = new HashMap<>(HashUtil.capacity(projectedTraces.size()));

        for (Entry<I, Collection<Pair<Word<I>, Word<O>>>> e : projectedTraces.entrySet()) {
            final I procedure = e.getKey();
            final PassiveMealyLearner<I, O> learner = learnerConstructor.apply(alphabet);

            for (Pair<Word<I>, Word<O>> pair : e.getValue()) {
                final Word<I> in = pair.getFirst();
                final Word<O> out = pair.getSecond();

                // ensure call, return, and error closure
                if (!in.isEmpty() && (Objects.equals(in.lastSymbol(), alphabet.getReturnSymbol()) ||
                                      alphabet.isCallSymbol(in.lastSymbol()) &&
                                      !terminatedProcedures.contains(in.lastSymbol())) ||
                    !out.isEmpty() && Objects.equals(out.lastSymbol(), errorSymbol)) {
                    // do two-step futures to prevent simple back loops when merging
                    for (I i : alphabet) {
                        for (I i2 : alphabet) {
                            learner.addSample(in.append(i).append(i2), out.append(errorSymbol).append(errorSymbol));
                        }
                    }
                }

                learner.addSample(pair.getFirst(), pair.getSecond());
            }

            final MealyMachine<?, I, ?, O> mealy = learner.computeModel();
            procedures.put(procedure, mealy);
        }

        return new StackSPMM<>(alphabet, initialProcedure, initialOutput, errorSymbol, procedures);
    }
}
