import javax.security.auth.callback.TextInputCallback;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Parser {
	private static final Pattern var_assign = Pattern.compile("^(.+) loadBar (.+) pump$");
	private static final Pattern func_dec = Pattern.compile("^workout (.+) (.+)[(](.*)[)] leftWeightClip$");
	private static final Pattern loop_dec = Pattern.compile("^set (.+), (.+) to (.+) leftWeightClip$");
	private static final Pattern type_var_dec = Pattern.compile("^(\\w+) (.+)$");
	private static final Pattern type = Pattern
			.compile("^ryanBullard$|^lightWeight$|^weight$|^cables$|^pr$|^samSulek$|^smallPlate$|^tryBench$");
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
	private static final Pattern increment = Pattern.compile("^(.+) superSet pump$");
	private static final Pattern if_expr = Pattern.compile("^canYouLift [(](.+)[)] leftWeightClip$");
	private static final Pattern else_expr = Pattern.compile("^rightWeightClip yourAFailureSo leftWeightClip$");

	private static final Pattern return_expr = Pattern.compile("^gains (.+) pump$");
	private static final Pattern print_expr = Pattern.compile("^showoff[(](.*)[)] pump$");
	private static final Pattern comment = Pattern.compile("^sayToGymBro(.+)$");

	private static final Pattern function_call_standalone = Pattern.compile("^(.+)[(](.*)[)] pump$");
	private static final Pattern function_call = Pattern.compile("^(.+)[(](.*)[)]$");

	private static final ScopeTracker scopeTracker = new ScopeTracker();
	private static final ClipTracker clipTracker = new ClipTracker(scopeTracker);

	private static boolean debugMode = true;
	private static String fileName;
	private static LastLine lastLine = LastLine.Unchecked;

	private static List<ScopeTracker.Type> intTypes;

	private static List<String> intOps;
	private static List<String> intComps;

	private static List<String> stringOps;
	private static List<String> boolOps;

	private static int lineNumber = 1;

	private enum LastLine {
		IfExpr,
		VarAssign,
		Return,
		Unchecked
	}

	public static void main(String[] args) throws Exception {
		intTypes = Arrays.asList(
                ScopeTracker.Type.pr, ScopeTracker.Type.lightWeight,
                ScopeTracker.Type.ryanBullard, ScopeTracker.Type.samSulek, ScopeTracker.Type.weight,
                ScopeTracker.Type.smallPlate);
		intOps = Arrays.asList("creatine", "restDay", "steroids", "vegan", "muscleMass");
		intComps = Arrays.asList("biggerThan", "sameSize", "smallerThan");
		stringOps = Arrays.asList("creatine", "steroids");
		boolOps = Arrays.asList("spotter", "crushed", "settle", "sameSize");
		addCommandLinesArgs(args);
		if (debugMode) {
			readLinesFromUser();
		} else {
			convertFileToPython(fileName);
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
			while((cmd = reader.readLine()) != null) {
				try {
					if(func_dec.matcher(cmd).find()) {
						funcDec(cmd);
						addFunction(cmd);
					}
				} catch (InvalidLineException e) {
					System.out.println("Invalid Function Declaration: " + cmd);
				}
			}
		} catch (IOException e) {
			System.out.println("Unable to read the file!");
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String cmd;
            while ((cmd = reader.readLine()) != null) {
            	convertLine(cmd);
				lineNumber++;
            }
        } catch (IOException e) {
			System.out.println("Unable to read the file!");
        }
	}

	private static void addFunction(String cmd) throws InvalidBlockException {
		Matcher matcher = func_dec.matcher(cmd);
		boolean match = matcher.find();
		if(match) {
			ScopeTracker.Type returnType = getType(matcher.group(1));
			String name = matcher.group(2);
			ArrayList<ScopeTracker.Type> params = null;
			if(!matcher.group(3).isEmpty()) {
				params = new ArrayList<>();
				for(String paramBlock : matcher.group(3).split(",")) {
					String[] paramInfo = paramBlock.trim().split(" ");
					params.add(getType(paramInfo[0]));
				}
			}
			scopeTracker.addNewFunc(name, returnType, params);
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
		if(pythonLine == null) throw new InvalidLineException("invalid line on line " + lineNumber + " { " + cmd + " } blud");
		//if (debugMode) System.out.println(pythonLine);
		clipTracker.addNewCodeline(cmd, pythonLine);
		if(debugMode) {
			scopeTracker.printCurrentScopeInfo();
		}
		checkTypeFromLastLine(cmd);
		if (debugMode) {
			System.out.print(">> ");
		}
	}

	private static void checkTypeFromLastLine(String cmd) throws InvalidLineException {
		if(lastLine == LastLine.Return) {
			checkReturnType(cmd);
		} else if(lastLine == LastLine.VarAssign) {
			checkVarAssignMatches(cmd);
		} else if(lastLine == LastLine.IfExpr) {
			checkBoolExpr(cmd);
		}
		lastLine = LastLine.Unchecked;
	}

	private static void checkBoolExpr(String cmd) throws InvalidLineException {
		String expr = cmd.substring("canYouLift(".length() + 1, cmd.length() - (") leftWeightClip".length()));
		String[] tokens = expr.split(" ");
		if(tokens.length == 1) {
			Matcher m = function_call.matcher(tokens[0]);
			if(var.matcher(tokens[0]).find()) {
				if(scopeTracker.getType(tokens[0]) != ScopeTracker.Type.bool) {
					System.out.println("Invalid type on line " + lineNumber + ", got " + scopeTracker.getType(tokens[0])
					+ ", expected boolean!");
					throw new InvalidLineException();
				}
			} else if(m.find()) {
				if(scopeTracker.getReturnType(m.group(2)) != ScopeTracker.Type.bool) {
					System.out.println("Invalid type from function on line " + lineNumber +"! Expected boolean!");
					throw new InvalidLineException();
				}
			}
		} else {
			// Only allow non-compound int comparisons.
			for(int i = 0; i < tokens.length; i++) {
				String token = tokens[i];
				if(token.startsWith("\"")) {
					for(int j = i + 1; j < tokens.length; j++) {
						token += tokens[j];
						if(token.endsWith("\"")) {
							tokens[j] = "";
							ArrayList<String >tokensTemp = new ArrayList<>();
							for(int k = 0; k < tokens.length; k++) {
								if(!tokens[k].trim().isEmpty()) {
									tokensTemp.add(tokens[k]);
								}
							}
							tokens = tokensTemp.toArray(new String[0]);
							break;
						}
						tokens[j] = "";
					}
				}
			}
			if(tokens.length == 3) {
				// Only allow string equals string when a string is involved
				if ((scopeTracker.getType(tokens[0]) == ScopeTracker.Type.cables) && str_val.matcher(tokens[0]).find()) {
					if (!tokens[1].equals("sameSize")) {
						throw new InvalidLineException();
					}
					if ((scopeTracker.getType(tokens[2]) != ScopeTracker.Type.cables) && str_val.matcher(tokens[2]).find()) {
						throw new InvalidLineException();
					}
				}
				// Int logic
				if ((intTypes.contains(scopeTracker.getType(tokens[0])) && (int_val.matcher(tokens[0]).find()))) {
					if(!intComps.contains(tokens[1])) {
						throw new InvalidLineException();
					}
					if(!intTypes.contains(scopeTracker.getType(tokens[2])) && !int_val.matcher(tokens[2]).find()) {
						throw new InvalidLineException();
					}
				}
				// Function call logic
				Matcher m = function_call.matcher(tokens[0]);
				if(m.find()) {
					if(scopeTracker.getReturnType(m.group(1)) == ScopeTracker.Type.bool) {
						if(!tokens[1].equals("sameSize")) {
							System.out.println("Invalid int comparison operation on line " + lineNumber);
							throw new InvalidLineException();
						}
						Matcher m2 = function_call.matcher(tokens[2]);
						if(m2.find()) {
							if(scopeTracker.getReturnType(m2.group(1)) != ScopeTracker.Type.cables) {
								System.out.println("Invalid return type on line " + lineNumber);
								throw new InvalidLineException();
							}
						} else {
							if ((scopeTracker.getType(tokens[2]) != ScopeTracker.Type.cables) && !str_val.matcher(tokens[2]).find()) {
								System.out.println("Error on line" + lineNumber + "Cannot compare a string to this: " + tokens[2]);
								throw new InvalidLineException();
							}
						}
					} else if(intTypes.contains(scopeTracker.getReturnType(m.group(1)))) {
						if(!intComps.contains(tokens[1])) {
							System.out.println("Invalid int comparison operation on line " + lineNumber);
							throw new InvalidLineException();
						}
						Matcher m2 = function_call.matcher(tokens[2]);
						if(m2.find()) {
							if(!intTypes.contains(scopeTracker.getReturnType(m2.group(1)))) {
								System.out.println("Invalid return type on line " + lineNumber);
								throw new InvalidLineException();
							}
						} else {
							if (!intTypes.contains(scopeTracker.getType(tokens[2])) && !int_val.matcher(tokens[2]).find()) {
								System.out.println("Error on line" + lineNumber + "Cannot compare an int to this: " + tokens[2]);
								throw new InvalidLineException();
							}
						}
					}
				}
			} else {
				for(String token : tokens) {
					Matcher m = function_call.matcher(token);
					if(m.find()) {
						if(scopeTracker.getReturnType(m.group(1)) != ScopeTracker.Type.bool) {
							System.out.println("Invalid type from function on line " + lineNumber + "! Expected boolean!");
							throw new InvalidLineException();
						}
					} else if(scopeTracker.getType(token) != ScopeTracker.Type.bool && !bool_val.matcher(token).find()
							&& !boolOps.contains(token)) {
						System.out.println("Invalid boolean operation on line " + lineNumber);
						throw new InvalidLineException();
					}
				}
			}
		}
	}

	private static void checkVarAssignMatches(String cmd) throws InvalidLineException {
		String[] splitLine = cmd.substring(0, cmd.length() - 5).split("loadBar");
		ScopeTracker.Type assignedType;
		try {
			assignedType = getType(splitLine[0].split(" ")[0]);
		} catch (InvalidBlockException e) {
			assignedType = ScopeTracker.Type.notImportant;
		}
		if(assignedType == ScopeTracker.Type.notImportant) {
			assignedType = scopeTracker.getType(splitLine[0].split(" ")[0]); // Try to recover type for reassignment
		}
		if(assignedType == ScopeTracker.Type.notImportant) {
			return;
		}
		if(assignedType == ScopeTracker.Type.cables) {
			checkCablesValidity(splitLine[1].trim());
		} else if(intTypes.contains(assignedType)) {
			checkIntsValidity(splitLine[1].trim());
		}
	}

	private static void checkFunctionCall(String call) throws InvalidLineException {
		Matcher matcher = function_call.matcher(call);
		if(matcher.find()) {
			String name = matcher.group(1);
			String[] params = matcher.group(2).trim().split(",");
			ArrayList<ScopeTracker.Type> paramTypes = scopeTracker.getArgumentTypes(name);
			if(paramTypes == null && params.length != 0) {
				if(params.length == 1 && params[0].isEmpty()) {
					return;
				}
				System.out.println("Invalid Function Call on line " + lineNumber + ", Function takes no arguments!");
				throw new InvalidLineException();
			}
            assert paramTypes != null;
            if(paramTypes.size() != params.length) {
				System.out.println("Invalid function call on line " + lineNumber + ": " + call + "!, too few or too many arguments.");
				throw new InvalidLineException();
			}
			for(int i = 0; i < paramTypes.size(); i++) {
				ScopeTracker.Type paramType = paramTypes.get(i);
				if(intTypes.contains(paramType)) {
					if(!int_val.matcher(params[i]).find() && !intTypes.contains(scopeTracker.getType(params[i]))) {
						System.out.println("Wrong parameter type on line " + lineNumber + "! Expected int type");
						throw new InvalidLineException();
					}
				}
				if(paramType == ScopeTracker.Type.bool) {
					if(!bool_val.matcher(params[i]).find() && scopeTracker.getType(params[i]) != ScopeTracker.Type.bool) {
						System.out.println("Wrong parameter type on line " + lineNumber + "! Expected boolean");
						throw new InvalidLineException();
					}
				}
				if(paramType == ScopeTracker.Type.cables) {
					if(!str_val.matcher(params[i]).find() && scopeTracker.getType(params[i]) != ScopeTracker.Type.cables) {
						System.out.println("Wrong parameter type on line " + lineNumber + "! Expected string");
						throw new InvalidLineException();
					}
				}
			}
		}
	}

	private static void checkCablesValidity(String cmd) throws InvalidLineException {
		String[] tokens = cmd.split(" ");
		for(int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if (token.isEmpty()) {
				continue;
			}
			if (token.startsWith("\"")) { // Reconstruct string literals
				for (int j = i + 1; j < tokens.length; j++) {
					token += " " + tokens[j];
					if (tokens[j].endsWith("\"")) {
						tokens[j] = "";
						break;
					}
					tokens[j] = "";
				}
			}
			if (i == 0) { // Need to begin with a string, so we can safely concatenate.
				Matcher m = function_call.matcher(token);
				ScopeTracker.Type type = scopeTracker.getType(token);
				if (type == ScopeTracker.Type.cables) {
					continue;
				}
				if (type != null && scopeTracker.getType(token) != ScopeTracker.Type.cables) {
					System.out.println("Line " + lineNumber + ": Need to begin with a string to type coerce!\nGot " + type + ".");
					scopeTracker.printCurrentScopeInfo();
					throw new InvalidLineException();
				} else if (str_val.matcher(token).find()) {
					continue;
				} else if (m.find()) {
					if (scopeTracker.getReturnType(m.group(1)) != ScopeTracker.Type.cables) {
						throw new InvalidLineException();
					} else {
						checkFunctionCall(token);
					}
				} else {
					System.out.println("Invalid concatenation on line " + lineNumber);
					throw new InvalidLineException();
				}
			} else if (var.matcher(token).matches()) {
				continue;
			} else if (stringOps.contains(token)) {
				continue;
			} else if (str_val.matcher(token).find()) {
				continue;
			} else if (int_val.matcher(token).find()) {
				continue;
			} else {
				System.out.println("Invalid string operation \"" + token + "\" on line " + lineNumber);
				throw new InvalidLineException();
			}
		}
	}

	private static void checkIntsValidity(String cmd) throws InvalidLineException {
		String[] tokens = cmd.split(" ");
		for(int i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			if(intOps.contains(token)) {
				continue;
			}
			if(!var.matcher(token).matches()) { // This is not a variable name, or a valid op.
				Matcher m = function_call.matcher(token);
				if(m.find()) {
					if(!intTypes.contains((scopeTracker.getReturnType(m.group(1))))) {
						System.out.println("What even is this? This needs to be an int bro: " + token
						+ "\nExpected integer type, got: " + scopeTracker.getReturnType(m.group(1)).toString());
						throw new InvalidLineException();
					}
					checkFunctionCall(token);
				} else {
					try {
						String val = intVal(token);
					} catch (InvalidLineException e) {
						System.out.println("What even is this? This needs to be an int bro: " + token
								+ "\nExpected integer type, got: " + scopeTracker.getReturnType(m.group(1)).toString());
						throw new InvalidLineException();
					}
				}
			}
		}
	}

	private static void checkReturnType(String cmd) throws InvalidLineException {
		ScopeTracker.Type returnType = scopeTracker.getReturnType();
		String expr = cmd.substring(6, cmd.length() - 5);
		if(intTypes.contains(returnType)) { // Int checking logic, all are int types, uses any int op
			checkIntsValidity(expr);
		} else if(returnType == ScopeTracker.Type.cables) { // String checking logic, first token must be a string, only uses mult or add
			checkCablesValidity(expr);
		}
	}

	private static String parseCmd(String cmd) throws InvalidBlockException {
		if(cmd.isEmpty()) {
			return " ";
		}
		String pythonLine;
		try {
			pythonLine = varAssign(cmd);
			lastLine = LastLine.VarAssign;
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = funcDec(cmd);
			if(debugMode) {
				addFunction(cmd);
			}
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
			scopeTracker.endBlock();
			return pythonLine;
		} catch (InvalidLineException e) {
		}
		try {
			pythonLine = ifExpr(cmd);
			lastLine = LastLine.IfExpr;
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
			lastLine = LastLine.Return;
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
		try {
			pythonLine = funcCallExpr(cmd);
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
			type(m.group(1)); // No need to check group two, it's always valid if found
			scopeTracker.setReturnType(getType(m.group(1)));
			String functionName = m.group(2);
			String parameters = "";
			if(!m.group(3).isEmpty()) {
				parameters = varDecList(m.group(3));
			}
			scopeTracker.insertNewBlock();
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

	private static String funcCallExpr(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = function_call_standalone.matcher(cmd);
		boolean match = m.find();
		String parameters = "";
		if(match) {
			// Group 1 is always valid if it's found, type checking will handle this later
			if(!m.group(2).isEmpty()) {
				int paramNum = 0;
				for(String s : m.group(2).trim().split(",")) {
					if(var.matcher(s).find()) {
						parameters += s;
					} else if (val(s) != null) {
						parameters += val(s);
					} else {
						throw new InvalidLineException();
					}
					if(paramNum != (m.group(2).trim().split(",").length - 1)) {
						parameters += ",";
					}
					paramNum++;
				}
			}
		}
		printMsg(match, "<func_call>", cmd, "function call");
		if (!match)
			throw new InvalidLineException();
		return m.group(1) + "(" + parameters + ")"; // Converted call to python
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
		}else if(type.equals("tryBench")) {
			typeEnum = ScopeTracker.Type.bool;
		} else {
			throw new InvalidBlockException();
		}
		return typeEnum;
	}

	private static String valList(String cmd) throws InvalidLineException, InvalidBlockException {
		String[] split = cmd.split(", ");
		StringBuilder valDec = new StringBuilder();
		for (int i = 0; i < split.length; i++) {
			if(split[i].isEmpty()) {
				continue;
			}
			String token = split[i];
			if(token.contains("\"")) {
				for(int j = i + 1; j < split.length; j++) {
					token += ", " + split[j];
					if(split[j].endsWith("\"")) {
						split[j] = "";
						break;
					}
					split[j] = "";
				}
			}
			String value = val(token);

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
		try {
			pythonLine = functionCall(cmd);
			return pythonLine;
		} catch (InvalidLineException e) {}

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
		Matcher m = bool_val.matcher(cmd);
		if (m.find()) {
			printMsg(true, "<bool>", cmd, "<bool>");
			if(cmd.equals("gotItUp"))
			{
				return "True";
			}	
			return "False";
		}
		printMsg(false, "<bool>", cmd, "<bool>");
		throw new InvalidLineException();
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
			if (leftExpr == null) {
				try {
					leftExpr = functionCall(m.group(1));
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
			if (leftExpr == null) {
				try {
					leftExpr = var(false, m.group(1), null);
				} catch (InvalidLineException e) {
				}
			}
			if (leftExpr == null) {
				try {
					leftExpr = functionCall(m.group(1));
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
			if (rightExpr == null) {
				try {
					rightExpr = var(false, m.group(2), null);
				} catch (InvalidLineException e) {
				}
			}
			if (rightExpr == null) {
				try {
					rightExpr = functionCall(m.group(2));
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

	private static String functionCall(String cmd) throws InvalidLineException, InvalidBlockException {
		Matcher m = function_call.matcher(cmd);
		boolean match = m.find();
		String parameters = "";
		if(match) {
			// Group 1 is always valid if it's found, type checking will handle this later
			if(!m.group(2).isEmpty()) {
				int paramNum = 0;
				for(String s : m.group(2).trim().split(",")) {
					if(var.matcher(s).find()) {
						parameters += s;
					} else if (val(s) != null) {
						parameters += val(s);
					} else {
						throw new InvalidLineException();
					}
					if(paramNum != (m.group(2).trim().split(",").length - 1)) {
						parameters += ",";
					}
					paramNum++;
				}
			}
		}
		printMsg(match, "<func_call>", cmd, "function call");
		if (!match)
			throw new InvalidLineException();
		return m.group(1) + "(" + parameters + ")"; // Converted call to python
	}

	private static String someIntExpr(String cmd, Matcher m, String exprName, String symbol) throws InvalidLineException, InvalidBlockException {

		if (m.find()) {
			String leftExpr = null;
			try {
				leftExpr = intVal(m.group(1));
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
					leftExpr = intExpr(m.group(1));
				} catch (InvalidLineException e) {
				}
			}

			if (leftExpr == null) {
				try {
					leftExpr = strVal(m.group(1));
				} catch (InvalidLineException e) {
				}
			}

			if (leftExpr == null) {
				try {
					leftExpr = functionCall(m.group(1));
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
                } catch (InvalidLineException e) {
				}
			}
			if (rightExpr == null) {
				try {
					rightExpr = intExpr(m.group(2));
				} catch (InvalidLineException e) {
				}
			}

			if (rightExpr == null) {
				try {
					rightExpr = strVal(m.group(2));
				} catch (InvalidLineException e) {
				}
			}

			if (rightExpr == null) {
				try {
					rightExpr = functionCall(m.group(2));
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
			if (leftExpr == null) {
				try {
					leftExpr = functionCall(m.group(1));
				} catch (InvalidLineException e) {
				}
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
		scopeTracker.endBlock();
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
			String value = "";
			boolean match = false;
			try {
				value = functionCall(m.group(1));
				match = true;
			} catch (InvalidLineException e) {}
			try {
				value = var(false, m.group(1), null);
				match = true;
			} catch (InvalidLineException e) {}
			try {
				value = val(m.group(1));
				match = true;
			} catch (InvalidLineException e) {}
			if(!match) {
				throw new InvalidLineException();
			}
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
