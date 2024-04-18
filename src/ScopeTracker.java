import java.util.ArrayList;

public class ScopeTracker {
	ArrayList<VarInfo> varTracker;
	private Type returnType;
	
	public ScopeTracker() {
		varTracker = new ArrayList<>();
		returnType = Type.notImportant;
	}
	
	public void addNewVar(String newVariable, Type type) {
		varTracker.add(new VarInfo(newVariable, type));
	} 
	
	public void checkVarUsable(VarInfo varToCheck) throws InvalidBlockException {
		if (!varTracker.contains(varToCheck)) {
			throw new InvalidBlockException("You gotta hit the gym before you can use that muscle group(please define "
					+ varToCheck.name + " before using)");
		}
	}

	public Type getType(String varName) {
		Type type = null;
		for (VarInfo var : varTracker) {
			if(var == null) {
				continue;
			}
			if(var.name.equals(varName)) {
				type = var.type;
				break;
			}
		}
		return type;
	}

	public Type getReturnType() {
		return returnType;
	}

	public void setReturnType(Type returnType) {
		this.returnType = returnType;
	}

	public void printCurrentScopeInfo() {
		for (VarInfo var : varTracker) {
			if(var == null) {
				System.out.println("Scope break");
			} else {
				System.out.println("Type: " + var.type + ", Name: " + var.name);
			}
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
		varTracker.remove(i); // Remove the scope break
		int nullCount = 0;
		for (VarInfo var : varTracker) {
			if(var == null) {
				nullCount++;
			}
		}
		if(nullCount == 0) {
			returnType = Type.notImportant; // We are out of a function scope, there's no return type.
		}
	}

	public enum Type {
		ryanBullard,
		lightWeight,
		weight,
		cables,
		pr,
		samSulek,
		smallPlate,
		notImportant
	}

	public static class VarInfo {
		String name;
		Type type;

		public VarInfo(String name, Type type) {
			this.name = name;
			this.type = type;
		}

		@Override
		public boolean equals(Object var) {
			if(var instanceof VarInfo) {
				VarInfo varInfo = (VarInfo)var;
				return this.name.equals(varInfo.name) && this.type == varInfo.type;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode() + this.type.hashCode();
		}
	}
}
