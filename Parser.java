import javax.swing.text.html.Option;
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
            BlockNode ifBlock = parseBlock(s);
            if (checkFor("else", s)) {
                BlockNode elseBlock = parseBlock(s);
                return new StatementNode(new IfNode(ifBlock, cond, elseBlock));
            }
            return new StatementNode(new IfNode(ifBlock, cond));
        }
        else if (checkFor("while", s)) {
            require(OPENPAREN, "Missing '('", s);
            ConditionNode cond = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new StatementNode(new WhileNode(parseBlock(s), cond));
        }
        else {
            ActionNode action = parseAction(s);
            require(";", "Missing semicolon", s);
            return new StatementNode(action);
        }
    }

    ConditionNode parseCond(Scanner s) {
        String relop = require("lt|gt|eq", "Invalid operator", s);
        require(OPENPAREN, "Missing '('", s);
        IntNode expr1 = parseExpression(s);
        require(",", "Missing ','", s);
        IntNode expr2 = parseExpression(s);
        require(CLOSEPAREN, "Missing ')'", s);
        return new ConditionNode(relop, expr1, expr2);
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
        if ((action.equals("move")||action.equals("wait"))&&checkFor(OPENPAREN, s)) {
            IntNode expr = parseExpression(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new ActionNode(action, expr);
        }
        return new ActionNode(action);
    }

    IntNode parseExpression(Scanner s) {
        if (s.hasNext(NUMPAT)) {
            return new NumberNode(s.nextInt());
        }
        else if (s.hasNext("fuelLeft|oppLR|oppFB|numBarrels|barrelLR|barrelFB|wallDist")) {
            return new SensorNode(s.next());
        }
        else {
            String op = require("add|sub|mul|div", "Invalid operation", s);
            require(OPENPAREN, "Missing '('", s);
            IntNode expr1 = parseExpression(s);
            require(",", "Missing ','", s);
            IntNode expr2 = parseExpression(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new OperationNode(expr1, expr2, op);
        }
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
    IntNode amount = null;
    ActionNode(String type) {actionType = type;}
    ActionNode(String type, IntNode amt) {actionType = type; amount = amt;}

    @Override
    public void execute(Robot robot) {
        int num = (amount != null) ? amount.evaluate(robot) : 1;
        switch(actionType){
            case "turnL" -> robot.turnLeft();
            case "turnR" -> robot.turnRight();
            case "turnAround" -> robot.turnAround();
            case "shieldOn" -> robot.setShield(true);
            case "shieldOff" -> robot.setShield(false);
            case "takeFuel" -> robot.takeFuel();
            case "move" -> {
                for (int i = 0; i < num; i++) {
                    robot.move();
                }
            }
            case "wait" -> {
                for (int i = 0; i < num; i++) {
                    robot.idleWait();
                }
            }
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
    BlockNode ifBlock;
    BlockNode elseBlock = null;
    ConditionNode cond;

    IfNode(BlockNode block, ConditionNode cond) {
        this.ifBlock = block;
        this.cond = cond;
    }

    IfNode(BlockNode ifBlock, ConditionNode cond, BlockNode elseBlock) {
        this.ifBlock = ifBlock;
        this.cond = cond;
        this.elseBlock = elseBlock;
    }

    @Override
    public void execute(Robot robot) {
        if (cond.evaluate(robot)) {
            ifBlock.execute(robot);
        } else {
            if (elseBlock != null) {
                elseBlock.execute(robot);
            }
        }
    }

    public String toString() {
        String toReturn = "if(" + cond.toString() + ")" + this.ifBlock.toString();
        if (elseBlock != null) {
            toReturn += "else" + this.elseBlock.toString();
        }
        return toReturn;
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
    IntNode expr1;
    IntNode expr2;

    ConditionNode(String relop, IntNode expr1, IntNode expr2) {
        this.relop = relop;
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean evaluate(Robot robot) {
        return switch (relop) {
            case "lt" -> expr1.evaluate(robot) < expr2.evaluate(robot);
            case "gt" -> expr1.evaluate(robot) > expr2.evaluate(robot);
            case "eq" -> expr1.evaluate(robot) == expr2.evaluate(robot);
            default -> throw new ParserFailureException("Invalid operator"); // this should never run
        };
    }

    public String toString() {
        return relop+"("+expr1.toString()+", "+expr2.toString()+")";
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

class NumberNode implements IntNode {
    int num;
    NumberNode(int num) {this.num = num;}
    @Override
    public int evaluate(Robot robot) {
        return num;
    }
}

class OperationNode implements IntNode {
    IntNode expr1;
    IntNode expr2;
    String operation;

    OperationNode(IntNode expr1, IntNode expr2, String op) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.operation = op;
    }


    @Override
    public int evaluate(Robot robot) {
        return switch(operation) {
            case "add" -> expr1.evaluate(robot)+expr2.evaluate(robot);
            case "sub" -> expr1.evaluate(robot)-expr2.evaluate(robot);
            case "mul" -> expr1.evaluate(robot)*expr2.evaluate(robot);
            case "div" -> expr1.evaluate(robot)/expr2.evaluate(robot);
            default -> throw new ParserFailureException("Invalid operation"); // this should never run
        };
    }
}