import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;

public class ClipTracker {
	private final Deque<PythonLine> codeIndentation;
	private final ScopeTracker scopeTracker;
	
	public ClipTracker(ScopeTracker scopeTracker) {
		codeIndentation = new ArrayDeque<>();
		this.scopeTracker = scopeTracker;
	}

	public void addNewCodeline(String codeLine, String pyLine) {
		int indentAmount = 0;
		int nextIndentAmount = 0;

		if (!codeIndentation.isEmpty()) {
			indentAmount = codeIndentation.peek().getNextIndentCount();
			nextIndentAmount = codeIndentation.peek().getNextIndentCount();
		}
		// determine if the line contains a open or closing bracket
		String[] lineMaterial = codeLine.split(" ");
		String closing = lineMaterial.length == 0 ? "" : lineMaterial[lineMaterial.length - 1];
		String opening = lineMaterial.length == 0 ? "" : lineMaterial[0];
		
		if(opening.equals("rightWeightClip")) {
			indentAmount = indentAmount - 1;
			nextIndentAmount = indentAmount;
		} 
		if (closing.equals("leftWeightClip")) {
			nextIndentAmount = indentAmount + 1;
		}
		
		// generate the next indented python line
		PythonLine pythonLine = new PythonLine(indentAmount, nextIndentAmount, codeLine, pyLine);
		// add the new line to out deque
		codeIndentation.push(pythonLine);
	}

	public void displayLines() throws Exception {
		while (!codeIndentation.isEmpty()) {
			PythonLine pythonLine = codeIndentation.removeLast();
			// check that there are matching left and right weight clips
			if (pythonLine.getIndentCount() < 0 || (codeIndentation.isEmpty() && pythonLine.getIndentCount() != 0)) {
				throw new Exception("Error: file has unmatching leftWeightClip and rightWeightClip");
			}
			if (!pythonLine.getPythonCodeLine().isEmpty()) System.out.println(pythonLine);
		}
	}
}
