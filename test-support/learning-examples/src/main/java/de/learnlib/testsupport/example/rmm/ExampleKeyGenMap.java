package de.learnlib.testsupport.example.rmm;

import java.util.Collections;

import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RMMLearningExample;
import de.learnlib.theory.IntegerEqualityTheory;
import de.learnlib.theory.Theories;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.automaton.ra.impl.CompactRMM;
import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.FreshOutput;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.FreshOutputGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.word.Word;

public class ExampleKeyGenMap
        extends DefaultLearningExample<InputSymbol, Word<OutputSymbol>, RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol>>
        implements RMMLearningExample<InputSymbol, OutputSymbol> {

    public static final DataType<Integer> T_INT = new DataType<>("int", BuiltinTypes.SINT32);

    public static final InputSymbol I_GET = new InputSymbol("get", T_INT);
    public static final InputSymbol I_PUT = new InputSymbol("put", T_INT);

    public static final OutputSymbol O_PUT = new OutputSymbol("put", T_INT);
    public static final OutputSymbol O_NULL = new OutputSymbol("NULL");
    public static final OutputSymbol O_ERR = new OutputSymbol("ERR");

    public ExampleKeyGenMap() {
        super(Alphabets.fromArray(I_GET, I_PUT), buildAutomaton());
    }

    private static RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol> buildAutomaton() {

        // registers and parameters
        FreshOutputGenerator fgen = new FreshOutputGenerator();
        FreshOutput<Integer> f1 = fgen.next(T_INT);
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r1 = rgen.next(T_INT);
        Register<Integer> r2 = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p1 = pgen.next(T_INT);

        CompactRMM<InputSymbol, OutputSymbol> rmm = new CompactRMM<>(Alphabets.fromArray(I_GET, I_PUT));

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState();
        Integer l2 = rmm.addState();

        // guards
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;
        Expression<Boolean> equalGuard = new NumericBooleanExpression(r1, NumericComparator.EQ, p1);
        Expression<Boolean> inequalGuard = new NumericBooleanExpression(r1, NumericComparator.NE, p1);

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(r1, r1);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(r1, p1);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> newKeyMapping = new VarMapping<>(p1, f1);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> outputMapping = new VarMapping<>(p1, r2);

        OutputAssignment copyAssign = new OutputAssignment(copyMapping);
        OutputAssignment newStoreAssign = new OutputAssignment(storeMapping, newKeyMapping);
        OutputAssignment copyOutputAssignment = new OutputAssignment(copyMapping, outputMapping);

        // initial location
        rmm.addTransition(l0, I_GET, rmm.createTransition(l0, trueGuard, new OutputAssignment(), O_NULL));
        rmm.addTransition(l0, I_PUT, rmm.createTransition(l1, trueGuard, newStoreAssign, O_PUT));

        // stored location
        rmm.addTransition(l1, I_GET, rmm.createTransition(l1, inequalGuard, copyAssign, O_NULL));
        rmm.addTransition(l1, I_GET, rmm.createTransition(l1, equalGuard, copyOutputAssignment, O_PUT));
        rmm.addTransition(l1, I_PUT, rmm.createTransition(l2, trueGuard, new OutputAssignment(), O_ERR));

        // error location
        rmm.addTransition(l2, I_GET, rmm.createTransition(l2, trueGuard, new OutputAssignment(), O_ERR));
        rmm.addTransition(l2, I_PUT, rmm.createTransition(l2, trueGuard, new OutputAssignment(), O_ERR));

        return rmm;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Collections.singletonMap(T_INT, new IntegerEqualityTheory(T_INT)));
    }
}
