package test;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jbf
 */
public class LoggerDemo {
    public static Logger logger2= Logger.getLogger("x");
    public static void main( String[] args ) {
        Logger logger= Logger.getLogger("x");
        logger.fine( "fine message" );
        logger.log(Level.FINE, "fine message {0}", 0);
        logger.log(Level.SEVERE,null,new Exception());
        logger.log(Level.FINER,"lsd is now {0}, width={1}", new Object[] { -1,-1 } );
        logger2.fine( "fine message" );
        logger2.log(Level.FINE, "fine message {0}", 0);
        logger2.log(Level.SEVERE,null,new Exception());
        logger2.log(Level.FINER,"lsd is now {0}, width={1}", new Object[] { -1,-1 } );
    }
}
