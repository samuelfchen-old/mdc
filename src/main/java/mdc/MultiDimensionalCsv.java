package mdc;

import java.util.List; 
import java.util.ArrayList; 

public class MultiDimensionalCsv extends DelimitedSection {
    public MultiDimensionalCsv(int maxIndexLevel) throws Exception {
        if (maxIndexLevel > DEFAULT_DELIMITER_CHARS.length) {
            throw new Exception("Header section level is max'd at " + DEFAULT_DELIMITER_CHARS.length + ".");
        } else if (maxIndexLevel < 1) {
            throw new Exception("Specified index-level " + String.valueOf(maxIndexLevel) + " must greater than 0.");
        } else {
            Delimiters = new char[maxIndexLevel];
            for (int i = 0; i < maxIndexLevel; i++) {
                Delimiters[i] = DEFAULT_DELIMITER_CHARS[i];
            }

            EscapeChar = DEFAULT_ESCAPE_CHAR;
        }
    }

    public static MultiDimensionalCsv Parse(String encodedString) {
        try {
            return new MultiDimensionalCsv(encodedString);
        } catch (Exception e) { e.printStackTrace(); return null; } 
    }

    public MultiDimensionalCsv(String value) throws Exception {
        if (value.length() < 3) {
            throw new Exception("Invalid Header string - Multi-Dimensional Csv requires minimumn 3 chars in the delimiters-header section.");
        } else {
            char[] valueCharArray = value.toCharArray();
            ThrowIfNotInRange(valueCharArray[0]);
            ThrowIfNotInRange(valueCharArray[1]);
            if (valueCharArray[1] == valueCharArray[0]) {
                throw new Exception("Mandatory Escape-char not defined.");
            }

            for (int i = 2; i < valueCharArray.length; i++) {
                char c = valueCharArray[i];

                ThrowIfNotInRange(c);

                for (int n = i - 1; n >= 0; n--) {
                    if (c == valueCharArray[n]) {
                        if (n == 0) {
                            Delimiters = new char[i - 1];
                            System.arraycopy(valueCharArray, 0, Delimiters, 0, i - 1);

                            EscapeChar = valueCharArray[i - 1];

                            ChildSections = ParseChildSections(value.substring(i + 1), this.Delimiters, this.EscapeChar);

                            return;
                        } else {
                            throw new Exception("Delimiter " + String.valueOf(c) + " is repeated in header section.");
                        }
                    }
                }
            }

            throw new Exception("Delimiters-section's ending char (a repeat of the primary-delimiter) not found.");
        }
    }

    private void ThrowIfNotInRange(char v) throws Exception {
        if (((v >= 33 && v <= 47) || (v >= 58 && v <= 64) || (v >= 91 && v <= 96) || (v >= 123 && v <= 126)) == false) {
            throw new Exception("Delimiter '" + String.valueOf(v) +"' is invalid - a delimiter must be printable, non-white-space and non-alpha-numeric.");
        }
    }

    @Override
    public String toString() {
        String header = new String(this.Delimiters) + String.valueOf(EscapeChar) + String.valueOf(this.Delimiters[0]);

        return header + this.getValue();
    }

    public void SetValue(int [] indexAtEachLevel, String sectionNewValue) {
        DelimitedSection section = GetSection(indexAtEachLevel);
        if (section != null) { section.setValue(sectionNewValue); }
    }

    public String GetValue(int[] indexAtEachLevel) {
        return GetSection(indexAtEachLevel).getValue(); // some random null checking stuff... confusing
    }

    public String[] GetChildValues(int[] indexesToParentSection) {
        List<String> result = new ArrayList<String>();
        DelimitedSection parent = GetSection(indexesToParentSection);
        if (parent != null) {
            if (parent.ChildSections.size() == 0) {
                result.add(parent.getValue());
            } else {
                parent.ChildSections.forEach(
                    (child) -> {
                        result.add(child.getValue() != null ? child.getValue() : "");
                    }
                );
            }
        }

        return result.toArray(new String[0]);
    }
}