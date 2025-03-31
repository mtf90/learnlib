package de.learnlib.oracle.mto;

import de.learnlib.data.Configuration;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.theory.Theories;
import de.learnlib.theory.restriction.SymbolicSuffixRestrictionBuilderImpl;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class MTORMMConfiguration<I extends ParameterizedSymbol, O extends ParameterizedSymbol>
        implements Configuration<I> {

    private final MembershipOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> oracle;
    private final Theories theories;
    private final Constants consts;
    private final ConstraintSolver solver;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    public MTORMMConfiguration(MembershipOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> oracle,
                               Constants consts,
                               Theories theories,
                               ConstraintSolver solver) {
        this(oracle, consts, theories, solver, new SymbolicSuffixRestrictionBuilderImpl(consts, theories));
    }

    public MTORMMConfiguration(MembershipOracle<SymbolInstance<I>, Word<SymbolInstance<O>>> oracle,
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
        throw new RuntimeException("TODO");
    }

    @Override
    public TreeOracle<I> getHypothesisOracle(RegisterAutomaton<?, I, ?> hypothesis) {
        throw new RuntimeException("TODO");
    }

    @Override
    public SDTLogicOracle getLogicOracle() {
        return new MultiTheorySDTLogicOracle(consts, solver);
    }

    @Override
    public ConstraintSolver getSolver() {
        return solver;
    }
}
