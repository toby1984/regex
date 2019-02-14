This *might* become a lexer generator but right now it just is a regex -> NFA -> DFA converter.
It uses Thompson construction to create a NFA and then uses subset construction to turn it into a DFA.

The following regular expression characters are recognized:

|   union
()  grouping
+   one or more
*   kleene operator
?   none or once
.   any character
[1-3] character class (shorthand for (1|2|3)]
