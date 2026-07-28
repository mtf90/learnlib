package de.learnlib.algorithm.register.radt;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.register.AutomatonBuilder;
import de.learnlib.algorithm.register.CounterexampleAnalysis;
import de.learnlib.algorithm.register.rastar.Hypothesis;
import de.learnlib.algorithm.register.CEAnalysisResult;
import de.learnlib.algorithm.register.LocationComponent;
import de.learnlib.data.Configuration;
import de.learnlib.data.SymbolicSuffixRestrictionBuilder;
import de.learnlib.logging.Category;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.query.DefaultQuery;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaDT<I extends ParameterizedSymbol>
        implements LearningAlgorithm<RegisterAutomaton<?, I, ?>, SymbolInstance<I>, Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaDT.class);

    private final Alphabet<I> alphabet;
    private final TreeOracle<I> sulOracle;
    private final Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory;
    private final SDTLogicOracle logicOracle;
    private final Constants consts;

    private final DT<I> dt;
    private final Deque<DefaultQuery<SymbolInstance<I>, Boolean>> counterexamples = new LinkedList<>();
    private DTHyp<I> hyp;

    private final OptimizedSymbolicSuffixBuilder<I> suffixBuilder;

    public RaDT(Alphabet<I> alphabet, Configuration<I> config) {
        this(alphabet, config.getTreeOracle(), config::getHypothesisOracle, config.getLogicOracle(), config.getConstants());
    }

    public RaDT(Alphabet<I> alphabet,
                TreeOracle<I> oracle,
                Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory,
                SDTLogicOracle logicOracle,
                Constants consts) {
        this(alphabet, oracle, hypOracleFactory, logicOracle, oracle.getRestrictionBuilder(), consts);
    }

    public RaDT(Alphabet<I> alphabet,
                TreeOracle<I> oracle,
                Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory,
                SDTLogicOracle logicOracle,
                SymbolicSuffixRestrictionBuilder restrictionBuilder,
                Constants consts) {
        this.alphabet = alphabet;
        this.sulOracle = oracle;
        this.hypOracleFactory = hypOracleFactory;
        this.logicOracle = logicOracle;
        this.consts = consts;
        this.suffixBuilder = new OptimizedSymbolicSuffixBuilder<>(consts, restrictionBuilder);
        this.dt = new DT<>(oracle, false, consts, alphabet);
    }

    @Override
    public void startLearning() {
        this.dt.initialize();
        learn();
    }

    private void buildHypothesis() {
        Map<Word<SymbolInstance<I>>, LocationComponent<I>> components = new LinkedHashMap(dt.getComponents());

        AutomatonBuilder ab = new AutomatonBuilder<>(alphabet, components, consts, dt);
        hyp = (DTHyp) ab.toRegisterAutomaton();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<SymbolInstance<I>, Boolean> ceQuery) {
        LOGGER.debug(Category.EVENT, "adding counterexample: {}", ceQuery);
        counterexamples.add(ceQuery);

        return learn();
    }

	private boolean learn() {
        if (hyp == null) {
            buildHypothesis();
        }

        boolean refined = false;

        while (analyzeCounterExample()) {
            refined = true;
        };

        return refined;
	}

    private boolean analyzeCounterExample() {
        LOGGER.info(Category.PHASE, "Analyzing Counterexample");
        if (counterexamples.isEmpty()) {
            return false;
        }

        DefaultQuery<SymbolInstance<I>, Boolean> ce = counterexamples.peek();

        // check if ce still is a counterexample ...
        boolean hypce = hyp.getSemantics().accepts(ce.getInput());
        boolean sulce = ce.getOutput();
        if (hypce == sulce) {
            LOGGER.info(Category.EVENT, "word is not a counterexample: {} - {}", ce, sulce);
            counterexamples.poll();
            return false;
        }

        TreeOracle<I> hypOracle = hypOracleFactory.apply(hyp);

        CounterexampleAnalysis<I> analysis = new CounterexampleAnalysis<>(sulOracle,
                                                                          hypOracle,
                                                                          hyp,
                                                                          logicOracle,
                                                                          new LinkedHashMap(dt.getComponents()));
        CEAnalysisResult<I> res = analysis.analyzeCounterexample(ce.getInput());


        Word<SymbolInstance<I>> accSeq = hyp.transformAccessSequence(res.getPrefix());
        DTLeaf<I> leaf = dt.getLeaf(accSeq);
        dt.addSuffix(res.getSuffix(), leaf);
        while(!dt.checkIOSuffixes());
        while(!dt.checkVariableConsistency(suffixBuilder));
        buildHypothesis();
        return true;
    }

	@Override
	public Hypothesis<I> getHypothesisModel() {
        Map<Word<SymbolInstance<I>>, LocationComponent<I>> components = new LinkedHashMap(dt.getComponents());
        AutomatonBuilder<I> ab = new AutomatonBuilder<>(alphabet, components, consts);
        return ab.toRegisterAutomaton();
	}

    public DT<I> getDT() {
        return dt;
    }

}
