package assignments.assignment5;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

class GlobalA5{
	// constants
	public static final int UPDATE_SCORE_INDX = 0;
	public static final int UPDATE_VISIT_INDX = 1;
	public final static int TIMEOUT_BUFFER = 2000;

	// hyperparameters
	public final static int EXPLORATION_PARAM = 12;
	public static final int EXPANSION_VISIT_COUNT = 8;
}

public class A5MCTS extends SampleGamer {

	private Node root = null;
	private int currPlayerIndx = 0;

	private long finishBy;

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

	}

	public double selectfn(int utilNum, int utilDenom, int pVisits, int visits, int doMax){
		return doMax*((double)utilNum)/utilDenom + GlobalA5.EXPLORATION_PARAM*Math.sqrt(Math.log(pVisits)/visits);
	}

	public int[] mcts(Node n) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		int[] childUtilNum = n.getChildUtilNum();
		int[] childUtilDenom = n.getChildUtilDenom();
		Node[] children = n.getChildren();

		int totalVisits = n.getTotalVisits();
		int[] visits = n.getChildVisits();

		double bestScore = -101;
		int nodeIndx = 0;
		Node result = null;
		int doMax = n.getDoMax() ? 1:-1;
		for(int i=0; i<childUtilNum.length; i++){
			double newscore = selectfn(childUtilNum[i], childUtilDenom[i], totalVisits, visits[i], doMax);
			if(newscore>bestScore){
				bestScore = newscore;
				result = children[i];
				nodeIndx = i;
			}
		}

		if(result==null){
			int[] playoutResult = new int[2];
			// expansion
			if(n.checkExpansion(nodeIndx)){
//				System.out.print(".");
				int[] childrenScores = n.getChildren()[nodeIndx].getChildUtilNum();
				for(int score : childrenScores){
					playoutResult[GlobalA5.UPDATE_SCORE_INDX] += score;
				}
				playoutResult[GlobalA5.UPDATE_VISIT_INDX] = GlobalA5.EXPANSION_VISIT_COUNT*childrenScores.length;
			}
			// simulation
			else{
				playoutResult[GlobalA5.UPDATE_SCORE_INDX] = n.playout(nodeIndx);
				playoutResult[GlobalA5.UPDATE_VISIT_INDX] = 1;
			}

			// backpropagation
			n.update(nodeIndx, playoutResult);
			return playoutResult;
		}

		// backpropagation
		int[] playoutResult = mcts(result);
		n.update(nodeIndx, playoutResult);

		return playoutResult;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=");
		System.out.println(currPlayerIndx);
		System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=");

		StateMachine machine = getStateMachine();
		Role player = getRole();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), player);
		List<Role> players = machine.getRoles();
		int numPlayers = players.size();

		Move selection = null;

		// create the root node if this is the first time
		List<GdlTerm> mostRecentMove = getMatch().getMostRecentMoves();
		if(mostRecentMove == null){
			root = new Node(machine, getCurrentState(), players.indexOf(player), players, player);
			currPlayerIndx = 0;
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
				root = new Node(machine, getCurrentState(), players.indexOf(player), players, player);
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

		// set finish time
		long start = System.currentTimeMillis();
		finishBy = timeout - GlobalA5.TIMEOUT_BUFFER;
		root.setFinishBy(finishBy);

		// run MCTS if there are more than 1 move available
		if(moves.size()>1){
			while(System.currentTimeMillis() < finishBy) {
				mcts(root);
			}

			Util.printTree(root, "", 4);

			// choose the action
			selection = root.getBestMove();
		}

		if(selection==null){
			selection = moves.get(0);
		}

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