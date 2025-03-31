/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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
package de.learnlib.algorithm.register.rastar;

import de.learnlib.algorithm.register.PrefixContainer;
import de.learnlib.data.Memorables;
import de.learnlib.data.RegisterAssignment;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTRelabeling;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValueGenerator;
import net.automatalib.data.SymbolicDataValueGenerator.RegisterGenerator;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A row in an observation table.
 *
 * @author falk
 */
public class Row<I extends ParameterizedSymbol> implements PrefixContainer<I> {

    private final Word<SymbolInstance<I>> prefix;

    private final Map<SymbolicSuffix<I>, Cell<I>> cells;

    private final RegisterGenerator regGen = new RegisterGenerator();

    private static final Logger LOGGER = LoggerFactory.getLogger(Row.class);

    private Row(Word<SymbolInstance<I>> prefix) {
        this.prefix = prefix;
        this.cells = new LinkedHashMap<>();
    }

    void addSuffix(SymbolicSuffix<I> suffix, TreeOracle<I> oracle) {
        Cell<I> c = Cell.computeCell(oracle, prefix, suffix);
        addCell(c);
    }

    private void addCell(Cell<I> c) {
        assert c.getPrefix().equals(this.prefix);
        assert !this.cells.containsKey(c.getSuffix());
        this.cells.put(c.getSuffix(), c);
    }

    SymbolicSuffix<I> getSuffixForMemorable(DataValue<?> d) {
        return cells.entrySet().stream()
                .filter(e -> e.getValue().getMemorableValues().contains(d))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("This line is not supposed to be reached."))
                .getKey();
    }

    SDT[] getSDTsForInitialSymbol(ParameterizedSymbol ps) {
        List<SDT> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix<I>, Cell<I>> c : cells.entrySet()) {
            Word<I> acts = c.getKey().getActions();
            if (!acts.isEmpty() && acts.firstSymbol().equals(ps)) {
//                System.out.println("Using " + c.getKey() + " for branching of " + ps + " after " + prefix);
                sdts.add(c.getValue().getSDT());
            }
        }
        return sdts.toArray(new SDT[]{});
    }

    public Set<DataValue<?>> memorableValues() {
        return cells.values().stream()
                .flatMap(c -> c.getMemorableValues().stream() )
                .collect(Collectors.toSet());
    }

    @Override
    public RegisterAssignment getAssignment() {
        RegisterAssignment ra = new RegisterAssignment();
        SymbolicDataValueGenerator.RegisterGenerator regGen =
                new SymbolicDataValueGenerator.RegisterGenerator();

        this.memorableValues().forEach(
                dv -> ra.put(dv, regGen.next(dv.getDataType()))
        );

        return ra;
    }

    @Override
    public Word<SymbolInstance<I>> getPrefix() {
        return this.prefix;
    }

    /**
     * checks rows for equality (disregarding of the prefix!). It is assumed
     * that both rows have the same set of suffixes.
     *
     * @param other
     * @return true if rows are equal
     */
    boolean isEquivalentTo(Row<I> other, SDTRelabeling renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        if (!Memorables.relabel(this.memorableValues(), renaming).equals(other.memorableValues())) {
            return false;
        }

        for (Entry<SymbolicSuffix<I>, Cell<I>> entry : this.cells.entrySet()) {
            Cell<I> c1 = entry.getValue();
            Cell<I> c2 = other.cells.get(entry.getKey());

            if (!c1.isEquivalentTo(c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param other
     * @return
     */
    boolean couldBeEquivalentTo(Row<I> other) {
        if (!Memorables.typedSize(this.memorableValues()).equals(Memorables.typedSize(other.memorableValues()))) {
            return false;
        }

        for (Entry<SymbolicSuffix<I>, Cell<I>> entry : this.cells.entrySet()) {
            Cell<I> c1 = entry.getValue();
            Cell<I> c2 = other.cells.get(entry.getKey());

            if (!c1.couldBeEquivalentTo(c2)) {
                return false;
            }
        }
        return true;
    }

    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     *
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return
     */
    static <I extends ParameterizedSymbol> Row<I> computeRow(TreeOracle<I> oracle,
            Word<SymbolInstance<I>> prefix, List<SymbolicSuffix<I>> suffixes) {

        Row<I> r = new Row<>(prefix);
        for (SymbolicSuffix<I> s : suffixes) {
            r.addCell(Cell.computeCell(oracle, prefix, s));
        }
        return r;
    }

    boolean isAccepting() {
        Cell<I> c = this.cells.get(RaStar.EMPTY_SUFFIX);
        return c.isAccepting();
    }

    @Override
    public String toString() {
        return this.prefix.toString();
    }

    void toString(StringBuilder sb) {
        sb.append("****** ROW: ").append(prefix).append("\n");
        for (Entry<SymbolicSuffix<I>, Cell<I>> c : this.cells.entrySet()) {
            c.getValue().toString(sb);
        }
    }

}
