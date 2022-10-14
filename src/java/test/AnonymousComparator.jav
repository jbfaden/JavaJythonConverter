// special behavior is removed
Comparator c= new Comparator() {       
    public int compare(Object o1, Object o2) {
        Double d1= Double.parseDouble((String)o1);
        Double d2= Double.parseDouble((String)o2);
        return d1.compareTo(d2);
    }
}; 
