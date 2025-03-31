package de.learnlib.data;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.SymbolicDataValue;

public class UnrestrictedSuffixValue extends SuffixValueRestriction {

	public UnrestrictedSuffixValue(SuffixValue parameter) {
		super(parameter);
	}

	public UnrestrictedSuffixValue(UnrestrictedSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue<?>> vals) {
		return ExpressionUtil.TRUE;
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new UnrestrictedSuffixValue(this, shiftStep);
	}

	@Override
	public String toString() {
		return "Unrestricted(" + parameter.toString() + ")";
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		return this;
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return true;
	}
}
