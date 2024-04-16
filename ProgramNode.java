/**
 * Interface for all nodes that can be executed,
 * including the top level program node
 */

interface ProgNode {
    public void execute(Robot robot, VariableStorage vars);
}
