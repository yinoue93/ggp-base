package assignments.assignment5;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

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

	public static double selectfn(int utilNum, int utilDenom, int pVisits, int visits, int doMax){
		return doMax*((double)utilNum)/utilDenom + GlobalA5.EXPLORATION_PARAM*Math.sqrt(Math.log(pVisits)/visits);
	}

	public static void printTree(Node n, String prefix, int depth) throws MoveDefinitionException, TransitionDefinitionException{
		if(depth==0){return;}

		Node[] children = n.getChildren();
		StateMachine machine = n.getMachine();
		MachineState state = n.getState();
		Role currRole = n.getCurrRole();
		List<Move> legalMoves = machine.getLegalMoves(state, currRole);

		int[] childUtilNum = n.getChildUtilNum();
		int[] childUtilDenom = n.getChildUtilDenom();

		int totalVisits = n.getTotalVisits();
		int[] visits = n.getChildVisits();

		int doMax = n.getDoMax() ? 1:-1;
		System.out.println(prefix + "--Total Visits:" + totalVisits + "--");
		for(int i=0; i<childUtilNum.length; i++){
//			System.out.println("---------------------------------------");

			System.out.println(prefix + "Move: " + legalMoves.get(i) + " UtilNum: " + childUtilNum[i] +
								" UtilDenom: " + childUtilDenom[i] + " Utility: " + (double)childUtilNum[i]/childUtilDenom[i] +
								" Visits: " + visits[i] + " SelectFn: "
								+ selectfn(childUtilNum[i], childUtilDenom[i], totalVisits, visits[i], doMax));

			if(children[i]!=null)
				printTree(children[i], prefix+"    ", depth-1);
		}
	}

}
