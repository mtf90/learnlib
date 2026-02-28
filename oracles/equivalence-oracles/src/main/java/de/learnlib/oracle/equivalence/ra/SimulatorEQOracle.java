package de.learnlib.oracle.equivalence.ra;

import de.learnlib.oracle.EquivalenceOracleGeneralization;
import de.learnlib.query.DefaultQuery;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import java.util.Collection;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.util.automaton.equivalence.RAEquivalence;
import net.automatalib.util.automaton.equivalence.RAEquivalence2;
import net.automatalib.word.Word;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SimulatorEQOracle<I extends ParameterizedSymbol>
        implements EquivalenceOracleGeneralization<RegisterAutomaton<?, I, ?>, I, SymbolInstance<I>, Boolean> {

    private final RegisterAutomaton<?, I, ?> ra;
    private final ConstraintSolver solver;

    public SimulatorEQOracle(RegisterAutomaton<?, I, ?> ra, ConstraintSolver solver) {
        this.ra = ra;
        this.solver = solver;
    }

    @Override
    public @Nullable DefaultQuery<SymbolInstance<I>, Boolean> findCounterExample(RegisterAutomaton<?, I, ?> hypothesis,
                                                                                 Collection<? extends I> inputs) {
        final Word<SymbolInstance<I>> sepWord =
                RAEquivalence2.findSeparatingWord(this.ra, hypothesis, inputs, this.solver);

        if (sepWord == null) {
            return null;
        }

        return new DefaultQuery<>(sepWord, this.ra.asAcceptor().accepts(sepWord));
    }
}
