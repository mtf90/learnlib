/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.testsupport.example.ra;

import de.learnlib.theory.Theories;
import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import de.learnlib.theory.IntegerEqualityTheory;
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

public final class ExampleLogin
        extends DefaultLearningExample<InputSymbol, Boolean, RegisterAutomaton<?, InputSymbol, ?>>
        implements RALearningExample<InputSymbol> {

    public static final DataType<Integer> T_UID = new DataType<>("T_uid", BuiltinTypes.SINT32);
    public static final DataType<Integer> T_PWD = new DataType<>("T_pwd", BuiltinTypes.SINT32);

    public static final InputSymbol REGISTER = new InputSymbol("register", T_UID, T_PWD);
    public static final InputSymbol LOGIN = new InputSymbol("login", T_UID, T_PWD);
    public static final InputSymbol LOGOUT = new InputSymbol("logout");

    public ExampleLogin() {
        super(Alphabets.fromArray(REGISTER, LOGIN, LOGOUT), buildAutomaton());
    }

    private static RegisterAutomaton<?, InputSymbol, ?> buildAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.fromArray(REGISTER, LOGIN, LOGOUT));

        // locations
        Integer l0 = ra.addInitialState(false);
        Integer l1 = ra.addState(false);
        Integer l2 = ra.addState(true);

        // registers and parameters
        RegisterGenerator rgen = new RegisterGenerator();
        Register<Integer> rUid = rgen.next(T_UID);
        Register<Integer> rPwd = rgen.next(T_PWD);
        ParameterGenerator pgen = new ParameterGenerator();
        Parameter<Integer> pUid = pgen.next(T_UID);
        Parameter<Integer> pPwd = pgen.next(T_PWD);

        // guards
        Expression<Boolean> condition =
                ExpressionUtil.and(new NumericBooleanExpression(rUid, NumericComparator.EQ, pUid),
                                   new NumericBooleanExpression(rPwd, NumericComparator.EQ, pPwd));
        Expression<Boolean> elseCond = new Negation(condition);
        Expression<Boolean> trueGuard = ExpressionUtil.TRUE;

        // assignments
        VarMapping<Register<?>, SymbolicDataValue<?>> copyMapping = new VarMapping<>(rUid, rUid, rPwd, rPwd);
        VarMapping<Register<?>, SymbolicDataValue<?>> storeMapping = new VarMapping<>(rUid, pUid, rPwd, pPwd);

        Assignment copyAssign = new Assignment(copyMapping);
        Assignment storeAssign = new Assignment(storeMapping);

        // initial location
        ra.addTransition(l0, REGISTER, ra.createTransition(l1, trueGuard, storeAssign));

        // reg. location
        ra.addTransition(l1, LOGIN, ra.createTransition(l2, condition, copyAssign));
        ra.addTransition(l1, LOGIN, ra.createTransition(l1, elseCond, copyAssign));

        // login location
        ra.addTransition(l2, LOGOUT, ra.createTransition(l1, trueGuard, copyAssign));

        return ra;
    }

    @Override
    public Theories getTeachers() {
        return new Theories(Map.of(T_UID, new IntegerEqualityTheory(T_UID), T_PWD, new IntegerEqualityTheory(T_PWD)));
    }
}
