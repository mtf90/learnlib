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
package de.learnlib.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

/**
 * static helper methods for data words.
 *
 * @author falk
 */
public final class DataWords {

    /**
     * returns sequence of data values of a specific type in a data word.
     *
     * @param word
     * @param t
     * @return
     */
    public static <I extends ParameterizedSymbol, T>  DataValue<T>[] valsOf(Word<? extends SymbolInstance<I>> word, DataType<T> t) {
        List<DataValue<T>> vals = new ArrayList<>();
        for (SymbolInstance<I> psi : word) {
            for (DataValue<?> d : psi.getParameterValues()) {
                if (d.getDataType().equals(t)) {
                    vals.add((DataValue<T>) d);
                }
            }
        }
        return vals.toArray(new DataValue[] {});
    }

    /**
     * returns sequence of all data values in a data word.
     *
     * @param word
     * @return
     */
    public static <I extends ParameterizedSymbol> DataValue<?>[] valsOf(Word<? extends SymbolInstance<I>> word) {
        DataValue<?>[] vals = new DataValue[DataWords.paramLength(actsOf(word))];
        int i = 0;
        for (SymbolInstance<?> psi : word) {
            for (DataValue<?> p : psi.getParameterValues()) {
                vals[i++] = p;
            }
        }
        return vals;
    }

    /**
     * returns a sequence of all data types in a data word
     *
     * @param word
     * @return
     */
    public static <I extends ParameterizedSymbol> DataType<?>[] typesOf(Word<I> word) {
    	DataType<?>[] types = new DataType[DataWords.paramLength(word)];
    	int i = 0;
    	for (ParameterizedSymbol ps : word) {
    		for (DataType<?> t : ps.getPtypes()) {
    			types[i++] = t;
    		}
    	}
    	return types;
    }

    /**
     * returns set of unique data values of some type in a data word.
     *
     * @param
     * @param word
     * @param t
     * @return
     */
    public static <I extends ParameterizedSymbol, T> Set<DataValue<T>> valSet(Word<SymbolInstance<I>> word, DataType<T> t) {
        Set<DataValue<T>> vals = new LinkedHashSet<>();
        for (SymbolInstance<I> psi : word) {
            for (DataValue<?> d : psi.getParameterValues()) {
                if (d.getDataType().equals(t)) {
                    vals.add((DataValue<T>) d);
                }
            }
        }
        return vals;
    }

    /**
     *
     * @param
     * @param in
     * @return
     */
    @SafeVarargs
	public static  Set<DataValue<?>> joinValsToSet(Collection<DataValue<?>> ... in) {
        Set<DataValue<?>> vals = new LinkedHashSet<>();
        for (Collection<DataValue<?>> s : in) {
            vals.addAll(s);
        }
        return vals;
    }

    /**
     * returns set of all unique data values in a data word.
     *
     * @param word
     * @return
     */
    public static <I extends ParameterizedSymbol> Set<DataValue<?>> valSet(Word<? extends SymbolInstance<I>> word) {
        Set<DataValue<?>> valset = new LinkedHashSet<>();
        for (SymbolInstance<I> psi : word) {
            valset.addAll(Arrays.asList(psi.getParameterValues()));
        }
        return valset;
    }

    /**
     * returns sequence of actions in a data word.
     * @param word
     * @return
     */
    public static <I extends ParameterizedSymbol> Word<I> actsOf(
            Word<? extends SymbolInstance<I>> word) {
        I[] symbols = (I[]) new ParameterizedSymbol[word.length()];
        int idx = 0;
        for (SymbolInstance<I> psi : word) {
            symbols[idx++] = psi.getBaseSymbol();
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * instantiates a data word from a sequence of actions and
     * a valuation.
     *
     * @param actions
     * @param dataValues
     * @return
     */
    public static <I extends ParameterizedSymbol> Word<SymbolInstance<I>> instantiate(
            Word<I> actions,
            List<DataValue<?>> dataValues) {

        SymbolInstance<I>[] symbols = new SymbolInstance[actions.length()];
        int idx = 0;
        int pid = 0;
        for (I ps : actions) {
            DataValue[] pvalues = new DataValue[ps.getArity()];
            for (int i = 0; i < ps.getArity(); i++) {
                pvalues[i] = dataValues.get(pid++);
            }
            symbols[idx++] = new SymbolInstance<>(ps, pvalues);
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * instantiates a data word from a sequence of actions and
     * a valuation.
     *
     * @param actions
     * @param dataValues
     * @return
     */
    public static <I extends ParameterizedSymbol> Word<SymbolInstance<I>> instantiate(
            Word<I> actions, DataValue<?>[] dataValues) {

        SymbolInstance<I>[] symbols = new SymbolInstance[actions.length()];
        int idx = 0;
        int pid = 1;
        for (I ps : actions) {
            DataValue<?>[] pvalues = new DataValue[ps.getArity()];
            for (int i = 0; i < ps.getArity(); i++) {
                pvalues[i] = dataValues[pid++ -1];
            }
            symbols[idx++] = new SymbolInstance<>(ps, pvalues);
        }
        return Word.fromSymbols(symbols);
    }

    /**
     * returns the number of data values in a sequence of actions.
     *
     * @param word
     * @return
     */
    public static int paramLength(Word<? extends ParameterizedSymbol> word) {
        int length = 0;
        for (ParameterizedSymbol psi : word) {
            length += psi.getArity();
        }
        return length;
    }

    /**
     * Returns the number of data values in a sequence of symbols
     *
     * @param word
     * @return
     */
    public static int paramValLength(Word<? extends SymbolInstance<?>> word) {
        int length = 0;
        for (SymbolInstance<?> psi : word) {
            length += psi.getParameterValues().length;
        }
        return length;
    }

}
