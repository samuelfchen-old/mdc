package mdc;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.stream.Collectors;

public class DelimitedSection {
    // Changed to protected 
    protected static final char[] DEFAULT_DELIMITER_CHARS = { '|', ';', ',', '^', ':', '~', '$', '&', '#', '=', '?', '"', '\'', '@', '_' };
    protected final char DEFAULT_ESCAPE_CHAR = '\\';

    public char[] Delimiters = DEFAULT_DELIMITER_CHARS;
    public char EscapeChar = DEFAULT_ESCAPE_CHAR;

    public List<DelimitedSection> ChildSections = new ArrayList<DelimitedSection>();
    private String _value = "";

    // public String Value;

    private static boolean isNullOrEmpty(String param) { 
        return param == null || param.trim().length() == 0;
    }

    public String getValue() {
        while (ChildSections.size() > 0 && isNullOrEmpty(this.ChildSections.get(this.ChildSections.size() - 1).getValue())) {
            this.ChildSections.remove(this.ChildSections.size() - 1);
        }

        return this.ChildSections.size() == 0 ? _value 
            : String.join(String.valueOf(Delimiters[0]), this.ChildSections.stream()
                                                            .map(s -> s.getValue())
                                                            .toArray(String[]::new)
                                                            // .collect(Collectors.toList());
            );
    }

    public void setValue(String value) {
        try {
            this.ChildSections = ParseChildSections(value, Delimiters, EscapeChar);

            if (this.ChildSections.size() == 0) {
                this._value = value != null ? value : "";
            } else {
                this._value = "";
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    protected DelimitedSection() { }

    public DelimitedSection(char[] delimiters, char escapeChar) {
        this.Delimiters = delimiters;
        this.EscapeChar = escapeChar;
    }

    public DelimitedSection(String value, char[] delimiters, char escapeChar) {
        this(delimiters, escapeChar);

        this.setValue(value);
    }

    public DelimitedSection get(int i) throws Exception {
        if (this.Delimiters == null || this.Delimiters.length == 0) {
            throw new Exception("No delimiter char defined at this level. Child-section dimension accessing invalid.");
        }

        // System.out.println(_value);
        // System.out.println(String.valueOf(i));


        if (!isNullOrEmpty(_value)) {
            this.ChildSections = ParseChildSections(_value, this.Delimiters, this.EscapeChar);
            this._value = "";
            // System.out.println("Called");
        }

        while (ChildSections.size() <= i) {
            ChildSections.add(MakeChildSection("", this.Delimiters, this.EscapeChar));
            // System.out.println("Child added");
        }

        return ChildSections.get(i);
    }

    private static DelimitedSection MakeChildSection(String value, char[] parentDelimiters, char parentEscapeChar) throws Exception {
        if (parentDelimiters == null || parentDelimiters.length == 0) {
            throw new Exception("No delimiter char defined. Child-section dimension accessing invalid.");
        } else {
            char[] delimiters = new char[parentDelimiters.length - 1];
            int i = delimiters.length;

            while (--i >= 0) { delimiters[i] = parentDelimiters[i + 1]; }

            // System.arraycopy(parentDelimiters, 1, delimiters, 0, delimiters.length);

            // System.out.println(Arrays.toString(delimiters));

            return new DelimitedSection(value, delimiters, parentEscapeChar);
        }
    }

    protected static List<DelimitedSection> ParseChildSections(String parentValue, char[] delimiters, char escapeChar) throws Exception { // not sure what equiv of internal is for java
        if (delimiters == null) {
            throw new Exception();
        }

        
        List<DelimitedSection> result = new ArrayList<DelimitedSection>();
        if (delimiters.length > 0) { 
            ParseChildSectionValues(parentValue, delimiters[0], escapeChar).forEach(
                (value) -> {
                    try {
                        result.add(MakeChildSection(value, delimiters, escapeChar));
                    } catch (Exception e) { e.printStackTrace(); }
                }
            );
        }

        return result;
    }

    private static List<String> ParseChildSectionValues(String parentValue, char childDelimiter, char escapeChar) {
        List<String> result = new ArrayList<String>();
        boolean escaping = false;
        int childSectionStartIndex = 0;
        char[] valueCharArray = parentValue.toCharArray();

        for (int currCharIndex = childSectionStartIndex; currCharIndex < valueCharArray.length; currCharIndex++) {
            if (valueCharArray[currCharIndex] == escapeChar) {
                escaping = !escaping;
            } else {
                if (!escaping && valueCharArray[currCharIndex] == childDelimiter) {
                    int sectionLength = currCharIndex - childSectionStartIndex;
                    String sectionValue = new String(valueCharArray, childSectionStartIndex, sectionLength);

                    result.add(sectionValue);

                    childSectionStartIndex = currCharIndex + 1;
                }

                escaping = false;
            }
        }

        if (valueCharArray.length > childSectionStartIndex) {
            String lastSectionValue = 
                new String(valueCharArray, childSectionStartIndex, valueCharArray.length - childSectionStartIndex);
            result.add(lastSectionValue);
        }

        return result;
    }

    public boolean charArrContains(char[] arr, char c) {
        boolean b = false;
        for (char d : arr) {
            if (c == d) {
                b = true;
                break;
            }
        }
        return b;
    }

    public String Escape(String sectionValue) {
        StringBuilder escaped = new StringBuilder();

        // For contains

        for (char c : sectionValue.toCharArray()) {
            if (charArrContains(this.Delimiters, c) || this.EscapeChar == c) {
                escaped.append(EscapeChar);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    public String UnEscape(String escapedSectionValue) {
        StringBuilder buffer = new StringBuilder();

        if (!isNullOrEmpty(escapedSectionValue)) {
            char[] valueCharArray = escapedSectionValue.toCharArray();
            for (int i = 0; i < valueCharArray.length; i++) {
                char curr = valueCharArray[i];
                if (curr == EscapeChar && i< valueCharArray.length - 1) { 
                    char next = valueCharArray[i + 1];
                    
                    if (charArrContains(this.Delimiters, next) || next == EscapeChar) {
                        continue;
                    }
                }

                buffer.append(curr);
            }
        }

        return buffer.toString();
    }

    public DelimitedSection GetSection(int[] sectionIndexAddress) {
        try {
            if (sectionIndexAddress == null || sectionIndexAddress.length == 0) {
                return this;
            } else if (Delimiters.length > 0) {
                int[] nextLevelSectionIndexAddress = new int[sectionIndexAddress.length - 1];
                
                System.arraycopy(sectionIndexAddress, 1, nextLevelSectionIndexAddress, 0, nextLevelSectionIndexAddress.length );
    
                DelimitedSection ret = this.get(sectionIndexAddress[0]).GetSection(nextLevelSectionIndexAddress);
                // System.out.println("Value: " + ret.toString());
                // System.out.println("Old: " + Arrays.toString(sectionIndexAddress));
                // System.out.println("New: " + Arrays.toString(nextLevelSectionIndexAddress));
                return  ret; //what? also missing null checking thing
            } else {
                // System.out.println("WACK");
                return null;
            }
        } catch (Exception e) { e.printStackTrace(); return null; }
        // return null;
    }

    @Override
    public String toString() {
        return getValue();
    }
}