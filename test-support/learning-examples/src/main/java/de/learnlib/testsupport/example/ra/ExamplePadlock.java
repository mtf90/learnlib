package de.learnlib.testsupport.example.ra;

import de.learnlib.theory.Theories;
import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import de.learnlib.theory.IntegerEqualityTheory;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collections;
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

public class ExamplePadlock extends DefaultLearningExample<InputSymbol, Boolean, RegisterAutomaton<?, InputSymbol, ?>>
        implements RALearningExample<InputSymbol> {

    public static final DataType<Integer> DIGIT = new DataType<>("id", BuiltinTypes.SINT32);

    public static final InputSymbol IN = new InputSymbol("in", DIGIT);

    public ExamplePadlock() {
        super(Alphabets.singleton(IN), buildAutomaton());
    }

    private static RegisterAutomaton<?, InputSymbol, ?> buildAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.singleton(IN));

        // locations
        Integer l0 = ra.addInitialState(true);
        Integer l1 = ra.addState(true);
        Integer l2 = ra.addState(true);
        Integer l3 = ra.addState(true);
        Integer l4 = ra.addState(false);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rVal = rgen.next(DIGIT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pVal = pgen.next(DIGIT);

        // guards
        Expression<Boolean> eqGuard = new NumericBooleanExpression(rVal, NumericComparator.EQ, pVal);
        Expression<Boolean> neqGuard = new NumericBooleanExpression(rVal, NumericComparator.NE, pVal);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rVal, rVal);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rVal, pVal);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);
        Assignment noAssign = new Assignment();

        // initial location
        ra.addTransition(l0, IN, ra.createTransition(l1, trueGuard, storeAssign));

        // IN 0 location
        ra.addTransition(l1, IN, ra.createTransition(l2, eqGuard, copyAssign));
        ra.addTransition(l1, IN, ra.createTransition(l0, neqGuard, noAssign));

        // IN 0 OK location
        ra.addTransition(l2, IN, ra.createTransition(l3, eqGuard, copyAssign));
        ra.addTransition(l2, IN, ra.createTransition(l0, neqGuard, noAssign));

        // IN 0 OK in 1 location
        ra.addTransition(l3, IN, ra.createTransition(l4, eqGuard, noAssign));
        ra.addTransition(l3, IN, ra.createTransition(l0, neqGuard, noAssign));

        ra.addTransition(l4, IN, ra.createTransition(l4, trueGuard, noAssign));

        return ra;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Collections.singletonMap(DIGIT, new IntegerEqualityTheory(DIGIT)));
    }
}
