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
package de.learnlib.algorithm.procedural.spmm.it;

import java.util.List;

import de.learnlib.algorithm.procedural.spmm.SPMMPassiveLearner;
import de.learnlib.algorithm.rpni.BlueFringeRPNIMealy;
import de.learnlib.testsupport.example.spmm.ExamplePalindrome;
import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.automaton.procedural.SPMM;
import net.automatalib.common.util.collection.IteratorUtil;
import net.automatalib.util.automaton.conformance.SPMMWMethodTestsIterator;
import net.automatalib.util.automaton.procedural.SPMMs;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SPMMPassiveIT {

    private final SPMM<?, Character, ?, Character> spmm;
    private final ProceduralInputAlphabet<Character> alphabet;
    private final Character errorSymbol;

    public SPMMPassiveIT() {
        final ExamplePalindrome example = ExamplePalindrome.createExample();
        this.spmm = example.getReferenceAutomaton();
        this.alphabet = example.getAlphabet();
        this.errorSymbol = spmm.getErrorOutput();
    }

    @Test
    public void testValidation() {

        final SPMMPassiveLearner<Character, Character> learner =
                new SPMMPassiveLearner<>(alphabet, errorSymbol, BlueFringeRPNIMealy::new);

        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("TR"), Word.fromString("aba")));

        learner.addSample(Word.fromString("SbR"), Word.fromString("aba"));
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("Sbr"), Word.fromString("bab")));

        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("SaRRS"), Word.fromString("ababa")));
    }

    @Test
    public void testRPNI() {
        final SPMMPassiveLearner<Character, Character> learner =
                new SPMMPassiveLearner<>(alphabet, errorSymbol, BlueFringeRPNIMealy::new);

        final List<Word<Character>> samples = IteratorUtil.list(new SPMMWMethodTestsIterator<>(spmm, alphabet, 0));

        for (Word<Character> s : samples) {
            learner.addSample(s, spmm.computeOutput(s));
        }

        SPMM<?, Character, ?, Character> hyp = learner.computeModel();
        Assert.assertTrue(SPMMs.testEquivalence(hyp, spmm, alphabet));
    }

    @Test
    public void testRPNIEquiv() {
        final SPMMPassiveLearner<Character, Character> learner =
                new SPMMPassiveLearner<>(alphabet, errorSymbol, BlueFringeRPNIMealy::new);

        SPMM<?, Character, ?, Character> hyp = learner.computeModel();
        Word<Character> ce;

        while ((ce = SPMMs.findSeparatingWord(hyp, spmm, alphabet)) != null) {
            final Word<Character> output = spmm.computeOutput(ce);
            learner.addSample(ce, output);
            hyp = learner.computeModel();
        }

        Assert.assertTrue(SPMMs.testEquivalence(hyp, spmm, alphabet));

    }

}
