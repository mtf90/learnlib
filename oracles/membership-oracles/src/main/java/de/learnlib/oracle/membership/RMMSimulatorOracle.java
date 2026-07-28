package de.learnlib.oracle.membership;

import java.util.Collection;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import net.automatalib.automaton.concept.SuffixOutput;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.data.VarMapping.GeneratorMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class RMMSimulatorOracle<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
        implements MembershipOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> {

    private final RegisterMealyMachine<?, I, ?, O> hypothesis;
    private final GeneratorMapping mapping;

    public RMMSimulatorOracle(RegisterMealyMachine<?, I, ?, O> hypothesis, GeneratorMapping mapping) {
        this.hypothesis = hypothesis;
        this.mapping = mapping;
    }

    @Override
    public void processQueries(Collection<? extends Query<SymbolInstance<I>, Word<SymbolInstance<O>>>> queries) {
        final SuffixOutput<SymbolInstance<I>, Word<SymbolInstance<O>>> output = hypothesis.getSemantics(mapping);
        for (Query<SymbolInstance<I>, Word<SymbolInstance<O>>> q : queries) {
            q.answer(output.computeSuffixOutput(q.getPrefix(), q.getSuffix()));
        }
    }
}
