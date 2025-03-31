package de.learnlib.theory;

import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueGenerator;
import de.learnlib.data.SuffixValueRestriction;
import gov.nasa.jpf.constraints.api.Expression;
import java.util.ArrayList;
import java.util.List;
import net.automatalib.automaton.ra.Util;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.Mapping;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.Valuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class EquivalenceClassFilter {

	private final List<DataValue> equivClasses;
	private final boolean useOptimization;

	public EquivalenceClassFilter(List<DataValue> equivClasses, boolean useOptimization) {
		this.equivClasses = equivClasses;
		this.useOptimization = useOptimization;
	}

	public List<DataValue> toList(SuffixValueRestriction restr,
								  Word<? extends SymbolInstance<?>> prefix, Word<? extends ParameterizedSymbol> suffix, List<DataValue<?>> valuation) {

		if (!useOptimization) {
			return equivClasses;
		}

		List<DataValue> filtered = new ArrayList<>();

		ParameterGenerator pgen = new ParameterGenerator();
		SuffixValueGenerator svgen = new SuffixValueGenerator();
		Mapping<SymbolicDataValue<?>, DataValue<?>> mapping = new Mapping<>();
		for (SymbolInstance<?> psi : prefix) {
			DataType[] dts = psi.getBaseSymbol().getPtypes();
			DataValue[] dvs = psi.getParameterValues();
			for (int i = 0; i < dvs.length; i++) {
				Parameter p = pgen.next(dts[i]);
				if (restr.getParameter().getDataType().equals(dts[i])) {
					mapping.put(p, dvs[i]);
				}
			}
		}
		for (ParameterizedSymbol ps : suffix) {
			DataType[] dts = ps.getPtypes();
			for (int i = 0; i < dts.length; i++) {
				SuffixValue sv = svgen.next(dts[i]);
				DataValue val = valuation.get(sv.getId());
				if (val != null && val.getDataType().equals(restr.getParameter().getDataType())) {
					mapping.put(sv, val);
				}
			}
		}

		Expression<Boolean> expr = restr.toGuardExpression(mapping.keySet());
		for (DataValue<?> ec : equivClasses) {
			Valuation<SymbolicDataValue<?>, DataValue<?>> ecMapping = new Valuation<>();
			ecMapping.putAll(mapping);
			ecMapping.put(restr.getParameter(), ec);
			//System.out.println(" -- " + expr + "  - " + ecMapping);
			if (expr.evaluateSMT(Util.compose(ecMapping))) {
				filtered.add(ec);
			}
		}
		return filtered;
	}
}
