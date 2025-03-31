package de.learnlib.data;

import java.util.List;
import net.automatalib.symbol.data.ParameterizedSymbol;

public interface SuffixSymbol<I extends ParameterizedSymbol> {

    I getAction();

    List<SuffixValueRestriction> getParameterRestrictions();
}
