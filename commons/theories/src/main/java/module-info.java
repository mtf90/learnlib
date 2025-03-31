open module de.learnlib.theory {
    requires de.learnlib.api;
    requires jconstraints.core;
    requires net.automatalib.api;
    requires org.slf4j;

    exports de.learnlib.theory;
    exports de.learnlib.theory.guard;
    exports de.learnlib.theory.inquality;
    exports de.learnlib.theory.restriction;
}