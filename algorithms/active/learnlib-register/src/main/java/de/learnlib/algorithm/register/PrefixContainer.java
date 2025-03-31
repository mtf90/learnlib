package de.learnlib.algorithm.register;

import de.learnlib.data.RegisterAssignment;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public interface PrefixContainer<I extends ParameterizedSymbol> {
	Word<SymbolInstance<I>> getPrefix();
	RegisterAssignment getAssignment();
}
