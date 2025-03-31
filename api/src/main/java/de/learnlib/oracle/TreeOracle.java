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
package de.learnlib.oracle;

import de.learnlib.data.Branching;
import de.learnlib.data.SDT;
import de.learnlib.data.SuffixValuation;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import java.util.List;
import java.util.Map;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * A tree oracle is the connection between the learning algorithm and theories for data values.
 *
 * @author falk
 */
public interface TreeOracle<I extends ParameterizedSymbol> {

    /**
     * performs a tree query, returning a SymbolicDecisionTree an an Assignment of registers of this tree with
     * parameters of the prefix.
     *
     * @param prefix
     * @param suffix
     *
     * @return
     */
    SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix);

    SDT treeQuery(Word<SymbolInstance<I>> prefix, SymbolicSuffix<I> suffix, List<DataValue<?>> values,
                                                                                          Constants constants, SuffixValuation suffixValues);

    /**
     * Computes a Branching from a set of SymbolicDecisionTrees.
     *
     * @param prefix
     * @param ps
     * @param piv
     * @param sdts
     *
     * @return
     */
    Branching<I> getInitialBranching(Word<SymbolInstance<I>> prefix,
                                  I ps, SDT... sdts);

    // TODO move to Theory
    Map<Word<SymbolInstance<I>>, Boolean> instantiate(Word<SymbolInstance<I>> prefix,
                                                    SymbolicSuffix<I> suffix, SDT sdt);

    SymbolicSuffixRestrictionBuilder getRestrictionBuilder();

}
