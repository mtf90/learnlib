/* Copyright (C) 2013 TU Dortmund
 * This file is part of LearnLib, http://www.learnlib.de/.
 * 
 * LearnLib is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 3.0 as published by the Free Software Foundation.
 * 
 * LearnLib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with LearnLib; if not, see
 * <http://www.gnu.de/documents/lgpl.en.html>.
 */
package de.learnlib.lstar.dfa;

import de.learnlib.api.MembershipOracle;
import de.learnlib.lstar.ExtensibleAutomatonLStar;
import de.learnlib.lstar.ce.ClassicLStarCEXHandler;
import de.learnlib.lstar.ce.ObservationTableCEXHandler;
import de.learnlib.lstar.closing.CloseFirstStrategy;
import de.learnlib.lstar.closing.ClosingStrategy;
import de.learnlib.lstar.table.Row;
import net.automatalib.automata.fsa.DFA;
import net.automatalib.automata.fsa.impl.compact.CompactDFA;
import net.automatalib.words.Alphabet;
import net.automatalib.words.Word;

import java.util.Collections;
import java.util.List;


/**
 * An implementation of Angluin's L* algorithm for learning DFAs, as described in the paper
 * "Learning Regular Sets from Queries and Counterexamples".
 * 
 * @author Malte Isberner <malte.isberner@gmail.com>
 *
 * @param <I> input symbol class.
 */
public class ExtensibleLStarDFA<I>
	extends ExtensibleAutomatonLStar<DFA<?,I>, I, Boolean, Integer, Integer, Boolean, Void, CompactDFA<I>> {

	
	public static <I> List<Word<I>> getDefaultInitialSuffixes() {
		return Collections.singletonList(Word.<I>epsilon());
	}
	
	public static <I> ObservationTableCEXHandler<I, Boolean> getDefaultCEXHandler() {
		return ClassicLStarCEXHandler.getInstance();
	}
	
	public static <I> ClosingStrategy<I,Boolean> getDefaultClosingStrategy() {
		return CloseFirstStrategy.getInstance();
	}
	
	/**
	 * Constructor.
	 * @param alphabet the learning alphabet.
	 * @param oracle the DFA oracle.
	 */
	public ExtensibleLStarDFA(Alphabet<I> alphabet, MembershipOracle<I,Boolean> oracle,
			List<Word<I>> initialSuffixes,
			ObservationTableCEXHandler<I, Boolean> cexHandler,
			ClosingStrategy<I, Boolean> closingStrategy) {
		super(alphabet, oracle, new CompactDFA<I>(alphabet),
				LStarDFAUtil.ensureSuffixCompliancy(initialSuffixes),
				cexHandler, closingStrategy);
	}
	
	public ExtensibleLStarDFA(Alphabet<I> alphabet, MembershipOracle<I,Boolean> oracle) {
		this(alphabet, oracle,
				ExtensibleLStarDFA.<I>getDefaultInitialSuffixes(),
				ExtensibleLStarDFA.<I>getDefaultCEXHandler(),
				ExtensibleLStarDFA.<I>getDefaultClosingStrategy());
	}

	
	/*
	 * (non-Javadoc)
	 * @see de.learnlib.lstar.AbstractLStar#initialSuffixes()
	 */
	@Override
	protected List<Word<I>> initialSuffixes() {
		return Collections.singletonList(Word.<I>epsilon());
	}


	/*
	 * (non-Javadoc)
	 * @see de.learnlib.lstar.AbstractAutomatonLStar#stateProperty(de.learnlib.lstar.Row)
	 */
	@Override
	protected Boolean stateProperty(Row<I> stateRow) {
		return table.cellContents(stateRow, 0);
	}

	/*
	 * (non-Javadoc)
	 * @see de.learnlib.lstar.AbstractAutomatonLStar#transitionProperty(de.learnlib.lstar.Row, int)
	 */
	@Override
	protected Void transitionProperty(Row<I> stateRow, int inputIdx) {
		return null;
	}


	@Override
	protected DFA<?, I> exposeInternalHypothesis() {
		return internalHyp;
	}


}