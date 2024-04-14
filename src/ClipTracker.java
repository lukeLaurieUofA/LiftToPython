import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

public class ClipTracker {
	private Deque<PythonLine> codeIndentation;
	private ScopeTracker scopeTracker;
	
	public ClipTracker(ScopeTracker scopeTracker) {
		codeIndentation = new ArrayDeque();
		this.scopeTracker = scopeTracker;
	}

	public void addNewCodeline(String codeLine) {
		int indentAmount = 0;
		int nextIndentAmount = 0;

		if (!codeIndentation.isEmpty()) {
			indentAmount = codeIndentation.peek().getNextIndentCount();
			nextIndentAmount = codeIndentation.peek().getNextIndentCount();
		}
		// determine if the line contains a open or closing bracket
		String[] lineMaterial = codeLine.split(" ");
		String closing = lineMaterial.length == 0 ? "" : lineMaterial[lineMaterial.length - 1];
		// generate the next python line
		PythonLine pythonLine;
		if (closing.equals("leftWeightClip")) {
			nextIndentAmount = indentAmount + 1;
			scopeTracker.insertNewBlock();
		} else if (closing.equals("rightWeightClip")) {
			indentAmount = indentAmount - 1;
			nextIndentAmount = indentAmount;
			scopeTracker.endBlock();
		}
		pythonLine = new PythonLine(indentAmount, nextIndentAmount, codeLine);
		// add the new line to out deque
		codeIndentation.push(pythonLine);
	}

	public void displayLines() throws Exception {
		System.out.println("displaying lines now:");
		while (!codeIndentation.isEmpty()) {
			PythonLine pythonLine = codeIndentation.removeLast();
			// check that there are matching left and right weight clips
			if (pythonLine.getIndentCount() < 0 || (codeIndentation.isEmpty() && pythonLine.getIndentCount() != 0)) {
				throw new Exception("Error: file has unmatching leftWeightClip and rightWeightClip");
			}
			System.out.println(pythonLine);
		}
	}
}
