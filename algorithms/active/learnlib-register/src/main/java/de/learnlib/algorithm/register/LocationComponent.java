package de.learnlib.algorithm.register;

import de.learnlib.data.Bijection;
import de.learnlib.data.Branching;
import java.util.Collection;
import net.automatalib.data.DataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public interface LocationComponent<I extends ParameterizedSymbol> {
	boolean isAccepting();
	Word<SymbolInstance<I>> getAccessSequence();

	/**
	 * Remapping under which r becomes identical to the
	 * primary prefix of this component.
	 */
	Bijection<DataValue<?>> getRemapping(PrefixContainer<I> r);
	Branching<I> getBranching(I action);
	PrefixContainer<I> getPrimePrefix();
	Collection<PrefixContainer<I>> getOtherPrefixes();
}
