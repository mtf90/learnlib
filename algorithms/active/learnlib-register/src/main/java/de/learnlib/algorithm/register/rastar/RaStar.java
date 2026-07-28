/*
 * Copyright (C) 2014-2015 The LearnLib Contributors
 * This file is part of LearnLib, http://www.learnlib.de/.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.learnlib.algorithm.register.rastar;

import de.learnlib.algorithm.LearningAlgorithm;
import de.learnlib.algorithm.register.AutomatonBuilder;
import de.learnlib.algorithm.register.CEAnalysisResult;
import de.learnlib.algorithm.register.CounterexampleAnalysis;
import de.learnlib.algorithm.register.LocationComponent;
import de.learnlib.data.Configuration;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.logging.Category;
import de.learnlib.oracle.MembershipOracle;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import de.learnlib.query.DefaultQuery;
import de.learnlib.query.Query;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.data.Constants;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.ts.acceptor.DeterministicAcceptorTS;
import net.automatalib.word.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Learning algorithm for register automata
 *
 * @author falk
 */
public class RaStar<I extends ParameterizedSymbol>
        implements LearningAlgorithm<RegisterAutomaton<?, I, ?>, SymbolInstance<I>, Boolean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RaStar.class);
    public static final SymbolicSuffix EMPTY_SUFFIX = new SymbolicSuffix<>();

    private final Alphabet<I> alphabet;
    private final TreeOracle<I> sulOracle;
    private final Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory;
    private final SDTLogicOracle logicOracle;
    private final Constants consts;

    private final ObservationTable<I> obs;
    private final Deque<DefaultQuery<SymbolInstance<I>, Boolean>> counterexamples;

    private Hypothesis<I> hyp;

    public RaStar(Alphabet<I> alphabet, Configuration<I> config) {
        this(alphabet, config.getConstants(), config.getTreeOracle(), config::getHypothesisOracle, config.getLogicOracle());
    }

    public RaStar(Alphabet<I> alphabet, Constants consts, TreeOracle<I> sulOracle,
                  Function<? super RegisterAutomaton<?, I, ?>, TreeOracle<I>> hypOracleFactory,
                  SDTLogicOracle sdtLogicOracle) {
        this.sulOracle = sulOracle;
        this.alphabet = alphabet;
        this.hypOracleFactory = hypOracleFactory;
        this.logicOracle = sdtLogicOracle;
        this.consts = consts;
        this.obs = new ObservationTable<>(alphabet, sulOracle);
        this.counterexamples = new ArrayDeque<>();
    }

    @Override
    public void startLearning() {
        this.obs.addPrefix(Word.epsilon());
        this.obs.addSuffix(EMPTY_SUFFIX);
        learn();
    }

    @Override
    public boolean refineHypothesis(DefaultQuery<SymbolInstance<I>, Boolean> ceQuery) {
        LOGGER.debug(Category.EVENT, "adding counterexample: {}", ceQuery);
        counterexamples.add(ceQuery);

        return learn();
    }

    private boolean learn() {

        boolean refined = false;

        if (hyp != null) {
            refined |= analyzeCounterExample();
        }

        do {
            LOGGER.debug(Category.PHASE, "completing observation table");
            while (!obs.complete()) {
                refined = true;
            }
            LOGGER.debug(Category.PHASE, "completed observation table");

            hyp = getHypothesisModel();

            LOGGER.debug(Category.MODEL, "{}", hyp);

        } while (analyzeCounterExample());

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
                                                                          new HashMap<>(obs.getComponents()));
        CEAnalysisResult<I> res = analysis.analyzeCounterexample(ce.getInput());

        obs.addSuffix(res.getSuffix());
        return true;
    }

    @Override
    public Hypothesis<I> getHypothesisModel() {
        Map<Word<SymbolInstance<I>>, LocationComponent<I>> components = new LinkedHashMap<>(obs.getComponents());
        AutomatonBuilder<I> ab = new AutomatonBuilder<>(alphabet, components, consts);
        return ab.toRegisterAutomaton();
    }

    public static class HypWrapper<I extends ParameterizedSymbol>
            implements MembershipOracle<SymbolInstance<I>, Boolean> {

        private final Hypothesis<I> hypothesis;

        public HypWrapper(Hypothesis<I> hypothesis) {
            this.hypothesis = hypothesis;
        }

        @Override
        public void processQueries(Collection<? extends Query<SymbolInstance<I>, Boolean>> queries) {
            final DeterministicAcceptorTS<?, SymbolInstance<I>> acceptor = hypothesis.getSemantics();
            for (Query<SymbolInstance<I>, Boolean> q : queries) {
                q.answer(acceptor.accepts(q.getInput()));
            }
        }
    }

}
