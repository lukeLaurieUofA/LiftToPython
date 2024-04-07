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
    private static final Pattern intVal = Pattern.compile("^\\d+$|^-\\d+$");
    private static final Pattern strVal = Pattern.compile("^\"(.*)\"$");
    private static final Pattern bool = Pattern.compile("^gotItUp$|^failed$");
    private static final Pattern end_scope = Pattern.compile("^rightWeightClip$");
    private static final Pattern int_expr = Pattern.compile("^(.+) creatine (.+)|(.+) restDay (.+)|(.+) steroids (.+)|(.+) vegan (.+)|(.+) muscleMass (.+)$");

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

    private static boolean val(String cmd) {
        boolean match = false;
        if (intVal(cmd)) {
            match = true;
        } else if (bool_val(cmd)) {
            match = true;
        } else if(var(cmd)) {
            match = true;
        } else if(str_val(cmd)) {
            match = true;
        } else if(intExpression(cmd)) {
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

    private static boolean bool_val(String cmd) {
        Matcher m = bool.matcher(cmd);
        boolean match = false;
        if(m.find()) {
            match = true;
            printMsg(true, "<bool>", cmd, "boolean");
        }
        printMsg(match, "<val>", cmd, "value");
        return match;
    }

    private static void printMsg(boolean match, String ntName, String cmd, String item) {
        if (match)
            System.out.println(ntName + ": " + cmd);
        else
            System.out.println("Failed to parse: {" + cmd + "} is not a valid " + item + ".");
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

    private static boolean intExpression(String cmd) {
        boolean match = false;
        Matcher m = int_expr.matcher(cmd);
        if (m.find()) {
            //can either match integer values or more integer expressions
            match = intVal(m.group(1)) || intVal(m.group(1));
            match = (match && intVal(m.group(2))) || (match && intVal(m.group(2)));
        }
        printMsg(match, "<int_expr>", cmd, "integer expression");
        return match;
    }
}
    
