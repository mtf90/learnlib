package de.learnlib.data;

import net.automatalib.data.DataType;
import net.automatalib.data.SymbolicDataValue;

/**
 * a parameter in a suffix or SDT guard
 */
public class SuffixValue<T> extends SymbolicDataValue<T> {

    public SuffixValue(DataType<T> dataType, int id) {
        super(dataType, "s", id);
    }

    @Override
    public SymbolicDataValue<T> copy() {
        return new SuffixValue<>(type, id);
    }
}
