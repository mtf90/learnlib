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
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import net.automatalib.data.GuardElement;

/**
 * @author falk
 */
public class TrueGuard<T> implements SDTGuard<T> {

    private final SuffixValue<T> parameter;

    public TrueGuard(SuffixValue<T> parameter) {
        this.parameter = parameter;
    }

    @Override
    public String toString() {
        return "TRUE: " + parameter;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {return false;}

        TrueGuard<?> trueGuard = (TrueGuard<?>) o;
        return Objects.equals(parameter, trueGuard.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameter);
    }

    @Override
    public SuffixValue<T> getParameter() {
        return this.parameter;
    }

    @Override
    public Set<GuardElement> getRegisters() {
        return Collections.emptySet();
    }

    @Override
    public Expression<Boolean> toExpr() {
        return ExpressionUtil.TRUE;
    }

    @Override
    public SDTGuard<T> relabel(SDTRelabeling relabeling) {
        return this;
    }

    @Override
    public Set<GuardElement> getComparands(GuardElement element) {
        return Collections.emptySet();
    }

}
