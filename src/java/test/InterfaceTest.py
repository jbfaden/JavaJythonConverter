class VersionComparator:
    def compare( self, v1, v2 ):
        pass

none= VersionComparator()
  
numeric =  VersionComparator()
def numeric_compare( v1,v2 ):
    d1= float(v1);
    d2= float(v2);
    return d1-d2;
numeric.compare= numeric_compare

def alphanumeric_compare( v1, v2 ):
    return v1.compareTo(v2);

alphanumeric = VersionComparator()
alphanumeric.compare= alphanumeric_compare
   
def main( args ):
    print( "alphaNumeric " + alphanumeric.compare( "abc","def" ) )
 
main([])
