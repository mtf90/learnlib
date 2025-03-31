package de.learnlib.algorithm.register.radt;

import de.learnlib.data.Bijection;
import de.learnlib.data.Branching;
import java.util.LinkedHashMap;
import java.util.Map;

import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
public class ShortPrefix<I extends ParameterizedSymbol> extends MappedPrefix<I> {


	private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<>();

	public ShortPrefix(Word<SymbolInstance<I>> prefix) {
		super(prefix, new Bijection<>());
	}

	public ShortPrefix(MappedPrefix mp) {
		super(mp, mp.getRemapping());
	}

	public Map<ParameterizedSymbol, Branching> getBranching() {
		return branching;
	}

	public Branching getBranching(ParameterizedSymbol ps) {
		return branching.get(ps);
	}

	void putBranching(ParameterizedSymbol ps, Branching b) {
		branching.put(ps, b);
	}
}
