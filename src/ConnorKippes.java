import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;

public class ConnorKippes {
    private static final Pattern var_assign = Pattern.compile("^(.+) loadBar (.+) pump$");
    private static final Pattern func_dec = Pattern.compile("^workout (.+) (.+)[(](.+)[)] leftWeightClip$");
    private static final Pattern loop_dec = Pattern.compile("^set (.+), (.+) to (.+) leftWeightClip$");
    private static final Pattern type_var_dec = Pattern.compile("^(\\w+) (\\w+)$");
    private static final Pattern type = Pattern.compile("^ryanBullard$|^lightWeight$|^weight$|^cables$|^pr$|^samSulek$");
    private static final Pattern var = Pattern.compile("^pecs(\\d)*$|^delts(\\d)*$|^lats(\\d)*$|^biceps(\\d)*$|^triceps(\\d)*$|^abs(\\d)*$|^obliques(\\d)*$|^quads(\\d)*$|^hamstrings(\\d)*$|^" +
            "glutes(\\d)*$|^calves(\\d)*$|^forearms(\\d)*$");
    private static final Pattern intVal = Pattern.compile("^\\d+$|^-\\d+$");
    private static final Pattern strVal = Pattern.compile("^\"(.*)\"$");
    private static final Pattern bool = Pattern.compile("^gotItUp$|^failed$");
    private static final Pattern end_scope = Pattern.compile("^rightWeightClip$");
    
    private static final Pattern increment = Pattern.compile("^(.+) superset$");
    
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
            return;
        } else if(funcDec(cmd)) {
            System.out.println("<stmt>");
            return;
        } else if(loopDec(cmd)) {
            System.out.println("<stmt>");
            return;
        } else if(endScope(cmd)) {
            System.out.println("<stmt>");
            return;
        }
        System.out.println("<stmt>");
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
            match = true;
            match = match && varDecList(m.group(1));
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
        boolean match = false;
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
    
    //changed this class
    private static boolean val(String cmd) {
        boolean match = false;
        if (intVal(cmd)) {
            match = true;
        } else if (boolVal(cmd)) {
            match = true;
        } else if(var(cmd)) {
            match = true;
        } else if(str_val(cmd)) {
            match = true;
            
        } else if(intExpr(cmd)) {
            match = true;
        
        } else if(boolExpr(cmd)) {
            match = true;
        }
        printMsg(match, "<val>", cmd, "value");
        return match;
    }

    private static boolean str_val(String cmd) {
        Matcher m = strVal.matcher(cmd);
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
    
    private static void printMsg(boolean match, String ntName, String cmd, String item) {
        if (match)
            System.out.println(ntName + ": " + cmd);
        else
            System.out.println("Failed to parse: {" + cmd + "} is not a valid " + item + ".");
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
        if (addExpr(cmd)) {
            match = true;
        } else if (orExpr(cmd)) {
            match = true;
        } else if(notExpr(cmd)) {
            match = true;
        }
        return match;
    }
    
    private static boolean andExpr(String cmd) {
        boolean match = false;
        Matcher m = and_expr.matcher(cmd);
        if (m.find()) {
            //can either match bool values or more and expressions
            match = andExpr(m.group(1)) || orExpr(m.group(1)) || boolVal(m.group(1));
            match = (match && andExpr(m.group(2))) || (match && orExpr(m.group(2))) || (match && boolVal(m.group(2)));
        }
        printMsg(match, "<and_expr>", cmd, "and expression");
        return match;
    }
    
    private static boolean orExpr(String cmd) {
        boolean match = false;
        Matcher m = or_expr.matcher(cmd);
        if (m.find()) {
            //can either match bool values or more and expressions
            match = orExpr(m.group(1)) || andExpr(m.group(1)) || boolVal(m.group(1));
            match = (match && orExpr(m.group(2))) || (match && andExpr(m.group(2))) || (match && boolVal(m.group(2)));
        }
        printMsg(match, "<or_expr>", cmd, "or expression");
        return match;
    }
    
    private static boolean notExpr(String cmd) {
        boolean match = false;
        Matcher m = not_expr.matcher(cmd);
        if (m.find()) {
            match = notExpr(m.group(1)) || orExpr(m.group(1)) || andExpr(m.group(1)) || boolVal(m.group(1));
        }
        printMsg(match, "<int_expr>", cmd, "integer expression");
        return match;
    }

    private static boolean intVal(String cmd) {
        Matcher m = intVal.matcher(cmd);
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
        }
        return match;
    }
    
    private static boolean addExpr(String cmd) {
        boolean match = false;
        Matcher m = add_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = ((match && intVal(m.group(2)) || match && intExpr(m.group(2))));
        }
        printMsg(match, "<add_expr>", cmd, "addition expression");
        return match;
    }
    
    private static boolean subExpr(String cmd) {
        boolean match = false;
        Matcher m = sub_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = ((match && intVal(m.group(2)) || match && intExpr(m.group(2))));
        }
        printMsg(match, "<sub_expr>", cmd, "subtraction expression");
        return match;
    }
    
    private static boolean multExpr(String cmd) {
        boolean match = false;
        Matcher m = mult_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = ((match && intVal(m.group(2)) || match && intExpr(m.group(2))));
        }
        printMsg(match, "<mult_expr>", cmd, "multiplication expression");
        return match;
    }
    
    private static boolean divExpr(String cmd) {
        boolean match = false;
        Matcher m = div_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = ((match && intVal(m.group(2)) || match && intExpr(m.group(2))));
        }
        printMsg(match, "<div_expr>", cmd, "division expression");
        return match;
    }
    
    private static boolean modExpr(String cmd) {
        boolean match = false;
        Matcher m = mod_expr.matcher(cmd);
        if (m.find()) {
            match = intVal(m.group(1)) || intExpr(m.group(1));
            match = ((match && intVal(m.group(2)) || match && intExpr(m.group(2))));
        }
        printMsg(match, "<mod_expr>", cmd, "modulus expression");
        return match;
    }
    
    public static boolean incrementExpr(String cmd)
    {
    	boolean match = false;
        Matcher m = increment.matcher(cmd);
        if (m.find()) {
            match = true;
            //can either match integer values or more integer expressions
            match = (match && intExpr(m.group(1)));
        }
        printMsg(match, "<increment_expr>", cmd, "integer increment expression");
        return match;
    }
}