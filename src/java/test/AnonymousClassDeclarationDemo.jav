    /**
     * Versioning types 
     */
    static enum VersioningType {
        
        none( null ),
        
        numeric( new Comparator() {       
            @Override
            public int compare(Object o1, Object o2) {
                Double d1= Double.parseDouble((String)o1);
                Double d2= Double.parseDouble((String)o2);
                return d1.compareTo(d2);
            }
        } ),

        alphanumeric(new Comparator() {   
            @Override
            public int compare(Object o1, Object o2) {
                return ((String)o1).compareTo((String)o2);
            }
        } );

        Comparator<String> comp;
        VersioningType( Comparator<String> comp ) {
            this.comp= comp;
        }
    };
