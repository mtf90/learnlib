package de.learnlib.algorithm.register.ralambda.it;

import de.learnlib.algorithm.register.ralambda.RaLambda;
import de.learnlib.data.Configuration;
import de.learnlib.testsupport.it.AbstractRALearnerIT;
import de.learnlib.testsupport.it.variant.LearnerVariantList.RALearnerVariantList;
import de.learnlib.theory.restriction.GenericSuffixRestrictionBuilderImpl;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.symbol.data.ParameterizedSymbol;

public class RaLambdaIT extends AbstractRALearnerIT {

    @Override
    protected <I extends ParameterizedSymbol> void addLearnerVariants(Alphabet<I> alphabet,
                                                                      Configuration<I> config,
                                                                      RALearnerVariantList<I> variants) {
        variants.addLearnerVariant("RaLambda [default]", new RaLambda<>(alphabet, config));
        variants.addLearnerVariant("RaLambda [generic]",
                                   new RaLambda<>(alphabet,
                                                  config.getConstants(),
                                                  config.getTreeOracle(),
                                                  config::getHypothesisOracle,
                                                  config.getLogicOracle(),
                                                  new GenericSuffixRestrictionBuilderImpl(config.getConstants()),
                                                  config.getSolver()));
    }
}
