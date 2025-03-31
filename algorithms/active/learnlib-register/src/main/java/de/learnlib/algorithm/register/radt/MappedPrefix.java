package de.learnlib.algorithm.register.radt;

import de.learnlib.algorithm.register.PrefixContainer;
import de.learnlib.algorithm.register.RemappingIterator;
import de.learnlib.data.Bijection;
import de.learnlib.data.Memorables;
import de.learnlib.data.RegisterAssignment;
import de.learnlib.data.SDT;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class MappedPrefix<I extends ParameterizedSymbol> implements PrefixContainer<I> {

	private final Word<SymbolInstance<I>> prefix;
	private final RegisterGenerator regGen = new RegisterGenerator();

	private Bijection<DataValue> remapping;
	private final Map<SymbolicSuffix<I>, SDT> tqrs = new LinkedHashMap<>();
	public final Set<DataValue> missingParameter = new LinkedHashSet<>();

	public MappedPrefix(Word<SymbolInstance<I>> prefix, Bijection<DataValue> remapping) {
		this.prefix = prefix;
		this.remapping = remapping;
	}

	MappedPrefix(MappedPrefix mp, Bijection<DataValue> remapping) {
		this.prefix = mp.getPrefix();
		tqrs.putAll(mp.getTQRs());
		this.remapping = remapping;
	}

	public Set<Bijection<DataValue<?>>> equivalentRenamings(Set<DataValue<?>> params) {
		assert new HashSet<>(memorableValues()).containsAll(params);

		Set<Bijection<DataValue<?>>> renamings = new LinkedHashSet<>();
		RemappingIterator<DataValue<?>> iter = new RemappingIterator<>(params, params);
		LOC: for (Bijection<DataValue<?>> b : iter) {
			for (SDT tqr : tqrs.values()) {
				if (!tqr.isEquivalent(tqr, b))
					continue LOC;
			}
			renamings.add(b);
		}
		return renamings;
	}

	/*
	 * Performs a tree query for the (new) suffix and stores it in its internal map.
	 * Returns the result.
	 */
	SDT computeTQR(SymbolicSuffix suffix, TreeOracle oracle) {
            SDT tqr = oracle.treeQuery(prefix, suffix);
	    addTQR(suffix, tqr);
	    return tqr;
	}

	void addTQR(SymbolicSuffix s, SDT tqr) {
	    if (tqrs.containsKey(s) || tqr == null) return;
		tqrs.put(s, tqr);
	}

	public Map<SymbolicSuffix<I>, SDT> getTQRs() {
		return tqrs;
	}

	@Override
	public Word<SymbolInstance<I>> getPrefix() {
		return this.prefix;
	}

	public Bijection<DataValue> getRemapping() {
		return remapping;
	}

	public void updateRemapping(Bijection<DataValue> remapping) {
		this.remapping = remapping;
	}

	public Set<DataValue<?>> memorableValues() {
		return Memorables.memorableValues(tqrs.values());
	}

	@Override
	public RegisterAssignment getAssignment() {
		return Memorables.getAssignment(tqrs.values());
	}

	@Override
	public String toString() {
		return "{" + prefix.toString() + ", " + Arrays.toString(memorableValues().toArray()) + "}";
	}

	SymbolicSuffix getSuffixForMemorable(DataValue d) {
		return tqrs.entrySet().stream()
				.filter(e -> e.getValue().getDataValues().contains(d))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("This line is not supposed to be reached."))
				.getKey();
	}

	List<SymbolicSuffix> getAllSuffixesForMemorable(DataValue d) {
		return tqrs.entrySet().stream()
				.filter(e -> e.getValue().getDataValues().contains(d))
				.map(Entry::getKey)
				.collect(Collectors.toList());
	}
}
