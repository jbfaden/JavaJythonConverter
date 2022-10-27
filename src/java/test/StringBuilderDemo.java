package test;

public class StringBuilderDemo {
    public static void main( String[] args ) {
        int seconds=12;
        int nanoseconds= 123456789;
        StringBuilder sb= new StringBuilder("P");
        sb.append("T");
        sb.append(String.format("%.3f",seconds + nanoseconds/1e9) ).append("S");
        System.out.println(sb.toString());
        
        sb= new StringBuilder("Hi Thee");
        sb.insert(6,"r");
        System.out.println(sb.toString());
        
        sb= new StringBuilder(" 12345 ");
        sb.insert( 4, "-");
        String s=  sb.toString().trim();        
    }
}