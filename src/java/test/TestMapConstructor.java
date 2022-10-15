
package test;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo bug seen when converting constructor with map.
 * @author jbf
 */
public class TestMapConstructor {
 
    Map<String,Integer> fh;
    
    public TestMapConstructor( String formatString ) {
                
        this.fh= new HashMap<>();
        
        this.fh.put("subsec",0); // converts to fh.put
        this.fh.put("hrinterval",0);
                 
        String[] ss = formatString.split("\\$");
        
        StringBuilder regex1 = new StringBuilder(100);
        regex1.append(ss[0].replaceAll("\\+","\\\\+"));
        
    }
    
    public static void main( String[] args ) {
        new TestMapConstructor("$Y$m$d.dat");
    }
}
