package de.learnlib.data;

import gov.nasa.jpf.constraints.api.Expression;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.automatalib.data.SymbolicDataValue;

public abstract class SuffixValueRestriction {
	protected final SuffixValue parameter;

	public SuffixValueRestriction(SuffixValue parameter) {
		this.parameter = parameter;
	}

	public SuffixValueRestriction(SuffixValueRestriction other) {
		parameter = new SuffixValue(other.parameter.getDataType(), other.parameter.getId());
	}

	public SuffixValueRestriction(SuffixValueRestriction other, int shift) {
		parameter = new SuffixValue(other.parameter.getDataType(), other.parameter.getId()+shift);
	}

	public SuffixValue getParameter() {
		return parameter;
	}

	public abstract SuffixValueRestriction shift(int shiftStep);

	public abstract Expression<Boolean> toGuardExpression(Set<SymbolicDataValue<?>> vals);

	public abstract SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior);

	public abstract boolean revealsRegister(SymbolicDataValue r);

	@Override
	public int hashCode() {
		return Objects.hash(parameter);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SuffixValueRestriction other = (SuffixValueRestriction) obj;
		return Objects.equals(parameter, other.parameter);
	}

}
