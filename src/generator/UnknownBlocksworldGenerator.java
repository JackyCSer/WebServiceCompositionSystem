package generator;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * 
 * Generator to create Blocksworld problem instances, where in the initial
 * state everything is unknown.
 * 
 * @author Manuela Ortlieb
 *
 */

abstract class Fact {
	protected boolean negated = false;
	
	public abstract void print(PrintStream printer);
	
	public Fact negate() {
		if (negated) {
			negated = false;
		}
		else {
			negated = true;
		}
		return this;
	}
	
	public abstract Fact id();
}

class On extends Fact {
	private int lower;
	private int upper;
	
	public On(int b1, int b2) {
		upper = b1;
		lower = b2;
	}
	
	public On(int b1, int b2, boolean negated) {
		this(b1, b2);
		this.negated = negated;
	}
	
	public void print(PrintStream printer) {
		printer.print(this);
	}
	
	@Override
	public String toString() {
		String str = "";
		if (negated) {
			str += "(not ";
		}
		str += "(on b" + upper + " b" + lower + ")";
		if (negated) {
			str += ")";
		}
		return str;
	}

	@Override
	public Fact id() {
		return new On(upper, lower, negated);
	}
}

class OnTable extends Fact {
	private int block;
	
	public OnTable(int b) {
		block = b;
	}
	
	public OnTable(int b, boolean negated) {
		this(b);
		this.negated = negated;
	}
	
	public void print(PrintStream printer) {
		if (negated) {
			printer.print("(not ");
		}
		printer.print("(on-table b" + block + ") ");
		if (negated) {
			printer.print(")");
		}
	}

	@Override
	public Fact id() {
		return new OnTable(block, negated);
	}
}

class Clear extends Fact {
	private int block;
	
	public Clear(int b) {
		block = b;
	}
	
	public Clear(int b, boolean negated) {
		this(b);
		this.negated = negated;
	}
	
	public void print(PrintStream printer) {
		if (negated) {
			printer.print("(not ");
		}
		printer.print("(clear b" + block + ") ");
		if (negated) {
			printer.print(")");
		}
	}

	@Override
	public Fact id() {
		return new Clear(block, negated);
	}
}

public class UnknownBlocksworldGenerator {
	
	public static int SEED = 8;
	
	public static double PROB_OF_STACKING = 0.0;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int numberOfBlocks = new Integer(args[0]);
		
		PrintStream printer = System.out;
		
		printer.println("(define (problem BW-unknown-" + numberOfBlocks + ")");
		String spaces = "";
		printer.println(spaces + "(:domain blocksworld)");
		printer.print(spaces + "(:objects ");
		for (int i = 1; i <= numberOfBlocks; i++) {
			printer.print("b" + i + " ");
		}
		printer.println(")");
		
		printInitUnknown(printer, numberOfBlocks);
		
		// each block is either on a table or on an other block
		for (int i = 1; i <= numberOfBlocks; i++) {
			printer.print(spaces + "(oneof (on-table b" + i + ") ");
			for (int j = 1; j <= numberOfBlocks; j++) {
				if (i != j) {
					printer.print("(on b" + i + " b" + j + ") ");
				}
			}
			printer.println(")");
		}
		
		// each block is either clear or an other block is on it
		for (int i = 1; i <= numberOfBlocks; i++) {
			printer.print(spaces + "(oneof (clear b" + i + ") ");
			for (int j = 1; j <= numberOfBlocks; j++) {
				if (i != j) {
					printer.print("(on b" + j + " b" + i + ") ");
				}
			}
			printer.println(")");
		}
		
		// Create facts
		List<Fact> blocksOnTable = new ArrayList<Fact>(numberOfBlocks);
		List<Fact> blocksClear = new ArrayList<Fact>(numberOfBlocks);
		for (int i = 1; i <= numberOfBlocks; i++) {
			blocksOnTable.add(new OnTable(i));
			blocksClear.add(new Clear(i));
		}
		
		// at least one block is on table
		exists(printer, blocksOnTable);
		
		// at least one block is clear
		exists(printer, blocksClear);

		// no two blocks are on the same other block	
	    for (int i = 1; i <= numberOfBlocks; i++) {
	    	for (int j = 1; j <= numberOfBlocks; j++) {
	    		for (int k = 1; k <= numberOfBlocks; k++) {
	    			if (i != j && i != k && k != j) {
	    				printer.println(spaces + "(or (not (on b" + i + " b" + j + ")) (not (on b" + k + " b" + j + ")))");
	    			}
	    		}
	    	}
	    }
	    
	    // no cyclic dependencies between blocks
	    noCyclesOfBlocks(printer, numberOfBlocks);
	    
