package de.learnlib.oracle.equivalence.rmm;

import java.util.Collection;

import de.learnlib.oracle.EquivalenceOracleGeneralization;
import de.learnlib.query.DefaultQuery;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.data.VarMapping.GeneratorMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.util.automaton.equivalence.RMMEquivalence;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimulatorEQOracle<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
        implements EquivalenceOracleGeneralization<RegisterMealyMachine<?, I, ?, O>, I, SymbolInstance<I>, Word<SymbolInstance<O>>> {

    private final RegisterMealyMachine<?, I, ?, O> ra;
    private final ConstraintSolver solver;
    private final GeneratorMapping generatorMapping;

    public SimulatorEQOracle(RegisterMealyMachine<?, I, ?, O> ra,
                             ConstraintSolver solver,
                             GeneratorMapping generatorMapping) {
        this.ra = ra;
        this.solver = solver;
        this.generatorMapping = generatorMapping;
    }

    @Override
    public @Nullable DefaultQuery<SymbolInstance<I>, Word<SymbolInstance<O>>> findCounterExample(RegisterMealyMachine<?, I, ?, O> hypothesis,
                                                                                                 Collection<? extends I> inputs) {
        final Word<SymbolInstance<I>> sepWord =
                RMMEquivalence.findSeparatingWord(this.ra, hypothesis, inputs, this.solver);

        if (sepWord == null) {
            return null;
        }

        return new DefaultQuery<>(sepWord, this.ra.asOutput(this.generatorMapping).computeOutput(sepWord));
    }
}
