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
package de.learnlib.algorithm.procedural.sba.it;

import java.util.Arrays;
import java.util.List;

import de.learnlib.algorithm.procedural.sba.SBAPassiveLearner;
import de.learnlib.algorithm.rpni.BlueFringeRPNIDFA;
import de.learnlib.testsupport.example.sba.ExamplePalindrome;
import net.automatalib.alphabet.ProceduralInputAlphabet;
import net.automatalib.automaton.procedural.SBA;
import net.automatalib.word.Word;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SBAPassiveIT {

    private final ProceduralInputAlphabet<Character> alphabet;
    private final List<Word<Character>> samples;

    public SBAPassiveIT() {
        final ExamplePalindrome example = ExamplePalindrome.createExample();
        this.alphabet = example.getAlphabet();
        this.samples = Arrays.asList(Word.fromString("SbR"), Word.fromString("SaSTcR"), Word.fromString("SaSTcRa"));
    }

    @Test
    public void testValidation() {
        final SBAPassiveLearner<Character> learner = new SBAPassiveLearner<>(alphabet, BlueFringeRPNIDFA::new);

        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("TR"), Boolean.FALSE));

        learner.addSample(Word.fromString("SbR"), Boolean.TRUE);
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("TbR"), Boolean.TRUE));

        Assert.assertThrows(IllegalArgumentException.class,
                            () -> learner.addSample(Word.fromString("SaRRS"), Boolean.TRUE));
    }

    @Test
    public void testRPNI() {
        final SBAPassiveLearner<Character> learner = new SBAPassiveLearner<>(alphabet, BlueFringeRPNIDFA::new);
        learner.addSamples(Boolean.TRUE, samples);
        final SBA<?, Character> sba = learner.computeModel();

        for (Word<Character> s : this.samples) {
            Assert.assertTrue(sba.accepts(s), s.toString());
        }
    }
}
