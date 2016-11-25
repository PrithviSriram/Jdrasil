/*
 * Copyright (c) 2016-present, Max Bannach, Sebastian Berndt, Thorsten Ehlers
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT
 * OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.uniluebeck.tcs.jdrasil.lowerbounds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uniluebeck.tcs.jdrasil.graph.Graph;
import de.uniluebeck.tcs.jdrasil.sat.Formula;
import de.uniluebeck.tcs.jdrasil.sat.encodings.IncrementalCardinalityEncoder;
import de.uniluebeck.tcs.jdrasil.sat.solver.GlucoseSATSolver;
import de.uniluebeck.tcs.jdrasil.sat.solver.SATSolver;

/**
 * This class computes a maximum clique of the graph. The size of a maximum clique is a lower bound for the tree-width.
 * Furthermore, the class provides methods to obtain the maximum clique.
 * 
 * The problem is solved with the SAT-solver SAT4J.
 * If the SAT-solver fails, -1 is returned.
 * 
 * @param <T>
 * @author Max Bannach
 */
public class CliqueLowerbound<T extends Comparable<T>> implements Lowerbound<T>{

	/** The graph for which the lower bound is computed. */
	private final Graph<T> graph;
	
	/** The sat algorithm instance. */
	private final SATSolver sat;
	
	/** The model, i.e., the vertices of a maximum clique. */
	private Set<T> model;
	
	/**
	 * Initialize the sat-solver with a graph.
	 * @param graph
	 * @param timeout
	 */
	public CliqueLowerbound(Graph<T> graph) {
		this.graph = graph;
		this.model = new HashSet<T>();
		this.sat = new GlucoseSATSolver();
	}
	
	private int cliqueSAT() {
		sat.initSolver();
		
		// compute bijection to {1,...,n}
		Map<T, Integer> vertexToInt = new HashMap<>();
		Map<Integer, T> intToVertex = new HashMap<>();
		int i = 1;
		for (T v : graph) {
			vertexToInt.put(v, i);
			intToVertex.put(i, v);
			i = i + 1;
		}
		
		// create a new formula
		Formula phi = new Formula();
		
		// for each non-edge, one of the two vertices can not be in the clique
		for (T v : graph) {
			for (T w : graph) {
				if (v.compareTo(w) == 0) continue;
				if (graph.isAdjacent(v, w)) continue;
				phi.addClause(-1*vertexToInt.get(v), -1*vertexToInt.get(w));
			}
		}		
				
		// track solution with cardinality constraint
		int k = 1;		
		IncrementalCardinalityEncoder cardinality = new IncrementalCardinalityEncoder();
		cardinality.initALK(phi, intToVertex.keySet(), k);
		
		// incremental search for clique
		sat.addFormula(phi);
		while (sat.solve()) {
			
			// store the found clique
			this.model.clear();
			Map<Integer, Boolean> currentModel = sat.getModel();
			for (T v : graph) {
				if (currentModel.get(vertexToInt.get(v))) this.model.add(v);
			}
			
			// try to improve
			k = k + 1;
			Formula psi = new Formula();
			psi.setHighestVariable(phi.getHighestVariable());
			cardinality.incrementALK(psi, k);
			sat.addFormula(psi);
			phi.setHighestVariable(psi.getHighestVariable());
			
		}
		
		// done
		return k-1;
	}

	/**
	 * Returns a model, i.e, a clique of maximum size.
	 * This method only returns a not empty set if call() was successfully called before.
	 * @return
	 */
	public Set<T> getModel() {
		return this.model;
	}
	
	@Override
	public Integer call() throws Exception {
		try {
			return cliqueSAT();
		} catch (Exception e) {
			return -1;
		}
	}

	@Override
	public Integer getCurrentSolution() {
		return null;
	}
	
}
