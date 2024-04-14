import java.util.ArrayList;

public class ScopeTracker {
	ArrayList<String> varTracker; 
	
	public ScopeTracker() {
		varTracker = new ArrayList<String>();
	}
	
	public void addNewVar(String newVariable) {
		varTracker.add(newVariable);
	} 
	
	public void checkVarUsable(String varToCheck) throws InvalidBlockException {
		if (!varTracker.contains(varToCheck)) {
			throw new InvalidBlockException("You gotta hit the gym before you can that muscle group(please define " + varToCheck + " before using)");
		}
	}
	
	public void insertNewBlock() {
		varTracker.add(null);
	}
	
	public void endBlock() {
		int i = varTracker.size() - 1;
		while (i >= 0 && varTracker.get(i) != null) {
			varTracker.remove(i);
			i--;
		}
	}
}
