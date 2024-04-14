import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

public class Parser {
	private static final Pattern var_assign = Pattern.compile("^(.+) loadBar (.+) pump$");
	private static final Pattern func_dec = Pattern.compile("^workout (.+) (.+)[(](.+)[)] leftWeightClip$");
	private static final Pattern loop_dec = Pattern.compile("^set (.+), (.+) to (.+) leftWeightClip$");
	private static final Pattern type_var_dec = Pattern.compile("^(\\w+) (.+)$");
	private static final Pattern type = Pattern
			.compile("^ryanBullard$|^lightWeight$|^weight$|^cables$|^pr$|^samSulek$");
	private static final Pattern var = Pattern.compile(
			"^pecs(\\d)*$|^delts(\\d)*$|^lats(\\d)*$|^biceps(\\d)*$|^triceps(\\d)*$|^abs(\\d)*$|^obliques(\\d)*$|^quads(\\d)*$|^hamstrings(\\d)*$|^"
					+ "glutes(\\d)*$|^calves(\\d)*$|^forearms(\\d)*$");
	private static final Pattern int_val = Pattern.compile("^\\d+$|^-\\d+$|^-\\d+[.]\\d+$|^\\d+[.]\\d+$");
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
	private static final Pattern if_expr = Pattern.compile("^canYouLift[(](.+)[)] leftWeightClip$");
	private static final Pattern else_expr = Pattern.compile("^rightWeightClip yourAFailureSo leftWeightClip$");

	private static final Pattern return_expr = Pattern.compile("^gains (.+) pump");
	private static final Exception InvalidLineExeption;=null;

