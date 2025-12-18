/* Copyright (C) 2013-2025 TU Dortmund University
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
package de.learnlib.testsupport.it;

import java.util.ArrayList;
import java.util.List;

import de.learnlib.data.Configuration;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.oracle.equivalence.rmm.SimulatorEQOracle;
import de.learnlib.oracle.membership.RMMSimulatorOracle;
import de.learnlib.oracle.mto.MTORMMConfiguration;
import de.learnlib.testsupport.example.LearningExample.RMMLearningExample;
import de.learnlib.testsupport.example.LearningExamples;
import de.learnlib.testsupport.it.testcase.RMMLearnerITCase;
import de.learnlib.testsupport.it.util.LearnerITUtil;
import de.learnlib.testsupport.it.util.RMMLockableOracle;
import de.learnlib.testsupport.it.variant.LearnerVariantList;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RMMLearnerVariantList;
import de.learnlib.testsupport.it.variant.LearnerVariantListImpl.RMMLearnerVariantListImpl;
import de.learnlib.theory.Theories;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.testng.annotations.Factory;

/**
 * Abstract integration test for {@link RegisterMealyMachine} learning algorithms.
 */
public abstract class AbstractRMMLearnerIT {

    private final ConstraintSolver solver;

    public AbstractRMMLearnerIT() {
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @Factory
    public Object[] createExampleITCases() {
        final List<RMMLearningExample<?, ?>> examples = LearningExamples.createRMMExamples(this.solver);
        final List<RMMLearnerITCase<?, ?>> result = new ArrayList<>();

        for (RMMLearningExample<?, ?> example : examples) {
            result.addAll(createAllVariantsITCase(example));
        }

        return result.toArray();
    }

    private <I extends ParameterizedSymbol, O extends ParameterizedSymbol> List<RMMLearnerITCase<I, O>> createAllVariantsITCase(
            RMMLearningExample<I, O> example) {

        final Alphabet<I> alphabet = example.getAlphabet();
        final RegisterMealyMachine<?, I, ?, O> rmm = example.getReferenceAutomaton();
        final Theories teachers = example.getTeachers();
        final MembershipOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> mqOracle =
                new RMMSimulatorOracle<>(rmm, teachers.toGeneratorMapping());
        final RMMLockableOracle<I, O> lockableOracle = new RMMLockableOracle<>(mqOracle);
        final Configuration<I> config = new MTORMMConfiguration<>(lockableOracle, rmm.getConstants(), teachers, solver);

        final RMMLearnerVariantListImpl<I, O> variants = new RMMLearnerVariantListImpl<>();
        addLearnerVariants(alphabet, config, variants);

        return LearnerITUtil.createExampleITCases(example,
                                                  variants,
                                                  lockableOracle,
                                                  new SimulatorEQOracle<>(rmm,
                                                                          this.solver,
                                                                          teachers.toGeneratorMapping()));
    }

    /**
     * Adds, for a given setup, all the variants of the RA learner to be tested to the specified
     * {@link LearnerVariantList variant list}.
     *
     * @param alphabet
     *         the input alphabet
     * @param config
     *         the oracle configuration
     * @param variants
     *         list to add the learner variants to
     * @param <I>
     *         input symbol type
     */
    protected abstract <I extends ParameterizedSymbol, O extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                                                              Configuration<I> config,
                                                                                                              RMMLearnerVariantList<I, O> variants);
}
