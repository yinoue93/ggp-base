package assignments.assignment5;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class PropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
        try {
            propNet = OptimizingPropNetFactory.create(description);
            roles = propNet.getRoles();
            ordering = getOrdering();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	markbases(state);
    	return propmarkp(propNet.getTerminalProposition());
    }

    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	Map<Role, Set<Proposition>> goalMap = propNet.getGoalPropositions();
    	if (goalMap.get(role).size() > 1) throw new GoalDefinitionException(state, role);

    	for(Proposition prop: goalMap.get(role) ) {
    		return getGoalValue(prop);
    	}
        return -1;
    }

    public void propagate(){
        List<Proposition> allProps = getOrdering();

        // propagate
        for(int i = 0; i < allProps.size(); i++) {
        	Proposition currProp = allProps.get(i);
        	boolean newValue = propmarkp(currProp);
        	currProp.setValue(newValue);
        }
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
        Proposition initProp = propNet.getInitProposition();

        initProp.setValue(true);
        //propogate

        propagate();

        return getStateFromBase();
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.
        Set<Proposition> legalProps = new HashSet<Proposition>();
        legalProps = propNet.getLegalPropositions().get(role);

    // Get legal moves

        List<Move> moves = new ArrayList<Move>();
        Map<Proposition, Proposition> legalInputMap = propNet.getLegalInputMap();

        for(Proposition prop: legalProps) {
        	if (propmarkp(prop)) {

        		// CHANGED
        		Proposition inputProp = legalInputMap.get(prop);
        		moves.add(PropNetStateMachine.getMoveFromProposition(inputProp));
        	}
        }
        return moves;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {
        // TODO: Compute legal moves.

    	markbases(state);
    	// Get legal propositions for role
    	 Set<Proposition> legalProps = new HashSet<Proposition>();
    	 legalProps = propNet.getLegalPropositions().get(role);

    	// Get legal moves

    	 List<Move> moves = new ArrayList<Move>();
    	 Map<Proposition, Proposition> legalInputMap = propNet.getLegalInputMap();

    	 for(Proposition prop: legalProps) {
    		 if (propmarkp(prop)) {

    			 // CHANGED
    			 Proposition inputProp = legalInputMap.get(prop);
    			 moves.add(PropNetStateMachine.getMoveFromProposition(inputProp));
    		 }
    	 }
    	 return moves;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
        // TODO: Compute the next state.
		markactions(moves);
		markbases(state);

		propagate();
		Set<Proposition> baseProps = new HashSet<Proposition>(propNet.getBasePropositions().values());
		Set<GdlSentence> nexts = new HashSet<GdlSentence>();

		System.out.println("-----------getNextState()----------");
		for(Proposition prop: baseProps) {
			if(prop.getSingleInput().getSingleInput().getValue()) {
				nexts.add(prop.getName());
			}
		}

		System.out.println("****************");
		Util.printTicTacToeState(new MachineState(nexts));
		System.out.println("****************");
		System.exit(0);
    	return new MachineState(nexts);

    }

    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

        Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();
        Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();
        Proposition initProp = propNet.getInitProposition();

        for(int i = 0; i < propositions.size(); i++) {
        	Proposition currProp = propositions.get(i);

        	if(!baseProps.containsValue(currProp) && !inputProps.containsValue(currProp) && currProp!=initProp) {
        		order.add(currProp);
        	}
        }

       /* while(!components.isEmpty()) {
        	Component node = components.get(0);
        	for(int i = propositions.size() - 1; i >= 0; i--) {
        		if(node == propositions.get(i)) {
        			Proposition prop = propositions.get(i);
        			propositions.remove(i);
        			order.add(prop);
        			Set<Component> outputEdges = node.getOutputs();
        			Iterator<Component> it = outputEdges.iterator();
        			while(it.hasNext()) {
        				Component curr = it.next();
        				Set<Component> outputNodes = (curr.getOutputs());
        				for(int j = components.size() - 1; j >= 0; j--) {
    						if(components.get(j) == curr) {
    							components.remove(j);
    							break;
    						}
    					}

        				Iterator<Component> outputIt = outputNodes.iterator();
        				while(outputIt.hasNext()) {
        					Component connectedNode = outputIt.next();
        					Set<Component> inputSet = connectedNode.getInputs();
        					if(inputSet.size() == 1) {
        						components.add(connectedNode);
        					}
        				}

        			}
        		}
        	}
        } */



        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }

 // chapter 10 basis function implementations
    public void markbases(MachineState s){
    	Set<GdlSentence> stateGdls = s.getContents();

    	Map<GdlSentence, Proposition> baseProps = propNet.getBasePropositions();

    	// create the proposition marking boolean array
    	int count = 0;
    	boolean[] baseMarks = new boolean[baseProps.size()];
    	Proposition[] props = new Proposition[baseProps.size()];
    	for(GdlSentence g : baseProps.keySet()){
    		baseMarks[count] = stateGdls.contains(g);
    		props[count++] = baseProps.get(g);
    	}
    	System.out.println("actions: ");
    	for (int i=0; i<props.length; i++){
    		props[i].setValue(baseMarks[i]);
    		System.out.println(baseMarks[i] + " : " + props[i]);
    	}
    }

    public void markactions(List<Move> moves){
    	List<GdlSentence> actionGdls = toDoes(moves);

    	Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();

    	// create the proposition marking boolean array
//    	int count = 0;
//    	boolean[] actionMarks = new boolean[inputProps.size()];
//    	Proposition[] props = new Proposition[inputProps.size()];
    	for(GdlSentence g : inputProps.keySet()){
//    		actionMarks[count] = actionGdls.contains(g);
//    		props[count++] = inputProps.get(g);
    		// CHANGED
    		inputProps.get(g).setValue(actionGdls.contains(g));
    	}



//    	count = 0;
//    	for (Proposition p : props){
//
//    		p.setValue(actionMarks[count++]);
//    	}

/*    	for (int i=0; i<props.length; i++){
    		props[i].setValue(actionMarks[i]);
    	}*/
    }

    public void clearpropnet(){
    	Map<GdlSentence, Proposition> props = propNet.getBasePropositions();
	    for (Proposition p : props.values()){
    		p.setValue(false);
    	}
    }

    public boolean propmarkp (Component p){

    	if (p instanceof Constant) {return p.getValue();}
    	if (p instanceof Not) {return propmarknegation(p);}
    	if (p instanceof And) {return propmarkconjunction(p);}
    	if (p instanceof Or) {return propmarkdisjunction(p);}

    	Map<GdlSentence, Proposition> bases = propNet.getBasePropositions();
    	if (bases.values().contains(p)) {return p.getValue();}

    	Map<GdlSentence, Proposition> inputs = propNet.getInputPropositions();
    	if (inputs.values().contains(p)) {return p.getValue();}



    	if (p==propNet.getInitProposition()) {return p.getValue();}

    	return propmarkp(p.getSingleInput());
    }

    public boolean propmarknegation (Component p){
    	return !propmarkp(p.getSingleInput());
    }

    public boolean propmarkconjunction (Component p){
    	Set<Component> sources = p.getInputs();
    	for (Component c : sources){
    		if (!propmarkp(c)) {return false;}
    	}
    	return true;
    }

    public boolean propmarkdisjunction (Component p){
    	Set<Component> sources = p.getInputs();
    	for (Component c : sources){
    		if (propmarkp(c)) {return true;}
    	}
    	return false;
    }
}