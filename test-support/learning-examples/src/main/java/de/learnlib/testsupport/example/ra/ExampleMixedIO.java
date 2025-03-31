package de.learnlib.testsupport.example.ra;

import de.learnlib.theory.Theories;
import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import de.learnlib.theory.IntegerEqualityTheory;
import de.learnlib.theory.inquality.DoubleInequalityTheory;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.Negation;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Map;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.impl.InputSymbol;

public class ExampleMixedIO extends DefaultLearningExample<InputSymbol, Boolean, RegisterAutomaton<?, InputSymbol, ?>>
        implements RALearningExample<InputSymbol> {

    public static final DataType<Integer> T_INT = new DataType<>("T_int", BuiltinTypes.SINT32);
    public static final DataType<Double> T_DOUBLE = new DataType<>("T_double", BuiltinTypes.DOUBLE);

    public static final InputSymbol I_FRAME = new InputSymbol("IFrame", T_INT, T_DOUBLE);

    private final ConstraintSolver solver;

    public ExampleMixedIO(ConstraintSolver solver) {
        super(Alphabets.singleton(I_FRAME), buildAutomaton());
        this.solver = solver;
    }

    private static RegisterAutomaton<?, InputSymbol, ?> buildAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.singleton(I_FRAME));

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(true);
        Integer l2 = ra.addState(false);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r1 = rgen.next(T_INT);
        Register<Double> r2 = rgen.next(T_DOUBLE);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p1 = pgen.next(T_INT);
        Parameter<Double> p2 = pgen.next(T_DOUBLE);

        // guards
        Expression<Boolean> okGuard = ExpressionUtil.and(new NumericBooleanExpression(r1, NumericComparator.EQ, p1),
                                                         new NumericBooleanExpression(r2, NumericComparator.LT, p2));
        Expression<Boolean> errorGuard = new Negation(okGuard);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(r1, p1, r2, p2);

        Assignment storeAssign = new Assignment(storeMapping);
        Assignment noAssign = new Assignment();

        // initial location
        ra.addTransition(l0, I_FRAME, ra.createTransition(l1, trueGuard, storeAssign));

        ra.addTransition(l1, I_FRAME, ra.createTransition(l1, okGuard, storeAssign));
        ra.addTransition(l1, I_FRAME, ra.createTransition(l2, errorGuard, storeAssign));

        ra.addTransition(l2, I_FRAME, ra.createTransition(l2, trueGuard, noAssign));

        return ra;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Map.of(T_INT,
                                   new IntegerEqualityTheory(T_INT),
                                   T_DOUBLE,
                                   new DoubleInequalityTheory(T_DOUBLE, solver)));
    }
}
