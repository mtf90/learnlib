package de.learnlib.data;

import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.api.Variable;
import gov.nasa.jpf.constraints.util.DuplicatingVisitor;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.VarMapping;

public class ReplacingVarsVisitor extends
        DuplicatingVisitor<VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue>> {

    @Override
    public <E> Expression<?> visit(Variable<E> v, VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> data) {
        SymbolicDataValue newVar = data.get(v);
        return (newVar != null) ? newVar : v;
    }

    public <T> Expression<T> apply(Expression<T> expr, VarMapping<? extends SymbolicDataValue, ? extends SymbolicDataValue> rename) {
        return visit(expr, rename).requireAs(expr.getType());
    }
}
