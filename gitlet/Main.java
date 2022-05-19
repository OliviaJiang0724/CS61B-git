package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Chuyue Jiang
 */
public class Main {


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        File gitlet = new File(".gitlet", "gitlet");
        Gitlet currentGitlet;

        if (args.length == 0) {
            exitWithError("Please enter a command.");
        } else {
            if (gitlet.exists()) {
                currentGitlet = Utils.readObject(gitlet, Gitlet.class);
            } else {
                currentGitlet  = new Gitlet();
            }
            currentGitlet.run(args);
            Utils.writeObject(gitlet, currentGitlet);
        }


    }


    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }


}
