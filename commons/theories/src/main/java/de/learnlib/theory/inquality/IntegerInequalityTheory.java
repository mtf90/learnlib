/*
 * Copyright (C) 2014-2025 The LearnLib Contributors
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
package de.learnlib.theory.inquality;

import gov.nasa.jpf.constraints.api.ConstraintSolver;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.automatalib.data.DataType;
import net.automatalib.data.DataValue;

/**
 *
 * @author Sofia Cassel
 * @author Fredrik Tåquist
 */
public class IntegerInequalityTheory extends AbstractNumberInequalityTheory<Integer> {

	public IntegerInequalityTheory(DataType<Integer> t, ConstraintSolver solver) {
        super(t, solver);
	}

	public IntegerInequalityTheory(DataType<Integer> t, ConstraintSolver solver, boolean useSuffixOpt) {
        super(t, solver, useSuffixOpt);
	}

	@Override
	public DataValue<Integer> getFreshValue(Collection<DataValue<Integer>> vals) {
		if (vals.isEmpty()) {
			return new DataValue<>(getDataType(), 1);
		}
		List<DataValue<Integer>> potential = getPotential(vals);
		if (potential.isEmpty()) {
			return new DataValue<>(getDataType(), 1);
		}
		//LOGGER.trace("smallest index of " + newDv.toString() + " in " + ifValues.toString() + " is " + smallest);
		DataValue<Integer> biggestDv = Collections.max(potential, getComparator());
		return new DataValue<>(getDataType(), biggestDv.getValue() + 1);
	}

	@Override
	public Collection<DataValue<Integer>> getAllNextValues(
			List<DataValue<Integer>> vals) {
        Set<DataValue<Integer>> nextValues = new LinkedHashSet<>(vals);
		if (vals.isEmpty()) {
			nextValues.add(new DataValue<>(getDataType(), 1));
		} else {
			if (vals.size() > 1) {
				vals.sort(getComparator());
				for (int i = 0; i < (vals.size() - 1); i++) {
					Integer d1 = vals.get(i).getValue();
					Integer d2 = vals.get(i + 1).getValue();
					nextValues.add(new DataValue<>(getDataType(),
												   (d1 + (d2 - d1) / 2)));
					//(d1 + ((d2 - d1) / 2))));
				}
			}
			nextValues.add(new DataValue<>(getDataType(), (Collections.min(vals, getComparator()).getValue() - 1)));
			nextValues.add(new DataValue<>(getDataType(), (Collections.max(vals, getComparator()).getValue() + 1)));
		}
		return nextValues;
	}

}
