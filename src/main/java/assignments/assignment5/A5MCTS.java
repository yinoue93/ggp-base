package assignments.assignment5;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;


class Node{
	private final static int EXPANSION_VISIT_COUNT = 10;
	private static List<Role> roles;
	private static Role player;
	private static StateMachine machine;
	private static long finishBy;

	private boolean doMax;
	private int currRoleIndx;
	private int totalVisits;
	private int[] childVisits;
	private int[] childUtilitySums;
	private Node[] childNodes;
	private Node pNode;
	private MachineState state;
	private List<Move> legalMoves;

	// Accessors
	public int[] getVisitCount(){return childVisits;}
	public int getTotalVisitCount(){return totalVisits;}
	public Node[] getChildren(){return childNodes;}
	public Node getParent(){return pNode;}
	public int[] getUtilitySums(){return childUtilitySums;}
	public List<Move> getLegalMoves(){return legalMoves;}
	public MachineState getState(){return state;}
	public StateMachine getMachine(){return machine;}
	public Role getPlayer(){return player;}
	public Role getCurrRole(){return roles.get(currRoleIndx);}
	public boolean getDoMax(){return doMax;}


	public Node(StateMachine m, MachineState s, long finishTime, int nodeRoleIndx, List<Role> rs, Role playerR)
					throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{

		if(m!=null){
			roles = rs;
			player = playerR;
			machine = m;
			finishBy = finishTime;
		}

		state = s;
		currRoleIndx = nodeRoleIndx;
		doMax = (player.getName()==roles.get(currRoleIndx).getName());

		legalMoves = machine.getLegalMoves(state, roles.get(currRoleIndx));
		int moveSz = legalMoves.size();
		childVisits = new int[moveSz];
		childNodes = new Node[moveSz];
		childUtilitySums = new int[moveSz];

		for(int i=0; i<moveSz; i++){
			childVisits[i]++;
			totalVisits++;
			childUtilitySums[i] += playout(i);
		}
	}

	public Node(MachineState s, int nodeRoleIndx)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		this(null, s, 0, nodeRoleIndx, null, null);
	}

	public int playout(int mvIndx) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException{
		Move mv = legalMoves.get(mvIndx);
		List<Move> M = machine.getRandomJointMove(state, roles.get(currRoleIndx), mv);
		return playout(M, state);
	}

	public int playout(List<Move> M, MachineState fromState) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException{
		if(System.currentTimeMillis() > finishBy) {return 0;}

		MachineState toState = machine.getNextState(fromState, M);
		if(machine.isTerminal(toState)) {return machine.getGoal(toState, player);}

		List<Move> randomM = machine.getRandomJointMove(toState);
		return playout(randomM, toState);
	}

	public boolean checkExpansion(int indx) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if(childVisits[indx]==EXPANSION_VISIT_COUNT){
			// EXPANSION_VISIT_COUNT exceeded, expand the node
			List<Move> M = machine.getRandomJointMove(state, roles.get(currRoleIndx), legalMoves.get(indx));
			MachineState childState = machine.getNextState(state, M);

			// if the next state is terminal, do not create a node
			if(machine.isTerminal(childState)) {return false;}

			int nextPlayerIndx = (currRoleIndx+1)%(roles.size());
			Node newChild = new Node(childState, nextPlayerIndx);
			childNodes[indx] = newChild;

			return true;
		}
		return false;
	}

	public Move getBestMove(){
		System.out.println("\nFinding the best move...");
		for(int i=0; i<childUtilitySums.length; i++){
			System.out.println(legalMoves.get(i));
			System.out.println("UtilitySum: "+childUtilitySums[i]+" Visits: "+childVisits[i]+" Utility: "+childUtilitySums[i]/childVisits[i]);
		}
		int bestScore = 0;
		Move bestMv = null;

		for(int i=0; i<childVisits.length; i++){
			int score = childUtilitySums[i]/childVisits[i];
			if(score>bestScore){
				bestScore = score;
				bestMv = legalMoves.get(i);
			}
		}
		return bestMv;
	}

	public void update(int nodeIndx, int[] playoutScore){
		childVisits[nodeIndx] += playoutScore.length;
		totalVisits += playoutScore.length;
		for(int score : playoutScore){
			childUtilitySums[nodeIndx] += score;
		}
	}
}

public class A5MCTS extends SampleGamer {

	private final static int TIMEOUT_BUFFER = 2000;
	private final static int EXPLORATION_PARAM = 8;

