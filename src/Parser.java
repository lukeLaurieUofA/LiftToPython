import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class Parser {
	private static final Pattern var_assign = Pattern.compile("^(.+) loadBar (.+) pump$");
	private static final Pattern func_dec = Pattern.compile("^workout (.+) (.+)[(](.+)[)] leftWeightClip$");
	private static final Pattern loop_dec = Pattern.compile("^set (.+), (.+) to (.+) leftWeightClip$");
	private static final Pattern type_var_dec = Pattern.compile("^(\\w+) (.+)$");
	private static final Pattern type = Pattern
			.compile("^ryanBullard$|^lightWeight$|^weight$|^cables$|^pr$|^samSulek$|^smallPlate$");
	private static final Pattern var = Pattern.compile(
			"^pecs(\\d)*$|^delts(\\d)*$|^lats(\\d)*$|^biceps(\\d)*$|^triceps(\\d)*$|^abs(\\d)*$|^obliques(\\d)*$|^quads(\\d)*$|^hamstrings(\\d)*$|^"
					+ "glutes(\\d)*$|^calves(\\d)*$|^forearms(\\d)*$");
	private static final Pattern int_val = Pattern.compile("^\\d+$|^-\\d+$|^2\\.5$");
	private static final Pattern str_val = Pattern.compile("^\"(.*)\"$");
	private static final Pattern bool_val = Pattern.compile("^gotItUp$|^failed$");
	private static final Pattern end_scope = Pattern.compile("^rightWeightClip$");
	private static final Pattern add_expr = Pattern.compile("^(.+) creatine (.+)$");
	private static final Pattern sub_expr = Pattern.compile("^(.+) restDay (.+)$");
	private static final Pattern mult_expr = Pattern.compile("^(.+) steroids (.+)$");
	private static final Pattern div_expr = Pattern.compile("^(.+) vegan (.+)$");
	private static final Pattern mod_expr = Pattern.compile("^(.+) muscleMass (.+)$");

	private static final Pattern and_expr = Pattern.compile("^(.+) crushed (.+)$");
	private static final Pattern or_expr = Pattern.compile("^(.+) settle (.+)$");
	private static final Pattern not_expr = Pattern.compile("^spotter (.+)$");

	private static final Pattern greater_than = Pattern.compile("^(.+) biggerThan (.+)$");
	private static final Pattern less_than = Pattern.compile("^(.+) smallerThan (.+)$");
	private static final Pattern equal_to = Pattern.compile("^(.+) sameSize (.+)$");
	private static final Pattern increment = Pattern.compile("^(.+) superset$");
	private static final Pattern if_expr = Pattern.compile("^canYouLift [(](.+)[)] leftWeightClip$");
	private static final Pattern else_expr = Pattern.compile("^rightWeightClip yourAFailureSo leftWeightClip$");

	private static final Pattern return_expr = Pattern.compile("^gains (.+) pump$");
	private static final Pattern print_expr = Pattern.compile("^showoff[(](.*)[)] pump$");
	private static final Pattern comment = Pattern.compile("^sayToGymBro(.+)$");

	private static final ScopeTracker scopeTracker = new ScopeTracker();
	private static final ClipTracker clipTracker = new ClipTracker(scopeTracker);

	private static boolean debugMode = true;
	private static String fileName;
	
	public static void main(String[] args) throws Exception {
		addCommandLinesArgs(args);
		if (debugMode) {
			readLinesFromUser();
		} else {
			convertFileToPython("sampleFile.txt");
		}
		clipTracker.displayLines();
	}
	
	private static void addCommandLinesArgs(String[] args) {
		String cmdArgs = "preworkout"; 
		for (int i = 0; i < args.length; i++) {
			// the first command line arg is the name of the file
			if (i == 0) {
				debugMode = false; 
				fileName = args[i];
				continue;
			}
			String value; 
			// check if string is an integer
			if (args[i].matches("-?\\d+")) {
				value = args[i];
			} else {
				value = "\"" + args[i] + "\"";
			}
			clipTracker.addNewCodeline("", String.format("%s%d = %s", cmdArgs, i, value));
		}
	}

	private static void convertFileToPython(String fileName) throws Exception {
		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String cmd;
            while ((cmd = reader.readLine()) != null) {
            	convertLine(cmd);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private static void readLinesFromUser() throws Exception {
		Scanner in = new Scanner(System.in);
		if (debugMode) System.out.print(">> ");
		String cmd = in.nextLine();
		while (!cmd.equals("leave gym")) {
			convertLine(cmd);
			cmd = in.nextLine();
		}
	}
	
	private static void convertLine(String cmd) throws InvalidLineException, InvalidBlockException {
		cmd = cmd.trim();
		String pythonLine = parseCmd(cmd);
		if(pythonLine == null) throw new InvalidLineException("invalid line {" + cmd + "} blud");
		//if (debugMode) System.out.println(pythonLine);
		clipTracker.addNewCodeline(cmd, pythonLine);
		if(debugMode) scopeTracker.printCurrentScopeInfo();
		if (debugMode) System.out.print(">> ");
	}
	
	private static String parseCmd(String cmd) throws InvalidBlockException {
		String pythonLine;
		try {
			pythonLine = varAssign(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = funcDec(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = loopDec(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = endScope(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = ifExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = elseExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = returnExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = printExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = incrExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = commentExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		return null;
	}

	private static String endScope(String cmd) throws InvalidLineException {
		Matcher m = end_scope.matcher(cmd);
		boolean match = m.find();
		printMsg(match, "\n<end_scope>", cmd, "end of scope");
		if (!match)
			throw new InvalidLineException();
		return "";
	}

	private static String loopDec(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = loop_dec.matcher(cmd);
		if (m.find()) {
			String variable = var(true, m.group(1), "weight"); // group 1 is variable name, group 2 is first bound, group 3 is
														// second bound
			String fromVal = val(m.group(2));
			String toVal = val(m.group(3));
			printMsg(true, "\n<loop_dec>", cmd, "loop declaration");
			return String.format("for %s in range(%s,%s):", variable, fromVal, toVal);
		} else {
			printMsg(false, "\n<loop_dec>", cmd, "loop declaration");
			throw new InvalidLineException();
		}
	}

	private static String funcDec(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = func_dec.matcher(cmd);
		if (m.find()) { // group 1 is the type, group 2 is the name, group 3 is the parameters
			scopeTracker.insertNewBlock();
			type(m.group(1)); // No need to check group two, it's always valid if found
			String functionName = m.group(2);
			String parameters = varDecList(m.group(3));
			printMsg(true, "\n<func_dec>", cmd, "function declaration");
			return String.format("def %s(%s):", functionName, parameters);
		}
		printMsg(false, "\n<func_dec>", cmd, "function declaration");
		throw new InvalidLineException();
	}

	private static String varAssign(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = var_assign.matcher(cmd);
		if (m.find()) {
			String variables = varDecList(m.group(1));
			String values = valList(m.group(2)); 
			printMsg(true, "\n<var_assign>", cmd, "variable assignment statement");
			return String.format("%s = %s", variables, values);
		}
		printMsg(false, "\n<var_assign>", cmd, "variable assignment statement");
		throw new InvalidLineException();
	}

	private static String varDecList(String cmd) throws InvalidLineException, InvalidBlockException {
		String[] split = cmd.split(", ");
		boolean match = true;
		StringBuilder varDec = new StringBuilder();
		for (String s : split) {
			String variable = varDec(s);
			if (varDec.length() == 0) varDec = new StringBuilder(variable);
			else varDec.append(", ").append(variable);
		}
		printMsg(match, "<var_dec_list>", cmd, "variable declaration list");
		return varDec.toString();
	}

	private static String varDec(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = type_var_dec.matcher(cmd);
		String var;
		if (m.find()) {
			type(m.group(1));
			var = var(true, m.group(2), m.group(1));
		} else {
			var = var(true, cmd, null);
		}
		printMsg(true, "<var_dec>", cmd, "variable declaration");
		return var;
	}

	private static void type(String cmd) throws InvalidLineException {
		Matcher m = type.matcher(cmd);
		boolean match = m.find();
		printMsg(match, "<type>", cmd, "type");
		if (!match)
			throw new InvalidLineException();
	}

	private static String var(boolean isNewVar, String cmd, String type) throws InvalidBlockException, InvalidLineException {
		Matcher m = var.matcher(cmd);
		boolean match = m.find();
		if (match) {
			if (isNewVar) {
					ScopeTracker.Type typeEnum = getType(type);
					scopeTracker.addNewVar(cmd, typeEnum);
			} else {
				scopeTracker.checkVarUsable(new ScopeTracker.VarInfo(cmd, scopeTracker.getType(cmd)));
			}
		} else {
			throw new InvalidLineException();
		}
		printMsg(true, "<var>", cmd, "variable");

        return cmd;
	}

	private static ScopeTracker.Type getType(String type) throws InvalidBlockException {
		ScopeTracker.Type typeEnum;
		if(type == null) {
			typeEnum = ScopeTracker.Type.notImportant;
		}else if(type.equals("ryanBullard")) {
			typeEnum = ScopeTracker.Type.ryanBullard;
		} else if(type.equals("lightWeight")) {
			typeEnum = ScopeTracker.Type.lightWeight;
		} else if(type.equals("pr")) {
			typeEnum = ScopeTracker.Type.pr;
		} else if(type.equals("cables")) {
			typeEnum = ScopeTracker.Type.cables;
		} else if(type.equals("samSulek")) {
			typeEnum = ScopeTracker.Type.samSulek;
		} else if(type.equals("smallPlate")) {
			typeEnum = ScopeTracker.Type.smallPlate;
		} else if(type.equals("weight")) {
			typeEnum = ScopeTracker.Type.weight;
		} else {
			throw new InvalidBlockException();
		}
		return typeEnum;
	}

	private static String valList(String cmd) throws InvalidLineException, InvalidBlockException {
		String[] split = cmd.split(", ");
		StringBuilder valDec = new StringBuilder();
		for (String s : split) {
			String value = val(s);
			if (valDec.length() == 0) valDec = new StringBuilder(value);
			else valDec.append(", ").append(value);
		}
		printMsg(true, "<val_list>", cmd, "value list");
		return valDec.toString();
	}

	private static String val(String cmd) throws InvalidLineException, InvalidBlockException {
		String pythonLine;
		try {
			pythonLine = intVal(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = boolVal(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = var(false, cmd, null);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = strVal(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = intExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = boolExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = incrExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}

		printMsg(false, "<val>", cmd, "value");
		throw new InvalidLineException();

	}

	private static String strVal(String cmd) throws InvalidLineException {
		Matcher m = str_val.matcher(cmd);
		boolean match = false;
		if (m.find()) {
			match = true;
			Matcher m2 = Pattern.compile("(?<!\\\\)\"").matcher(m.group(1));
			if (m2.find()) {
				match = false;
			}
		}

		printMsg(match, "<str_val>", cmd, "<str_val>");
		if (match)
			return cmd;
		throw new InvalidLineException();
	}

	private static String boolVal(String cmd) throws InvalidLineException {
		return someVal(bool_val.matcher(cmd), cmd, "<bool>");
	}

	private static String intVal(String cmd) throws InvalidLineException {
		return someVal(int_val.matcher(cmd), cmd, "<int>");
	}

	private static String someVal(Matcher m, String cmd, String type) throws InvalidLineException {
		if (m.find()) {
			printMsg(true, type, cmd, type);
			return cmd;
		}
		printMsg(false, type, cmd, type);
		throw new InvalidLineException();
	}

	private static String boolExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		String pythonLine;
		try {
			pythonLine = andExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = orExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = notExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = equalExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = lessExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = greaterExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {}
		printMsg(false, "<val>", cmd, "value");
		throw new InvalidLineException();		
	}

	private static String andExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someBoolExpr(cmd, and_expr.matcher(cmd), "<and_expr>", "and");
	}

	private static String orExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someBoolExpr(cmd, or_expr.matcher(cmd), "<or_expr>", "or");
	}

	private static String notExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		boolean match = false;
		Matcher m = not_expr.matcher(cmd);
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = boolVal(m.group(1));
			} catch (InvalidLineException e) {
			}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1), null);
				} catch (InvalidLineException e) {
				}
			}
			if (leftExpr == null) {
				try {
					leftExpr = boolExpr(m.group(1));
				} catch (InvalidLineException e) {
				}
			}

			if (leftExpr != null) {
				printMsg(true, "<not_expr>", cmd, "<not_expr>");
				return String.format("not %s", leftExpr);
			}
		}
		printMsg(match, "<not_expr>", cmd, "<not_expr>");
		throw new InvalidLineException();
	}

	private static String someBoolExpr(String cmd, Matcher m, String exprName, String symbol) throws InvalidLineException, InvalidBlockException {
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = boolVal(m.group(1));
			} catch (InvalidLineException e) {
			}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1), null);
				} catch (InvalidLineException e) {
				}
			}
			if (leftExpr == null) {
				try {
					leftExpr = boolExpr(m.group(1));
				} catch (InvalidLineException e) {
				}
			}

			String rightExpr = null;
			try {
				rightExpr = boolVal(m.group(2));
			} catch (InvalidLineException e) {
			}
			if (rightExpr == null) {
				try {
					rightExpr = var(false, m.group(2), null);
				} catch (InvalidLineException e) {
				}
			}
			if (rightExpr == null) {
				try {
					rightExpr = boolExpr(m.group(2));
				} catch (InvalidLineException e) {
				}
			}
			if (leftExpr != null && rightExpr != null) {
				printMsg(true, exprName, cmd, exprName);
				return String.format("%s %s %s", leftExpr, symbol, rightExpr);
			}
		}
		printMsg(false, exprName, cmd, exprName);
		throw new InvalidLineException();
	}

	private static String equalExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, equal_to.matcher(cmd), "<equal_expr>", "==");
	}

	private static String lessExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, less_than.matcher(cmd), "<less_expr>", "<");
	}

	private static String greaterExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, greater_than.matcher(cmd), "<greater_expr>", ">");
	}

	private static void printMsg(boolean match, String ntName, String cmd, String item) {
		if (match)
			if (debugMode) {
				System.out.println(ntName + ": " + cmd);
			}
		else
			if (debugMode) {
				System.out.println("Failed to parse: {" + cmd + "} is not a valid " + item + ".");
			}
	}

	private static String intExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		String pythonLine;
		try {
			pythonLine = addExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = subExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = multExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = divExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = modExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}

		printMsg(false, "<val>", cmd, "value");
		throw new InvalidLineException();
	}

	private static String addExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, add_expr.matcher(cmd), "<add_expr>", "+");
	}

	private static String subExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, sub_expr.matcher(cmd), "<sub_expr>", "-");
	}

	private static String multExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, mult_expr.matcher(cmd), "<mult_expr>", "*");
	}

	private static String divExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, div_expr.matcher(cmd), "<div_expr>", "/");
	}

	private static String modExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		return someIntExpr(cmd, mod_expr.matcher(cmd), "<mod_expr>", "%");
	}

	private static String someIntExpr(String cmd, Matcher m, String exprName, String symbol) throws InvalidLineException, InvalidBlockException {
		ScopeTracker.Type leftType = ScopeTracker.Type.notImportant;
		ScopeTracker.Type rightType = ScopeTracker.Type.notImportant;

		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = intVal(m.group(1));
			} catch (InvalidLineException e) {
			}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1), null);
					leftType = scopeTracker.getType(m.group(1));
				} catch (InvalidLineException e) {
				}
			}
			if (leftExpr == null) {
				try {
					leftExpr = intExpr(m.group(1));
				} catch (InvalidLineException e) {
				}
			}

			String rightExpr = null;
			try {
				rightExpr = intVal(m.group(2));
			} catch (InvalidLineException e) {
			}
			if (rightExpr == null) {
				try {
					rightExpr = var(false, m.group(2), null);
					rightType = scopeTracker.getType(m.group(2));
				} catch (InvalidLineException e) {
				}
			}
			if (rightExpr == null) {
				try {
					rightExpr = intExpr(m.group(2));
				} catch (InvalidLineException e) {
				}
			}

			if (leftExpr != null && rightExpr != null) {
				if(leftType != ScopeTracker.Type.notImportant && rightType != ScopeTracker.Type.notImportant) {
					// Check that types cannot be coerced
					if(leftType == ScopeTracker.Type.cables && rightType != ScopeTracker.Type.cables) {
						System.out.println("Can't so math with a cable!");
						throw new InvalidLineException();
					}// else if (leftType != ScopeTracker.Type.cables && rightType == ScopeTracker.Type.cables) {
//						System.out.println("Can't so math with a cable!");
//						throw new InvalidLineException();
//					} else if (leftType == ScopeTracker.Type.cables && rightType == ScopeTracker.Type.cables) {
//						System.out.println("Can't so math with a cable!");
//						throw new InvalidLineException();
//					}
				}
				printMsg(true, exprName, cmd, exprName);
				return String.format("%s %s %s", leftExpr, symbol, rightExpr);
			}
		}
		printMsg(false, exprName, cmd, exprName);
		throw new InvalidLineException();
	}

	public static String ifExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = if_expr.matcher(cmd);
		if (m.find()) {
			// can either match integer values or more integer expressions
			String leftExpr = null;
			try {
				leftExpr = boolExpr(m.group(1));
			} catch (InvalidLineException e) {}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1), null);
				} catch (InvalidLineException e) {}
			}
			if(leftExpr == null) {
				try {
					leftExpr = boolVal(m.group(1));
				} catch (InvalidLineException e) {}
			}
			
			if (leftExpr != null) {
				scopeTracker.insertNewBlock();
				printMsg(true, "<if_expr>", cmd, "if expression");
				return String.format("if %s:",leftExpr);
			}
		}
		printMsg(false, "<if_expr>", cmd, "if expression");
		throw new InvalidLineException();
	}

	public static String elseExpr(String cmd) throws InvalidLineException {
		Matcher m = else_expr.matcher(cmd);
		if(!m.find()) {
			printMsg(false, "<else_expr>", cmd, "else expression");
			throw new InvalidLineException();
		}
		scopeTracker.insertNewBlock();
		printMsg(true, "<else_expr>", cmd, "else expression");
		return "else:";
	}

	public static String returnExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = return_expr.matcher(cmd);
		if (m.find()) {
			String value = val(m.group(1));
			printMsg(true, "<return_expr>", cmd, "return expression");
			return String.format("return %s", value);
		}
		printMsg(false, "<return_expr>", cmd, "return expression");
		throw new InvalidLineException();
	}
	
	private static String printExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = print_expr.matcher(cmd);
		if (m.find()) {
			boolean match = false;
			try {
				var(false, m.group(1), null);
				match = true;
			} catch (InvalidLineException e) {}
			try {
				strVal(m.group(1));
				match = true;
			} catch (InvalidLineException e) {}
			if(!match) {
				throw new InvalidLineException();
			}
			String value = m.group(1);
			printMsg(true, "<print_expr>", cmd, "print expression");
			return String.format("print(%s)", value);
		}
		printMsg(false, "<print_expr>", cmd, "print expression");
		throw new InvalidLineException();
	}
	
	public static String incrExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = increment.matcher(cmd);
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = var(false, m.group(1), null);
			} catch (InvalidLineException e) {}
			
			if (leftExpr != null) {
				printMsg(true, "<increment_expr>", cmd, "<increment_expr>");
				return String.format("%s += 1",leftExpr);
			}
		}
		printMsg(false, "<increment_expr>", cmd, "integer increment expression");
		throw new InvalidLineException();
	}
	
	public static String commentExpr(String cmd) throws InvalidLineException{
		Matcher m = comment.matcher(cmd);
		if (m.find()) {
			String leftExpr = m.group(1);
			printMsg(true, "<comment>", cmd, "<comment>");
			return String.format("#%s",leftExpr);
		}
		printMsg(false, "<comment>", cmd, "<comment>");
		throw new InvalidLineException();
	}
}
