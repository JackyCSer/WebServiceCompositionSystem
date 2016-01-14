package mynd.simulator;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mynd.Global;
import mynd.explicit.ExplicitAxiomEvaluator;
import mynd.explicit.ExplicitState;
import mynd.parser.SasParser;
import mynd.search.StateActionTable;
import mynd.state.Operator;
import mynd.util.Pair;


/**
 * 
 * @author Robert Mattmueller
 *
 */
public class PlanReader {
	
	ExplicitAxiomEvaluator axiomEvaluator = new ExplicitAxiomEvaluator();

    public static void main(String[] args) throws FileNotFoundException, IOException {
        String sasFilename = args[0];
        String planFilename = args[1];
        new SasParser().parse(new FileInputStream(sasFilename));
        assert (Global.problem.isFullObservable);
        //Global.problem.getSingleInitialState().uniqueID; TODO check this? was uniqueID();
        
        PlanReader reader = new PlanReader();
        StateActionTable plan = reader.readPlan(planFilename);

        //System.out.println(plan);
        double planCost = new PlanSimulator().performValueIteration(plan);

        System.err.println("Plan cost (expected number of steps to goal): " + planCost);
        System.out.println(plan.toStringPolicy());

    }

    public PlanReader() {}

    private int parseEntry(List<Pair<Integer, Integer>> propositionIndices, List<Operator> operators, String[] policy,
            StateActionTable plan, int currentIndex) {
        int numPreconditions = Integer.parseInt(policy[currentIndex]);

        List<Pair<Integer, Integer>> pairs = new ArrayList<Pair<Integer, Integer>>();
        for (int i = 0; i < numPreconditions; i++) {
            int precond = Integer.parseInt(policy[currentIndex + i + 1]);
            Pair<Integer, Integer> pair = propositionIndices.get(precond);
            if (pair != null) {
                pairs.add(pair);
            }
            else {
                // System.err.println("WARNING: Variable not found in SAS encoding. Maybe compiled away?");
            }
        }

        // FIXME: Can we really be sure that, if a proposition is false in the
        // Boolean encoding, and if there is no proposition in a variable group
        // that leads to the corresponding variable getting a defined value,
        // then that variable *must* have a "<none of those>" value, which is
        // the correct one to chose?
        int[] values = new int[Global.problem.variableNames.size()];
        Arrays.fill(values, -1);
        for (int i = 0; i < pairs.size(); i++) {
            int var = pairs.get(i).first;
            int val = pairs.get(i).second;
            assert var < values.length;
            assert values[var] == -1;
            values[var] = val;
        }
        for (int var = 0; var < values.length; var++) {
            if (values[var] == -1) {
                values[var] = Global.problem.domainSizes.get(var) - 1;

                String propositionName = Global.problem.propositionNames.get(var).get(values[var]);

                if (!propositionName.startsWith("(not ")) {
                    System.err.println("Ooops. Wrong value ...");
                    System.exit(1);
                }
            }
        }

        int action = Integer.parseInt(policy[currentIndex + numPreconditions + 1]);
        Operator operator = operators.get(action);

        if (operator != null) {
            plan.addEntry(new ExplicitState(values, axiomEvaluator), operator); // FIXME
        }
        else {
            // System.err.println("WARNING: Operator not found in SAS encoding. Maybe compiled away?");
        }

        return numPreconditions + 2;
    }

    private void parsePolicy(List<Pair<Integer, Integer>> propositionIndices, List<Operator> operators,
            String[] policy, StateActionTable plan) {
        int numEntries = Integer.parseInt(policy[1]);
        int currentIndex = 2;
        for (int i = 0; i < numEntries; i++) {
            System.err.println("Parsing policy entry " + i + " of " + numEntries);
            int offset = parseEntry(propositionIndices, operators, policy, plan, currentIndex);
            currentIndex += offset;
        }

    }

    private List<String> parsePropositionsOrActions(String propositions) {
        Pattern p = Pattern.compile("\\(.*?\\)");
        Matcher m = p.matcher(propositions);

        List<String> result = new ArrayList<String>();

        while (m.find()) {
            String find = m.group();
            // System.err.println(find);
            result.add(find);
        }

        return result;
    }

    private List<String> readFile(String filename) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String line;
        List<String> lines = new ArrayList<String>();
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.equals("")) {
                lines.add(line);
            }
        }
        reader.close();
        return lines;
    }

    public StateActionTable readPlan(String filename) throws FileNotFoundException, IOException {
        StateActionTable plan = new StateActionTable();
        List<String> lines = readFile(filename);
        assert lines.size() == 5;
        assert lines.get(1).equals("%%");
        assert lines.get(3).equals("%%");
        List<String> propositions = parsePropositionsOrActions(lines.get(0));
        List<String> actions = parsePropositionsOrActions(lines.get(2));

        List<Pair<Integer, Integer>> propositionIndices = translatePropositionIndices(propositions);
        List<Operator> operators = translateActionIndices(actions);

        String[] policy = lines.get(4).split(" ");

        parsePolicy(propositionIndices, operators, policy, plan);

        return plan;
    }

    private List<Operator> translateActionIndices(List<String> actions) {
        List<Operator> result = new ArrayList<Operator>();
        Set<Operator> ops = Global.problem.getOperators();
        Map<String, Operator> inverseOperatorNames = new HashMap<String, Operator>();
        for (Operator op : ops) {
            String name = op.getName();
            name = "(" + name + ")";
            inverseOperatorNames.put(name, op);
        }
        for (String action : actions) {
            Operator op = inverseOperatorNames.get(action);

            if (op == null) {
                System.err.println("WARNING: Removed unnecessary luna.action " + action);
            }

            result.add(op);
        }
        return result;
    }

    private List<Pair<Integer, Integer>> translatePropositionIndices(List<String> propositions) {
        List<Pair<Integer, Integer>> result = new ArrayList<Pair<Integer, Integer>>();
        List<List<String>> propositionNamesInProblem = Global.problem.propositionNames;
        Map<String, Pair<Integer, Integer>> inversePropositionNames = new HashMap<String, Pair<Integer, Integer>>();
        for (int var = 0; var < propositionNamesInProblem.size(); var++) {
            for (int val = 0; val < propositionNamesInProblem.get(var).size(); val++) {
                String propName = propositionNamesInProblem.get(var).get(val);
                inversePropositionNames.put(propName, new Pair<Integer, Integer>(var, val));
            }
        }
        for (String prop : propositions) {
            Pair<Integer, Integer> pair = inversePropositionNames.get(prop);
            result.add(pair);
        }
        return result;
    }
    

}