	private Node root = null;
	private int currPlayerIndx = 0;

	private long finishBy;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

	}

	public Role getNextPlayer(Role currPlayer){
		List<Role> roles = getStateMachine().getRoles();
		int oldIndx = roles.indexOf(currPlayer);
		int newIndx = (oldIndx+1==roles.size()) ? 0:(oldIndx+1);

		return roles.get(newIndx);
	}

	public double mobilityRecurse(Role currPlayer, MachineState state2Eval, int steps) throws MoveDefinitionException, TransitionDefinitionException{
		StateMachine machine = getStateMachine();
		Role player = getRole();

		if((System.currentTimeMillis() > finishBy) || steps==0 || machine.isTerminal(state2Eval)) {
			double moveSz = machine.getLegalMoves(state2Eval, currPlayer).size();
			double feasibleSz = machine.findActions(player).size();
			return moveSz/feasibleSz;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state2Eval, currPlayer);

		int nextStep = (player==currPlayer) ? steps-1:steps;
		Role nextPlayer = getNextPlayer(currPlayer);
		double results = 0;
		for (Move m : moves){
			List<Move> M = machine.getRandomJointMove(state2Eval, currPlayer, m);
			MachineState nextState = machine.getNextState(state2Eval, M);

			results += mobilityRecurse(nextPlayer, nextState, nextStep);
		}

		return results/moves.size();
	}

	public int mobility(MachineState state2Eval, int steps) throws MoveDefinitionException, TransitionDefinitionException{
		Role player = getRole();
		double mobilityScore = mobilityRecurse(player, state2Eval, steps);
		return (int)(mobilityScore * 100);
	}

	public int focus(MachineState state2Eval, int steps) throws MoveDefinitionException, TransitionDefinitionException{
		int mobilityScore = mobility(state2Eval, steps);

		return 100 - mobilityScore;
	}

	public int getUtility(MachineState state2Eval, int steps) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();

		List<Integer> Goals = machine.getGoals(state2Eval);
		if((System.currentTimeMillis() > finishBy) || steps==0 || machine.isTerminal(state2Eval)) {
			return Goals.get(0);
		}
		int utilityValue = 0;
		for (int goal : Goals){
			if (goal > utilityValue) {
				utilityValue = goal;
			}
		}


		return utilityValue;
	}

	public int minOpponentMobility(MachineState state2Eval, int steps, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException {
		StateMachine machine = getStateMachine();

		int totalMoves = 0;
		List<Move> numMoves = machine.getLegalMoves(state2Eval, opponents.get(0));
		if((System.currentTimeMillis() > finishBy) || steps==0 || machine.isTerminal(state2Eval)) {
			return numMoves.size();
		}
		for (Role opponent : opponents) {
			List<Move> oppMoves = machine.getLegalMoves(state2Eval, opponent);
			totalMoves = totalMoves + oppMoves.size();

		}
		int avg = totalMoves/opponents.size();

		return 100-avg;
	}

	public int evalfn(MachineState state2Eval, int steps, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		double coef_mobility = 0;
		double coef_opp_mobility = 0;
		double coef_utility = 0;
		double coef_montecarlo = 1;

		int mobilityScore = (coef_mobility==0) ? 0:mobility(state2Eval, steps);
		int opponentMobility = (coef_opp_mobility==0) ? 0:minOpponentMobility(state2Eval, steps, opponents);
		int utilityValue = (coef_utility==0) ? 0:getUtility(state2Eval, steps);
//		int monteScore = (coef_montecarlo==0) ? 0:montecarlo(player, state2Eval);
		int monteScore = 0;

		int score = (int)(coef_opp_mobility*opponentMobility
						  + coef_utility*utilityValue
						  + coef_mobility*mobilityScore
						  + coef_montecarlo*monteScore);
//		System.out.println("Score: " + score);
		return score;
	}

	public int scoreRecurse(Role currPlayer, MachineState state2Eval, boolean doMax, int level, int steps, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		StateMachine machine = getStateMachine();
		Role player = getRole();

		if(machine.isTerminal(state2Eval)) {
			return machine.getGoal(state2Eval, player);
		}
		else if(System.currentTimeMillis() > finishBy){
			return doMax ? 0:100;
		}
		else if(doMax && level==0){
			return evalfn(state2Eval, steps, opponents);
		}
		else if(!doMax){
			level--;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state2Eval, currPlayer);
		int score = doMax ? 0:100;

		Role nextPlayer = getNextPlayer(currPlayer);
		for (Move m : moves){
			List<Move> M = machine.getRandomJointMove(state2Eval, currPlayer, m);
			MachineState nextState = machine.getNextState(state2Eval, M);

			int result = scoreRecurse(nextPlayer, nextState, !doMax, level, steps, opponents);

			if((doMax && result==100) || (!doMax && result==0)){return result;}
			score = doMax ? Math.max(result, score):Math.min(result, score);
		}

		return score;
	}

	public double selectfn(int util, int visits, int pVisits, int doMax){
		return doMax*((double)util)/visits + EXPLORATION_PARAM*Math.sqrt(Math.log(pVisits)/visits);
	}

	public int[] mcts(Node n) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		int[] utils = n.getUtilitySums();
		int[] visits = n.getVisitCount();
		Node[] children = n.getChildren();
		int totalVisits = n.getTotalVisitCount();

		double bestScore = -101;
		int nodeIndx = 0;
		Node result = null;
		int doMax = n.getDoMax() ? 1:-1;
		for(int i=0; i<utils.length; i++){
			double newscore = selectfn(doMax, utils[i], visits[i], totalVisits);
			if(newscore>bestScore){
				bestScore = newscore;
				result = children[i];
				nodeIndx = i;
			}
		}

		if(result==null){
			int[] playoutScore;
			// expansion
			if(n.checkExpansion(nodeIndx)){
				System.out.println("Expanding node...");
				playoutScore = n.getChildren()[nodeIndx].getUtilitySums();
			}
			// simulation
			else{
				playoutScore = new int[1];
				playoutScore[0] = n.playout(nodeIndx);
			}

			// backpropagation
			n.update(nodeIndx, playoutScore);
			return playoutScore;
		}

		// backpropagation
		int[] playoutScore = mcts(result);
		n.update(nodeIndx, playoutScore);

		return playoutScore;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=");
		System.out.println(currPlayerIndx);
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=");

		long start = System.currentTimeMillis();
		finishBy = timeout - TIMEOUT_BUFFER;

		StateMachine machine = getStateMachine();
		Role player = getRole();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), player);
		List<Role> players = machine.getRoles();
		int numPlayers = players.size();

		Move selection = null;

		// create the root node if this is the first time
		List<GdlTerm> mostRecentMove = getMatch().getMostRecentMoves();
		if(mostRecentMove == null){
			root = new Node(machine, getCurrentState(), finishBy,
							players.indexOf(player), players, player);
		}
		// update the root as the subtree from the previous turn
		else{
			// update the root node
			int prevPlayerIndx = (currPlayerIndx-1+numPlayers) % numPlayers;

			// figure out the last move taken
			Move prevMove = machine.getMoveFromTerm(mostRecentMove.get(prevPlayerIndx));
			System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+");
			System.out.println("Move taken in last turn: " + prevMove);
			System.out.println("_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+_+");

			// choose the correct subtree and set it as the root node
			List<Move> childrenMoves = root.getLegalMoves();
			int indexChosen = childrenMoves.indexOf(prevMove);
			Node[] chNode = root.getChildren();
			System.out.println(indexChosen);
			for(int i=0; i<chNode.length; i++){
				System.out.println(i + ": " + childrenMoves.get(i));
				System.out.println(chNode[i]);
			}

			// the subtree was not expanded fully- recreate root
			if(indexChosen==-1 || root.getChildren()[indexChosen]==null){
				System.out.println("Recreating root node...");
				root = new Node(machine, getCurrentState(), finishBy,
								players.indexOf(player), players, player);
			}
			else{
				root = root.getChildren()[indexChosen];
			}

			// for debugging on tic tac toe
//					System.out.println("====================================");
//					System.out.println("Actual Board:");
//					Util.printTicTacToeState(getCurrentState());
//					System.out.println("Expected:");
//					Util.printTicTacToeState(root.getState());
//					System.out.println("====================================");
		}

		// run MCTS if there are more than 1 move available
		if(moves.size()>1){
			while(System.currentTimeMillis() < finishBy) {
				mcts(root);
			}
			// choose the action
			selection = root.getBestMove();
		}

		if(selection==null){
			selection = moves.get(0);
		}

		Util.printTree(root, "");

		long stop = System.currentTimeMillis();

		currPlayerIndx = (currPlayerIndx+1) % numPlayers;
		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public String getName() {
		return "A5MCTS";
	}
}