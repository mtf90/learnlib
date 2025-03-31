package de.learnlib.algorithm.register.radt;

import de.learnlib.data.SMTUtil;
import de.learnlib.algorithm.register.rastar.Hypothesis;
import de.learnlib.data.Branching;
import de.learnlib.data.RegisterAssignment;
import gov.nasa.jpf.constraints.api.Expression;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.Util;
import net.automatalib.automaton.ra.impl.CompactRATransition;
import net.automatalib.common.util.Pair;
import net.automatalib.data.Constants;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.RegisterValuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;

public class DTHyp<I extends ParameterizedSymbol> extends Hypothesis<I> {

    private final Alphabet<I> alphabet;
    private final DT<I> dt;

    public DTHyp(Alphabet<I> alphabet, DT<I> dt, Constants consts) {
        super(alphabet, consts);
        this.alphabet = alphabet;
        this.dt = dt;
    }

//    @Override
//    public boolean accepts(Word<SymbolInstance<I>> word) {
//        Word<SymbolInstance<I>> as = transformAccessSequence(word);
//        DTLeaf l = dt.getLeaf(as);
//        assert l != null;
//        return l.isAccepting();
//    }

    @Override
    public boolean isAccessSequence(Word<SymbolInstance<I>> word) {
        if (super.isAccessSequence(word))
            return true;
        DTLeaf leaf = dt.getLeaf(word);
        if (leaf == null)
            return false;
        return leaf.getAccessSequence().equals(word) ||
	       leaf.getShortPrefixes().contains(word);
    }

    @Override
    public Word<SymbolInstance<I>> transformAccessSequence(Word<SymbolInstance<I>> word) {
    	List<Word<SymbolInstance<I>>> tseq = getDTTransitions(word);
        if (tseq == null) {
            return null;
        }
        if (tseq.isEmpty()) {
            return Word.epsilon();
        } else {
            return dt.getLeaf(tseq.get(tseq.size() - 1)).getAccessSequence();
        }
    }

    @Override
    public Set<Word<SymbolInstance<I>>> possibleAccessSequences(Word<SymbolInstance<I>> word) {
        Set<Word<SymbolInstance<I>>> ret = new LinkedHashSet<>();
        Word<SymbolInstance<I>> as = transformAccessSequence(word);
        ret.add(as);

        DTLeaf<I> leaf = dt.getLeaf(as);
        assert leaf != null;
        for (MappedPrefix<I> mp : leaf.getShortPrefixes().get())
            ret.add(mp.getPrefix());
        return ret;
    }

