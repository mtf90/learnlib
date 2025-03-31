package de.learnlib.oracle.membership;

import java.util.Collection;

import de.learnlib.oracle.MembershipOracle;
import de.learnlib.query.Query;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

public class RASimulatorOracle<I extends ParameterizedSymbol> implements MembershipOracle<SymbolInstance<I>, Boolean> {

    private final RegisterAutomaton<?, I, ?> hypothesis;

    public RASimulatorOracle(RegisterAutomaton<?, I, ?> hypothesis) {
        this.hypothesis = hypothesis;
    }

    @Override
    public void processQueries(Collection<? extends Query<SymbolInstance<I>, Boolean>> queries) {
        final DeterministicAcceptorTS<?, SymbolInstance<I>> acceptor = hypothesis.asAcceptor();
        for (Query<SymbolInstance<I>, Boolean> q : queries) {
            q.answer(acceptor.computeSuffixOutput(q.getPrefix(), q.getSuffix()));
        }
    }
}
