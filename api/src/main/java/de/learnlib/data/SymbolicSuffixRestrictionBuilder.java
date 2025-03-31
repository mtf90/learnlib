package de.learnlib.data;

import java.util.Map;

import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public interface SymbolicSuffixRestrictionBuilder {

    public <I extends ParameterizedSymbol> Map<SuffixValue<?>, SuffixValueRestriction> restrictSuffix(Word<? extends SymbolInstance<I>> prefix, Word<? extends SymbolInstance<I>> suffix);

//    /**
//     * Generate a generic restriction using Fresh, Unrestricted and Equal restriction types
//     *
//     * @param sv
//     * @param prefix
//     * @param suffix
//     * @param consts
//     * @return
//     */
//    public <I extends ParameterizedSymbol> SuffixValueRestriction genericRestriction(SuffixValue sv, Word<? extends SymbolInstance<I>> prefix, Word<? extends SymbolInstance<I>> suffix, Constants consts);

//    public SuffixValueRestriction genericRestriction(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior);

    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior);

}
