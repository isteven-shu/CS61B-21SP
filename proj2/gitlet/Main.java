package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Shuyuan Wang
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                validateNumArgs(args, 1);
                Repository.InitRepo();
                break;
            case "add":
                validateNumArgs(args, 2);
                Repository.AddFile(args[1]);
                break;
            case "commit":
                validateNumArgs(args, 2);
                Repository.NewCommit(args[1]);
                break;
            case "rm":
                validateNumArgs(args, 2);
                Repository.remove(args[1]);
                break;
            case "log":
                validateNumArgs(args, 1);
                Repository.log();
                break;
            case "global-log":
                validateNumArgs(args, 1);
                Repository.globalLog();
                break;
            case "status":
                validateNumArgs(args, 1);
                Repository.status();
                break;
            case "find":
                validateNumArgs(args, 2);
                Repository.find(args[1]);
                break;
            case "checkout":
                if (args.length == 2) {
                    Repository.checkoutBranch(args[1]);
                } else if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        throw new RuntimeException(String.format("Incorrect operands."));
                    }
                    Repository.checkoutFilefromHEAD(args[2]);
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        throw new RuntimeException(String.format("Incorrect operands."));
                    }
                    Repository.checkoutFilefromCommitID(args[1], args[3]);
                }
                break;
            case "branch":
                validateNumArgs(args, 2);
                Repository.newBranch(args[1]);
                break;
        }
    }

    /**
     * Checks the number of arguments versus the expected number,
     * throws a RuntimeException if they do not match.
     *
     * @param cmd Name of command you are validating
     * @param args Argument array from command line
     * @param n Number of expected arguments
     */
    public static void validateNumArgs(String[] args, int n) {
        if (args.length != n) {
            throw new RuntimeException(
                    String.format("Incorrect operands."));
        }
    }
}
