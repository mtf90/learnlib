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
package de.learnlib.theory.guard;

import de.learnlib.data.SDTGuard;
import de.learnlib.data.SDTRelabeling;
import de.learnlib.data.SuffixValue;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.expressions.NumericBooleanExpression;
import gov.nasa.jpf.constraints.expressions.NumericComparator;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import net.automatalib.data.GuardElement;

/**
 * @author falk
 */
public class EqualityGuard<T> implements SDTGuard<T> {

    private final SuffixValue<T> parameter;
    private final GuardElement register;

    public EqualityGuard(SuffixValue<T> parameter, GuardElement register) {
        this.parameter = parameter;
        this.register = register;
    }

    @Override
    public String toString() {
        return "(" + parameter + "=" + register + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}

        EqualityGuard<?> that = (EqualityGuard<?>) o;
        return Objects.equals(parameter, that.parameter) && Objects.equals(register, that.register);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(parameter);
        result = 31 * result + Objects.hashCode(register);
        return result;
    }

    @Override
    public SuffixValue<T> getParameter() {
        return this.parameter;
    }

    @Override
    public Set<GuardElement> getRegisters() {
        return Collections.singleton(register);
    }

    public GuardElement getRegister() {
        return register;
    }

    @Override
    public Expression<Boolean> toExpr() {
        return new NumericBooleanExpression(parameter, NumericComparator.EQ, register.asExpression());
    }

    @Override
    public SDTGuard<T> relabel(SDTRelabeling relabeling) {
        return new EqualityGuard<>(relabeling.getIfAvailable(parameter), relabeling.getIfAvailable(register));
    }

    @Override
    public Set<GuardElement> getComparands(GuardElement element) {
        if (this.parameter.equals(element)) {
            return Collections.singleton(this.register);
        } else if (this.register.equals(element)) {
            return Collections.singleton(this.parameter);
        } else {
            return Collections.emptySet();
        }
    }
}
