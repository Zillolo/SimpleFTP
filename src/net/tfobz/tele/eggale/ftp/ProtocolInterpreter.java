/**
 * 
 */
package net.tfobz.tele.eggale.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author alex
 * 
 *         An Interpreter that in combination with the 'Command'-class gives the
 *         user a method to obtain which command has been received and what its
 *         arguments were. A 'Reply' can be used to send pre-formulated
 *         messages.
 */
public abstract class ProtocolInterpreter {

    /**
     * A Reader that reads from a user-specified InputStream.
     */
    protected BufferedReader input = new BufferedReader(new InputStreamReader(
                    System.in));

    /**
     * A Printer that outputs to a user-specified OutputStream.
     */
    protected PrintWriter output = new PrintWriter(System.out);

    /**
     * The argument of the last received command. Is empty in case of missing
     * argument or obstructed command.
     */
    protected String argument = "";

    /**
     * Creates a ProtocolInterpreter with the given InputStream and
     * OutputStream.
     * 
     * @param input
     *            The InputStream this Interpreter makes use of.
     * @param output
     *            The OutputStream this Interpreter makes use of.
     */
    public ProtocolInterpreter(InputStream input, OutputStream output) {
        setInput(input);
        setOutput(output);
    }

    /**
     * select() returns the type of command sent by the client. The argument
     * must be read using the getArgument()-function.
     * 
     * This function is blocking. Calling it and not receiving data will result
     * in a lock.
     * 
     * @return The last received command.
     * @throws IOException
     *             In case the communication with the InputStream fails.
     */
    public abstract Command select() throws IOException;

    /**
     * Replies using a preformulated message for a specific reply.
     * 
     * @param reply
     *            The reply to send.
     */
    public abstract void reply(Reply reply);

    /**
     * Replies with a user-specified message and reply code.
     * 
     * @param code
     *            The reply code of the message.
     * @param message
     *            The message to send.
     */
    public void reply(int code, String message) {
        output.print(code + " " + message + "\r\n");
        output.flush();
    }

    /**
     * Returns the argument of the last command. Once read the argument will be
     * cleared.
     * 
     * @return The argument of the last received command.
     */
    public String getArgument() {
        String temp = this.argument;
        this.argument = "";
        return temp;
    }

    public void setInput(InputStream input) {
        if (input != null) {
            this.input = new BufferedReader(new InputStreamReader(input));
        }
    }

    public void setOutput(OutputStream outputStream) {
        if (outputStream != null) {
            this.output = new PrintWriter(outputStream);
        }
    }
}
