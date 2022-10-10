
StringBuilder sb= new StringBuilder("P");
sb.append("T");
sb.append(String.format("%.3f",seconds + nanoseconds/1e9) ).append("S");
System.out.println(sb.toString());