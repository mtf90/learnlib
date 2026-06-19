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
package de.learnlib.algorithm.rpni;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.learnlib.algorithm.PassiveLearningAlgorithm.PassiveDFALearner;
import de.learnlib.datastructure.pta.BlueFringePTA;
import de.learnlib.datastructure.pta.wrapper.DFAWrapper;
import de.learnlib.query.DefaultQuery;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.fsa.DFA;
import net.automatalib.word.Word;

/**
 * A learner that uses the classic RPNI strategy but considers all prefixes of training samples as negative.
 *
 * @param <I>
 *         input symbol type
 */
public class PessimisticLearner2<I> extends AbstractBlueFringeRPNI<I, Boolean, Boolean, Void, DFA<?, I>>
        implements PassiveDFALearner<I> {

    private final Set<Word<I>> positive = new HashSet<>();

    /**
     * Constructor.
     *
     * @param alphabet
     *         the alphabet
     */
    public PessimisticLearner2(Alphabet<I> alphabet) {
        super(alphabet);
    }

    @Override
    public void addSamples(Collection<? extends DefaultQuery<I, Boolean>> samples) {
        for (DefaultQuery<I, Boolean> query : samples) {
            if (!query.getOutput()) {
                throw new IllegalArgumentException("Only positive examples are allowed");
            }
            positive.add(query.getInput());
        }
    }

    @Override
    protected BlueFringePTA<Boolean, Void> fetchPTA() {
        final BlueFringePTA<Boolean, Void> pta = new BlueFringePTA<>(alphabet.size());

        for (Word<I> p : positive) {
            for (Word<I> prefix : p.prefixes(false)) {
                pta.addSample(prefix.asIntSeq(alphabet), positive.contains(prefix));
            }
        }

        return pta;
    }

    @Override
    protected DFA<?, I> ptaToModel(BlueFringePTA<Boolean, Void> pta) {
        return new DFAWrapper<>(alphabet, pta);
    }
}
