package de.learnlib.algorithm.register.ralambda;

import de.learnlib.data.SymbolicSuffix;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class SymbolicWord<I extends ParameterizedSymbol> {
	private final Word<SymbolInstance<I>> prefix;
	private final SymbolicSuffix<I> suffix;

	public SymbolicWord(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}

	public Word<SymbolInstance<I>> getPrefix() {
		return prefix;
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SymbolicWord other = (SymbolicWord)obj;
		if (!prefix.equals(other.getPrefix()))
			return false;
//		if (!other.getSuffix().getActions().equals(suffix.getActions()))
        return other.getSuffix().equals(suffix);
    }

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash * getPrefix().hashCode();
		hash = 31 * hash * getSuffix().hashCode();
		return hash;
	}

        @Override
	public String toString() {
		return "{" + prefix.toString() + ", " + suffix.toString() + "}";
	}
}
