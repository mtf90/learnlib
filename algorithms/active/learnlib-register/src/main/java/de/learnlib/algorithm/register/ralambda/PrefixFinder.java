package de.learnlib.algorithm.register.ralambda;

import de.learnlib.algorithm.register.CEAnalysisResult;
import de.learnlib.data.Memorables;
import de.learnlib.algorithm.register.RemappingIterator;
import de.learnlib.data.SMTUtil;
import de.learnlib.algorithm.register.rastar.Hypothesis;
import de.learnlib.data.Bijection;
import de.learnlib.data.SDT;
import de.learnlib.data.SuffixValue;
import de.learnlib.data.SuffixValueGenerator;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.logging.Category;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.query.DefaultQuery;
import gov.nasa.jpf.constraints.api.Expression;
import gov.nasa.jpf.constraints.util.ExpressionUtil;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.automatalib.data.Constants;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;
import net.automatalib.data.Mapping;
import net.automatalib.data.ParameterValuation;
import net.automatalib.data.SymbolicDataValue;
import net.automatalib.data.SymbolicDataValue.Parameter;
import net.automatalib.data.SymbolicDataValueGenerator.ParameterGenerator;
import net.automatalib.data.VarMapping;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrefixFinder<I extends ParameterizedSymbol> {

    private final TreeOracle<I> sulOracle;

    private TreeOracle<I> hypOracle;

    private Hypothesis<I> hypothesis;

    private final SDTLogicOracle sdtOracle;

    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;

    private final Constants consts;

    private SymbolicWord[] candidates;

    private final Map<SymbolicWord, SDT> candidateCEs = new LinkedHashMap<SymbolicWord, SDT>();
    private final Map<SymbolicWord, SDT> storedQueries = new LinkedHashMap<SymbolicWord, SDT>();

    private static final Logger LOGGER = LoggerFactory.getLogger(PrefixFinder.class);

    public PrefixFinder(TreeOracle sulOracle, TreeOracle hypOracle,
                        Hypothesis hypothesis, SDTLogicOracle sdtOracle,
                        Constants consts) {

        this.sulOracle = sulOracle;
        this.hypOracle = hypOracle;
        this.hypothesis = hypothesis;
        this.sdtOracle = sdtOracle;
        this.consts = consts;
        this.restrictionBuilder = sulOracle.getRestrictionBuilder();
    }

    public CEAnalysisResult analyzeCounterexample(Word<SymbolInstance<I>> ce) {
		int idx = findIndex(ce);
        SymbolicWord sw = new SymbolicWord(candidates[idx].getPrefix(), candidates[idx].getSuffix());
        SDT tqr = null; //storedQueries.get(sw);
        if (tqr == null) {
        	// THIS CAN (possibly) BE DONE WITHOUT A NEW TREE QUERY
        	tqr = sulOracle.treeQuery(sw.getPrefix(), sw.getSuffix());
        }
        CEAnalysisResult result = new CEAnalysisResult(candidates[idx].getPrefix(),
        		                                       candidates[idx].getSuffix(),
        		                                       tqr);

        candidateCEs.put(candidates[idx], tqr);
        storeCandidateCEs(ce, idx);

        return result;
    }

	private int findIndex(Word<SymbolInstance<I>> ce) {
		candidates = new SymbolicWord[ce.length()];
		int max = ce.length() - 1;
		for (int idx=max; idx>=0; idx = idx-1) {

			Word<SymbolInstance<I>> prefix = ce.prefix(idx);
			Word<SymbolInstance<I>> nextPrefix = ce.prefix(idx+1);

			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ce:     {}", idx, ce);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} prefix: {}", idx, prefix);
			LOGGER.trace(Category.DATASTRUCTURE, "idx: {} next:   {}", idx, nextPrefix);

			// check for location counterexample ...
			//
			Word<SymbolInstance<I>> suffix = ce.suffix(ce.length() - nextPrefix.length());
			SymbolicSuffix symSuffix = new SymbolicSuffix(nextPrefix, suffix, restrictionBuilder);
			LOC_CHECK: for (Word<SymbolInstance<I>> u : hypothesis.possibleAccessSequences(prefix)) {
				Word<SymbolInstance<I>> uAlpha = hypothesis.transformTransitionSequence(nextPrefix, u);
				SDT uAlphaResult = sulOracle.treeQuery(uAlpha, symSuffix);
				storedQueries.put(new SymbolicWord(uAlpha, symSuffix), uAlphaResult);

				// check if the word is inequivalent to all access sequences
				//
				for (Word<SymbolInstance<I>> uPrime : hypothesis.possibleAccessSequences(nextPrefix)) {
					SDT uPrimeResult = sulOracle.treeQuery(uPrime, symSuffix);
					storedQueries.put(new SymbolicWord(uPrime, symSuffix), uPrimeResult);

					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u:  {}", idx, u);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} ua: {}", idx, uAlpha);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} u': {}", idx, uPrime);
					LOGGER.trace(Category.DATASTRUCTURE, "idx: {} v:  {}", idx, symSuffix);

					// different sizes
					//
					if (!Memorables.typedSize(uPrimeResult.getDataValues()).equals(
							Memorables.typedSize(uAlphaResult.getDataValues()))) {
						continue;
					}

					// remapping
					//
					RemappingIterator<DataValue<?>> iterator = new RemappingIterator<>(
							uPrimeResult.getDataValues(), uAlphaResult.getDataValues());

					for (Bijection<DataValue<?>> m : iterator) {
						if (uAlphaResult.isEquivalent(uPrimeResult, m)) {
							continue LOC_CHECK;
						}
					}

				}
				// found a counterexample!
				candidates[idx] = new SymbolicWord(uAlpha, symSuffix);
				LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for location");
				return idx;
			}

			// check for transition counterexample ...
			//
			if (transitionHasCE(ce, idx-1)) {
				LOGGER.trace(Category.COUNTEREXAMPLE, "Counterexample for transition");
				return idx;
			}
		}
		throw new RuntimeException("should not reach here");
	}

