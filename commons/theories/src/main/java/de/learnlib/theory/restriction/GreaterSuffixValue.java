package de.learnlib.theory.restriction;

import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.UnrestrictedSuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.SymbolicDataValue;

public class GreaterSuffixValue extends SuffixValueRestriction {

	public GreaterSuffixValue(SuffixValue param) {
		super(param);
	}

	public GreaterSuffixValue(GreaterSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new GreaterSuffixValue(this, shiftStep);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue<?>> vals) {
		List<Expression<Boolean>> expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			Expression<Boolean> g = new NumericBooleanExpression(parameter, NumericComparator.GT, sdv);
			expr.add(g);
		}
		Expression<Boolean> expArr[] = new Expression[expr.size()];
		expArr = expr.toArray(expArr);
		return ExpressionUtil.and(expArr);
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		if (other instanceof GreaterSuffixValue || other instanceof FreshSuffixValue) {
			return this;
		}
		if (other instanceof LesserSuffixValue || other instanceof EqualRestriction) {
			return new UnrestrictedSuffixValue(parameter);
		}
		return other.merge(other, prior);
	}

	@Override
	public boolean revealsRegister(SymbolicDataValue r) {
		return false;
	}

	@Override
	public String toString() {
		return "Greater(" + parameter.toString() + ")";
	}
}
