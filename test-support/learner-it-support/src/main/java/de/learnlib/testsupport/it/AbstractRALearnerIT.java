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
import de.learnlib.oracle.equivalence.ra.SimulatorEQOracle;
import de.learnlib.oracle.membership.RASimulatorOracle;
import de.learnlib.oracle.mto.MTOConfiguration;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import de.learnlib.testsupport.example.LearningExamples;
import de.learnlib.testsupport.it.util.LearnerITUtil;
import de.learnlib.testsupport.it.util.LockableOracle;
import de.learnlib.testsupport.it.variant.LearnerVariantList;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RALearnerVariantList;
import de.learnlib.testsupport.it.variant.LearnerVariantListImpl.RALearnerVariantListImpl;
import de.learnlib.theory.Theories;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import org.testng.annotations.Factory;

/**
 * Abstract integration test for {@link RegisterAutomaton} learning algorithms.
 */
public abstract class AbstractRALearnerIT {

    private final ConstraintSolver solver;

    public AbstractRALearnerIT() {
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @Factory
    public Object[] createExampleITCases() {
        final List<RALearningExample<?>> examples = LearningExamples.createRAExamples(this.solver);
        final List<RALearnerITCase<?>> result = new ArrayList<>();

        for (RALearningExample<?> example : examples) {
            result.addAll(createAllVariantsITCase(example));
        }

        return result.toArray();
    }

    private <I extends ParameterizedSymbol> List<RALearnerITCase<I>> createAllVariantsITCase(RALearningExample<I> example) {

        final Alphabet<I> alphabet = example.getAlphabet();
        final RegisterAutomaton<?, I, ?> ra = example.getReferenceAutomaton();
        final Theories teachers = example.getTeachers();

        final MembershipOracle<SymbolInstance<I>, Boolean> mqOracle = new RASimulatorOracle<>(ra);
        final LockableOracle<SymbolInstance<I>, Boolean> lockableOracle = new LockableOracle<>(mqOracle);
        final Configuration<I> config = new MTOConfiguration<>(lockableOracle, ra.getConstants(), teachers, solver);

        final RALearnerVariantListImpl<I> variants = new RALearnerVariantListImpl<>();
        addLearnerVariants(alphabet, config, variants);

        return LearnerITUtil.createExampleITCases(example, variants, lockableOracle, new SimulatorEQOracle<>(ra, this.solver));
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
    protected abstract <I extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                               Configuration<I> config,
                                                                               RALearnerVariantList<I> variants);
}
