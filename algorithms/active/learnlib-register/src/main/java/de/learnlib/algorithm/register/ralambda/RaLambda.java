package de.learnlib.algorithm.register.ralambda;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.register.AutomatonBuilder;
import de.learnlib.algorithm.register.CEAnalysisResult;
import de.learnlib.algorithm.register.SDTUtil;
import de.learnlib.algorithm.register.radt.DT;
import de.learnlib.algorithm.register.radt.DTHyp;
import de.learnlib.algorithm.register.radt.DTLeaf;
import de.learnlib.algorithm.register.radt.MappedPrefix;
import de.learnlib.algorithm.register.radt.OptimizedSymbolicSuffixBuilder;
import de.learnlib.algorithm.register.radt.ShortPrefix;
import de.learnlib.algorithm.register.rastar.Hypothesis;
import de.learnlib.data.Bijection;
import de.learnlib.data.Branching;
import de.learnlib.algorithm.register.LocationComponent;
import de.learnlib.data.Configuration;
import de.learnlib.data.SDT;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.logging.Category;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.query.DefaultQuery;
import gov.nasa.jpf.constraints.api.ConstraintSolver;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaLambda<I extends ParameterizedSymbol>
		implements LearningAlgorithm<RegisterAutomaton<?, I, ?>, SymbolInstance<I>, Boolean> {

	private static final Logger LOGGER = LoggerFactory.getLogger(RaLambda.class);

	private final Alphabet<I> alphabet;
	private final TreeOracle<I> sulOracle;
	private final Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory;
	private final SDTLogicOracle sdtLogicOracle;
	private final Constants consts;
	private final ConstraintSolver solver;

    private final DT<I> dt;
	private DTHyp<I> hyp;

    private final Deque<DefaultQuery<SymbolInstance<I>, Boolean>> counterexamples = new LinkedList<>();
    private final Deque<DefaultQuery<SymbolInstance<I>, Boolean>> candidateCEs = new LinkedList<>();

    private final OptimizedSymbolicSuffixBuilder suffixBuilder;
    private final SymbolicSuffixRestrictionBuilder restrictionBuilder;
    private PrefixFinder<I> prefixFinder = null;

	public RaLambda(Alphabet<I> alphabet, Configuration<I> config) {
		this(alphabet, config.getConstants(), config.getTreeOracle(), config::getHypothesisOracle, config.getLogicOracle(), config.getSolver());
	}

	public RaLambda(Alphabet<I> alphabet, Constants consts, TreeOracle<I> oracle,
					Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory,
					SDTLogicOracle sdtLogicOracle, ConstraintSolver solver) {
		this(alphabet, consts, oracle, hypOracleFactory, sdtLogicOracle, oracle.getRestrictionBuilder(), solver);
	}

	public RaLambda(Alphabet<I> alphabet, Constants consts, TreeOracle<I> oracle,
					Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory,
					SDTLogicOracle sdtLogicOracle, SymbolicSuffixRestrictionBuilder restrictionBuilder, ConstraintSolver solver) {
		this.alphabet = alphabet;
		this.consts = consts;
		this.sulOracle = oracle;
		this.hypOracleFactory = hypOracleFactory;
		this.sdtLogicOracle = sdtLogicOracle;
		this.solver = solver;
		this.restrictionBuilder = restrictionBuilder;
		this.suffixBuilder = new OptimizedSymbolicSuffixBuilder<>(consts, restrictionBuilder);
		this.dt = new DT<>(oracle, false, consts, alphabet);
	}


	@Override
	public void startLearning() {
		this.dt.initialize();
		learn();
	}

	@Override
	public boolean refineHypothesis(DefaultQuery<SymbolInstance<I>, Boolean> ceQuery) {
		LOGGER.debug(Category.EVENT, "adding counterexample: {}", ceQuery);
		counterexamples.add(ceQuery);

		return learn();
	}

    private boolean learn() {

        if (hyp == null) {
            buildNewHypothesis();
        }

		boolean refined = false;

		while (analyzeCounterExample()) {
			refined = true;
		};

		return refined;
    }

    private void buildNewHypothesis() {

        Map<Word<SymbolInstance<I>>, LocationComponent> components = new LinkedHashMap<Word<SymbolInstance<I>>, LocationComponent>();
        components.putAll(dt.getComponents());
        AutomatonBuilder ab = new AutomatonBuilder(alphabet, components, consts, dt);

        hyp = (DTHyp) ab.toRegisterAutomaton();
        if (prefixFinder != null) {
        	prefixFinder.setHypothesis(hyp);
                //prefixFinder.setComponents(components);
        	prefixFinder.setHypothesisTreeOracle(buildHypOracle(hyp));
        }
    }

    private boolean analyzeCounterExample() {
//        if (useOldAnalyzer)
//            return analyzeCounterExampleOld();
        LOGGER.info(Category.PHASE, "Analyzing Counterexample");

        if (candidateCEs.isEmpty()) {
        	prefixFinder = null;
        	if (counterexamples.isEmpty()) {
        		assert noShortPrefixes() || !dt.isMissingParameter();
        		return false;
        	}
        	else {
    			DefaultQuery<SymbolInstance<I>, Boolean> ce = counterexamples.poll();
    			candidateCEs.push(ce);
    		}
        }

        boolean foundce = false;
        DefaultQuery<SymbolInstance<I>, Boolean> ce = null;
        Deque<DefaultQuery<SymbolInstance<I>, Boolean>> ces = new ArrayDeque<DefaultQuery<SymbolInstance<I>, Boolean>>();
        ces.addAll(candidateCEs);
        while(!foundce && !ces.isEmpty()) {
        	ce = ces.poll();
        	boolean hypce = hyp.asAcceptor().accepts(ce.getInput());
        	boolean sulce = ce.getOutput();
            //System.out.println("ce: " + ce + " - " + sulce + " vs. " + hypce);
        	foundce = hypce != sulce;
        }

        if (!foundce) {
        	candidateCEs.clear();
        	return false;
        }

		if (prefixFinder == null) {
			//Map<Word<SymbolInstance<I>>, LocationComponent> components = new LinkedHashMap<Word<SymbolInstance<I>>, LocationComponent>();
			//components.putAll(dt.getComponents());
			prefixFinder = new PrefixFinder<>(sulOracle, buildHypOracle(hyp), hyp, sdtLogicOracle, consts);
		}

        Word<SymbolInstance<I>> ceWord = ce.getInput();
        CEAnalysisResult<I> result = prefixFinder.analyzeCounterexample(ceWord);
        Word<SymbolInstance<I>> transition = result.getPrefix();						// u alpha(d)
        //System.out.println("new prefix: " + transition);

        for (DefaultQuery<SymbolInstance<I>, Boolean> q : prefixFinder.getCounterExamples()) {
        	if (!candidateCEs.contains(q))
        		candidateCEs.addLast(q);
        }


        if (isGuardRefinement(transition)) {
        	addPrefix(transition);
        }
        else {
        	expand(transition);
        }

        while (!dt.checkIOSuffixes());

        boolean consistent = false;
        while (!consistent) {

            consistent = checkLocationConsistency();

        	if (!checkRegisterClosedness()) {
        		consistent = false;
        	}

        	if (!checkGuardConsistency()) {
        		consistent = false;
        	}

        	if (!checkRegisterConsistency()) {
        		consistent = false;
        	}
        }

        if (noShortPrefixes() && !dt.isMissingParameter()) {
        	buildNewHypothesis();
        }
        //System.out.println(hyp);
        return true;
    }

    private boolean isGuardRefinement(Word<SymbolInstance<I>> word) {
    	return dt.getLeaf(word) == null;
    }

    private void addPrefix(Word<SymbolInstance<I>> u) {
    	dt.sift(u, true);
    }

    private void expand(Word<SymbolInstance<I>> u) {
    	DTLeaf l = dt.getLeaf(u);
    	assert l != null;
    	l.elevatePrefix(dt, u, hyp, sdtLogicOracle);
    }

    private boolean checkLocationConsistency() {

    	for (DTLeaf l : dt.getLeaves()) {
    		MappedPrefix mp = l.getPrimePrefix();
    		Iterator<MappedPrefix> it = l.getShortPrefixes().iterator();
    		while (it.hasNext()) {
    			ShortPrefix sp = (ShortPrefix)it.next();
    			SymbolicSuffix suffix = null;
    			for (ParameterizedSymbol psi : dt.getInputs()) {
    				Branching<I> access_b = l.getBranching(psi);
    				Branching<I> prefix_b = sp.getBranching(psi);
    				for (Word<SymbolInstance<I>> ws : prefix_b.getBranches().keySet()) {
    					Word<SymbolInstance<I>> wa = DTLeaf.branchWithSameGuard(ws, prefix_b, l.getRemapping(sp), access_b, sdtLogicOracle);
                        //System.out.println("wa: " + wa + ", ws: " + ws);
    					DTLeaf la = dt.getLeaf(wa);
    					DTLeaf ls = dt.getLeaf(ws);
    					if (la != ls) {
    						SymbolicSuffix v = distinguishingSuffix(wa, la, ws, ls);
    						if (suffix == null || suffix.length() > v.length()) {
    							suffix = v;
    						}
                            assert suffix != null;
    					}
    				}
    			}
    			if (suffix != null) {
    				dt.split(sp.getPrefix(), suffix, l);
    				return false;
    			}
    		}
    	}
    	return true;
    }

    private boolean checkRegisterClosedness() {
    	return dt.checkVariableConsistency(suffixBuilder);
    }

    private boolean checkRegisterConsistency() {
    	return dt.checkRegisterConsistency(suffixBuilder);
    }

    private boolean checkGuardConsistency() {
    	for (DTLeaf<I> dest_c : dt.getLeaves()) {
    		Collection<Word<SymbolInstance<I>>> words = new LinkedHashSet<>();
    		words.add(dest_c.getAccessSequence());
    		words.addAll(dest_c.getPrefixes().getWords());
    		words.addAll(dest_c.getShortPrefixes().getWords());
    		for (Word<SymbolInstance<I>> dest_id : words) {
    			if (dest_id.length() == 0) {
    				continue;
    			}
    			Word<SymbolInstance<I>> src_id = dest_id.prefix(dest_id.length() - 1);
    			DTLeaf src_c = dt.getLeaf(src_id);

    			Branching hypBranching = null;
    			if (src_c.getAccessSequence().equals(src_id)) {
    				hypBranching = src_c.getBranching(dest_id.lastSymbol().getBaseSymbol());
    			} else {
    				ShortPrefix sp = (ShortPrefix) src_c.getShortPrefixes().get(src_id);
    				assert sp != null;
    				hypBranching = sp.getBranching(dest_id.lastSymbol().getBaseSymbol());
    			}
    			if (hypBranching.getBranches().get(dest_id) != null) {
    				// word already in branching, no guard refinement needed
    				continue;
    			}
    			Word<SymbolInstance<I>> hyp_id = branchWithSameGuard(dest_c.getPrefix(dest_id), hypBranching);

    			SymbolicSuffix suffix = null;

    			DTLeaf<I> hyp_c = dt.getLeaf(hyp_id);
    			if (hyp_c != dest_c) {
    				suffix = distinguishingSuffix(hyp_id, hyp_c, dest_id, dest_c);
    			} else {
    				List<SymbolicSuffix> suffixes = new LinkedList<>();
    				Map<SymbolicSuffix<I>, SDT> dest_sdts = new LinkedHashMap<>();
    				Map<SymbolicSuffix<I>, SDT> hyp_sdts = new LinkedHashMap<>();
    				for (Map.Entry<SymbolicSuffix<I>, SDT> e : dest_c.getPrefix(dest_id).getTQRs().entrySet()) {
    					SymbolicSuffix s = e.getKey();
    					SDT dest_sdt = e.getValue();
    					SDT hyp_sdt = hyp_c.getPrefix(hyp_id).getTQRs().get(s);
    					assert hyp_sdt != null;

    					if (!SDT.equivalentUnderId(dest_sdt.toRegisterSDT(dest_id, consts), hyp_sdt.toRegisterSDT(hyp_id, consts))) {
    						suffixes.add(s);
    						dest_sdts.put(s, dest_sdt);
    						hyp_sdts.put(s, hyp_sdt);
    					}
    				}

    				if (suffixes.isEmpty()) {
    					continue;
    				}

    				Collections.sort(suffixes, (sa, sb) -> sa.length() > sb.length() ? 1 :
    						sa.length() < sb.length() ? -1 : 0);

    				for (SymbolicSuffix s : suffixes) {
    					SymbolicSuffix testSuffix;
    					SDT hyp_sdt = hyp_sdts.get(s);

    					if (suffixBuilder != null) {
    						SDT dest_sdt = dest_sdts.get(s);
    						DataValue[] regs = remappedRegisters(dest_sdt, hyp_sdt);
    						testSuffix = suffixBuilder.extendSuffix(dest_id, dest_sdt, s, regs);
    					} else {
    						testSuffix = new SymbolicSuffix(src_id, dest_id.suffix(1), restrictionBuilder);
    						testSuffix = testSuffix.concat(s);
    					}

    					SDT testSDT = sulOracle.treeQuery(src_id, testSuffix);
    					Branching testBranching = hypBranching.updateBranching(src_id, dest_id.lastSymbol().getBaseSymbol(), testSDT);
    					if (testBranching.getBranches().get(dest_id) != null) {
    						suffix = testSuffix;
    						break;
    					}
    				}
    			}

    			if (suffix != null) {
    				dt.addSuffix(suffix, src_c);
    				return false;
    			}
    		}
    	}

    	return true;
    }

    private SymbolicSuffix distinguishingSuffix(Word<SymbolInstance<I>> wa, DTLeaf ca, Word<SymbolInstance<I>> wb, DTLeaf cb) {
    	Word<SymbolInstance<I>> sa = wa.suffix(1);
    	Word<SymbolInstance<I>> sb = wb.suffix(1);

    	assert sa.getSymbol(0).getBaseSymbol().equals(sb.getSymbol(0).getBaseSymbol());

    	SymbolicSuffix v = dt.findLCA(ca, cb).getSuffix();

    	Word<SymbolInstance<I>> prefixA = wa.prefix(wa.length() - 1);
    	Word<SymbolInstance<I>> prefixB = wb.prefix(wb.length() - 1);

    	SDT tqrA = ca.getTQR(wa, v);
        SDT tqrB = cb.getTQR(wb, v);

    	assert tqrA != null && tqrB != null;

        SDT sdtA = tqrA;
        SDT sdtB = tqrB;

        if (suffixBuilder != null && solver != null) {
    		//return suffixBuilder.extendDistinguishingSuffix(wa, sdtA, wb, sdtB, v);
            SymbolicSuffix suffix = suffixBuilder.distinguishingSuffixFromSDTs(wa,  sdtA, wb,  sdtB, v.getActions(), solver);
           	return suffix;
        }

    	SymbolicSuffix alpha_a = new SymbolicSuffix(prefixA, sa, restrictionBuilder);
    	SymbolicSuffix alpha_b = new SymbolicSuffix(prefixB, sb, restrictionBuilder);
    	return alpha_a.getFreeValues().size() > alpha_b.getFreeValues().size()
    		   ? alpha_a.concat(v)
    		   : alpha_b.concat(v);
    }

    private boolean noShortPrefixes() {
    	for (DTLeaf l : dt.getLeaves()) {
    		if (!l.getShortPrefixes().isEmpty()) {
                return false;
            }
    	}
    	return true;
    }

    private Word<SymbolInstance<I>> branchWithSameGuard(MappedPrefix mp, Branching branching) {
    	Word<SymbolInstance<I>> dw = mp.getPrefix();

    	return branching.transformPrefix(dw);
    }

    private DataValue[] remappedRegisters(SDT sdt1, SDT sdt2) {
    	Bijection<DataValue<?>> bijection = SDTUtil.equivalentUnderBijection(sdt1, sdt2);
    	assert bijection != null;
    	List<DataValue> vals = new LinkedList<>();
    	for (Map.Entry<DataValue<?>, DataValue<?>> e : bijection.entrySet()) {
    		if (!e.getKey().equals(e.getValue())) {
    			vals.add(e.getKey());
    			vals.add(e.getValue());
    		}
    	}
    	return vals.toArray(new DataValue[vals.size()]);
    }

    @Override
	public Hypothesis<I> getHypothesisModel() {
        Map<Word<SymbolInstance<I>>, LocationComponent> components = new LinkedHashMap<>();
        components.putAll(dt.getComponents());
        AutomatonBuilder ab = new AutomatonBuilder(alphabet, components, consts);
        return ab.toRegisterAutomaton();
    }

    public DT getDT() {
        return dt;
    }

    public DTHyp getDTHyp() {
        return hyp;
    }

    public Map<Word<SymbolInstance<I>>, LocationComponent> getComponents() {
        return dt.getComponents();
    }

	private TreeOracle<I> buildHypOracle(Hypothesis<I> hypothesis) {
		return hypOracleFactory.apply(hypothesis);
	}

//    @Override
//    public String toString() {
//        return dt.toString();
//    }
}
