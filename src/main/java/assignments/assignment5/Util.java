package assignments.assignment5;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class Util {

	public static void printTicTacToeState(MachineState state2Eval){
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

	public static void printTree(Node n, String prefix) throws MoveDefinitionException{
		int[] utils = n.getUtilitySums();
		int[] visits = n.getVisitCount();
		Node[] children = n.getChildren();
		int totalVisits = n.getTotalVisitCount();
		StateMachine machine = n.getMachine();
		MachineState state = n.getState();
		Role currRole = n.getCurrRole();
		List<Move> legalMoves = machine.getLegalMoves(state, currRole);

		System.out.println(prefix + "--Total Visits:" + totalVisits + "--");
		for(int i=0; i<utils.length; i++){
//			System.out.println("---------------------------------------");
			System.out.println(prefix + "Move: " + legalMoves.get(i) + " UtilitySum: " + utils[i] +
								" Utility: " + utils[i]/visits[i] + " Visits: " + visits[i]);
//			System.out.println(selectfn(utils[i], visits[i], totalVisits));

			if(children[i]!=null)
				printTree(children[i], prefix+"    ");
		}
	}

}