	private static ScopeTracker scopeTracker = new ScopeTracker();
	private static ClipTracker clipTracker = new ClipTracker(scopeTracker);

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		System.out.print(">> ");
		String cmd = in.nextLine();
		while (!cmd.equals("exit")) {
			String pythonLine = parseCmd(cmd);
			clipTracker.addNewCodeline(cmd);
			System.out.print(">> ");
			cmd = in.nextLine();
		}
	}

	private static String parseCmd(String cmd) {
		String pythonLine = "";
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
		
		//ADD INCREMENT EXPRESSION AS ANOTHER POSSIBLE LINE LUKE LAURIE
		return null;
	}

	private static String endScope(String cmd) {
		Matcher m = end_scope.matcher(cmd);
		boolean match = m.find();
		printMsg(match, "\n<end_scope>", cmd, "end of scope");
		if (!match)
			throw new InvalidLineException();
		return "";
	}

	private static String loopDec(String cmd) {
		Matcher m = loop_dec.matcher(cmd);
		if (m.find()) {
			String variable = var(false, m.group(1)); // group 1 is variable name, group 2 is first bound, group 3 is
														// second bound
			String fromVal = val(m.group(2));
			String toVal = val(m.group(3));
			printMsg(true, "\n<loop_dec>", cmd, "loop declaration");
			return "for {} in range({},{}):".format(variable, fromVal, toVal);
		} else {
			printMsg(false, "\n<loop_dec>", cmd, "loop declaration");
			throw new InvalidLineException();
		}

		return "";
	}

	private static boolean funcDec(String cmd) throws InvalidLineException {
		Matcher m = func_dec.matcher(cmd);
		boolean match = false;
		if (m.find()) { // group 1 is the type, group 2 is the name, group 3 is the parameters
			match = type(m.group(1)); // No need to check group two, it's always valid if found
			match = match && varDecList(m.group(3));
		}
		printMsg(match, "\n<func_dec>", cmd, "function declaration");
		return match;
	}

	private static boolean varAssign(String cmd) throws InvalidLineException {
		Matcher m = var_assign.matcher(cmd);
		boolean match = false;
		if (m.find()) {
			match = varDecList(m.group(1));
			match = match && valList(m.group(2));
		}
		printMsg(match, "\n<var_assign>", cmd, "variable assignment statement");
		return match;
	}

	private static boolean varDecList(String cmd) throws InvalidLineException {
		String[] split = cmd.split(", ");
		boolean match = true;
		for (String s : split) {
			match = match && varDec(s);
		}
		printMsg(match, "<var_dec_list>", cmd, "variable declaration list");
		return match;
	}

	private static String varDec(String cmd) throws InvalidLineException, InvalidBlockException {
		boolean match;
		Matcher m = type_var_dec.matcher(cmd);
		String var = "";
		if (m.find()) {
			type(m.group(1));
			var = var(true, m.group(2));
		} else
			var = var(true, cmd);
		printMsg(match, "<var_dec>", cmd, "variable declaration");
		if (!match)
			throw new InvalidLineException();
		return var;
	}

	private static void type(String cmd) throws InvalidLineException {
		Matcher m = type.matcher(cmd);
		boolean match = m.find();
		printMsg(match, "<type>", cmd, "type");
		if (!match)
			throw new InvalidLineException();
	}

	private static String var(boolean isNewVar, String cmd) throws InvalidBlockException, InvalidLineException {
		Matcher m = var.matcher(cmd);
		boolean match = m.find();
		if (match) {
			if (isNewVar) {
				scopeTracker.addNewVar(cmd);
			} else {
				scopeTracker.checkVarUsable(cmd);
			}
		} else
			throw new InvalidLineException();
		printMsg(match, "<var>", cmd, "variable");
		if (!match)
			throw new InvalidLineException();

		return cmd;
	}

	private static boolean valList(String cmd) {
		String[] split = cmd.split(", ");
		boolean match = true;
		for (String s : split) {
			match = match && val(s);
		}
		printMsg(match, "<val_list>", cmd, "value list");
		return match;
	}

	private static String val(String cmd) {
		String pythonLine = "";
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
			pythonLine = var(false, cmd);
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
			pythonLine = incrementExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}

		printMsg(false, "<val>", cmd, "value");
		throw new InvalidLineException();

	}

	private static String strVal(String cmd) {
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

	private static String boolVal(String cmd) {
		Matcher m = bool_val.matcher(cmd);
		someVal(bool_val.matcher(cmd), "<bool>", "<bool>");
	}

	private static String intVal(String cmd) {
		Matcher m = int_val.matcher(cmd);
		someVal(int_val.matcher(cmd), "<int>", "<int>");
	}

	private static String someVal(Matcher m, String cmd, String type) {
		if (m.find()) {
			printMsg(true, type, cmd, type);
			return cmd;
		}
		printMsg(false, type, cmd, type);
		throw new InvalidLineException();
	}

	private static boolean boolExpr(String cmd) {
		String pythonLine = "";
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
			pythonLine = var(false, cmd);
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
			pythonLine = incrementExpr(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {
		}

		printMsg(false, "<val>", cmd, "value");
		throw new InvalidLineException();

		
		
		boolean match = false;
		if (andExpr(cmd)) {
			match = true;
		} else if (orExpr(cmd)) {
			match = true;
		} else if (notExpr(cmd)) {
			match = true;
		} else if (equalExpr(cmd)) {
			match = true;
		} else if (lessExpr(cmd)) {
			match = true;
		} else if (greaterExpr(cmd)) {
			match = true;
		}
		return match;
	}

	private static String andExpr(String cmd) {
		return someBoolExpr(cmd, and_expr.matcher(cmd), "<and_expr>", "and");
	}

	private static String orExpr(String cmd) {
		return someBoolExpr(cmd, or_expr.matcher(cmd), "<or_expr>", "or");
	}

	private static boolean notExpr(String cmd) {
		boolean match = false;
		Matcher m = increment.matcher(cmd);
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = var(false, m.group(1));
			} catch (InvalidLineException e) {}
			
			if (leftExpr != null) {
				printMsg(true, "<increment_expr>", cmd, "<increment_expr>");
				return "not {} += 1".format(leftExpr);
			}
		}
		printMsg(false, "<increment_expr>", cmd, "integer increment expression");
		throw new InvalidLineException();
		
		boolean match = false;
		Matcher m = not_expr.matcher(cmd);
		if (m.find()) {
			match = boolVal(m.group(1)) || var(false, m.group(1)) || boolExpr(m.group(1));
		}
		printMsg(match, "<not_expr>", cmd, "<not_expr>");
		return match;
	}

	private static String someBoolExpr(String cmd, Matcher m, String exprName, String symbol) {
		boolean match = false;
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = boolVal(m.group(1));
			} catch (InvalidLineException e) {
			}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1));
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
					rightExpr = var(false, m.group(2));
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
				return "{} {} {}".format(leftExpr, symbol, rightExpr);
			}
		}
		printMsg(false, exprName, cmd, exprName);
		throw new InvalidLineException();
	}

	private static boolean equalExpr(String cmd) {
		return someIntExpr(cmd, equal_to.matcher(cmd), "<equal_expr>");
	}

	private static boolean lessExpr(String cmd) {
		return someIntExpr(cmd, less_than.matcher(cmd), "<less_expr>");
	}

	private static boolean greaterExpr(String cmd) {
		return someIntExpr(cmd, greater_than.matcher(cmd), "<greater_expr>");
	}

	private static void printMsg(boolean match, String ntName, String cmd, String item) {
		if (match)
			System.out.println(ntName + ": " + cmd);
		else
			System.out.println("Failed to parse: {" + cmd + "} is not a valid " + item + ".");
	}

	private static String intExpr(String cmd) {
		String pythonLine = "";
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

	private static String addExpr(String cmd) {
		return someIntExpr(cmd, add_expr.matcher(cmd), "<add_expr>", "+");
	}

	private static String subExpr(String cmd) {
		return someIntExpr(cmd, sub_expr.matcher(cmd), "<sub_expr>", "-");
	}

	private static String multExpr(String cmd) {
		return someIntExpr(cmd, mult_expr.matcher(cmd), "<mult_expr>", "*");
	}

	private static String divExpr(String cmd) {
		return someIntExpr(cmd, div_expr.matcher(cmd), "<div_expr>", "/");
	}

	private static String modExpr(String cmd) {
		return someIntExpr(cmd, mod_expr.matcher(cmd), "<mod_expr>", "%");
	}

	private static String someIntExpr(String cmd, Matcher m, String exprName, String symbol) {
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = intVal(m.group(1));
			} catch (InvalidLineException e) {
			}
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1));
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
					rightExpr = var(false, m.group(2));
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
				printMsg(true, exprName, cmd, exprName);
				return "{} {} {}".format(leftExpr, symbol, rightExpr);
			}
		}
		printMsg(false, exprName, cmd, exprName);
		throw new InvalidLineException();
	}
	
	public static String incrementExpr(String cmd) {
		boolean match = false;
		Matcher m = increment.matcher(cmd);
		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = var(false, m.group(1));
			} catch (InvalidLineException e) {}
			
			if (leftExpr != null) {
				printMsg(true, "<increment_expr>", cmd, "<increment_expr>");
				return "{} += 1".format(leftExpr);
			}
		}
		printMsg(false, "<increment_expr>", cmd, "integer increment expression");
		throw new InvalidLineException();
	}

	public static boolean ifExpr(String cmd) {
		boolean match = false;
		Matcher m = if_expr.matcher(cmd);
		if (m.find()) {
			// can either match integer values or more integer expressions
			match = boolExpr(m.group(1)) || var(false, m.group(1));
		}
		printMsg(match, "<if_expr>", cmd, "if expression");
		return match;
	}

	public static boolean elseExpr(String cmd) {
		boolean match;
		Matcher m = else_expr.matcher(cmd);
		match = m.find();
		printMsg(match, "<else_expr>", cmd, "else expression");
		return match;
	}

	public static boolean returnExpr(String cmd) {
		boolean match = false;
		Matcher m = return_expr.matcher(cmd);
		if (m.find()) {
			match = val(m.group(1));
		}
		printMsg(match, "<return_expr>", cmd, "return expression");
		return match;
	}
}
