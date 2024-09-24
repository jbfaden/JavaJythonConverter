# JavaJythonConverter

Converter from Java to Jython (and close to Python)

This was all motivated by finally finding an ideal AST for Java, so I can create a more exact
translation.  (Autoplot's Java to Python converter was basically a bunch of regex checks, and
this is already a huge improvement.)  Also we'd like to convert TimeUtil.java and URITemplates.java
into Python for use in HAPI.

See https://cottagesystems.com/JavaJythonConverter/, where this is accessible.

# Notes on use
Do not expect for this to completely translate the code.  This is probably an impossible goal,
and the intent is that this will reduce the work needed to be done.  This should remove the 
tedium so that you can focus on the parts that really need human judgement.

Also, this may introduce subtle bugs for new use cases.  This was written for a Java code which
worked with integer arrays, Strings, and Characters, and has somewhat dodgy code to figure out
the Python translation.  This might introduce bugs in the new code, and this should only be used
when a thorough set of unit tests have been written already.  (This will convert the unit tests
and well and this must be done unless the result is reviewed line-by-line.)

# Mappings
| name|  Java  	| Jython  	| Notes |
|---	|---	|---	|--- |
| string function | .toUpperCase() | .upper() | many other string functions translate |
| format  	| String.format(a,...)  	| a.format(...)  	| |
|  stringbuilder 	|  sb.append() 	| sb+= | just use strings, sources say this is no slower 	|
| length | x.length | len(x) | The type of the object could be considered |
| regex | Pattern/Matcher | re | some patterns recognized and converted |
