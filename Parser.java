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
    private int indentLevel = 0;

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
        else if (checkFor("if", s)) {
            require(OPENPAREN, "Missing '('", s);
            ConditionNode cond = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new StatementNode(new IfNode(parseBlock(s), cond));
        }
        else if (checkFor("while", s)) {
            require(OPENPAREN, "Missing '('", s);
            ConditionNode cond = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new StatementNode(new WhileNode(parseBlock(s), cond));
        }
        else {
            return new StatementNode(parseAction(s));
        }
    }

    ConditionNode parseCond(Scanner s) {
        String relop = require("lt|gt|eq", "Invalid operator", s);
        require(OPENPAREN, "Missing '('", s);
        String sensor = require("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist", "Invalid sensor", s);
        require(",", "Missing ','", s);
        int num = requireInt(NUMPAT, "Invalid number", s);
        require(CLOSEPAREN, "Missing ')'", s);
        return new ConditionNode(relop, new SensorNode(sensor), num);
    }

    BlockNode parseBlock(Scanner s) {
        indentLevel++;
        List<StatementNode> statements = new ArrayList<>();
        require(OPENBRACE, "Missing '{'", s);
        while (!checkFor(CLOSEBRACE, s)) {
            statements.add(parseStatement(s));
        }
        if (statements.isEmpty()) { fail("Empty loop", s); }
        BlockNode node = new BlockNode(statements, indentLevel);
        indentLevel--;
        return node;
    }

    ActionNode parseAction(Scanner s) {
        String action = require("move|turnL|turnR|turnAround|shieldOn|shieldOff|takeFuel|wait", "Invalid action", s);
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
        for (StatementNode statement : statements) {
            statement.execute(robot);
        }
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
            case "turnAround" -> robot.turnAround();
            case "shieldOn" -> robot.setShield(true);
            case "shieldOff" -> robot.setShield(false);
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
    int indent;

    BlockNode(List<StatementNode> statements, int indent) {
        this.statements = statements;
        this.indent = indent;
    }

    @Override
    public void execute(Robot robot) {
        for (StatementNode statement : statements) {
            statement.execute(robot);
        }
    }

    public String toString() {
        String toReturn = "{\n";
        for (StatementNode statement : statements) {
            toReturn += "    ".repeat(indent)+statement.toString();
        }
        toReturn += "    ".repeat(indent-1)+"}";
        return toReturn;
    }
}

class IfNode implements ProgramNode {
    BlockNode block;
    ConditionNode cond;
    IfNode(BlockNode block, ConditionNode cond) { this.block = block; this.cond = cond; }

    @Override
    public void execute(Robot robot) {
        if (cond.evaluate(robot)) {
            block.execute(robot);
        }
    }

    public String toString() {
        return "if("+cond.toString()+")"+this.block.toString();
    }
}

class WhileNode implements ProgramNode {
    BlockNode block;
    ConditionNode cond;
    WhileNode(BlockNode block, ConditionNode cond) { this.block = block; this.cond = cond; }
    @Override
    public void execute(Robot robot) {
        while (cond.evaluate(robot)) {
            block.execute(robot);
        }
    }

    public String toString() {
        return "while("+cond.toString()+")"+this.block.toString();
    }
}

class ConditionNode implements BooleanNode {
    String relop;
    SensorNode sensor;
    int num;

    ConditionNode(String relop, SensorNode sensor, int number) {
        this.relop = relop;
        this.sensor = sensor;
        this.num = number;
    }

    @Override
    public boolean evaluate(Robot robot) {
        return switch (relop) {
            case "lt" -> sensor.evaluate(robot) < num;
            case "gt" -> sensor.evaluate(robot) > num;
            case "eq" -> sensor.evaluate(robot) == num;
            default -> throw new ParserFailureException("Invalid operator"); // this should never run
        };
    }

    public String toString() {
        return relop+"("+sensor.toString()+", "+num+")";
    }
}

class SensorNode implements IntNode {
    String sensor;
    SensorNode(String sensor) { this.sensor = sensor; }
    @Override
    public int evaluate(Robot robot) {
        return switch(sensor) {
            case "fuelLeft" -> robot.getFuel();
            case "oppLR" -> robot.getOpponentLR();
            case "oppFB" -> robot.getOpponentFB();
            case "numBarrels" -> robot.numBarrels();
            case "barrelLR" -> robot.getClosestBarrelLR();
            case "barrelFB" -> robot.getClosestBarrelFB();
            case "wallDist" -> robot.getDistanceToWall();
            default -> throw new ParserFailureException("Invalid sensor"); // this should never run
        };
    }

    public String toString() {
        return sensor;
    }
}
