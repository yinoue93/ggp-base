package assignments.assignment4;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class A4MCS extends SampleGamer {

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

	public void printTicTacToeState(MachineState state2Eval){
		String stateStr = state2Eval.toString();

		char[] ticStr = new char[12];
		ticStr[3] = '\n';
		ticStr[7] = '\n';
		ticStr[11] = '\n';
		Matcher m = Pattern.compile("cell [1-3] [1-3] [xob]").matcher(stateStr);
		while (m.find()) {
			String cellStr = m.group();
			int row = Character.getNumericValue(cellStr.charAt(5)) - 1;
			int col = Character.getNumericValue(cellStr.charAt(7)) - 1;
			char symbol = cellStr.charAt(9);

			ticStr[col*4+row] = symbol;
		}

		for(char c : ticStr){
			System.out.print(c);
		}
	}

	public double mobilityRecurse(StateMachine machine, Role player, Role currPlayer, MachineState state2Eval, int steps, long finishBy) throws MoveDefinitionException, TransitionDefinitionException{
		if((System.currentTimeMillis() > finishBy) || steps==0 || machine.isTerminal(state2Eval)) {
			double moveSz = machine.getLegalMoves(state2Eval, currPlayer).size();
			double feasibleSz = machine.findActions(player).size();
			return moveSz/feasibleSz;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state2Eval, currPlayer);

		int nextStep = (player==currPlayer) ? steps-1:steps;
		Role nextPlayer = getNextPlayer(machine, currPlayer);
		double results = 0;
		for (Move m : moves){
			List<Move> M = machine.getRandomJointMove(state2Eval, currPlayer, m);
			MachineState nextState = machine.getNextState(state2Eval, M);

			results += mobilityRecurse(machine, player, nextPlayer, nextState, nextStep, finishBy);
		}

		return results/moves.size();
	}

	public int mobility(StateMachine machine, Role player, MachineState state2Eval, int steps, long finishBy) throws MoveDefinitionException, TransitionDefinitionException{
		double mobilityScore = mobilityRecurse(machine, player, player, state2Eval, steps, finishBy);
		return (int)(mobilityScore * 100);
	}

	public int focus(StateMachine machine, Role player, MachineState state2Eval, int steps, long finishBy) throws MoveDefinitionException, TransitionDefinitionException{
		int mobilityScore = mobility(machine, player, state2Eval, steps, finishBy);

		return 100 - mobilityScore;
	}

	public int getUtility(StateMachine machine, Role player, MachineState state2Eval, int steps, long finishBy) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
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

	public int minOpponentMobility(StateMachine machine, Role player, MachineState state2Eval, int steps, long finishBy, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException {
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

	public int montecarlo(StateMachine machine, Role player, MachineState state2Eval, long finishBy) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		int total = 0;
		int num_trials = 100;

		for(int i=0; i<num_trials; i++){
			total += playout(machine, player, state2Eval, finishBy);

			if(System.currentTimeMillis() > finishBy)
				return total/(i+1);
		}

		return total/num_trials;
	}

	public int playout(StateMachine machine, Role player, MachineState state2Eval, long finishBy) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException{
		if(System.currentTimeMillis() > finishBy) {return 0;}

		if(machine.isTerminal(state2Eval)) {return machine.getGoal(state2Eval, player);}

		List<Move> randomMoves = machine.getRandomJointMove(state2Eval);
		MachineState newState = machine.getNextState(state2Eval, randomMoves);

		return playout(machine, player, newState, finishBy);
	}

	public int evalfn(StateMachine machine, Role player, MachineState state2Eval, int steps, long finishBy, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		double coef_mobility = 0;
		double coef_opp_mobility = 0;
		double coef_utility = 0;
		double coef_montecarlo = 1;

		int mobilityScore = (coef_mobility==0) ? 0:mobility(machine, player, state2Eval, steps, finishBy);
		int opponentMobility = (coef_opp_mobility==0) ? 0:minOpponentMobility(machine, player, state2Eval, steps, finishBy, opponents);
		int utilityValue = (coef_utility==0) ? 0:getUtility(machine, player, state2Eval, steps, finishBy);
		int monteScore = (coef_montecarlo==0) ? 0:montecarlo(machine, player, state2Eval, finishBy);

		int score = (int)(coef_opp_mobility*opponentMobility
						  + coef_utility*utilityValue
						  + coef_mobility*mobilityScore
						  + coef_montecarlo*monteScore);
//		System.out.println("Score: " + score);
		return score;
	}

	public int scoreRecurse(StateMachine machine, Role playerRole, Role currPlayer, MachineState state2Eval, boolean doMax, long finishBy, int level, int steps, List<Role> opponents) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		if(machine.isTerminal(state2Eval)) {
			return machine.getGoal(state2Eval, playerRole);
		}
		else if(System.currentTimeMillis() > finishBy){
			return doMax ? 0:100;
		}
		else if(doMax && level==0){
			return evalfn(machine, playerRole, state2Eval, steps, finishBy, opponents);
		}
		else if(!doMax){
			level--;
		}

		List<Move> moves = getStateMachine().getLegalMoves(state2Eval, currPlayer);
		int score = doMax ? 0:100;

		Role nextPlayer = getNextPlayer(machine, currPlayer);
		for (Move m : moves){
			List<Move> M = machine.getRandomJointMove(state2Eval, currPlayer, m);
			MachineState nextState = machine.getNextState(state2Eval, M);

			int result = scoreRecurse(machine, playerRole, nextPlayer, nextState, !doMax, finishBy, level, steps, opponents);

			if((doMax && result==100) || (!doMax && result==0)){return result;}
			score = doMax ? Math.max(result, score):Math.min(result, score);
		}

		return score;
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();
		long finishBy = timeout - 2000;

		// level=10, mobilitySteps=3 scores 100 for Task 3.3
		int level = 1;
		int mobilitySteps = 3;
//		int level = 10;
//		int mobilitySteps = 3;

		StateMachine machine = getStateMachine();
		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		List<Role> opponents = machine.getRoles();
		//System.out.println(opponents);
		//opponents.remove(getRole());

		Move selection = moves.get(0);
		int score = 0;

		while(System.currentTimeMillis() < finishBy) {
			int result = 0;
			//System.out.println("Level: " + level);

			for(Move m : moves){
				List<Move> M = machine.getRandomJointMove(getCurrentState(), getRole(), m);
				MachineState nextState = machine.getNextState(getCurrentState(), M);

				result = scoreRecurse(machine, getRole(), getNextPlayer(machine, getRole()), nextState, false, finishBy, level, mobilitySteps, opponents);

				if(result==100){
					selection = m;
					break;
				}
				else if(result>score){
					score = result;
					selection = m;
				}
			}

			if(result == 100) {
				break;
			}

			level++;
		}

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
		return selection;
	}

	@Override
	public String getName() {
		return "A4MCS";
	}
}