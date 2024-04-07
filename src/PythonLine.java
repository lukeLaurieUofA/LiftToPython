
public class PythonLine {
	
	private int indentCount; 
	private int nextIndentCount; 
	private String codeLine;
	
	public PythonLine(int indentCount, int nextIndentCount, String codeLine) {
		super();
		this.indentCount = indentCount;
		this.codeLine = codeLine;
		this.nextIndentCount = nextIndentCount;
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
		return tabCount + codeLine;
	}
}
