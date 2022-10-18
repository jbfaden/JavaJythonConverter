
import java.util.Comparator;
// Python <3.0 has __cmp__ built-in
public class InterfaceTest {
    
    interface VersionComparator extends Comparator { };

    public static VersionComparator none= new VersionComparator() {
         public int compare( Object v1, Object v2 ) {
              return v1.hashCode()-v2.hashCode();
         } 
    };
          
    public static VersionComparator numeric =  new VersionComparator() {       
            @Override
            public int compare(Object o1, Object o2) {
                Double d1= Double.parseDouble((String)o1);
                Double d2= Double.parseDouble((String)o2);
                return d1.compareTo(d2);
            }
        };
    
    public static VersionComparator alphanumeric = new VersionComparator() {   
            @Override
            public int compare(Object o1, Object o2) {
                return ((String)o1).compareTo((String)o2);
            }
        };
    
    public static void main( String[] args ) {
        System.out.println( "alphaNumeric " + alphanumeric.compare( "abc","def" ) );
                
    }
} 