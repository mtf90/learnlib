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

import de.learnlib.oracle.EquivalenceOracle;
import de.learnlib.oracle.EquivalenceOracleGeneralization;
import de.learnlib.testsupport.example.LearningExample.RMMLearningExample;
import de.learnlib.testsupport.it.testcase.AbstractLearnerVariantITCase;
import de.learnlib.testsupport.it.util.LockableOracle;
import de.learnlib.testsupport.it.variant.LearnerVariant;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.solvers.ConstraintSolverFactory;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.util.automaton.equivalence.RMMEquivalence;
import net.automatalib.word.Word;

public class RMMLearnerITCase<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
        extends AbstractLearnerVariantITCase<I, SymbolInstance<I>, Word<SymbolInstance<O>>, RegisterMealyMachine<?, I, ?, O>> {

    private final RMMLearningExample<I, O> example;
    private final ConstraintSolver solver;

    public RMMLearnerITCase(LearnerVariant<RegisterMealyMachine<?, I, ?, O>, SymbolInstance<I>, Word<SymbolInstance<O>>> variant,
                            RMMLearningExample<I, O> example,
                            LockableOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> lockableOracle,
                            EquivalenceOracleGeneralization<? super RegisterMealyMachine<?, I, ?, O>, I, SymbolInstance<I>, Word<SymbolInstance<O>>> eqOracle) {
        super(variant, example, lockableOracle, eqOracle);
        this.example = example;
        this.solver = ConstraintSolverFactory.createSolver("z3");
    }

    @Override
    protected boolean testEquivalence(RegisterMealyMachine<?, I, ?, O> hypothesis) {
        return RMMEquivalence.findSeparatingWord(this.example.getReferenceAutomaton(),
                                                 hypothesis,
                                                 this.example.getAlphabet(),
                                                 solver) == null;
    }
}
