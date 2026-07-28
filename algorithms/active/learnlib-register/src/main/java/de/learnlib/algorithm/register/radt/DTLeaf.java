package de.learnlib.algorithm.register.radt;

import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import de.learnlib.algorithm.register.AutomatonBuilder;
import de.learnlib.algorithm.register.LocationComponent;
import de.learnlib.algorithm.register.PrefixContainer;
import de.learnlib.algorithm.register.rastar.RaStar;
import de.learnlib.data.Bijection;
import de.learnlib.data.Branching;
import de.learnlib.data.DataWords;
import de.learnlib.data.RegisterAssignment;
import de.learnlib.data.SDT;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.SDTLogicOracle;
import de.learnlib.oracle.TreeOracle;
import gov.nasa.jpf.constraints.api.Expression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.automatalib.alphabet.Alphabet;
import net.automatalib.automaton.ra.Assignment;
import net.automatalib.common.util.Pair;
import net.automatalib.data.Constants;
import net.automatalib.data.DataValue;
import net.automatalib.data.Valuation;
import net.automatalib.symbol.data.ParameterizedSymbol;
import net.automatalib.symbol.data.SymbolInstance;
import net.automatalib.symbol.impl.InputSymbol;
import net.automatalib.word.Word;

public class DTLeaf<I extends ParameterizedSymbol> extends DTNode implements LocationComponent<I> {

    private MappedPrefix<I> access;
    private PrefixSet<I> shortPrefixes;
    private PrefixSet<I> otherPrefixes;

    private final Map<ParameterizedSymbol, Branching> branching = new LinkedHashMap<ParameterizedSymbol, Branching>();
    private final TreeOracle oracle;

    public DTLeaf(TreeOracle oracle) {
        super();
        access = null;
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
        this.oracle = oracle;
    }

    public DTLeaf(MappedPrefix as, TreeOracle oracle) {
        super();
        access = as;
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
        this.oracle = oracle;
    }

    public DTLeaf(DTLeaf l) {
        access = new MappedPrefix(l.access, l.access.getRemapping());
        shortPrefixes = new PrefixSet(l.shortPrefixes);
        otherPrefixes = new PrefixSet(l.otherPrefixes);
        branching.putAll(l.branching);
        oracle = l.oracle;
    }

    public void addPrefix(Word<SymbolInstance<I>> p) {
        otherPrefixes.add(new MappedPrefix(p, new Bijection<>()));
    }

    public void addPrefix(MappedPrefix p) {
        assert p.memorableValues().size() == access.memorableValues().size();
        otherPrefixes.add(p);
    }

    void setAccessSequence(MappedPrefix mp) {
        access = mp;
    }

    public void addShortPrefix(ShortPrefix prefix) {
        if (otherPrefixes.contains(prefix.getPrefix()))
            otherPrefixes.remove(prefix.getPrefix());
        assert access != null;
        shortPrefixes.add(prefix);
    }

    public boolean removeShortPrefix(Word<SymbolInstance<?>> p) {
        return shortPrefixes.remove(p);
    }

    public boolean removePrefix(Word<SymbolInstance<?>> p) {
        return shortPrefixes.removeIf((e) -> e.getPrefix().equals(p)) || otherPrefixes.removeIf((e) -> e.getPrefix().equals(p));
    }

    /**
     * Clears the short and other prefix sets.
     * The only prefix remaining will be the leaf's access sequence.
     */
    public void clear() {
        shortPrefixes = new PrefixSet();
        otherPrefixes = new PrefixSet();
    }

    public PrefixSet<I> getShortPrefixes() {
        return shortPrefixes;
    }

