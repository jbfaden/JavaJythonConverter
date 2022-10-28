
package test;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

public class InnerClass2 {
       
    String[] qualifiers;
    Map<String,String>[] qualifiersMaps;
    
    Map<String,FieldHandler> fieldHandlers;
    Map<String,FieldHandler> fieldHandlersById;

    
    /**
     * Interface to add custom handlers for strings with unique formats.  For 
     * example, the RPWS group had files with two-hex digits indicating the 
     * ten-minute interval covered by the file name. 
     */
    public interface FieldHandler {

        public String configure( Map<String,String> args );

        public String getRegex();

        public void parse( String fieldContent, 
                int[] startTime, 
                int[] timeWidth, 
                Map<String,String> extra ) throws ParseException;
        
        public String format( int[] startTime, 
                int[] timeWidth, 
                int length, 
                Map<String,String> extra ) throws IllegalArgumentException;

    }
    
    public static class SubsecFieldHandler implements FieldHandler {

        int places;
        int nanosecondsFactor;
        String format;
        
        @Override
        public String configure(Map<String, String> args) {
            places= Integer.parseInt( "9" );
            if ( places>9 ) throw new IllegalArgumentException("only nine places allowed.");
            nanosecondsFactor= (int)( Math.pow( 10, (9-places) ) ); 
            format= "%0"+places+"d";
            return null;
        }

        @Override
        public String getRegex() {
            StringBuilder b= new StringBuilder();
            for ( int i=0; i<places; i++ ) b.append("[0-9]");
            return b.toString();
        }

        @Override
        public void parse(String fieldContent, int[] startTime, int[] timeWidth, Map<String, String> extra) throws ParseException {
            double value= Double.parseDouble(fieldContent);
            startTime[6]= (int)( value*nanosecondsFactor );
            timeWidth[5]= 0;
            timeWidth[6]= nanosecondsFactor;
        }

        @Override
        public String format( int[] startTime, int[] timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            double nn= startTime[6] / nanosecondsFactor;
            return String.format( format, (int)Math.round(nn) ); 
        }
        
    }
    
    public static class HrintervalFieldHandler implements FieldHandler {

        Map<String,Integer> values;
        Map<Integer,String> revvalues;
        
        int mult; 
        
        @Override
        public String configure(Map<String, String> args) {
            String vs= "01,02,03,04";
            String[] values1= vs.split(",",-2);
            mult= 24 / values1.length;
            if ( 24 - mult*values1.length != 0 ) {
                throw new IllegalArgumentException("only 1,2,3,4,6,8 or 12 intervals");
            }
            values= new HashMap();
            revvalues= new HashMap();
            for ( int i=0; i<values1.length; i++ ) {
                values.put( values1[i], i );
                revvalues.put( i, values1[i] );
            }
            return null;
        }

        @Override
        public String getRegex() {
            Iterator<String> vv= values.keySet().iterator();            
            StringBuilder r= new StringBuilder(vv.next());
            while ( vv.hasNext() ) {
                r.append("|").append(vv.next());
            }
            return r.toString();
        }

        @Override
        public void parse(String fieldContent,  int[] startTime, int[] timeWidth, Map<String, String> extra) throws ParseException {
            Integer ii;
            if ( values.containsKey(fieldContent) ) {
                ii= values.get(fieldContent);
            } else {
                throw new ParseException( "expected one of "+getRegex(),0 );
            }
            int hour= mult * ii;
            startTime[3]= hour;
            timeWidth[3]= mult;
            timeWidth[0]= 0;
            timeWidth[1]= 0;
            timeWidth[2]= 0;
        }

        @Override
        public String format(  int[] startTime, int[] timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            Integer key= startTime[3]/mult;
            if ( revvalues.containsKey(key) ) {
                String v= revvalues.get(key);
                return v;
            } else {
                throw new IllegalArgumentException("unable to identify enum for hour "+startTime[3]);
            }
        }
        
    }
    
    
    public static class IgnoreFieldHandler implements FieldHandler {

        String regex;
        Pattern pattern;
        String name;
        
        @Override
        public String configure(Map<String, String> args) {
            regex= "[abc][abc]";
            if ( regex!=null ) {
                pattern= Pattern.compile(regex);
            }
            name= "unnamed";
            return null;
        }

        @Override
        public String getRegex() {
            return regex; // note this can be null (None).
        }

