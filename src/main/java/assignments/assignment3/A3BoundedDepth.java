package assignments.assignment3;

import java.util.List;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class A3BoundedDepth extends SampleGamer {

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{

	}

	public Role getNextPlayer(StateMachine machine, Role currPlayer){
		List<Role> roles = machine.getRoles();
		int oldIndx = roles.indexOf(currPlayer);
		int newIndx = (oldIndx+1==roles.size()) ? 0:(oldIndx+1);

		return roles.get(newIndx);
	}

	public int scoreRecurse(StateMachine machine, Role playerRole, Role currPlayer, MachineState state2Eval, boolean doMax, long finishBy) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if(machine.isTerminal(state2Eval)) {
			return machine.getGoal(state2Eval, playerRole);
		}
		else if(System.currentTimeMillis() > finishBy){
			return doMax ? 0:100;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state2Eval, currPlayer);
		int score = doMax ? 0:100;

		Role nextPlayer = getNextPlayer(machine, currPlayer);
		for (Move m : moves){
			List<Move> M = machine.getRandomJointMove(state2Eval, currPlayer, m);
			MachineState nextState = machine.getNextState(state2Eval, M);

			int result = scoreRecurse(machine, playerRole, nextPlayer, nextState, !doMax, finishBy);
			score = doMax ? Math.max(result, score):Math.min(result, score);
		}

		return score;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		long finishBy = timeout - 500;

		StateMachine machine = getStateMachine();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());

		Move selection = moves.get(0);
		int score = 0;
		for(Move m : moves){
			List<Move> M = machine.getRandomJointMove(getCurrentState(), getRole(), m);
			MachineState nextState = machine.getNextState(getCurrentState(), M);

			int result = scoreRecurse(machine, getRole(), getNextPlayer(machine, getRole()), nextState, false, finishBy);
			if(result>score){
				score = result;
				selection = m;
			}
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public String getName() {
		return "A3BoundedDepth";
	}
}
