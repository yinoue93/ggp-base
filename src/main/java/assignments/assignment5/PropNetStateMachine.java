package assignments.assignment5;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
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

//    private propTypeMap;

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
    	propNet.clearpropnet();
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
    	propNet.clearpropnet();
    	markbases(state);
    	Set<Proposition> goals = propNet.getGoalPropositions().get(role);
    	//if (goalMap.get(role).size() > 1) throw new GoalDefinitionException(state, role);

    	for(Proposition prop: goals) {
    		if(propmarkp(prop)) {
    			return getGoalValue(prop);
    		}
    	}
        return -1;
    }

    public void propagate(){
        // propagate
        for(Proposition p : ordering) {
        	p.setValue(p.getSingleInput().getValue());
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
    	propNet.clearpropnet();
    	markbases(state);
    	// Get legal propositions for role
    	Set<Proposition> legalProps = new HashSet<Proposition>();
    	legalProps = propNet.getLegalPropositions().get(role);

    	// Get legal moves

    	List<Move> moves = new ArrayList<Move>();
    	Map<Proposition, Proposition> legalInputMap = propNet.getLegalInputMap();

    	for(Proposition prop: legalProps) {
    		if (propmarkp(prop)) {

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
    	propNet.clearpropnet();
		markactions(moves);
		markbases(state);

		propagate();

		Set<Proposition> baseProps = new HashSet<Proposition>(propNet.getBasePropositions().values());
		Set<GdlSentence> nexts = new HashSet<GdlSentence>();

		//System.out.println("-----------getNextState()----------");
		for(Proposition prop: baseProps) {
			if(prop.getSingleInput().getSingleInput().getValue()) {
				nexts.add(prop.getName());
			}
		}

		/*System.out.println("****************");
		Util.printTicTacToeState(new MachineState(nexts));
		System.out.println("****************");
		System.exit(0);*/
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

//        for(int i = 0; i < propositions.size(); i++) {
//        	Proposition currProp = propositions.get(i);
//
//        	if(!baseProps.containsValue(currProp) && !inputProps.containsValue(currProp) && currProp!=initProp) {
//        		order.add(currProp);
//        	}
//        }

        List<Component> S = new LinkedList<Component>();
        for(Component c : components){
        	if((c.getInputs().size()==0) || (c.getSingleInput() instanceof Transition)){
        		S.add(c);
        	}
        }

        List<Component> L = new LinkedList<Component>();
        int V = components.size();
        Map<Component, Boolean> visited = new HashMap<Component, Boolean>();

        for (Component c : components){
        	visited.put(c, false);
        }

        for(Component c : visited.keySet()){
        	if(!visited.get(c)){
        		topoVisit(c, visited, L);
        	}
        }

        for(Component c : L){
        	if((c instanceof Proposition) && !((c.getInputs().size()==0) || (c.getSingleInput() instanceof Transition))){
        		order.add(0, (Proposition)c);
        	}
        }

        return order;
    }

    // a helper function for the topological sorting procedure
    public void topoVisit(Component n, Map<Component, Boolean> visited, List<Component> L){
    	visited.put(n, true);

    	for(Component m : n.getOutputs()){
    		if(!visited.get(m))
    			topoVisit(m, visited, L);
    	}

    	L.add(n);
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

    	for(GdlSentence gdl : stateGdls){
    		baseProps.get(gdl).setValue(true);
    	}
    }

    public void markactions(List<Move> moves){
    	List<GdlSentence> actionGdls = toDoes(moves);
    	Map<GdlSentence, Proposition> inputProps = propNet.getInputPropositions();

    	// create the proposition marking boolean array
    	for(GdlSentence gdl : actionGdls){
    		inputProps.get(gdl).setValue(true);
    	}
    }



    public boolean propmarkp (Component p){

    	// input or base
    	if (p.getInputs().size()==0 || p.getSingleInput() instanceof Transition) {
    		return p.getValue();
    	}

    	if (p instanceof Not) {return propmarknegation(p);}
    	if (p instanceof And) {return propmarkconjunction(p);}
    	if (p instanceof Or) {return propmarkdisjunction(p);}

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