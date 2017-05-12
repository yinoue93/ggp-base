package assignments.assignment5;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class Node{

	private static List<Role> roles;
	private static Role player;
	private static StateMachine machine;
	private static long finishBy;

	private boolean doMax;
	private int currRoleIndx;
	private int totalVisits;
	private int[] childVisits;
	private int[] childUtilDenom;
	private int[] childUtilNum;
	private Node[] childNodes;
	private MachineState state;
	private List<Move> legalMoves;

	// Accessors
	public int[] getChildVisits() {return childVisits;}
	public int getTotalVisits() {return totalVisits;}
	public int[] getChildUtilDenom() {return childUtilDenom;}
	public Node[] getChildren() {return childNodes;}
	public int[] getChildUtilNum() {return childUtilNum;}
	public List<Move> getLegalMoves() {return legalMoves;}
	public MachineState getState() {return state;}
	public StateMachine getMachine() {return machine;}
	public Role getPlayer() {return player;}
	public Role getCurrRole() {return roles.get(currRoleIndx);}
	public boolean getDoMax() {return doMax;}

	// Mutators
	public void setFinishBy(long f) {finishBy = f;}

	public Node(StateMachine m, MachineState s, int nodeRoleIndx, List<Role> rs, Role playerR)
					throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{

		if(m!=null){
			roles = rs;
			player = playerR;
			machine = m;
		}

		state = s;
		currRoleIndx = nodeRoleIndx;
		doMax = (player.getName()==roles.get(currRoleIndx).getName());

		legalMoves = machine.getLegalMoves(state, roles.get(currRoleIndx));
		int moveSz = legalMoves.size();
		childUtilDenom = new int[moveSz];
		childNodes = new Node[moveSz];
		childUtilNum = new int[moveSz];

		childVisits = new int[moveSz];
		totalVisits = moveSz;

		for(int i=0; i<moveSz; i++){
			childVisits[i] = 1;
			childUtilDenom[i] += GlobalA5.EXPANSION_VISIT_COUNT;
			for(int j=0; j<GlobalA5.EXPANSION_VISIT_COUNT; j++){
				childUtilNum[i] += playout(i);
			}
		}
	}

	public Node(MachineState s, int nodeRoleIndx)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		this(null, s, nodeRoleIndx, null, null);
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
		if(childUtilDenom[indx]==GlobalA5.EXPANSION_VISIT_COUNT){
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
		for(int i=0; i<childUtilNum.length; i++){
			System.out.println(legalMoves.get(i));
			System.out.println("UtilNum: " + childUtilNum[i] + " UtilDenom: " + childUtilDenom[i]
								+ " Utility: " + (double)childUtilNum[i]/childUtilDenom[i] + " Visits: " + childVisits[i]);
		}
		double bestScore = 0;
		Move bestMv = null;

		for(int i=0; i<childUtilDenom.length; i++){
			double score = (double)childUtilNum[i]/childUtilDenom[i];
			if(score>bestScore){
				bestScore = score;
				bestMv = legalMoves.get(i);
			}
		}
		return bestMv;
	}

	public void update(int nodeIndx, int[] playoutResult){
		// update the values
		childVisits[nodeIndx] += 1;
		totalVisits += 1;

		childUtilDenom[nodeIndx] += playoutResult[GlobalA5.UPDATE_VISIT_INDX];
		childUtilNum[nodeIndx] += playoutResult[GlobalA5.UPDATE_SCORE_INDX];

		// check if the values are getting too large
		if((childVisits[nodeIndx] >> 30) == 1){
			for(int i=0; i<childVisits.length; i++){
				childVisits[i] = (childVisits[i] >> 20);
			}
			totalVisits = totalVisits >> 20;
		}

		if((childUtilDenom[nodeIndx] >> 30) == 1){
			childUtilDenom[nodeIndx] = (childUtilDenom[nodeIndx] >> 20);
			childUtilNum[nodeIndx] = (childUtilNum[nodeIndx] >> 20);
		}
	}

}
