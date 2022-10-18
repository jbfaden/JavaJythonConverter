
    class VersionComparator(Comparator):
         def compare( v1, v2 ):
             pass
 
    none= VersionComparator();
          
    numeric =  VersionComparator();
    def numeric_compare( v1,v2 ):
        d1= float((String)o1);
        d2= float((String)o2);
        return d1-d2;
    numeric.compare= numeric_compare
    
    def alphanumeric_compare( v1, v2 ):
        return ((String)o1).compareTo((String)o2);

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