    protected List<Word<SymbolInstance<I>>> getDTTransitions(Word<SymbolInstance<I>> dw) {
        RegisterValuation vars = new RegisterValuation(getInitialRegisters());
        DTLeaf current = dt.getLeaf(Word.epsilon());
        List<Word<SymbolInstance<I>>> tseq = new ArrayList<>();
        for (SymbolInstance<I> psi : dw) {
            ParameterValuation pars = new ParameterValuation(psi);

            Map<Word<SymbolInstance<I>>, Expression<Boolean>> candidates =
                current.getBranching(psi.getBaseSymbol()).getBranches();

            if (candidates == null) {
                return null;
            }

            RegisterAssignment ra = current.getPrimePrefix().getAssignment();

            boolean found = false;
            for (Map.Entry<Word<SymbolInstance<I>>, Expression<Boolean>> e : candidates.entrySet()) {
                Expression<Boolean> g = e.getValue();
                g = SMTUtil.valsToRegisters(g, ra);
                if (g.evaluateSMT(Util.compose(vars, pars, super.getConstants()))) {
                    Word<SymbolInstance<I>> w = e.getKey();
                    vars = current.getAssignment(w, dt.getLeaf(w)).compute(vars, pars, super.getConstants());
                    current = dt.getLeaf(w);
                    tseq.add(w);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }
        return tseq;
    }

    @Override
    public Word<SymbolInstance<I>> transformTransitionSequence(Word<SymbolInstance<I>> word) {
        List<Word<SymbolInstance<I>>> tseq = getDTTransitions(word);
        if (tseq == null)
            return dt.getLeaf(word).getAccessSequence();
        assert tseq.size() == word.size();
        return tseq.get(tseq.size() - 1);
    }

    @Override
    public Word<SymbolInstance<I>> transformTransitionSequence(Word<SymbolInstance<I>> word,
			Word<SymbolInstance<I>> location) {
        Word<SymbolInstance<I>> suffix = word.suffix(1);

        DTLeaf<I> leaf = dt.getLeaf(location);
        assert leaf != null;
        assert leaf.getAccessSequence().equals(location) || leaf.getShortPrefixes().contains(location);

        if (leaf.getAccessSequence().equals(location)) {
            Word<SymbolInstance<I>> tseq = transformTransitionSequence(word);
            //System.out.println("TSEQ: " + tseq);
            if (tseq == null) {
                ParameterizedSymbol ps = suffix.firstSymbol().getBaseSymbol();
                for (Word<SymbolInstance<I>> p : leaf.getBranching(ps).getBranches().keySet()) {
                    DTLeaf<I> l = dt.getLeaf(p);
                    if (l != null && l == dt.getSink())
                        return p;
                }
            }
            return tseq;
        }

        ParameterizedSymbol ps = suffix.firstSymbol().getBaseSymbol();

        ShortPrefix sp = (ShortPrefix)leaf.getShortPrefixes().get(location);
        Word<SymbolInstance<I>> ret = branchWithSameGuard(word, sp.getBranching(ps));
        assert ret != null;
        return ret;
    }

    public Word<SymbolInstance<I>> branchWithSameGuard(Word<SymbolInstance<I>> word, Branching<I> branching) {
        ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();

        List<Pair<CompactRATransition, RegisterValuation>> tvseq = getTransitionsAndValuations(word);
        RegisterValuation vars = tvseq.get(tvseq.size() - 1).getSecond();
        ParameterValuation pval = new ParameterValuation(word.lastSymbol());

        for (Map.Entry<Word<SymbolInstance<I>>, Expression<Boolean>> e : branching.getBranches().entrySet()) {
            if (e.getKey().lastSymbol().getBaseSymbol().equals(ps)) {
                Word<SymbolInstance<I>> prefix = e.getKey().prefix(e.getKey().size() - 1);
                RegisterValuation varsRef =
                        getTransitionsAndValuations(prefix).get(getTransitionsAndValuations(prefix).size() - 1)
                                                           .getSecond();
                // System.out.println(varsRef);
                RegisterAssignment ra = new RegisterAssignment();
                varsRef.forEach((key, value) -> ra.put(value, key));
                Expression<Boolean> guard = SMTUtil.valsToRegisters(e.getValue(), ra);
                if (guard.evaluateSMT(Util.compose(vars, pval, getConstants()))) {
                    return e.getKey();
                }
            }
        }
        return null;
    }

    private List<Pair<CompactRATransition,RegisterValuation>> getTransitionsAndValuations(Word<SymbolInstance<I>> dw) {
        RegisterValuation vars = new RegisterValuation(getInitialRegisters());
        Integer current = getInitialState();
        List<Pair<CompactRATransition,RegisterValuation>> tvseq = new ArrayList<>();
        Constants constants = getConstants();
        for (SymbolInstance<I> psi : dw) {

            ParameterValuation pars = new ParameterValuation(psi);

            Collection<CompactRATransition> candidates = getTransitions(current, psi.getBaseSymbol());

            if (candidates == null) {
                return null;
            }

            boolean found = false;
            for (CompactRATransition t : candidates) {
                if (t.isEnabled(vars, pars, constants)) {
                    vars = t.execute(vars, pars, constants);
                    current = getSuccessor(t);
                    tvseq.add(Pair.of(t, new RegisterValuation(vars)));
                    found = true;
                    break;
                }
            }

            if (!found) {
                return null;
            }
        }
        return tvseq;
    }

    public Word<SymbolInstance<I>> branchWithSameGuard(Word<SymbolInstance<I>> word, MappedPrefix src_id, Branching branching) {
        return branching.transformPrefix(word);
    }
}
