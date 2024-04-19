import java.util.ArrayList;

public class ScopeTracker {
	ArrayList<VarInfo> varTracker;
	private Type returnType;
	private ArrayList<FuncInfo> funcs;
	
	public ScopeTracker() {
		varTracker = new ArrayList<>();
		funcs = new ArrayList<>();
		returnType = Type.notImportant;
	}
	
	public void addNewVar(String newVariable, Type type) {
		if(type == Type.notImportant) { // Reassignment?
			boolean exists = false;
			for(VarInfo varInfo : varTracker) {
				if(varInfo == null) {
					continue;
				}
                if (varInfo.name.equals(newVariable)) {
                    exists = true;
                    break;
                }
			}
			if(exists) {
				return;
			}
		}
		varTracker.add(new VarInfo(newVariable, type));
	}

	public void addNewFunc(String name, Type returnType, ArrayList<Type> parameterTypes) {
		funcs.add(new FuncInfo(name, returnType, parameterTypes));
	}

	public Type getReturnType(String name) {
		for(FuncInfo funcInfo : funcs) {
			if(funcInfo.name.equals(name)) {
				return funcInfo.returnType;
			}
		}
		return null;
	}

	public ArrayList<Type> getArgumentTypes(String name) {
		for(FuncInfo funcInfo : funcs) {
			if(funcInfo.name.equals(name)) {
				return funcInfo.params;
			}
		}
		return null;
	}
	
	public void checkVarUsable(VarInfo varToCheck) throws InvalidBlockException {
		if (!varTracker.contains(varToCheck)) {
			throw new InvalidBlockException("You gotta hit the gym before you can use that muscle group(please define "
					+ varToCheck.name + " before using)");
		}
	}

	public Type getType(String varName) {
		Type type = null;
		for (int i = varTracker.size() - 1; i >= 0; i--) {
			VarInfo var = varTracker.get(i);
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
		System.out.println("Functions:");
		for(FuncInfo funcInfo : funcs) {
			System.out.println("Name: " + funcInfo.name + " ReturnType:  " + funcInfo.returnType);
			if(funcInfo.params != null) {
				System.out.println("Parameters:");
				for (Type type : funcInfo.params) {
					System.out.println("\t" + type.toString());
				}
			}
		}
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
		bool,
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

	public static class FuncInfo {
		String name;
		Type returnType;
		ArrayList<Type> params;

		public FuncInfo(String name, Type type, ArrayList<Type> params) {
			this.name = name;
			this.returnType = type;
			this.params = params;
		}

		@Override
		public boolean equals(Object var) {
			if(var instanceof FuncInfo) {
				FuncInfo funcInfo = (FuncInfo)var;
                return this.name.equals(funcInfo.name) && this.returnType == funcInfo.returnType && this.params.equals(funcInfo.params);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return this.name.hashCode() + this.returnType.hashCode() + this.params.size();
		}
	}
}
