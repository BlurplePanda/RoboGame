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

    ProgramNode parseProg(Scanner s) {
        if (!s.hasNext()) {
            System.out.println("Provided file is empty, running default program.");
            return null;
        }
        List<ProgNode> statements = new ArrayList<>();
        while (s.hasNext()) {
            statements.add(parseStatement(s));
        }
        return new ProgramNode(statements);
    }

    ProgNode parseStatement(Scanner s) {
        if (checkFor("loop", s)) {
            return new LoopNode(parseBlock(s));
        } else if (checkFor("if", s)) {
            List<ConditionBlock> condPairs = new ArrayList<>();
            do {
                require(OPENPAREN, "Missing '('", s);
                BooleanNode cond = parseCond(s);
                require(CLOSEPAREN, "Missing ')'", s);
                BlockNode block = parseBlock(s);
                condPairs.add(new ConditionBlock(cond, block));
            } while (checkFor("elif", s));

            if (checkFor("else", s)) {
                BlockNode elseBlock = parseBlock(s);
                return new IfNode(condPairs, elseBlock);
            }
            return new IfNode(condPairs);
        } else if (checkFor("while", s)) {
            require(OPENPAREN, "Missing '('", s);
            BooleanNode cond = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new WhileNode(parseBlock(s), cond);
        } else if (s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
            String name = s.next();
            require("\\=", "Expected '='", s);
            IntNode value = parseExpression(s);
            require(";", "Missing semicolon", s);
            return new AssignNode(name, value);
        } else {
            ActionNode action = parseAction(s);
            require(";", "Missing semicolon", s);
            return action;
        }
    }

    BooleanNode parseCond(Scanner s) {
        if (checkFor("not", s)) {
            require(OPENPAREN, "Missing '('", s);
            BooleanNode cond = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new NotNode(cond);
        } else if (s.hasNext("and|or")) {
            String logOp = s.next();
            require(OPENPAREN, "Missing '('", s);
            BooleanNode cond1 = parseCond(s);
            require(",", "Missing ','", s);
            BooleanNode cond2 = parseCond(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return switch (logOp) {
                case "and" -> new AndNode(cond1, cond2);
                case "or" -> new OrNode(cond1, cond2);
                default -> throw new IllegalStateException("Invalid operator"); // this should never run
            };
        } else {
            String relop = require("lt|gt|eq", "Invalid operator", s);
            require(OPENPAREN, "Missing '('", s);
            IntNode expr1 = parseExpression(s);
            require(",", "Missing ','", s);
            IntNode expr2 = parseExpression(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new RelopNode(relop, expr1, expr2);
        }
    }

    BlockNode parseBlock(Scanner s) {
        indentLevel++;
        List<ProgNode> statements = new ArrayList<>();
        require(OPENBRACE, "Missing '{'", s);
        while (!checkFor(CLOSEBRACE, s)) {
            statements.add(parseStatement(s));
        }
        if (statements.isEmpty()) {
            fail("Empty loop", s);
        }
        BlockNode node = new BlockNode(statements, indentLevel);
        indentLevel--;
        return node;
    }

    ActionNode parseAction(Scanner s) {
        String action = require("move|turnL|turnR|turnAround|shieldOn|shieldOff|takeFuel|wait", "Invalid action", s);
        if ((action.equals("move") || action.equals("wait")) && checkFor(OPENPAREN, s)) {
            IntNode expr = parseExpression(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new ActionNode(action, expr);
        }
        return new ActionNode(action);
    }

    IntNode parseExpression(Scanner s) {
        if (s.hasNext(NUMPAT)) {
            return new NumberNode(s.nextInt());
        } else if (s.hasNext("fuelLeft|oppLR|oppFB|numBarrels|wallDist")) {
            return new SensorNode(s.next());
        } else if (s.hasNext("barrelLR|barrelFB")) {
            String sensor = s.next();
            if (checkFor(OPENPAREN, s)) {
                IntNode arg = parseExpression(s);
                require(CLOSEPAREN, "Expected ')'", s);
                return new SensorNode(sensor, arg);
            } else {
                return new SensorNode(sensor);
            }
        } else if (s.hasNext("\\$[A-Za-z][A-Za-z0-9]*")) {
            return new VariableNode(s.next());
        } else {
            String op = require("add|sub|mul|div", "Invalid operation", s);
            require(OPENPAREN, "Missing '('", s);
            IntNode expr1 = parseExpression(s);
            require(",", "Missing ','", s);
            IntNode expr2 = parseExpression(s);
            require(CLOSEPAREN, "Missing ')'", s);
            return new MathNode(expr1, expr2, op);
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
        if (s.hasNext(p)) {
            return s.next();
        }
        fail(message, s);
        return null;
    }

    static String require(Pattern p, String message, Scanner s) {
        if (s.hasNext(p)) {
            return s.next();
        }
        fail(message, s);
        return null;
    }

    /**
     * Requires that the next token matches a pattern (which should only match a
     * number) if it matches, it consumes and returns the token as an integer
     * if not, it throws an exception with an error message
     */
    static int requireInt(String p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {
            return s.nextInt();
        }
        fail(message, s);
        return -1;
    }

    static int requireInt(Pattern p, String message, Scanner s) {
        if (s.hasNext(p) && s.hasNextInt()) {
            return s.nextInt();
        }
        fail(message, s);
        return -1;
    }

    /**
     * Checks whether the next token in the scanner matches the specified
     * pattern, if so, consumes the token and return true. Otherwise returns
     * false without consuming anything.
     */
    static boolean checkFor(String p, Scanner s) {
        if (s.hasNext(p)) {
            s.next();
            return true;
        }
        return false;
    }

    static boolean checkFor(Pattern p, Scanner s) {
        if (s.hasNext(p)) {
            s.next();
            return true;
        }
        return false;
    }

}

// You could add the node classes here or as separate java files.
// (if added here, they must not be declared public or private)
// For example:
//  class BlockNode implements ProgNode {.....
//     with fields, a toString() method and an execute() method
//

/**
 * Interface for all nodes that return a (integer) number,
 * including sensors, math operations, etc.
 */

interface IntNode {
    int evaluate(Robot robot, VariableStorage vars);
}

/**
 * Interface for all nodes that return true or false,
 * including conditions (eg in if statements)
 */

interface BooleanNode {
    boolean evaluate(Robot robot, VariableStorage vars);
}

/**
 * Node representing the program
 * (The root node of the generated program tree)
 * Stores any statements in the program
 */
class ProgramNode {
    List<ProgNode> statements;

    ProgramNode(List<ProgNode> statements) {
        this.statements = statements;
    }
    
    public void execute(Robot robot) {
        VariableStorage vars = new VariableStorage();
        for (ProgNode statement : statements) {
            statement.execute(robot, vars);
        }
    }

    public String toString() {
        String toReturn = "";
        for (ProgNode statement : statements) {
            toReturn += statement.toString();
        }
        return toReturn.substring(0, toReturn.length() - 1);
    }
}

/**
 * Node representing a robot action
 * Stores the type of action, and the amount if it exists
 */
class ActionNode implements ProgNode {
    String actionType;
    IntNode amount = null;

    ActionNode(String type) {
        actionType = type;
    }

    ActionNode(String type, IntNode amt) {
        actionType = type;
        amount = amt;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        int num = (amount != null) ? amount.evaluate(robot, vars) : 1;
        switch (actionType) {
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
        return actionType + ";";
    }
}

/**
 * Node representing a loop
 * Stores the block of statements inside the loop
 * Executes them forever (until robot runs out of fuel)
 */
class LoopNode implements ProgNode {
    BlockNode block;

    LoopNode(BlockNode block) {
        this.block = block;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        while (true) {
            block.execute(robot, vars);
        }
    }

    public String toString() {
        return "loop" + this.block.toString();
    }
}

/**
 * Node representing a block (inside loop/if/while)
 * Stores any statements in the block
 * Also stores an indent level for pretty printing
 */
class BlockNode implements ProgNode {
    List<ProgNode> statements;
    int indent;

    BlockNode(List<ProgNode> statements, int indent) {
        this.statements = statements;
        this.indent = indent;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        for (ProgNode statement : statements) {
            statement.execute(robot, vars);
        }
    }

    public String toString() {
        String toReturn = "{\n";
        for (ProgNode statement : statements) {
            toReturn += "    ".repeat(indent) + statement.toString();
        }
        toReturn += "    ".repeat(indent - 1) + "}";
        return toReturn;
    }
}

/**
 * Node representing an if statement
 * Stores condition+block for if and any elifs
 * Stores optional else block
 */
class IfNode implements ProgNode {
    List<ConditionBlock> conditionPairs;
    BlockNode elseBlock = null;

    IfNode(List<ConditionBlock> conditionPairs) {
        this.conditionPairs = conditionPairs;
    }

    IfNode(List<ConditionBlock> conditionPairs, BlockNode elseBlock) {
        this.conditionPairs = conditionPairs;
        this.elseBlock = elseBlock;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        for (ConditionBlock pair : conditionPairs) {
            if (pair.cond.evaluate(robot, vars)) {
                pair.block.execute(robot, vars);
                return;
            }
        }
        if (elseBlock != null) {
            elseBlock.execute(robot, vars);
        }

    }

    public String toString() {
        ConditionBlock firstIf = conditionPairs.get(0);
        String toReturn = "if(" + firstIf.cond.toString() + ")" + firstIf.block.toString();
        if (conditionPairs.size() > 1) {
            for (ConditionBlock pair : conditionPairs) {
                toReturn += " elif(" + pair.cond.toString() + ")" + pair.block.toString();
            }
        }
        if (elseBlock != null) {
            toReturn += " else" + this.elseBlock.toString();
        }
        return toReturn;
    }
}

/**
 * Node representing a while loop
 * Stores the block in the loop, and condition for stopping
 */
class WhileNode implements ProgNode {
    BlockNode block;
    BooleanNode cond;

    WhileNode(BlockNode block, BooleanNode cond) {
        this.block = block;
        this.cond = cond;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        while (cond.evaluate(robot, vars)) {
            block.execute(robot, vars);
        }
    }

    public String toString() {
        return "while(" + cond.toString() + ")" + this.block.toString();
    }
}

/**
 * Node representing a logical "and" condition
 * Stores the two conditions to compare
 */
class AndNode implements BooleanNode {
    BooleanNode cond1;
    BooleanNode cond2;

    AndNode(BooleanNode cond1, BooleanNode cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    @Override
    public boolean evaluate(Robot robot, VariableStorage vars) {
        return cond1.evaluate(robot, vars) && cond2.evaluate(robot, vars);
    }

    public String toString() {
        return "and(" + cond1.toString() + ", " + cond2.toString() + ")";
    }
}

/**
 * Node representing a logical "or" condition
 * Stores the two conditions to compare
 */
class OrNode implements BooleanNode {
    BooleanNode cond1;
    BooleanNode cond2;

    OrNode(BooleanNode cond1, BooleanNode cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    @Override
    public boolean evaluate(Robot robot, VariableStorage vars) {
        return cond1.evaluate(robot, vars) || cond2.evaluate(robot, vars);
    }

    public String toString() {
        return "or(" + cond1.toString() + ", " + cond2.toString() + ")";
    }
}

/**
 * Node representing a logical "not" condition
 * Stores the condition to negate
 */
class NotNode implements BooleanNode {
    BooleanNode cond;

    NotNode(BooleanNode cond) {
        this.cond = cond;
    }

    @Override
    public boolean evaluate(Robot robot, VariableStorage vars) {
        return !cond.evaluate(robot, vars);
    }

    public String toString() {
        return "not(" + cond + ")";
    }
}

/**
 * Node representing a relative operation
 * lt (less than), gt (greater than), or eq (equal to)
 * Stores type of operation, and the two expressions to compare
 */
class RelopNode implements BooleanNode {
    String relop;
    IntNode expr1;
    IntNode expr2;

    RelopNode(String relOp, IntNode expr1, IntNode expr2) {
        this.relop = relOp;
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public boolean evaluate(Robot robot, VariableStorage vars) {
        return switch (relop) {
            case "lt" -> expr1.evaluate(robot, vars) < expr2.evaluate(robot, vars);
            case "gt" -> expr1.evaluate(robot, vars) > expr2.evaluate(robot, vars);
            case "eq" -> expr1.evaluate(robot, vars) == expr2.evaluate(robot, vars);
            default -> throw new IllegalStateException("Invalid relative operator"); // this should never run
        };
    }

    public String toString() {
        return relop + "(" + expr1.toString() + ", " + expr2.toString() + ")";
    }
}

/**
 * Node representing a robot sensor
 * Stores the name of the sensor and optional amount
 */
class SensorNode implements IntNode {
    String sensor;
    IntNode amount = null;

    SensorNode(String sensor) {
        this.sensor = sensor;
    }

    SensorNode(String sensor, IntNode amt) {
        this.sensor = sensor;
        this.amount = amt;
    }

    @Override
    public int evaluate(Robot robot, VariableStorage vars) {
        return switch (sensor) {
            case "fuelLeft" -> robot.getFuel();
            case "oppLR" -> robot.getOpponentLR();
            case "oppFB" -> robot.getOpponentFB();
            case "numBarrels" -> robot.numBarrels();
            case "barrelLR" -> amount == null ? robot.getClosestBarrelLR() : robot.getBarrelLR(amount.evaluate(robot, vars));
            case "barrelFB" -> amount == null ? robot.getClosestBarrelFB() : robot.getBarrelFB(amount.evaluate(robot, vars));
            case "wallDist" -> robot.getDistanceToWall();
            default -> throw new IllegalStateException("Invalid sensor"); // this should never run
        };
    }

    public String toString() {
        return sensor;
    }
}

/**
 * Node representing a (integer) number
 * Stores the number
 */
class NumberNode implements IntNode {
    int num;

    NumberNode(int num) {
        this.num = num;
    }

    @Override
    public int evaluate(Robot robot, VariableStorage vars) {
        return num;
    }

    public String toString() {
        return "" + num;
    }
}

/**
 * Node representing a basic mathematical operation
 * ie addition, subtraction, multiplication, division
 * Stores type of operation, and the two operands
 */
class MathNode implements IntNode {
    IntNode expr1;
    IntNode expr2;
    String operation;

    MathNode(IntNode expr1, IntNode expr2, String op) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.operation = op;
    }


    @Override
    public int evaluate(Robot robot, VariableStorage vars) {
        return switch (operation) {
            case "add" -> expr1.evaluate(robot, vars) + expr2.evaluate(robot, vars);
            case "sub" -> expr1.evaluate(robot, vars) - expr2.evaluate(robot, vars);
            case "mul" -> expr1.evaluate(robot, vars) * expr2.evaluate(robot, vars);
            case "div" -> expr1.evaluate(robot, vars) / expr2.evaluate(robot, vars);
            default -> throw new IllegalStateException("Invalid operation"); // this should never run
        };
    }

    public String toString() {
        return operation + "(" + expr1.toString() + ", " + expr2.toString() + ")";
    }
}

/**
 * Represents the pair of a condition and block
 * Useful for repetition for if/elif statements
 */
class ConditionBlock {
    BlockNode block;
    BooleanNode cond;

    ConditionBlock(BooleanNode cond, BlockNode block) {
        this.block = block;
        this.cond = cond;
    }
}

/**
 * Node representing a user-defined variable
 * Stores the name and integer value of the variable
 */
class VariableNode implements IntNode {
    String name;
    VariableStorage vars;

    VariableNode(String name) {
        this.name = name;
    }

    @Override
    public int evaluate(Robot robot, VariableStorage vars) {
        return vars.getVar(name);
    }

    public String toString() {
        return name;
    }
}

/**
 * Node representing a variable assignment statement
 * Stores the name and value of the variable
 * Also stores the instance of VariableStorage to add the variable to
 */
class AssignNode implements ProgNode {
    String name;
    IntNode value;

    AssignNode(String name, IntNode value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void execute(Robot robot, VariableStorage vars) {
        vars.setVar(name, value.evaluate(robot, vars));
    }

    public String toString() {
        return name + " = " + value + ";";
    }
}

/**
 * Stores variables allowing access from other classes
 * without using static as that can cause issues with multiple robots
 */
class VariableStorage {
    public Map<String, Integer> variables;

    VariableStorage() {
        variables = new HashMap<>();
    }

    public int getVar(String name) {
        return variables.computeIfAbsent(name, k->0);
    }

    public void setVar(String name, int value) {
        variables.put(name, value);
    }
}