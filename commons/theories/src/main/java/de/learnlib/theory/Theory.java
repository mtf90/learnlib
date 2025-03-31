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
package de.learnlib.theory;

import de.learnlib.data.SDT;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SuffixValuation;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.data.FreshValueGenerator;
import net.automatalib.data.TypedValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * @param <T>
 *
 * @author falk
 */
public interface Theory<T> extends FreshValueGenerator<T>, TypedValue {

//    /**
//     * Returns a fresh data value.
//     *
//     * @param vals
//     *
//     * @return a fresh data value of type T
//     */
//    public DataValue<T> getFreshValue(List<DataValue<T>> vals);

    /**
     * Implements a tree query for this theory. This tree query will only work on one parameter and then call the
     * TreeOracle for the next parameter.
     * <p>
     * This method should contain (a) creating all values for the current parameter and (b) merging the corresponding
     * sub-trees.
     *
     * @param prefix
     *         prefix word.
     * @param suffix
     *         suffix word.
     * @param values
     *         found values for complete word (pos -> dv)
     * @param piv
     *         memorable data values of the prefix (dv <-> itr)
     * @param constants
     * @param suffixValues
     *         map of already instantiated suffix data values (sv -> dv)
     * @param oracle
     *         the tree oracle in control of this query
     *
     * @return a symbolic decision tree and updated piv
     */
    // TODO move to / integrate in TreeOracle
    <I extends ParameterizedSymbol> SDT treeQuery(
            Word<SymbolInstance<I>> prefix,
            SymbolicSuffix<I> suffix,
            List<DataValue<?>> values,
            Constants constants,
            SuffixValuation suffixValues,
            TreeOracle<I> oracle);

    /**
     * returns all next data values to be tested (for vals).
     *
     * @param vals
     *
     * @return
     */
    public Collection<DataValue<T>> getAllNextValues(List<DataValue<T>> vals);

    <I extends ParameterizedSymbol> DataValue instantiate(Word<SymbolInstance<I>> prefix,
                                                          I ps, SuffixValuation pval,
                                                          Constants constants,
                                                          SDTGuard guard, SuffixValue<T> param, Set<DataValue<T>> oldDvs);

    <I extends ParameterizedSymbol> SuffixValueRestriction restrictSuffixValue(SuffixValue suffixValue, Word<? extends SymbolInstance<I>> prefix, Word<? extends SymbolInstance<I>> suffix, Constants consts);

    SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior);

}
