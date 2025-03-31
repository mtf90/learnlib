package de.learnlib.algorithm.register.radt;

import de.learnlib.data.Bijection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.automatalib.data.DataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class PrefixSet<I extends ParameterizedSymbol> {
	private final Set<MappedPrefix<I>> prefixes;

	public PrefixSet() {
		prefixes = new LinkedHashSet<>();
	}

	public PrefixSet(PrefixSet<I> ps) {
		prefixes = new LinkedHashSet<>(ps.get());
	}

	public void add(MappedPrefix<I> p) {
		prefixes.add(p);
	}

	public void add(Word<SymbolInstance<I>> p, Bijection<DataValue> pmap) {
		prefixes.add(new MappedPrefix<>(p, pmap));
	}

	public boolean remove(MappedPrefix<I> p) {
		return prefixes.remove(p);
	}

	public boolean removeIf(Predicate<? super MappedPrefix<I>> filter) {
		return prefixes.removeIf(filter);
	}

	public boolean remove(Word<SymbolInstance<?>> p) {
		return prefixes.removeIf(mp -> mp.getPrefix().equals(p));
	}

	public Set<MappedPrefix<I>> get() {
		return prefixes;
	}

	public MappedPrefix get(Word<? extends SymbolInstance<?>> p) {
		for (MappedPrefix mp : prefixes) {
			if (mp.getPrefix().equals(p))
				return mp;
		}
		return null;
	}

	public Iterator<MappedPrefix<I>> iterator() {
		return prefixes.iterator();
	}

	public boolean contains(MappedPrefix p) {
		return prefixes.contains(p);
	}

	public boolean contains(Word<? extends SymbolInstance<?>> word) {
		return prefixes.stream().anyMatch(mp -> mp.getPrefix().equals(word));
	}

	public Collection<Word<SymbolInstance<I>>> getWords() {
		Collection<Word<SymbolInstance<I>>> words = new LinkedHashSet<>();
		for (MappedPrefix<I> p : prefixes)
			words.add(p.getPrefix());
		return words;
	}

	public int length() {
		return prefixes.size();
	}

	public boolean isEmpty() {
		return prefixes.isEmpty();
	}

	@Override
	public String toString() {
		return prefixes.toString();
	}
}