        @Override
        public void parse(String fieldContent, int[] startTime, int[] timeWidth, Map<String, String> extra) throws ParseException {
            if ( regex!=null ) {
                if ( !pattern.matcher(fieldContent).matches() ) {
                    throw new ParseException("ignore content doesn't match regex: "+fieldContent,0);
                }
            }
            if ( !name.equals("unnamed") ) {
                extra.put( name, fieldContent );
            }
        }

        @Override
        public String format( int[] startTime, int[] timeWidth, int length, Map<String, String> extra) throws IllegalArgumentException {
            return "";
        }
        
    }
    
    static enum VersioningType {
        
        none( null ),
        
        numeric( new Comparator<String>() {       // 4.10 > 4.01
            @Override
            public int compare(String s1, String s2) {
                Double d1= Double.parseDouble(s1);
                Double d2= Double.parseDouble(s2);
                return d1.compareTo(d2);
            }
        } ),

        alphanumeric(new Comparator<String>() {   // a001
            @Override
            public int compare(String s1, String s2) {
                return (s1.compareTo(s2));
            }
        } ),

        numericSplit( new Comparator<String>() {  // 4.3.23   // 1.1.3-01 for RBSP (rbspice lev-2 isrhelt)
           @Override
           public int compare(String s1, String s2) {
                String[] ss1= s1.split("[\\.-]",-2);
                String[] ss2= s2.split("[\\.-]",-2);
                int n= Math.min( ss1.length, ss2.length );
                for ( int i=0; i<n; i++ ) {
                    int d1= Integer.parseInt(ss1[i]);
                    int d2= Integer.parseInt(ss2[i]);
                    if ( d1<d2 ) {
                        return -1;
                    } else if ( d1>d2 ) {
                        return 1;
                    }
                }
                return ss1.length - ss2.length;  // the longer version wins (3.2.1 > 3.2)
            } 
        });

        Comparator<String> comp;
        VersioningType( Comparator<String> comp ) {
            this.comp= comp;
        }
    };


    /**
     * Version field handler.  Versions are codes with special sort orders.
     */
    public static class VersionFieldHandler implements FieldHandler {
        VersioningType versioningType;
                        
        @Override
        public String configure( Map<String,String> args ) {
            String sep= null;
            if ( sep==null && args.containsKey("dotnotation")) {
                sep= "T";
            }
            String alpha= null;
            if ( alpha==null && args.containsKey("alphanumeric") ) {
                alpha="T";
            }
            String type= args.get("type");
            if ( type!=null ) {
                if ( type.equals("sep") || type.equals("dotnotation") ) {
                    sep= "T";
                } else if (type.equals("alpha") || type.equals("alphanumeric") ) {
                    alpha="T"; 
                }
            }
            if ( alpha!=null ) {  
                if ( sep!=null ) {
                    return "alpha with split not supported";
                } else {
                    versioningType= VersioningType.alphanumeric;
                }
            } else {
                if ( sep!=null ) {
                    versioningType= VersioningType.numericSplit;
                } else {
                    versioningType= VersioningType.numeric;
                }
            }
            return null;
        }

        @Override
        public void parse( String fieldContent, int[] startTime, int[] timeWidth, Map<String,String> extra ) {
            String v= null;
            if ( v!=null ) {
                versioningType= VersioningType.numericSplit; 
                fieldContent= v+"."+fieldContent; // Support $v.$v.$v
            } 
            extra.put( "v", fieldContent );                    
        }

        @Override
        public String getRegex() {
            return ".*";
        }

        @Override
        public String format( int[] startTime, int[] timeWidth, int length, Map<String, String> extra ) {
            return null;
        }
    };
    
    public InnerClass2() throws ParseException {
                
        this.fieldHandlers= new HashMap<>();
        
        this.fieldHandlers.put("subsec",new SubsecFieldHandler());
        this.fieldHandlers.put("hrinterval",new HrintervalFieldHandler());
        this.fieldHandlers.put("x",new IgnoreFieldHandler());
        this.fieldHandlers.put("v",new VersionFieldHandler());

        FieldHandler fh1 = this.fieldHandlers.get("hrinterval");
        fh1.configure(null);
        
        int[] startTime= new int[7];
        int[] timeWidth= new int[7];
        Map<String,String> extra= new HashMap<>();
        
        fh1.parse("03", startTime, timeWidth, extra );
        System.err.println( Arrays.toString(timeWidth) );
        
        FieldHandler fh2 = this.fieldHandlers.get("v");
        fh2.configure(Collections.singletonMap("type","alpha"));
        fh2.parse("a2", startTime, timeWidth, extra );
        
        System.err.println( extra.get("v") );
        

    }
    
    public static void main( String[] args ) throws ParseException {
        new InnerClass2();
    }
        
}