//    private Pair<SDT, SDT> checkForCE(Word<SymbolInstance<I>> prefix, SymbolicSuffix suffix, Word<SymbolInstance<I>> transition) {
//    	SymbolicWord symWord = new SymbolicWord(prefix, suffix);
//    	SDT resHyp = hypOracle.treeQuery(prefix, suffix);
//    	SDT resSul;
//    	if (storedQueries.containsKey(symWord))
//    		resSul = storedQueries.get(symWord);
//    	else {
//    		resSul = sulOracle.treeQuery(prefix, suffix);
//    		storedQueries.put(symWord, resSul);
//    	}
//
//        boolean hasCE = sdtOracle.hasCounterexample(prefix,
//                resHyp.getSdt(), resHyp.getPiv(),
//                resSul.getSdt(), resSul.getPiv(),
//                new TransitionGuard(), transition);
//
//        return hasCE ? new ImmutablePair<SDT, SDT>(resHyp, resSul) : null;
//    }

    private boolean transitionHasCE(Word<SymbolInstance<I>> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return false;

    	Word<SymbolInstance<I>> prefix = ce.prefix(idx+1);

    	Word<SymbolInstance<I>> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

    	Set<Word<SymbolInstance<I>>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<SymbolInstance<I>> location : locations) {
	        Word<SymbolInstance<I>> transition = hypothesis.transformTransitionSequence(
	                ce.prefix(idx+2), location);

    		SDT resHyp = hypOracle.treeQuery(location, symSuffix);
    		SDT resSul;
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		if (storedQueries.containsKey(symWord))
    			resSul = storedQueries.get(symWord);
    		else {
    			resSul = sulOracle.treeQuery(location, symSuffix);
    			storedQueries.put(symWord, resSul);
    		}

    		boolean hasCE = sdtOracle.hasCounterexample(location,
	                resHyp,
	                resSul,
					ExpressionUtil.TRUE, transition);

    		if (hasCE) {
				SymbolicWord sw = candidate(location, symSuffix, resSul, resHyp, ce);
				// new by falk
				candidates[idx+1] = sw;
				return true;
			}
    	}
    	return false;
    }

    private void storeCandidateCEs(Word<SymbolInstance<I>> ce, int idx) {
    	if (idx+1 >= ce.length())
    		return;
    	Word<SymbolInstance<I>> prefix = ce.prefix(idx+1);

    	Word<SymbolInstance<I>> suffix = ce.suffix(ce.length() - (idx+1));
    	SymbolicSuffix symSuffix = new SymbolicSuffix(prefix, suffix, restrictionBuilder);

    	Set<Word<SymbolInstance<I>>> locations = hypothesis.possibleAccessSequences(prefix);
    	for (Word<SymbolInstance<I>> location : locations) {
    		SymbolicWord symWord = new SymbolicWord(location, symSuffix);
    		SDT tqr = storedQueries.get(symWord);

    		assert tqr != null;

    		candidateCEs.put(symWord, tqr);
    	}
    }

    private SymbolicWord candidate(Word<SymbolInstance<I>> prefix,
                                   SymbolicSuffix<I> symSuffix, SDT sdtSul,
                                   SDT sdtHyp, Word<SymbolInstance<I>> ce) {
    	Word<SymbolInstance<I>> candidate = null;

    	Expression<Boolean> expr = sdtOracle.getCEGuard(prefix, sdtSul, sdtHyp);

        Map<Word<SymbolInstance<I>>, Boolean> sulPaths = sulOracle.instantiate(prefix, symSuffix, sdtSul);
        for (Word<SymbolInstance<I>> path : sulPaths.keySet()) {
        	ParameterGenerator parGen = new ParameterGenerator();
        	for (SymbolInstance<I> psi : prefix) {
        		for (DataType dt : psi.getBaseSymbol().getPtypes())
        			parGen.next(dt);
        	}

        	VarMapping<SuffixValue<I>, Parameter<?>> renaming = new VarMapping<>();
        	SuffixValueGenerator svGen = new SuffixValueGenerator();
        	for (I ps : symSuffix.getActions()) {
        		for (DataType dt : ps.getPtypes()) {
        			Parameter p = parGen.next(dt);
        			SuffixValue sv = svGen.next(dt);
        			renaming.put(sv, p);
        		}
        	}
        	Expression<Boolean> exprR = SMTUtil.renameVars(expr, renaming);

        	ParameterValuation pars = new ParameterValuation(path);
        	Mapping<SymbolicDataValue, DataValue> vals = new Mapping<>();
        	vals.putAll(pars);
        	vals.putAll(consts);

        	if (exprR.evaluateSMT(SMTUtil.compose(vals))) {
        		candidate = path.prefix(prefix.length() + 1);
        		SymbolicSuffix suffix = new SymbolicSuffix(candidate, ce.suffix(symSuffix.length() - 1), restrictionBuilder);
        		return new SymbolicWord(candidate, suffix);
        	}
        }
        throw new IllegalStateException("No CE transition found");
    }

    public Set<DefaultQuery<SymbolInstance<I>, Boolean>> getCounterExamples() {
    	Set<DefaultQuery<SymbolInstance<I>, Boolean>> ces = new LinkedHashSet<DefaultQuery<SymbolInstance<I>, Boolean>>();
    	for (Map.Entry<SymbolicWord, SDT> e : candidateCEs.entrySet()) {
    		SymbolicWord sw = e.getKey();
    		SDT tqr = e.getValue();
    		Map<Word<SymbolInstance<I>>, Boolean> cemaps = sulOracle.instantiate(sw.getPrefix(), sw.getSuffix(), tqr);
    		for (Map.Entry<Word<SymbolInstance<I>>, Boolean> c : cemaps.entrySet()) {
    			ces.add(new DefaultQuery<SymbolInstance<I>, Boolean>(c.getKey(), c.getValue()));
    		}
    	}

    	return ces;
    }

    public void setHypothesisTreeOracle(TreeOracle hypOracle) {
        this.hypOracle = hypOracle;
    }

    public void setHypothesis(Hypothesis hyp) {
    	this.hypothesis = hyp;
    }

    //public void setComponents(Map<Word<SymbolInstance<I>>, LocationComponent> components) {
    //    this.components = components;
    //}
}
