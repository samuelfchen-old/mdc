package mdc;

public class Parser {
    private String[] delims;
    private String escape;

    public Parser() {}

    public Node parseTree(String data) {
        // Split the string into the header and body 
        String firstDelim = String.valueOf(data.charAt(0));
        String[] parts = data.split("\\" + firstDelim, 3); // escape character added for regex

        String header = firstDelim + parts[1];
        String body = parts[2];

        // String[] e = body.split("\\" + firstDelim);

        // for (String s : e) { System.out.println(s); }

        escape = String.valueOf(header.charAt(header.length() - 1));
        delims = new String[header.length() - 1];

        for (int i = 0; i < header.length() - 1; i++) {
            delims[i] = String.valueOf(header.charAt(i));
        }

        // for (String s : delims) { System.out.println(s); }

        // System.out.println("Escape " + escape);

        return null;
    }
}