    public PrefixSet<I> getPrefixes() {
        return otherPrefixes;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public boolean isAccepting() {
        SDT tqr = access.getTQRs().get(RaStar.EMPTY_SUFFIX);
        return tqr.isAccepting();
    }

    MappedPrefix<I> getMappedPrefix(Word<? extends SymbolInstance<?>> p) {
        if (access.getPrefix().equals(p))
            return access;
        MappedPrefix<I> ret = shortPrefixes.get(p);
        if (ret != null)
            return ret;
        ret = otherPrefixes.get(p);
        return ret;
    }

    @Override
    public Word<SymbolInstance<I>> getAccessSequence() {
        return access.getPrefix();
    }

    @Override
    public Bijection<DataValue<?>> getRemapping(PrefixContainer<I> r) {
        if (r.getPrefix().equals(this.getAccessSequence()))
            return Bijection.identity(this.access.memorableValues());
        MappedPrefix mp = otherPrefixes.get(r.getPrefix());
        if (mp == null)
            mp = shortPrefixes.get(r.getPrefix());

        assert mp != null;
        return mp.getRemapping();
    }

    @Override
    public Branching<I> getBranching(ParameterizedSymbol action) {
        return branching.get(action);
    }

    public Branching getBranching(ParameterizedSymbol action, Word<SymbolInstance<I>> src) {
    	Branching b = null;
    	ShortPrefix sp = (ShortPrefix)shortPrefixes.get(src);
    	if (sp != null)
    		b = sp.getBranching(action);
    	if (b == null)
    		return getBranching(action);
    	return b;
    }

    @Override
    public MappedPrefix getPrimePrefix() {
        return access;
    }

    Collection<Word<SymbolInstance<I>>> getAllPrefixes() {
        Collection<Word<SymbolInstance<I>>> prefs = new ArrayList<>();
        prefs.add(this.getAccessSequence());
        Iterator<MappedPrefix<I>> it = shortPrefixes.iterator();
        while (it.hasNext())
            prefs.add(it.next().getPrefix());
        it = otherPrefixes.iterator();
        while (it.hasNext())
            prefs.add(it.next().getPrefix());
        return prefs;
    }

    void getMappedExtendedPrefixes(Collection<MappedPrefix> mps) {
        mps.addAll(shortPrefixes.get());
        mps.addAll(otherPrefixes.get());
    }

    @Override
    public DTInnerNode getParent() {
        return parent;
    }

    public MappedPrefix<I> getPrefix(Word<SymbolInstance<I>> prefix) {
    	if (getAccessSequence().equals(prefix))
            return getPrimePrefix();
        MappedPrefix mp = shortPrefixes.get(prefix);
    	if (mp == null)
            mp = otherPrefixes.get(prefix);
    	return mp;
    }

    public SDT getTQR(Word<SymbolInstance<I>> prefix, SymbolicSuffix suffix) {
    	MappedPrefix<I> mp = getMappedPrefix(prefix);
    	return mp.getTQRs().get(suffix);
    }

    void addTQRs(SymbolicSuffix suffix) {
        Iterator<MappedPrefix> it = Iterators.concat(shortPrefixes.iterator(), otherPrefixes.iterator());
        while (it.hasNext()) {
            MappedPrefix mp = it.next();
            mp.computeTQR(suffix, oracle);
        }
    }

    void addTQRs(SymbolicSuffix s, boolean addToAccess) {
        if (addToAccess) {
            access.computeTQR(s, oracle);
        }
        addTQRs(s);
    }

    @Override
    public Collection<PrefixContainer<I>> getOtherPrefixes() {
        return Stream.concat(otherPrefixes.get().stream(), shortPrefixes.get().stream()).collect(Collectors.toList());
    }

    /**
     * Elevate a prefix from the set of other prefixes to the set of short prefix,
     * and checks whether this leads to a refinement. The branches of prefix are
     * sifted into the tree, and added to their respective leaves. If a branch of
     * prefix leads to another location than the same branch of the access sequence,
     * returns the diverging words as a Pair, otherwise returns null.
     *
     * @param dt
     * @param prefix
     * @param hyp
     * @param logicOracle
     * @return Pair of diverging words, if such a pair of words exists. Otherwise
     *         null.
     */
    public Pair<Word<SymbolInstance<I>>, Word<SymbolInstance<I>>> elevatePrefix(DT dt, Word<SymbolInstance<I>> prefix,
                                                                                DTHyp hyp, SDTLogicOracle logicOracle) {
        assert !shortPrefixes.contains(prefix) : prefix + " is already a short prefix";
        MappedPrefix mp = otherPrefixes.get(prefix);
        assert mp != null : "Cannot elevate prefix that is not contained in leaf " + this + " === " + prefix;
        ShortPrefix sp = new ShortPrefix(mp);
        addShortPrefix(sp);

        return startPrefix(dt, sp, hyp, logicOracle);
    }

    private Pair<Word<SymbolInstance<I>>, Word<SymbolInstance<I>>> startPrefix(DT<I> dt, ShortPrefix<I> mp, DTHyp hyp, SDTLogicOracle logicOracle) {

        Pair<Word<SymbolInstance<I>>, Word<SymbolInstance<I>>> divergance = null;
        boolean input = DTLeaf.isInput(mp.getPrefix().lastSymbol().getBaseSymbol());
        for (I ps : dt.getInputs()) {

            SDT[] sdtsP = this.getSDTsForInitialSymbol(mp, ps);
            Branching<I> prefixBranching = oracle.getInitialBranching(mp.getPrefix(), ps, sdtsP);
            mp.putBranching(ps, prefixBranching);

            SDT[] sdtsAS = this.getSDTsForInitialSymbol(this.getPrimePrefix(), ps);
            Branching<I> accessBranching = oracle.getInitialBranching(getAccessSequence(), ps, sdtsAS);

            assert prefixBranching.getBranches().size() == accessBranching.getBranches().size();

            for (Word<SymbolInstance<I>> p : prefixBranching.getBranches().keySet()) {
                if (dt.getIoMode() && ((input ^ !isInput(ps)) || hyp.getSemantics().getState(p) == null)) {
                    dt.getSink().addPrefix(p);
                    continue;
                }

                Word<SymbolInstance<I>> a = null;
                if (hyp == null) {
                    // no hypothesis yet, assume all true guards
                    for (Word<SymbolInstance<I>> w : accessBranching.getBranches().keySet()) {
                        if (w.lastSymbol().getBaseSymbol().equals(p.lastSymbol().getBaseSymbol())) {
                            a = w;
                            break;
                        }
                    }
                } else {
                	a = branchWithSameGuard(p, prefixBranching, this.getRemapping(mp), accessBranching, logicOracle);
                }
                assert a != null;
                DTLeaf leaf = dt.sift(p, true);

                if (!dt.getLeaf(a).equals(leaf)) {
                    divergance = Pair.of(p, a);
                }
            }
        }
        return divergance;
    }

    public static <I extends ParameterizedSymbol> Word<SymbolInstance<I>> branchWithSameGuard(Word<SymbolInstance<I>> word, Branching<I> wordBranching, Bijection<DataValue<?>> remapping, Branching<I> accBranching, SDTLogicOracle oracle) {
    	Word<SymbolInstance<I>> a = null;
        Expression<Boolean> g = null;
    	for (Entry<Word<SymbolInstance<I>>, Expression<Boolean>> e : wordBranching.getBranches().entrySet()) {
    		if (e.getKey().equals(word)) {
    			g = e.getValue();
    			break;
    		}
    	}
        assert g != null;
        //System.out.println("w:" + word);
        //System.out.println("g: " + g);
        //System.out.println("pi: " + remapping);
    	for (Entry<Word<SymbolInstance<I>>, Expression<Boolean>> e : accBranching.getBranches().entrySet()) {
            Expression<Boolean> ag = e.getValue();
            //System.out.println("ag: " + ag);
            boolean eq = oracle.areEquivalent(ag, remapping, g, new Valuation<>());
            if (eq) {
                a = e.getKey();
                break;
            }
    	}
        assert a != null;
    	return a;
    }

    public boolean isRefinemement(DT dt, Word<SymbolInstance<I>> word) {
        ParameterizedSymbol ps = word.lastSymbol().getBaseSymbol();
        DTLeaf target = dt.getLeaf(word);

        Branching<I> b = getBranching(ps);
        boolean refinement = true;
        for (Word<SymbolInstance<I>> w : b.getBranches().keySet()) {
            if (dt.getLeaf(w) == target)
                refinement = false;
        }

        return refinement;
    }

    void start(DT dt, boolean ioMode, Alphabet<I> inputs) {
        boolean input = isInputComponent();
        for (ParameterizedSymbol ps : inputs) {

            SDT[] sdts = this.getSDTsForInitialSymbol(ps);
            Branching<I> b = oracle.getInitialBranching(getAccessSequence(), ps, sdts);
            branching.put(ps, b);
            for (Word<SymbolInstance<I>> prefix : b.getBranches().keySet()) {
                if (ioMode && (dt.getSink() != null) && (input ^ isInput(ps)))
                    dt.getSink().addPrefix(prefix);
                else
                    dt.sift(prefix, true);
            }
        }
    }

    void start(DT dt, Map<ParameterizedSymbol, Branching> branching) {
        this.branching.putAll(branching);
    }

    boolean updateBranching(DT dt) {
        boolean ret = true;
        for (ParameterizedSymbol p : branching.keySet()) {
            boolean ub = updateBranching(p, dt);
            ret = ret && ub;
        }
        return ret;
    }

    private boolean updateBranching(ParameterizedSymbol ps, DiscriminationTree dt) {
        Branching b = branching.get(ps);
        SDT[] sdts = getSDTsForInitialSymbol(ps);
        Branching<I> newB = b.updateBranching(access.getPrefix(), ps, sdts);
        boolean ret = true;

        for (Word<SymbolInstance<I>> prefix : newB.getBranches().keySet()) {
            if (!b.getBranches().containsKey(prefix)) {
                dt.sift(prefix, true);
                ret = false;
            }
        }

        for (MappedPrefix sp : shortPrefixes.get()) {
            ret &= updateBranching(ps, (ShortPrefix) sp, dt);
        }

        branching.put(ps, newB);
        return ret;
    }

    private boolean updateBranching(ParameterizedSymbol ps, ShortPrefix sp, DiscriminationTree dt) {
    	Branching b = sp.getBranching(ps);
        SDT[] sdts = getSDTsForInitialSymbol(sp, ps);
        Branching<I> newB = b.updateBranching(sp.getPrefix(), ps, sdts);
        boolean ret = true;

        for (Word<SymbolInstance<I>> prefix : newB.getBranches().keySet()) {
            if (!b.getBranches().containsKey(prefix)) {
                dt.sift(prefix, true);
                ret = false;
            }
        }
        sp.putBranching(ps, newB);
        return ret;
    }

    private SDT[] getSDTsForInitialSymbol(ParameterizedSymbol p) {
        return getSDTsForInitialSymbol(this.getPrimePrefix(), p);
    }

    private SDT[] getSDTsForInitialSymbol(MappedPrefix<I> mp, ParameterizedSymbol p) {
        List<SDT> sdts = new ArrayList<>();
        for (Entry<SymbolicSuffix<I>, SDT> e : mp.getTQRs().entrySet()) {
            Word<I> acts = e.getKey().getActions();
            if (acts.length() > 0 && acts.firstSymbol().equals(p)) {
                sdts.add(e.getValue());
            }
        }
        return sdts.toArray(new SDT[] {});
    }

    public boolean checkVariableConsistency(DT dt, Constants consts, OptimizedSymbolicSuffixBuilder suffixBuilder) {
        if (!checkVariableConsistency(access, dt, consts, suffixBuilder)) {
            return false;
        }

        Iterator<MappedPrefix<I>> it = otherPrefixes.iterator();
        while (it.hasNext()) {
            if (!checkVariableConsistency(it.next(), dt, consts, suffixBuilder))
                return false;
        }
        it = shortPrefixes.iterator();
        while (it.hasNext()) {
            if (!checkVariableConsistency(it.next(), dt, consts, suffixBuilder))
                return false;
        }
        return true;
    }

    private boolean checkVariableConsistency(MappedPrefix<I> mp, DT dt, Constants consts, OptimizedSymbolicSuffixBuilder suffixBuilder) {
        if (mp.getPrefix().length() < 2)
            return true;

        Word<SymbolInstance<I>> prefix = mp.getPrefix().prefix(mp.getPrefix().length() - 1);
        DTLeaf prefixLeaf = dt.getLeaf(prefix);
        MappedPrefix prefixMapped = prefixLeaf.getMappedPrefix(prefix);

        Set<DataValue> memPrefix = prefixMapped.memorableValues();
        Set<DataValue<?>> memMP = mp.memorableValues();

        int max = DataWords.paramLength(DataWords.actsOf(prefix));
        List<DataValue> mpVals = Arrays.asList(DataWords.valsOf(mp.getPrefix()));

        for (DataValue d : memMP) {
            boolean prefixMissingParam = !memPrefix.contains(d) || prefixMapped.missingParameter.contains(d);
            if (prefixMissingParam && mpVals.indexOf(d) < max) {
            	List<SymbolicSuffix> prefixSuffixes = prefixMapped.getAllSuffixesForMemorable(d);
            	List<SymbolicSuffix> suffixes = mp.getAllSuffixesForMemorable(d);
            	assert !suffixes.isEmpty();
            	for (SymbolicSuffix suffix : suffixes) {
                    SDT sdt = mp.getTQRs().get(suffix);
                    // suffixBuilder == null ==> suffix.isOptimizedGeneric()
                    assert suffixBuilder != null || suffix.isOptimizationGeneric() : "Optimized with restriction builder, but no restriction builder provided";
                    SymbolicSuffix newSuffix = suffixBuilder != null ?
            				suffixBuilder.extendSuffix(mp.getPrefix(), sdt, suffix, d) :
            				new SymbolicSuffix(mp.getPrefix(), suffix, oracle.getRestrictionBuilder());
                    if (prefixSuffixes.contains(newSuffix))
                        continue;
                    SDT tqr = oracle.treeQuery(prefix, newSuffix);

                    if (tqr.getDataValues().contains(d)) {
                        dt.addSuffix(newSuffix, prefixLeaf);
                        mp.missingParameter.remove(d);
                        return false;
                    }
            	}
            	if (!prefixMapped.missingParameter.contains(d)) {
                    mp.missingParameter.add(d);
            	}
            } else {
            	mp.missingParameter.remove(d);
            }
        }

        return true;
    }

    public boolean checkRegisterConsistency(DT dt, Constants consts, OptimizedSymbolicSuffixBuilder suffixBuilder) {
        if (access.memorableValues().isEmpty())
    		return true;

    	if (!checkRegisterConsistency(access, dt, consts, suffixBuilder))
    		return false;

    	Iterator<MappedPrefix<I>> it = otherPrefixes.iterator();
    	while (it.hasNext()) {
    		if (!checkRegisterConsistency(it.next(), dt, consts, suffixBuilder))
    			return false;
    	}
    	it = shortPrefixes.iterator();
    	while (it.hasNext()) {
    		if (!checkRegisterConsistency(it.next(), dt, consts, suffixBuilder))
    			return false;
    	}
    	return true;
    }

    public boolean checkRegisterConsistency(MappedPrefix<I> mp, DT dt, Constants consts, OptimizedSymbolicSuffixBuilder suffixBuilder) {
    	if (mp.getPrefix().length() < 2)
    		return true;

    	Set<DataValue> memMP = new HashSet<>(mp.memorableValues());
    	if (memMP.isEmpty())
    		return true;

    	Word<SymbolInstance<I>> prefix = mp.getPrefix().prefix(mp.getPrefix().length() - 1);
    	DTLeaf prefixLeaf = dt.getLeaf(prefix);
    	MappedPrefix prefixMapped = prefixLeaf.getMappedPrefix(prefix);
        Set<DataValue<?>> memPrefix = new HashSet<>(prefixMapped.memorableValues());

        Set<DataValue<?>> paramsIntersect = Sets.intersection(memPrefix, memMP);
        if (prefixMapped.equivalentRenamings(memPrefix).size() < 2)
        	return true;
//        if (renamingsPrefix.size() < 2)
//        	return true;    // there are no equivalent renamings

        if (!paramsIntersect.isEmpty() && paramsIntersect.size() < memPrefix.size()) {
        	// word shares some data values with prefix, but not all
        	for (Entry<SymbolicSuffix<I>, SDT> e : mp.getTQRs().entrySet()) {
        		Set<DataValue> pmap = Set.of(e.getValue().getDataValues().toArray(new DataValue[0]));
        		if (!Sets.intersection(pmap, paramsIntersect).isEmpty()) {
        			SDT sdt = e.getValue();
        			SymbolicSuffix newSuffix = suffixBuilder != null ?
        					suffixBuilder.extendSuffix(mp.getPrefix(), sdt, e.getKey()) :
        					new SymbolicSuffix(mp.getPrefix(), e.getKey(), oracle.getRestrictionBuilder());
        			if (!prefixMapped.getTQRs().containsKey(newSuffix)) {
        				dt.addSuffix(newSuffix, prefixLeaf);
        				return false;
        			}
        		}
        	}
        }

        Set<Bijection<DataValue<?>>> renamingsPrefix = prefixMapped.equivalentRenamings(paramsIntersect);
        if (renamingsPrefix.size() < 2)
        	return true;    // there are no equivalent renamings
        Set<Bijection<DataValue<?>>> renamingsMP = mp.equivalentRenamings(paramsIntersect);
        Set<Bijection<DataValue<?>>> difference = Sets.difference(renamingsPrefix, renamingsMP);
        if (!difference.isEmpty()) {
        	// there are symmetric parameter mappings in the prefix which are not symmetric in mp
        	for (Entry<SymbolicSuffix<I>, SDT> e : mp.getTQRs().entrySet()) {
        		SDT sdt = e.getValue();
        		for (Bijection<DataValue<?>> vm : difference) {
                	//VarMapping<Register, Register> renaming = new VarMapping<>();
                	/*for (Map.Entry<Parameter, Parameter> paramRenaming : vm.entrySet()) {
                		Register oldRegister = memPrefix.get(paramRenaming.getKey());
                		Register newRegister = memPrefix.get(paramRenaming.getValue());
                		renaming.put(oldRegister, newRegister);
                	}*/
        			if (!sdt.isEquivalent(sdt, vm)) {
        				SymbolicSuffix newSuffix = suffixBuilder != null  ?
            					suffixBuilder.extendSuffix(mp.getPrefix(), sdt, e.getKey()) :
            					new SymbolicSuffix(mp.getPrefix(), e.getKey(), oracle.getRestrictionBuilder());
            			dt.addSuffix(newSuffix, prefixLeaf);
            			return false;
        			}
        		}
        	}
        }
        return true;
    }
    public boolean isMissingVariable() {
    	Collection<MappedPrefix> prefixes = new ArrayList<>();
    	getMappedExtendedPrefixes(prefixes);
    	for (MappedPrefix mp : prefixes) {
    		if (!mp.missingParameter.isEmpty())
    			return true;
    	}
    	return !access.missingParameter.isEmpty();
    }

    public boolean isInputComponent() {
        if (this.getAccessSequence().length() == 0)
            return true;

        ParameterizedSymbol ps = this.getAccessSequence().lastSymbol().getBaseSymbol();
        return !isInput(ps);
    }

    public static boolean isInput(ParameterizedSymbol ps) {
        return (ps instanceof InputSymbol);
    }

    public Assignment getAssignment(Word<SymbolInstance<I>> dest_id, DTLeaf dest_c) {
    	MappedPrefix r = dest_c.getPrefix(dest_id);
        RegisterAssignment srcAssign = getPrimePrefix().getAssignment();
        RegisterAssignment dstAssign = dest_c.access.getAssignment();
        Bijection<DataValue<?>> remap = dest_c.getRemapping(r);
        return AutomatonBuilder.computeAssignment(r.getPrefix(), srcAssign, dstAssign, remap);
    }

    @Override
    public DTLeaf copy() {
        return new DTLeaf(this);
    }

    @Override
    public String toString() {
        return getAllPrefixes().toString();
    }
}
