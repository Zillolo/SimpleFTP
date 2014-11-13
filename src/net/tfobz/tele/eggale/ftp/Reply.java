/**
 * 
 */
package net.tfobz.tele.eggale.ftp;


/**
 * @author alex
 *
 */
public enum Reply {
    FILE_ACTION_OK(150),
    COMMAND_OK(200),
    COMMAND_NOT_IMPLEMENTED(202),
    GREETING(220),
    CLOSING(221),
    FILE_ACTION_COMPLETE(226),
    PASSIVE_MODE(227),
    USER_LOGGED_IN(230),
    USER_OK_PASSWORD_NEEDED(331),
    NEED_ACCOUNT(332),
    LOCAL_PROCESSING_ERROR(451),
    ILLEGAL_COMMAND(500),
    SYNTAX_ERROR(501),
    BAD_SEQUENCE(503),
    COMMAND_WRONG_PARAM(504),
    LOGIN_INCORRECT(530),
    FILE_NOT_FOUND(550);
    
    private Reply(int code) {
    }                    
}
