import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

public class Parser {
    private static final Pattern var_assign = Pattern.compile("^(.+) loadBar (.+) pump$");
    private static final Pattern func_dec = Pattern.compile("^workout (.+) (.+)[(](.+)[)] leftWeightClip$");
    private static final Pattern loop_dec = Pattern.compile("^set (.+), (.+) to (.+) leftWeightClip$");
    private static final Pattern type_var_dec = Pattern.compile("^(\\w+) (\\w+)$");
    private static final Pattern type = Pattern.compile("^ryanBullard$|^lightWeight$|^weight$|^cables$|^pr$|^samSulek$");
    private static final Pattern var = Pattern.compile("^pecs(\\d)*$|^delts(\\d)*$|^lats(\\d)*$|^biceps(\\d)*$|^triceps(\\d)*$|^abs(\\d)*$|^obliques(\\d)*$|^quads(\\d)*$|^hamstrings(\\d)*$|^" +
            "glutes(\\d)*$|^calves(\\d)*$|^forearms(\\d)*$");
    private static final Pattern int_val = Pattern.compile("^\\d+$|^-\\d+$|^-\\d+[.]\\d+$|^\\d+[.]\\d+$");
    
    private static final Pattern str_val = Pattern.compile("^\"(.*)\"$");
    private static final Pattern bool = Pattern.compile("^gotItUp$|^failed$");
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

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.print(">> ");
        String cmd = in.nextLine();
        while (!cmd.equals("exit")) {
            parseCmd(cmd);
            System.out.print(">> ");
            cmd = in.nextLine();
        }
    }

    private static void parseCmd(String cmd) {
        if(varAssign(cmd)) {
            System.out.println("<stmt>");
        } else if(funcDec(cmd)) {
            System.out.println("<stmt>");
        } else if(loopDec(cmd)) {
            System.out.println("<stmt>");
        } else if(endScope(cmd)) {
            System.out.println("<stmt>");
        } else if(ifExpr(cmd)) {
            System.out.println("<stmt>");
        } else if(elseExpr(cmd)) {
            System.out.println("<stmt>");
        } else if(returnExpr(cmd)) {
            System.out.println("<stmt>");
        }
    }

    private static boolean endScope(String cmd) {
        Matcher m = end_scope.matcher(cmd);
        boolean match = m.find();
        printMsg(match, "\n<end_scope>", cmd, "end of scope");
        return match;
    }

    private static boolean loopDec(String cmd) {
        Matcher m = loop_dec.matcher(cmd);
        boolean match = false;
        if(m.find()) {
            match = var(m.group(1)); // group 1 is variable name, group 2 is first bound, group 3 is second bound
            match = match && val(m.group(2));
            match = match && val(m.group(3));
        }
        printMsg(match, "\n<loop_dec>", cmd, "loop declaration");
        return match;
    }

    private static boolean funcDec(String cmd) {
        Matcher m = func_dec.matcher(cmd);
        boolean match = false;
        if (m.find()) { // group 1 is the type, group 2 is the name, group 3 is the parameters
            match = type(m.group(1)); // No need to check group two, it's always valid if found
            match = match && varDecList(m.group(3));
        }
        printMsg(match, "\n<func_dec>", cmd, "function declaration");
        return match;
    }

    private static boolean varAssign(String cmd) {
        Matcher m = var_assign.matcher(cmd);
        boolean match = false;
        if (m.find()) {
            match = varDecList(m.group(1));
            match = match && valList(m.group(2));
        }
        printMsg(match, "\n<var_assign>", cmd, "variable assignment statement");
        return match;
    }

    private static boolean varDecList(String cmd) {
        String[] split = cmd.split(", ");
        boolean match = true;
        for (String s : split) {
            match = match && varDec(s);
        }
        printMsg(match, "<var_dec_list>", cmd, "variable declaration list");
        return match;
    }

    private static boolean varDec(String cmd) {
        boolean match;
        Matcher m = type_var_dec.matcher(cmd);
        if (m.find()) {
            match = type(m.group(1));
            match = match && var(m.group(2));
        } else
            match = var(cmd);
        printMsg(match, "<var_dec>", cmd, "variable declaration");
        return match;
    }

    private static boolean type(String cmd) {
        Matcher m = type.matcher(cmd);
        boolean match = m.find();
        printMsg(match, "<type>", cmd, "type");
        return match;
    }

    private static boolean var(String cmd) {
        Matcher m = var.matcher(cmd);
        boolean match = m.find();
        printMsg(match, "<var>", cmd, "variable");
        return match;
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

    private static boolean val(String cmd) {
        boolean match = false;
        if (intVal(cmd)) {
            match = true;
        } else if (boolVal(cmd)) {
            match = true;
        } else if(var(cmd)) {
            match = true;
        } else if(strVal(cmd)) {
            match = true;
        } else if(intExpr(cmd)) {
            match = true;
        } else if(boolExpr(cmd)) {
            match = true;
        } else if(incrementExpr(cmd)) {
            match = true;
        }
        printMsg(match, "<val>", cmd, "value");
        return match;
    }

    private static boolean strVal(String cmd) {
        Matcher m = str_val.matcher(cmd);
        boolean match = false;
        if(m.find()) {
            match = true;
            Matcher m2 = Pattern.compile("(?<!\\\\)\"").matcher(m.group(1));
            if(m2.find()) {
                match = false;
            }
        }
        printMsg(match, "\n<str_val>", cmd, "string value");
        return match;
    }

    private static boolean boolVal(String cmd) {
        Matcher m = bool.matcher(cmd);
        boolean match = m.find();
        if (match) {
            printMsg(true, "<bool>", cmd, "boolean");
        }
        return match;
    }

    private static boolean boolExpr(String cmd) {
        boolean match = false;
        if (andExpr(cmd)) {
            match = true;
        } else if (orExpr(cmd)) {
            match = true;
        } else if(notExpr(cmd)) {
            match = true;
        } else if(equalExpr(cmd)) {
            match = true;
        } else if(lessExpr(cmd)) {
            match = true;
        } else if(greaterExpr(cmd)) {
            match = true;
        }
        return match;
    }

    private static boolean andExpr(String cmd) {
        boolean match = false;
        Matcher m = and_expr.matcher(cmd);
        if (m.find()) {
            //can either match bool values or more and expressions
            match = boolExpr(m.group(1)) || val(m.group(1));
            match = match && (boolExpr(m.group(2)) || val(m.group(2)));
        }
        printMsg(match, "<and_expr>", cmd, "and expression");
        return match;
    }

    private static boolean orExpr(String cmd) {
        boolean match = false;
        Matcher m = or_expr.matcher(cmd);
        if (m.find()) {
            //can either match bool values or more and expressions
            match = boolExpr(m.group(1)) || val(m.group(1));
            match = match && (boolExpr(m.group(2)) || val(m.group(2)));
        }
        printMsg(match, "<or_expr>", cmd, "or expression");
        return match;
    }

    private static boolean notExpr(String cmd) {
        boolean match = false;
        Matcher m = not_expr.matcher(cmd);
        if (m.find()) {
            match = boolExpr(m.group(1)) || val(m.group(1));
        }
        printMsg(match, "<int_expr>", cmd, "integer expression");
        return match;
    }

    private static boolean equalExpr(String cmd) {
        boolean match = false;
        Matcher m = equal_to.matcher(cmd);
        if (m.find()) {
            match = boolExpr(m.group(1)) || val(m.group(1)) || intExpr(m.group(1));
            match = match && (boolExpr(m.group(2)) || val(m.group(2)) || intExpr(m.group(1)));
        }
        printMsg(match, "<equal_expr>", cmd, "equality expression");
        return match;
    }

    private static boolean lessExpr(String cmd) {
        boolean match = false;
        Matcher m = less_than.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = match && (intVal(m.group(2)) || intExpr(m.group(2)));
        }
        printMsg(match, "<less_expr>", cmd, "less than expression");
        return match;
    }

    private static boolean greaterExpr(String cmd) {
        boolean match = false;
        Matcher m = greater_than.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = match && (intVal(m.group(1)) || intExpr(m.group(1)));
        }
        printMsg(match, "<greater_expr>", cmd, "greater than expression");
        return match;
    }

    private static void printMsg(boolean match, String ntName, String cmd, String item) {
        if (match)
            System.out.println(ntName + ": " + cmd);
        else
            System.out.println("Failed to parse: {" + cmd + "} is not a valid " + item + ".");
    }

    private static boolean intVal(String cmd) {
        Matcher m = int_val.matcher(cmd);
        boolean match = m.find();
        if (match) {
            printMsg(true, "<int>", cmd, "integer");
        }
        printMsg(match, "<val>", cmd, "integer value");
        return match;
    }

    private static boolean intExpr(String cmd) {
        boolean match = false;
        if (addExpr(cmd)) {
            match = true;
        } else if (subExpr(cmd)) {
            match = true;
        } else if(multExpr(cmd)) {
            match = true;
        } else if (divExpr(cmd)) {
            match = true;
        } else if(modExpr(cmd)) {
            match = true;
        } else if(incrementExpr(cmd)) {
            match = true;
        }
        return match;
    }

    private static boolean addExpr(String cmd) {
        boolean match = false;
        Matcher m = add_expr.matcher(cmd);
        if (m.find()) {
        	match = intVal(m.group(1)) || var(m.group(1)) || intExpr(m.group(1));
            match = (match && intVal(m.group(2))) || (match && var(m.group(2))) || (match && intExpr(m.group(2)));
        }
        printMsg(match, "<add_expr>", cmd, "addition expression");
        return match;
    }

    private static boolean subExpr(String cmd) {
        boolean match = false;
        Matcher m = sub_expr.matcher(cmd);
        if (m.find()) {
        	match = intVal(m.group(1)) || var(m.group(1)) || intExpr(m.group(1));
            match = (match && intVal(m.group(2))) || (match && var(m.group(2))) || (match && intExpr(m.group(2)));
        }
        printMsg(match, "<sub_expr>", cmd, "subtraction expression");
        return match;
    }

    private static boolean multExpr(String cmd) {
        boolean match = false;
        Matcher m = mult_expr.matcher(cmd);
        if (m.find()) {
        	match = intVal(m.group(1)) || var(m.group(1)) || intExpr(m.group(1));
            match = (match && intVal(m.group(2))) || (match && var(m.group(2))) || (match && intExpr(m.group(2)));
        }
        printMsg(match, "<mult_expr>", cmd, "multiplication expression");
        return match;
    }

    private static boolean divExpr(String cmd) {
        boolean match = false;
        Matcher m = div_expr.matcher(cmd);
        if (m.find()) {
        	match = intVal(m.group(1)) || var(m.group(1)) || intExpr(m.group(1));
            match = (match && intVal(m.group(2))) || (match && var(m.group(2))) || (match && intExpr(m.group(2)));
        }
        printMsg(match, "<div_expr>", cmd, "division expression");
        return match;
    }

    private static boolean modExpr(String cmd) {
        boolean match = false;
        Matcher m = mod_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || var(m.group(1)) || intExpr(m.group(1));
            match = (match && intVal(m.group(2))) || (match && var(m.group(2))) || (match && intExpr(m.group(2)));
        }
        printMsg(match, "<mod_expr>", cmd, "modulus expression");
        return match;
    }

    public static boolean incrementExpr(String cmd) {
        boolean match = false;
        Matcher m = increment.matcher(cmd);
        if (m.find()) {
            //can either match integer values or more integer expressions
            match = intExpr(m.group(1)) || intVal(m.group(1)) || var(m.group(1));
        }
        printMsg(match, "<increment_expr>", cmd, "integer increment expression");
        return match;
    }

    public static boolean ifExpr(String cmd) {
        boolean match = false;
        Matcher m = if_expr.matcher(cmd);
        if (m.find()) {
            //can either match integer values or more integer expressions
            match = boolExpr(m.group(1)) || var(m.group(1));
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
        if(m.find()) {
            match = val(m.group(1));
        }
        printMsg(match, "<return_expr>", cmd, "return expression");
        return match;
    }
}
    
