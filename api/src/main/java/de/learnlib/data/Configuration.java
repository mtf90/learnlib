package de.learnlib.data;

import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.symbol.data.ParameterizedSymbol;

public interface Configuration<I extends ParameterizedSymbol> {

    Constants getConstants();

    TreeOracle<I> getTreeOracle();

    TreeOracle<I> getHypothesisOracle(RegisterAutomaton<?, I, ?> hypothesis);

    SDTLogicOracle getLogicOracle();

    ConstraintSolver getSolver();

}