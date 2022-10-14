"abc".equals("def")

/* -------------------------------------- */

if (stopTime.length() < 10 || Character.isDigit(stopTime.charAt(10))) {
            throw new IllegalArgumentException("arguments must be $Y-$m-$dZ");
        }

/* -------------------------------------- */

d -= 4;

/* -------------------------------------- */

while ( offs<timeString.length() && Character.isWhitespace( timeString.charAt(offs) ) ) offs++;

/* -------------------------------------- */

try ( BufferedReader r= new BufferedReader( new InputStreamReader(System.in) ) ) {
    filen1= r.readLine();
    while ( filen1!=null ) {
        URITemplate ut= new URITemplate(template);
        int[] itimeRange= ut.parse( filen1, argsm );
        System.out.print( TimeUtil.isoTimeFromArray( Arrays.copyOfRange( itimeRange, 0, 7 ) ) );
        System.out.print( "/" );
        System.out.println( TimeUtil.isoTimeFromArray( Arrays.copyOfRange( itimeRange, 7, 14 ) ) );                            
        filen1= r.readLine();
    }

} catch ( IOException ex ) {

} catch ( ParseException ex ) {
    printUsage();
    System.err.println("parseException from "+filen1);
    System.exit(-3);
}

/* -------------------------------------- */

    /**
     * regular intervals are numbered:
     * $(periodic;offset=0;start=2000-001;period=P1D) "0" &rarr; "2000-001"
     */
    public static class PeriodicFieldHandler implements FieldHandler {
    }

/* -------------------------------------- */