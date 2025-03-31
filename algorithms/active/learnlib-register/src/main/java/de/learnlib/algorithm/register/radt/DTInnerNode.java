package de.learnlib.algorithm.register.radt;

import de.learnlib.data.Bijection;
import de.learnlib.data.SymbolicSuffix;
import de.learnlib.oracle.TreeOracle;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.automatalib.common.util.Pair;
import net.automatalib.data.DataValue;

public class DTInnerNode extends DTNode {

	private final SymbolicSuffix suffix;

	private final Set<DTBranch> branches;

	public DTInnerNode(SymbolicSuffix suffix) {
		super();
		this.suffix = suffix;
		branches = new LinkedHashSet<DTBranch>();
	}

	public DTInnerNode(SymbolicSuffix suffix, Set<DTBranch> branches) {
		super();
		this.suffix = suffix;
		this.branches = branches;
	}

	public DTInnerNode(DTInnerNode n) {
		suffix = n.suffix;
		branches = new LinkedHashSet<DTBranch>();
		for (DTBranch b : n.branches) {
			DTBranch nb = new DTBranch(b);
			b.getChild().setParent(this);
			branches.add(nb);
		}
	}

	protected Pair<DTNode, PathResult> sift(MappedPrefix prefix, TreeOracle oracle, boolean ioMode) {
		PathResult r = PathResult.computePathResult(oracle, prefix, getSuffixes(), ioMode);
		for (DTBranch b : branches) {
			Bijection<DataValue<?>> remapping = b.matches(r);
			if (remapping != null) {
				r.setRemapping(remapping);
				return Pair.of(b.getChild(), r);
			}
		}
		r.setRemapping(Bijection.identity(r.memorableValues()));
		return null;
	}

	public void addBranch(DTBranch b) {
		branches.add(b);
	}

	public Set<DTBranch> getBranches() {
		return branches;
	}

	public SymbolicSuffix getSuffix() {
		return suffix;
	}

	List<SymbolicSuffix> getSuffixes() {
		LinkedList<SymbolicSuffix> suffixes = new LinkedList<>();
		getSuffixes(suffixes);
		return suffixes;
	}

	void getSuffixes(LinkedList<SymbolicSuffix> suffixes) {
		suffixes.addFirst(suffix);
		if (parent != null) {
			parent.getSuffixes(suffixes);
		}
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public DTInnerNode copy() {
		return new DTInnerNode(this);
	}

	@Override
	public String toString() {
		return "(" +  suffix.toString() + ")";
	}
}
