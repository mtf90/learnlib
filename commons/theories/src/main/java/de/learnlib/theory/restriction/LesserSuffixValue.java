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

public class LesserSuffixValue extends SuffixValueRestriction {

	public LesserSuffixValue(SuffixValue param) {
		super(param);
	}

	public LesserSuffixValue(LesserSuffixValue other, int shift) {
		super(other, shift);
	}

	@Override
	public SuffixValueRestriction shift(int shiftStep) {
		return new LesserSuffixValue(this, shiftStep);
	}

	@Override
	public Expression<Boolean> toGuardExpression(Set<SymbolicDataValue<?>> vals) {
		List<Expression<Boolean>> expr = new ArrayList<>();
		for (SymbolicDataValue sdv : vals) {
			return new NumericBooleanExpression(parameter, NumericComparator.LT, sdv);
		}
		Expression<Boolean> exprArr[] = new Expression[expr.size()];
		exprArr = expr.toArray(exprArr);
		return ExpressionUtil.and(exprArr);
	}

	@Override
	public SuffixValueRestriction merge(SuffixValueRestriction other, Map<SuffixValue, SuffixValueRestriction> prior) {
		if (other instanceof LesserSuffixValue || other instanceof FreshSuffixValue) {
			return this;
		}
		if (other instanceof GreaterSuffixValue || other instanceof EqualRestriction) {
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
		return "Lesser(" + parameter.toString() + ")";
	}
}
