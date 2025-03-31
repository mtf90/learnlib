package de.learnlib.theory.restriction;

import de.learnlib.data.DataWords;
import de.learnlib.data.SDTGuard;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueGenerator;
import de.learnlib.data.SuffixValueRestriction;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.data.UnrestrictedSuffixValue;
import de.learnlib.theory.guard.DisequalityGuard;
import de.learnlib.theory.guard.EqualityGuard;
import de.learnlib.theory.guard.TrueGuard;
import java.util.LinkedHashMap;
import java.util.Map;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.GuardElement;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class GenericSuffixRestrictionBuilderImpl implements SymbolicSuffixRestrictionBuilder {

    private final Constants consts;

    public GenericSuffixRestrictionBuilderImpl(Constants consts) {
        this.consts = consts;
    }

    @Override
    public <I extends ParameterizedSymbol> Map<SuffixValue<?>, SuffixValueRestriction> restrictSuffix(Word<? extends SymbolInstance<I>> prefix,
                                                                                                      Word<? extends SymbolInstance<I>> suffix) {
        DataType[] types = DataWords.typesOf(DataWords.actsOf(suffix));
        Map<SuffixValue<?>, SuffixValueRestriction> restrictions = new LinkedHashMap<>();
        SuffixValueGenerator svgen = new SuffixValueGenerator();
        for (DataType t : types) {
            SuffixValue sv = svgen.next(t);
            SuffixValueRestriction restr = genericRestriction(sv, prefix, suffix, consts);
            restrictions.put(sv, restr);
        }
        return restrictions;
    }

    @Override
    public SuffixValueRestriction restrictSuffixValue(SDTGuard guard, Map<SuffixValue, SuffixValueRestriction> prior) {
        return genericRestriction(guard, prior);
    }

    /**
     * Generate a generic restriction using Fresh, Unrestricted and Equal restriction types
     *
     * @param sv
     * @param prefix
     * @param suffix
     * @param consts
     *
     * @return
     */
    public static <I extends ParameterizedSymbol> SuffixValueRestriction genericRestriction(SuffixValue sv,
                                                                                            Word<? extends SymbolInstance<I>> prefix,
                                                                                            Word<? extends SymbolInstance<I>> suffix,
                                                                                            Constants consts) {
        DataValue[] prefixVals = DataWords.valsOf(prefix);
        DataValue[] suffixVals = DataWords.valsOf(suffix);
        DataType[] prefixTypes = DataWords.typesOf(DataWords.actsOf(prefix));
        DataType[] suffixTypes = DataWords.typesOf(DataWords.actsOf(suffix));
        DataValue val = suffixVals[sv.getId() - 1];
        int firstSymbolArity = suffix.length() > 0 ? suffix.getSymbol(0).getBaseSymbol().getArity() : 0;

        boolean unrestricted = false;
        for (int i = 0; i < prefixVals.length; i++) {
            DataValue dv = prefixVals[i];
            DataType dt = prefixTypes[i];
            if (dt.equals(sv.getDataType()) && dv.equals(val)) {
                unrestricted = true;
                break;
            }
        }
        if (consts.containsValue(val)) {
            unrestricted = true;
        }
        boolean equalsSuffixValue = false;
        int equalSV = -1;
        for (int i = 0; i < sv.getId() - 1 && !equalsSuffixValue; i++) {
            DataType dt = suffixTypes[i];
            if (dt.equals(sv.getDataType()) && suffixVals[i].equals(val)) {
                if (sv.getId() <= firstSymbolArity) {
                    unrestricted = true;
                } else {
                    equalsSuffixValue = true;
                    equalSV = i;
                }
            }
        }

        // case equal to previous suffix value
        if (equalsSuffixValue && !unrestricted) {
            SuffixValueRestriction restr =
                    new EqualRestriction(sv, new SuffixValue(suffixVals[equalSV].getDataType(), equalSV + 1));
            return restr;
        }
        // case fresh
        else if (!equalsSuffixValue && !unrestricted) {
            return new FreshSuffixValue(sv);
        }
        // case unrestricted
        else {
            return new UnrestrictedSuffixValue(sv);
        }
    }

    public static SuffixValueRestriction genericRestriction(SDTGuard guard,
                                                            Map<SuffixValue, SuffixValueRestriction> prior) {
        SuffixValue suffixValue = guard.getParameter();
        // case fresh
        if (guard instanceof TrueGuard || guard instanceof DisequalityGuard) {
            return new FreshSuffixValue(suffixValue);
            // case equal to previous suffix value
        } else if (guard instanceof EqualityGuard) {
            GuardElement param = ((EqualityGuard) guard).getRegister();
            if (param instanceof SuffixValue) {
                SuffixValueRestriction restr = prior.get(param);
                if (restr instanceof FreshSuffixValue) {
                    return new EqualRestriction(suffixValue, (SuffixValue) param);
                } else if (restr instanceof EqualRestriction) {
                    return new EqualRestriction(suffixValue, ((EqualRestriction) restr).getEqualParameter());
                } else {
                    return new UnrestrictedSuffixValue(suffixValue);
                }
            } else {
                return new UnrestrictedSuffixValue(suffixValue);
            }
            // case unrestricted
        } else {
            return new UnrestrictedSuffixValue(suffixValue);
        }
    }

}
