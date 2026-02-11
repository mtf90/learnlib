package de.learnlib.testsupport.example.rmm;

import java.util.Collections;

import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RMMLearningExample;
import de.learnlib.theory.IntegerEqualityTheory;
import de.learnlib.theory.Theories;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.types.BuiltinTypes;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.OutputAssignment;
import net.automatalib.automaton.ra.RegisterMealyMachine;
import net.automatalib.automaton.ra.impl.CompactRMM;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.RegisterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Constant;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValue.Register;
import net.automatalib.data.SymbolicDataValueGenerator.ConstantGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;
import net.automatalib.word.Word;

public class ExampleContainerSUL
        extends DefaultLearningExample<InputSymbol, Word<OutputSymbol>, RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol>>
        implements RMMLearningExample<InputSymbol, OutputSymbol> {

    public static final DataType<Integer> T_INT = new DataType<>("int", BuiltinTypes.SINT32);

    public static final InputSymbol I_GET = new InputSymbol("get");
    public static final InputSymbol I_PUT = new InputSymbol("put", T_INT);

    public static final OutputSymbol O_V = new OutputSymbol("V");
    public static final OutputSymbol O_GET = new OutputSymbol("get", T_INT);

    public ExampleContainerSUL() {
        super(Alphabets.fromArray(I_GET, I_PUT), buildAutomaton());
    }

    private static RegisterMealyMachine<?, InputSymbol, ?, OutputSymbol> buildAutomaton() {

        // constants, registers and parameters
        ConstantGenerator cgen = new ConstantGenerator();
        Constant<Integer> c1 = cgen.next(T_INT);
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> r1 = rgen.next(T_INT);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> p1 = pgen.next(T_INT);

        Constants constants = new Constants();
        constants.put(c1, new DataValue<>(T_INT, 0));

        RegisterValuation initialRegisters = new RegisterValuation();

        CompactRMM<InputSymbol, OutputSymbol> rmm =
                new CompactRMM<>(Alphabets.fromArray(I_GET, I_PUT), initialRegisters, constants);

        // locations
        Integer l0 = rmm.addInitialState();
        Integer l1 = rmm.addState();

        // guards
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(r1, r1);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(r1, p1);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> constantMapping = new VarMapping<>(p1, c1);
        VarMapping<Parameter<?>, SymbolicDataValue<?>> outputMapping = new VarMapping<>(p1, r1);

        OutputAssignment storeAssign = new OutputAssignment(storeMapping);
        OutputAssignment constantOutput = new OutputAssignment(new VarMapping<>(), constantMapping);
        OutputAssignment outputAssignment = new OutputAssignment(copyMapping, outputMapping);

        // initial location
        rmm.addTransition(l0, I_GET, rmm.createTransition(l0, trueGuard, constantOutput, O_GET));
        rmm.addTransition(l0, I_PUT, rmm.createTransition(l1, trueGuard, storeAssign, O_V));

        // stored location
        rmm.addTransition(l1, I_GET, rmm.createTransition(l1, trueGuard, outputAssignment, O_GET));
        rmm.addTransition(l1, I_PUT, rmm.createTransition(l1, trueGuard, storeAssign, O_V));

        return rmm;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Collections.singletonMap(T_INT, new IntegerEqualityTheory(T_INT)));
    }
}
