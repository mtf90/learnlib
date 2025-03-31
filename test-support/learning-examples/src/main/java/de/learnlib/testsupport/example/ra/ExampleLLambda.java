package de.learnlib.testsupport.example.ra;

import de.learnlib.theory.Theories;
import de.learnlib.testsupport.example.DefaultLearningExample;
import de.learnlib.testsupport.example.LearningExample.RALearningExample;
import net.automatalib.alphabet.impl.Alphabets;
import net.automatalib.automaton.ra.RegisterAutomaton;
import net.automatalib.automaton.ra.impl.CompactRA;
import net.automatalib.symbol.impl.InputSymbol;

public class ExampleLLambda extends DefaultLearningExample<InputSymbol, Boolean, RegisterAutomaton<?, InputSymbol, ?>>
        implements RALearningExample<InputSymbol> {

    public static final InputSymbol A = new InputSymbol("a");
    public static final InputSymbol B = new InputSymbol("b");

    public ExampleLLambda() {
        super(Alphabets.fromArray(A, B), buildAutomaton());
    }

    private static RegisterAutomaton<?, InputSymbol, ?> buildAutomaton() {
        CompactRA<InputSymbol> ra = new CompactRA<>(Alphabets.fromArray(A, B));

        // locations
        Integer l0 = ra.addInitialState(true);
        Integer l1 = ra.addState(false);
        Integer l2 = ra.addState(false);
        Integer l3 = ra.addState(false);
        Integer l4 = ra.addState(true);
        Integer l5 = ra.addState(false);
        Integer l6 = ra.addState(false);

        // transitions
        ra.addTransition(l0, A, ra.createTransition(l1));
        ra.addTransition(l0, B, ra.createTransition(l5));

        ra.addTransition(l1, A, ra.createTransition(l2));
        ra.addTransition(l1, B, ra.createTransition(l5));

        ra.addTransition(l2, A, ra.createTransition(l3));
        ra.addTransition(l2, B, ra.createTransition(l6));

        ra.addTransition(l3, A, ra.createTransition(l4));
        ra.addTransition(l3, B, ra.createTransition(l3));

        ra.addTransition(l4, A, ra.createTransition(l4));
        ra.addTransition(l4, B, ra.createTransition(l4));

        ra.addTransition(l5, A, ra.createTransition(l2));
        ra.addTransition(l5, B, ra.createTransition(l6));

        ra.addTransition(l6, A, ra.createTransition(l2));
        ra.addTransition(l6, B, ra.createTransition(l0));

        return ra;
    }

    @Override
    public Theories getTeachers() {
        return new Theories();
    }
}