	    printer.println(")");
	    createRandomGoal(printer, numberOfBlocks);
	}
	
	private static void printInitUnknown(PrintStream printer, int numberOfBlocks) {
		printer.println("(:init");
		for (int i = 1; i <= numberOfBlocks; i++) {
			printer.println("(unknown (on-table b" + i + "))");
			printer.println("(unknown (clear b" + i + "))");
			for (int j = 1; j <= numberOfBlocks; j++) {
				if (i != j) {
					printer.println("(unknown (on b" + i + " b" + j + "))");
				}
			}
			
		}
	}
	
	private static void createRandomGoal(PrintStream printer, int numberOfBlocks) {
		printer.println("(:goal");
		printer.println("(and");
		List<Integer> blockpositions = new LinkedList<Integer>(); // 1 => block b1, 2 => block b2 ...
		Random generator = new Random(SEED);
		for (int i = 1; i <= numberOfBlocks; i++) {
			double rand = generator.nextDouble();
			if (blockpositions.size() > 0 && rand >= 1 - PROB_OF_STACKING) {
				// choose a block for block i
				int j = generator.nextInt(blockpositions.size());
				printer.println("(on b" + i + " b" + blockpositions.get(j) + ")");
				blockpositions.remove(j);
				blockpositions.add(i);
			}
			else {
				// put block i on the table
				printer.println("(on-table b" + i + ")");
				blockpositions.add(i);
			}
		}
		printer.println(")");
		printer.println(")");
		printer.println(")");
	}
	
	private static void exists(PrintStream printer, List<Fact> facts) { // or at least one
		printer.print("(or ");
		for (Fact fact : facts) {
			fact.print(printer);
		}
		printer.println(")");
	}
	
	private static void and(PrintStream printer, List<Fact> facts) {
		assert !facts.isEmpty();
		if (facts.size() == 1) {
			facts.get(0).print(printer);
		}
		else {
			printer.print("(not (or ");
			for (Fact fact : facts) {
				fact.id().negate().print(printer);
				printer.print(" ");
			}
			printer.print("))");
		}
	}
	
//	private static void not(PrintStream printer, Fact fact) {
//		printer.print("(not ");
//		fact.print(printer);
//		printer.print(")");
//	}
	
//	private static void implication(PrintStream printer, Fact a, Fact b) {
//		printer.print("(or ");
//		not(printer, a);
//		b.print(printer);
//		printer.print(")");
//	}
	
	private static void noCyclesOfBlocks(PrintStream printer, int numberOfBlocks) {
		printer.println();
		printer.println("; No cyclic dependencies of blocks");
		for (int i = 1; i < numberOfBlocks; i++) {
			for (int j = i + 1; j <= numberOfBlocks; j++) {
				List<int[]> listOfBlocks = createArrays(i, j, numberOfBlocks);
				for (int[] blocks : listOfBlocks) {
					List<int[]> permutations = new ArrayList<int[]>();
					permute(blocks.length, blocks, permutations);
					// Now we have all permutations of given blocks.
					for (int[] permutation : permutations) {
						printNoCycleCondition(permutation, printer);
					}
				}
			}
		}
	}
	
	private static List<int[]> createArrays(int i, int j, int numberOfBlocks) {
		assert j > i;
		List<int[]> result = new ArrayList<int[]>();
		result.add(new int[]{i, j});
		LinkedList<int[]> queue = new LinkedList<int[]>();
		if (j < numberOfBlocks) {
			queue.add(result.get(0));
		}
		while (!queue.isEmpty()) {
			// extend the array in the queue
			int[] array = queue.poll();
			int maxInt = array[array.length - 1];
			for (int k = maxInt + 1; k <= numberOfBlocks; k++) {
				int[] successor = Arrays.copyOf(array, array.length + 1);
				successor[array.length] = k;
				result.add(successor);
				// if this successor array is extendible, add it to the queue
				if (k < numberOfBlocks) {
					queue.add(successor);
				}
			}
		}
		return result;
	}
	
	private static void printNoCycleCondition(int[] permutation, PrintStream printer) {
		assert permutation.length >= 2;
		List<Fact> facts = new ArrayList<Fact>(permutation.length - 1); 
		for (int i = 0; i < permutation.length - 1; i++) {
			facts.add(new On(permutation[i], permutation[i+1]));
		}
		printer.print("(or (not ");
		and(printer, facts);
		printer.print(") ");
		Fact fact = new On(permutation[permutation.length - 1], permutation[0], true);
		fact.print(printer);
		printer.println(")");
	}


	public static void permute(int n, int[] array, List<int[]> result) {
		if (n == 0) { 
			result.add(Arrays.copyOf(array, array.length));
		} 
		else {
			for (int i = 0; i < n; i++) {
				swap(array, i, n-1); 
				permute(n-1, array, result); 
				swap(array, i, n-1); 
			}
		}
	}

	private static void swap(int[] array, int i, int n) { 
		int temp = array[i]; 
		array[i] = array[n];
		array[n] = temp; 
	}
	
	public static void print(int[] array, PrintStream printer) {
		printer.print("[");
		for (int i = 0; i < array.length; i++) {
			printer.print(array[i]);
			if (i < array.length - 1) {
				printer.print(", ");
			}
		}
		printer.println("]");

	}
}
