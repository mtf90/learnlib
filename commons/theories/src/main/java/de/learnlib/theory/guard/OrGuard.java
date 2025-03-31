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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.automatalib.data.GuardElement;

/**
 * @author falk
 */
public class OrGuard<T> implements SDTGuard<T> {

    private final SuffixValue<T> parameter;
    private final List<SDTGuard<T>> disjuncts;

    public OrGuard(SuffixValue<T> parameter, List<SDTGuard<T>> disjuncts) {
        this.parameter = parameter;
        this.disjuncts = disjuncts;
    }

    @Override
    public String toString() {
        String p = "ORCOMPOUND: " + parameter;
        if (disjuncts.isEmpty()) {
            return p + "empty";
        }
        return p + disjuncts;
    }

    @Override
    public SuffixValue<T> getParameter() {
        return this.parameter;
    }

    @Override
    public Set<GuardElement> getRegisters() {
        Set<GuardElement> ret = new HashSet<>();
        for (SDTGuard<T> c : disjuncts) {
            ret.addAll(c.getRegisters());
        }
        return ret;
    }

    @Override
    public Expression<Boolean> toExpr() {
        List<Expression<Boolean>> subExp = new ArrayList<>(this.disjuncts.size());
        for (SDTGuard<T> c : disjuncts) {
            subExp.add(c.toExpr());
        }
        return ExpressionUtil.or(subExp);
    }

    @Override
    public SDTGuard<T> relabel(SDTRelabeling relabeling) {
        throw new IllegalStateException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrGuard<?> that = (OrGuard<?>) o;
        return Objects.equals(parameter, that.parameter) &&
               new HashSet<>(disjuncts).equals(new HashSet<>(that.disjuncts));
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameter, new HashSet<>(disjuncts));
    }

    @Override
    public Set<GuardElement> getComparands(GuardElement element) {
        final Set<GuardElement> result = new LinkedHashSet<>();

        for (SDTGuard<T> disjunct : this.disjuncts) {
            result.addAll(disjunct.getComparands(element));
        }

        return result;
    }
}
