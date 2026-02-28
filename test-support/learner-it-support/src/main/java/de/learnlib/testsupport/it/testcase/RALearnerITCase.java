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
package de.learnlib.testsupport.it.testcase;

import de.learnlib.oracle.EquivalenceOracleGeneralization;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import de.learnlib.testsupport.it.util.RALockableOracle;
import de.learnlib.testsupport.it.variant.LearnerVariant;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.util.automaton.equivalence.RAEquivalence;
import net.automatalib.util.automaton.equivalence.RAEquivalence2;

public class RALearnerITCase<I extends ParameterizedSymbol>
        extends AbstractLearnerVariantITCase<I, SymbolInstance<I>, Boolean, RegisterAutomaton<?, I, ?>> {

    private final RALearningExample<I> example;
    private final ConstraintSolver solver;

    public RALearnerITCase(LearnerVariant<RegisterAutomaton<?, I, ?>, SymbolInstance<I>, Boolean> variant,
                           RALearningExample<I> example,
                           RALockableOracle<I> lockableOracle,
                           EquivalenceOracleGeneralization<? super RegisterAutomaton<?, I, ?>, I, SymbolInstance<I>, Boolean> eqOracle) {
        super(variant, example, lockableOracle, eqOracle);
        this.example = example;
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @Override
    protected boolean testEquivalence(RegisterAutomaton<?, I, ?> hypothesis) {
        return RAEquivalence2.findSeparatingWord(this.example.getReferenceAutomaton(),
                                                 hypothesis,
                                                 this.example.getAlphabet(),
                                                 solver) == null;
    }
}
