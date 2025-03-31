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

public class ExampleStack extends DefaultLearningExample<InputSymbol, Boolean, RegisterAutomaton<?, InputSymbol, ?>>
        implements RALearningExample<InputSymbol> {

    public static final DataType<Integer> T_INT = new DataType<>("T_int", BuiltinTypes.SINT32);

    public static final InputSymbol I_PUSH = new InputSymbol("push", T_INT);
    public static final InputSymbol I_POP = new InputSymbol("pop", T_INT);

    public ExampleStack() {
        super(Alphabets.fromArray(I_PUSH, I_POP), buildAutomaton());
    }

    private static RegisterAutomaton<?, InputSymbol, ?> buildAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.fromArray(I_PUSH, I_POP));

        // locations
        Integer l0 = ra.addInitialState(true);
        Integer l1 = ra.addState(true);
        Integer l2 = ra.addState(true);
        Integer ls = ra.addState(false);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rVal1 = rgen.next(T_INT);
        Register<Integer> rVal2 = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pVal = pgen.next(T_INT);

        // guards
        Expression<Boolean> okGuard1 = new NumericBooleanExpression(rVal1, NumericComparator.EQ, pVal);
        Expression<Boolean> okGuard2 = new NumericBooleanExpression(rVal2, NumericComparator.EQ, pVal);
        Expression<Boolean> errorGuard1 = new NumericBooleanExpression(rVal1, NumericComparator.NE, pVal);
        Expression<Boolean> errorGuard2 = new NumericBooleanExpression(rVal2, NumericComparator.NE, pVal);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rVal1, rVal1);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping1 = new VarMapping<>(rVal1, pVal);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping2 = new VarMapping<>(rVal1, rVal1, rVal2, pVal);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign1 = new Assignment(storeMapping1);
        Assignment storeAssign2 = new Assignment(storeMapping2);
        Assignment noAssign = new Assignment();

        // initial location
        ra.addTransition(l0, I_PUSH, ra.createTransition(l1, trueGuard, storeAssign1));
        ra.addTransition(l0, I_POP, ra.createTransition(ls, trueGuard, noAssign));

        // push location
        ra.addTransition(l1, I_POP, ra.createTransition(l0, okGuard1, copyAssign));
        ra.addTransition(l1, I_POP, ra.createTransition(ls, errorGuard1, noAssign));
        ra.addTransition(l1, I_PUSH, ra.createTransition(l2, trueGuard, storeAssign2));

        // push push location
        ra.addTransition(l2, I_POP, ra.createTransition(l1, okGuard2, copyAssign));
        ra.addTransition(l2, I_POP, ra.createTransition(ls, errorGuard2, noAssign));
        ra.addTransition(l2, I_PUSH, ra.createTransition(ls, trueGuard, noAssign));

        // sink location
        ra.addTransition(ls, I_POP, ra.createTransition(ls, trueGuard, noAssign));
        ra.addTransition(ls, I_PUSH, ra.createTransition(ls, trueGuard, noAssign));

        return ra;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Collections.singletonMap(T_INT, new IntegerEqualityTheory(T_INT)));
    }
}
