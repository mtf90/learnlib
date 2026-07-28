package de.learnlib.oracle.mto;

import java.util.Collection;

import de.learnlib.data.Configuration;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.query.Query;
import de.learnlib.theory.Theories;
import de.learnlib.theory.restriction.SymbolicSuffixRestrictionBuilderImpl;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;

public class MTOConfiguration<I extends ParameterizedSymbol> implements Configuration<I> {

    private final MembershipOracle<SymbolInstance<I>, Boolean> oracle;
    private final Theories theories;
    private final Constants consts;
    private final ConstraintSolver solver;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    public MTOConfiguration(MembershipOracle<SymbolInstance<I>, Boolean> oracle,
                            Constants consts,
                            Theories theories,
                            ConstraintSolver solver) {
        this(oracle, consts, theories, solver, new SymbolicSuffixRestrictionBuilderImpl(consts, theories));
    }

    public MTOConfiguration(MembershipOracle<SymbolInstance<I>, Boolean> oracle,
                            Constants consts,
                            Theories theories,
                            ConstraintSolver solver,
                            SymbolicSuffixRestrictionBuilder restrictionBuilder) {
        this.oracle = oracle;
        this.theories = theories;
        this.consts = consts;
        this.solver = solver;
        this.restrictionBuilder = restrictionBuilder;
    }

    @Override
    public Constants getConstants() {
        return consts;
    }

    public TreeOracle<I> getTreeOracle() {
        return new MultiTheoryTreeOracle<>(oracle, theories, consts, solver, restrictionBuilder);
    }

    @Override
    public TreeOracle<I> getHypothesisOracle(RegisterAutomaton<?, I, ?> hypothesis) {
        return new MultiTheoryTreeOracle<>(new Wrapper<>(hypothesis), theories, consts, solver, restrictionBuilder);
    }

    @Override
    public SDTLogicOracle getLogicOracle() {
        return new MultiTheorySDTLogicOracle(consts, solver);
    }

    @Override
    public ConstraintSolver getSolver() {
        return solver;
    }

    private static class Wrapper<I extends ParameterizedSymbol>
            implements MembershipOracle<SymbolInstance<I>, Boolean> {

        private final RegisterAutomaton<?, I, ?> hypothesis;

        public Wrapper(RegisterAutomaton<?, I, ?> hypothesis) {
            this.hypothesis = hypothesis;
        }

        @Override
        public void processQueries(Collection<? extends Query<SymbolInstance<I>, Boolean>> queries) {
            final DeterministicAcceptorTS<?, SymbolInstance<I>> acceptor = hypothesis.getSemantics();
            for (Query<SymbolInstance<I>, Boolean> q : queries) {
                q.answer(acceptor.computeSuffixOutput(q.getPrefix(), q.getSuffix()));
            }
        }
    }
}
