import java.util.*;
import java.util.regex.*;

/**
 * See assignment handout for the grammar.
 * You need to implement the parse(..) method and all the rest of the parser.
 * There are several methods provided for you:
 * - several utility methods to help with the parsing
 * See also the TestParser class for testing your code.
 */
public class Parser {


    // Useful Patterns

    static final Pattern NUMPAT = Pattern.compile("-?[1-9][0-9]*|0"); 
    static final Pattern OPENPAREN = Pattern.compile("\\(");
    static final Pattern CLOSEPAREN = Pattern.compile("\\)");
    static final Pattern OPENBRACE = Pattern.compile("\\{");
    static final Pattern CLOSEBRACE = Pattern.compile("\\}");

    //----------------------------------------------------------------
    /**
     * The top of the parser, which is handed a scanner containing
     * the text of the program to parse.
     * Returns the parse tree.
     */
    ProgramNode parse(Scanner s) {
        // Set the delimiter for the scanner.
        s.useDelimiter("\\s+|(?=[{}(),;])|(?<=[{}(),;])");
        return parseProg(s);
    }

    ProgNode parseProg(Scanner s) {
        if (!s.hasNext()) {
            System.out.println("Provided file is empty, running default program.");
            return null;
        }
        List<StatementNode> statements = new ArrayList<>();
        while (s.hasNext()) {
            statements.add(parseStatement(s));
        }
        return new ProgNode(statements);
    }

    StatementNode parseStatement(Scanner s) {
        if (checkFor("loop", s)) {
            return new StatementNode(new LoopNode(parseBlock(s)));
        }
        else {
            return new StatementNode(parseAction(s));
        }
    }

    BlockNode parseBlock(Scanner s) {
        List<StatementNode> statements = new ArrayList<>();
        require(OPENBRACE, "Missing '{'", s);
        while (!checkFor(CLOSEBRACE, s)) {
            statements.add(parseStatement(s));
        }
        if (statements.isEmpty()) { fail("Empty loop", s); }
        return new BlockNode(statements);
    }

    ActionNode parseAction(Scanner s) {
        String action = require("move|turnL|turnR|takeFuel|wait", "Invalid action", s);
        require(";", "Missing semicolon", s);
        return new ActionNode(action);
    }





    //----------------------------------------------------------------
    // utility methods for the parser
    // - fail(..) reports a failure and throws exception
    // - require(..) consumes and returns the next token as long as it matches the pattern
    // - requireInt(..) consumes and returns the next token as an int as long as it matches the pattern
    // - checkFor(..) peeks at the next token and only consumes it if it matches the pattern

    /**
     * Report a failure in the parser.
     */
    static void fail(String message, Scanner s) {
        String msg = message + "\n   @ ...";
        for (int i = 0; i < 5 && s.hasNext(); i++) {
            msg += " " + s.next();
        }
        throw new ParserFailureException(msg + "...");
    }

    /**
     * Requires that the next token matches a pattern if it matches, it consumes
     * and returns the token, if not, it throws an exception with an error
     * message
     */
    static String require(String p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    static String require(Pattern p, String message, Scanner s) {
        if (s.hasNext(p)) {return s.next();}
        fail(message, s);
        return null;
    }

    /**
     * Requires that the next token matches a pattern (which should only match a
     * number) if it matches, it consumes and returns the token as an integer
     * if not, it throws an exception with an error message
     */
    static int requireInt(String p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    static int requireInt(Pattern p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {return s.nextInt();}
        fail(message, s);
        return -1;
    }

    /**
     * Checks whether the next token in the scanner matches the specified
     * pattern, if so, consumes the token and return true. Otherwise returns
     * false without consuming anything.
     */
    static boolean checkFor(String p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;}
        return false;
    }

    static boolean checkFor(Pattern p, Scanner s) {
        if (s.hasNext(p)) {s.next(); return true;} 
        return false;
    }

}

// You could add the node classes here or as separate java files.
// (if added here, they must not be declared public or private)
// For example:
//  class BlockNode implements ProgramNode {.....
//     with fields, a toString() method and an execute() method
//

class ProgNode implements ProgramNode {
    List<StatementNode> statements;
    ProgNode(List<StatementNode> statements) { this.statements = statements; }

    @Override
    public void execute(Robot robot) {

    }

    public String toString() {
        String toReturn = "";
        for (StatementNode statement : statements) {
            toReturn += statement.toString();
        }
        return toReturn.substring(0, toReturn.length()-1);
    }
}

class StatementNode implements ProgramNode {
    ProgramNode statement;
    StatementNode(ProgramNode statement) { this.statement = statement; }
    @Override
    public void execute(Robot robot) {
        statement.execute(robot);
    }

    public String toString() {
        return this.statement.toString()+"\n";
    }
}

class ActionNode implements ProgramNode {
    String actionType;
    ActionNode(String type) {actionType = type;}

    @Override
    public void execute(Robot robot) {
        switch(actionType){
            case "move" -> robot.move();
            case "turnL" -> robot.turnLeft();
            case "turnR" -> robot.turnRight();
            case "takeFuel" -> robot.takeFuel();
            case "wait" -> robot.idleWait();
        }
    }

    public String toString() {
        return actionType+";";
    }
}

class LoopNode implements ProgramNode {
    BlockNode block;
    LoopNode(BlockNode block) { this.block = block; }

    @Override
    public void execute(Robot robot) {
        while (true) {
            block.execute(robot);
        }
    }

    public String toString() {
        return "loop" + this.block.toString();
    }
}

class BlockNode implements ProgramNode {
    List<StatementNode> statements;

    BlockNode(List<StatementNode> statements) { this.statements = statements; }

    @Override
    public void execute(Robot robot) {
        for (StatementNode statement : statements) {
            statement.execute(robot);
        }
    }

    public String toString() {
        String toReturn = "{\n";
        for (StatementNode statement : statements) {
            toReturn += "\t"+statement.toString();
        }
        toReturn += "}";
        return toReturn;
    }
}


