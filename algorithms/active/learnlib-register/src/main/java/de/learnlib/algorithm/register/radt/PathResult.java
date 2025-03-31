package de.learnlib.algorithm.register.radt;

import de.learnlib.algorithm.register.rastar.RaStar;
import de.learnlib.data.Bijection;
import de.learnlib.data.Memorables;
import de.learnlib.data.SDT;
import de.learnlib.data.SDTRelabeling;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.automatalib.data.DataValue;
import net.automatalib.data.SymbolicDataValueGenerator;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.symbol.impl.OutputSymbol;

/**
 * this is a copy of the functionality of row
 * with additions / modifications for use in the DT
 *
 * @author falk
 */
public class PathResult {

    private final Map<SymbolicSuffix, SDT> results;

    private Bijection<DataValue<?>> remapping;

    private final SymbolicDataValueGenerator.RegisterGenerator regGen = new SymbolicDataValueGenerator.RegisterGenerator();

    private final boolean ioMode;

    private PathResult(boolean ioMode) {
        this.results = new LinkedHashMap<>();
        this.ioMode = ioMode;
    }

    private void addResult(SymbolicSuffix s, SDT tqr) {
       this.results.put(s, tqr);
    }

    public boolean isEquivalentTo(PathResult other, Bijection<DataValue<?>> renaming) {
        return isEquivalentTo(other, SDTRelabeling.fromBijection(renaming));
    }

    public boolean isEquivalentTo(PathResult other, SDTRelabeling renaming) {
        if (!couldBeEquivalentTo(other)) {
            return false;
        }

        if (!Memorables.relabel(this.memorableValues(), renaming).equals(other.memorableValues())) {
            return false;
        }

        for (Map.Entry<SymbolicSuffix, SDT> e : this.results.entrySet()) {
            SDT c1 = e.getValue();
            SDT c2 = other.results.get(e.getKey());

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!isEquivalentTo(c1, c2, renaming)) {
                return false;
            }
        }
        return true;
    }

    boolean isEquivalentTo(SDT c1, SDT c2, SDTRelabeling renaming) {
        if (!couldBeEquivalentTo(c1, c2)) return false;
        return  Memorables.relabel(c1.getDataValues(), renaming).equals(c2.getDataValues()) && c2.isEquivalent(c1, renaming);
    }

    boolean couldBeEquivalentTo(PathResult other) {
        if (!Memorables.typedSize(this.memorableValues()).equals(Memorables.typedSize(other.memorableValues()))) {
            return false;
        }

        for (Map.Entry<SymbolicSuffix, SDT> e : this.results.entrySet()) {
            SDT c1 = e.getValue();
            SDT c2 = other.results.get(e.getKey());

            if (ioMode) {
                if (c1 == null && c2 == null) {
                    continue;
                }

                if (c1 == null || c2 == null) {
                    return false;
                }
            }

            if (!couldBeEquivalentTo(c1, c2)) {
                return false;
            }
        }
        return true;
    }

    public Set<DataValue<?>> memorableValues() {
        return results.values().stream()
                .flatMap(sdt -> sdt.getDataValues().stream())
                .collect(Collectors.toSet());
    }


    private boolean couldBeEquivalentTo(SDT c1, SDT c2) {
        return Memorables.typedSize(c1.getDataValues()).equals(Memorables.typedSize(c2.getDataValues()));
    }

    /**
     * computes a new row object from a prefix and a set of symbolic suffixes.
     *
     * @param oracle
     * @param prefix
     * @param suffixes
     * @return
     */
    public static <I extends ParameterizedSymbol> PathResult computePathResult(TreeOracle oracle,
                                                                               MappedPrefix<I> prefix, List<SymbolicSuffix> suffixes, boolean ioMode) {

        PathResult r = new PathResult(ioMode);
        for (SymbolicSuffix s : suffixes) {
            //todo: potential for optimization
            if (ioMode && !s.getActions().isEmpty()) {
                // error row
                if (!prefix.getPrefix().isEmpty() && !r.isAccepting()) {
                    //log.log(Level.INFO, "Not adding suffix " + s + " to error row " + r.getPrefix());
                    continue;
                }
                // unmatching suffix
                if ((prefix.getPrefix().isEmpty() && (s.getActions().firstSymbol() instanceof OutputSymbol))
                        || (!prefix.getPrefix().isEmpty() && prefix.getPrefix().lastSymbol().getBaseSymbol() instanceof InputSymbol == s.getActions().firstSymbol() instanceof InputSymbol)) {
                    //log.log(Level.INFO, "Not adding suffix " + s + " to unmatching row " + r.getPrefix());
                    continue;
                }
            }
            SDT tqr = prefix.getTQRs().get(s);
            if (tqr == null) {
                tqr = oracle.treeQuery(prefix.getPrefix(), s);
            }
            //System.out.println("TQ: " + prefix + " : " + s + " : " + tqr);
            r.addResult(s, tqr);
        }
        return r;
    }

    public boolean isAccepting() {
        SDT c = this.results.get(RaStar.EMPTY_SUFFIX);
        return c.isAccepting();
    }

    public Bijection<DataValue<?>> getRemapping() {
        return remapping;
    }

    public void setRemapping(Bijection<DataValue<?>> remapping) {
        this.remapping = remapping;
    }

    public SDT getSDTforSuffix(SymbolicSuffix suffix) {
        return results.get(suffix);
    }

    public PathResult copy() {
        PathResult r = new PathResult(ioMode);
        r.results.putAll(this.results);
        return  r;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.results.forEach((key, value) -> sb.append("[").append(key).append("->")
                .append(value.toString().replaceAll("\\s+", " ")).append("] "));
        return sb.toString();
    }
}
