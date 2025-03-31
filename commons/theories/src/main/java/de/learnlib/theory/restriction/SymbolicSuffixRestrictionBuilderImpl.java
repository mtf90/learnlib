package de.learnlib.theory.restriction;

import de.learnlib.data.DataWords;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueGenerator;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.theory.Theories;
import de.learnlib.theory.Theory;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class SymbolicSuffixRestrictionBuilderImpl implements SymbolicSuffixRestrictionBuilder {

    private final Theories teachers;

    private final Constants consts;

    public SymbolicSuffixRestrictionBuilderImpl(Constants consts, Theories teachers) {
        this.consts = consts;
        this.teachers = teachers;
    }

    @Override
    public <I extends ParameterizedSymbol> Map<SuffixValue<?>, SuffixValueRestriction> restrictSuffix(Word<? extends SymbolInstance<I>> prefix,
                                                                                                      Word<? extends SymbolInstance<I>> suffix) {
        DataType[] types = DataWords.typesOf(DataWords.actsOf(suffix));
        Map<SuffixValue<?>, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
        SuffixValueGenerator svgen = new SuffixValueGenerator();
        for (DataType t : types) {
            SuffixValue sv = svgen.next(t);
            // theory-specific restrictions
            Theory theory = teachers.get(t);
            SuffixValueRestriction restr = theory.restrictSuffixValue(sv, prefix, suffix, consts);
            restrictions.put(sv, restr);
        }
        return restrictions;
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
        Theory theory = teachers.get(guard.getParameter().getDataType());
        return theory.restrictSuffixValue(guard, prior);
    }

}
