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
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import net.automatalib.data.GuardElement;

/**
 * @author falk
 */
public class IntervalGuard<T> implements SDTGuard<T> {

    private final SuffixValue<T> parameter;
    private final GuardElement smallerElement;
    private final GuardElement greaterElement;
    private final boolean smallerEqual;
    private final boolean greaterEqual;

    public IntervalGuard(SuffixValue<T> parameter, GuardElement smallerElement, GuardElement greaterElement) {
        this(parameter, smallerElement, greaterElement, false, false);
    }

    public IntervalGuard(SuffixValue<T> parameter,
                         GuardElement smallerElement,
                         GuardElement greaterElement,
                         boolean smallerEqual,
                         boolean greaterEqual) {
        if (smallerElement == null && greaterElement == null) {
            throw new IllegalArgumentException("at least one boundary must be set");
        }
        this.parameter = parameter;
        this.smallerElement = smallerElement;
        this.greaterElement = greaterElement;
        this.smallerEqual = smallerEqual;
        this.greaterEqual = greaterEqual;
    }

    public static <T> IntervalGuard<T> lessGuard(SuffixValue<T> param, GuardElement r) {
        return new IntervalGuard<>(param, null, r, false, false);
    }

    public static <T> IntervalGuard<T> lessOrEqualGuard(SuffixValue<T> param, GuardElement r) {
        return new IntervalGuard<>(param, null, r, false, true);
    }

    public static <T> IntervalGuard<T> greaterGuard(SuffixValue<T> param, GuardElement r) {
        return new IntervalGuard<>(param, r, null, false, false);
    }

    public static <T> IntervalGuard<T> greaterOrEqualGuard(SuffixValue<T> param, GuardElement r) {
        return new IntervalGuard<>(param, r, null, true, false);
    }

    public boolean isSmallerGuard() {
        return smallerElement == null;
    }

    public boolean isBiggerGuard() {
        return greaterElement == null;
    }

    public GuardElement getSmallerElement() {
        return smallerElement;
    }

    public GuardElement getGreaterElement() {
        return greaterElement;
    }

    public boolean isIntervalGuard() {
        return (smallerElement != null && greaterElement != null);
    }

    public boolean isLeftClosed() {
        return smallerEqual;
    }

    public boolean isRightClosed() {
        return greaterEqual;
    }

    @Override
    public String toString() {
        if (smallerElement == null) {
            // no smaller element, parameter is less than the greater (e.g., s1 < r1)
            return "(" + this.getParameter().toString() + (greaterEqual ? "<=" : "<") + this.greaterElement.toString() +
                   ")";
        }
        if (greaterElement == null) {
            // no greater element, parameter is greater than the smaller (e.g., s1 > r1)
            return "(" + this.getParameter().toString() + (smallerEqual ? ">=" : ">") + this.smallerElement.toString() +
                   ")";
        }
        // interval (e.g., r1 < s1 < r2)
        return "(" + smallerElement.toString() + (smallerEqual ? "<=" : "<") + this.getParameter().toString() +
               (greaterEqual ? "<=" : "<") + this.greaterElement.toString() + ")";
    }

    @Override
    public SuffixValue<T> getParameter() {
        return this.parameter;
    }

    @Override
    public Set<GuardElement> getRegisters() {
        if (smallerElement == null) {
            if (greaterElement == null) {
                return Collections.emptySet();
            } else {
                return Collections.singleton(greaterElement);
            }
        } else {
            if (greaterElement == null) {
                return Collections.singleton(smallerElement);
            } else {
                return Set.of(smallerElement, greaterElement);
            }
        }
    }

    @Override
    public Expression<Boolean> toExpr() {
        final Expression<Boolean> smaller;
        final Expression<Boolean> greater;

        if (smallerElement == null) {
            smaller = ExpressionUtil.TRUE;
        } else {
            smaller = new NumericBooleanExpression(parameter,
                                                   smallerEqual ? NumericComparator.GE : NumericComparator.GT,
                                                   smallerElement.asExpression());
        }

        if (greaterElement == null) {
            greater = ExpressionUtil.TRUE;
        } else {
            greater = new NumericBooleanExpression(parameter,
                                                   greaterEqual ? NumericComparator.LE : NumericComparator.LT,
                                                   greaterElement.asExpression());

        }

        return ExpressionUtil.and(smaller, greater);
    }

    @Override
    public SDTGuard<T> relabel(SDTRelabeling relabeling) {
        if (smallerElement == null) {
            return new IntervalGuard<>(relabeling.getIfAvailable(parameter),
                                       null,
                                       relabeling.getIfAvailable(greaterElement),
                                       false,
                                       greaterEqual);
        } else if (greaterElement == null) {
            return new IntervalGuard<>(relabeling.getIfAvailable(parameter),
                                       relabeling.getIfAvailable(smallerElement),
                                       null,
                                       smallerEqual,
                                       false);
        } else {
            return new IntervalGuard<>(relabeling.getIfAvailable(parameter),
                                       relabeling.getIfAvailable(smallerElement),
                                       relabeling.getIfAvailable(greaterElement),
                                       smallerEqual,
                                       greaterEqual);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        IntervalGuard<?> that = (IntervalGuard<?>) o;
        return smallerEqual == that.smallerEqual && greaterEqual == that.greaterEqual &&
               Objects.equals(parameter, that.parameter) && Objects.equals(smallerElement, that.smallerElement) &&
               Objects.equals(greaterElement, that.greaterElement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameter, smallerEqual, greaterEqual, smallerElement, greaterElement);
    }

    @Override
    public Set<GuardElement> getComparands(GuardElement element) {
        if (element.equals(smallerElement) || element.equals(greaterElement)) {
            return Collections.singleton(parameter);
        } else if (element.equals(parameter)) {
            if (smallerElement == null) {
                return Collections.singleton(greaterElement);
            } else if (greaterElement == null) {
                return Collections.singleton(smallerElement);
            } else {
                return Set.of(smallerElement, greaterElement);
            }
        } else {
            return Collections.emptySet();
        }
    }
}
