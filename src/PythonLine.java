import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonLine {
	
	private String pythonCodeLine;
	private int indentCount; 
	private int nextIndentCount; 
	private String codeLine;
	
	public PythonLine(int indentCount, int nextIndentCount, String codeLine, String pyLine) {
		this.pythonCodeLine = pyLine;
		this.indentCount = indentCount;
		this.codeLine = codeLine;
		this.nextIndentCount = nextIndentCount;
	}
	
	public String getPythonCodeLine() {
		return pythonCodeLine;
	}

	public void setPythonCodeLine(String pythonCodeLine) {
		this.pythonCodeLine = pythonCodeLine;
	}

	public int getIndentCount() {
		return indentCount;
	}
	
	public void setIndentCount(int indentCount) {
		this.indentCount = indentCount;
	}
	
	public String getCodeLine() {
		return codeLine;
	}
	
	public void setCodeLine(String codeLine) {
		this.codeLine = codeLine;
	}
	
	public int getNextIndentCount() {
		return nextIndentCount;
	}

	public void setNextIndentCount(int nextIndentCount) {
		this.nextIndentCount = nextIndentCount;
	}
	
	@Override
	public String toString() {
		// add the correct amount of tabs
		String tabCount = ""; 
		for (int i = 0; i < indentCount; i++) {
			tabCount += "\t";
		}
		return tabCount + pythonCodeLine;
	}
}