using System;
using System.Collections.Generic;
using System.Linq;

namespace FolddaConnect.Codec
{
    /* UDC - Universal Data Container */
    /*
     * Multi-Dimemsional CSV (MD-CSV) is a delimiter-based string-encoding scheme for storing multi-dimensional string values in an encoded string. The scheme utilises multiple 
     * delimiter charactors - one for separating values at each level of the dimentions, and these delimiters are themselves embeded in the beginning of encoded string, allowing a parser
     * to dynamically decode the string without any other extra input. Each stored value is accessible through an unique multi-dimenstional index (integer) array which, through
     * an API, can be added, updated, or deleted.
     * 
     * MD-CSV is the default header encoder for Universal Data Container
     */

    public class MultiDimensionalCsv : DelimitedSection
    {
        //construct a default encoding-section using the default delimitors and escape-char
        public MultiDimensionalCsv(int maxIndexLevel)
        {
            if(maxIndexLevel > DEFAULT_DELIMITER_CHARS.Length)
            {
                throw new Exception($"Header section level is max'd at {DEFAULT_DELIMITER_CHARS.Length}.");
            }
            else if(maxIndexLevel < 1)
            {
                throw new Exception($"Specified index-level [{maxIndexLevel}] must greater than 0.");
            }
            else
            {
                Delimiters = new char[maxIndexLevel];
                for(int i = 0; i < maxIndexLevel; i++) 
                { 
                    Delimiters[i] = DEFAULT_DELIMITER_CHARS[i]; 
                }

                EscapeChar = DEFAULT_ESCAPE_CHAR;
            }

        }

        public static MultiDimensionalCsv Parse(string encodedString)
        {
            return new MultiDimensionalCsv(encodedString);
        }

        public MultiDimensionalCsv(string value)
        {
            //at the minimun, the encoder section must have at least 2 chars, the first is the primary-delimiter, the second is the mandatory escape-char, 
            //a repeat of the primary-delimiter marks the end of the encoder section, and the start of the payload sections 
            //thus a minimal MDV must be at least 3-chars long. In addtion, chars in the encoder section must be not-white-space, printable, and non-alphanumeric
            if (value?.Length < 3)
            {
                throw new Exception("Invalid Header string - Multi-Dimensional Csv requires minimumn 3 chars in the delimiters-header section.");
            }
            else
            {
                char[] valueCharArray = value.ToCharArray();
                ThrowIfNotInRange(valueCharArray[0]);  // throws exception
                ThrowIfNotInRange(valueCharArray[1]);  // throws exception
                if(valueCharArray[1] == valueCharArray[0])
                {
                    throw new Exception("Mandatory Escape-char not defined.");
                }

                //From the third char of the string, look for the end of the header ("delimiters") section, and validate ...
                //1. if the char violates the Adaptive Indexing encoding rule, we throw exception, by which the Container would mark the string's encoding as "Custom" 
                //2. if there is no char value violation, continue collecting additional delimiters, until we find the first repeat of the primary delimiter
                //3. whenever we encountered the primary delimiter char the second time, the delimiters-section parsing ends and header value-sections starts
                for (int i = 2; i < valueCharArray.Length; i++)
                {
                    char c = valueCharArray[i];

                    //validate the char
                    ThrowIfNotInRange(c);

                    //reverse scan for repeat
                    for (int n = i - 1 ; n >= 0; n--)
                    {
                        if (c == valueCharArray[n])
                        {
                            if (n == 0)
                            {
                                //found header-section-ending char, set the encoder parameters (delimiters + the escape-char)
                                Delimiters = new char[i - 1];
                                Array.Copy(valueCharArray, Delimiters, i - 1);
                                EscapeChar = valueCharArray[i - 1];

                                //decoding the rest of the header string.
                                ChildSections = ParseChildSections(value.Substring(i + 1) /* header body */, this.Delimiters, this.EscapeChar);
                                return;
                            }
                            else
                            {
                                throw new Exception($"Delimiter [{c}] is repeated in header section.");
                            }
                        }
                    }
                }
                throw new Exception("Delimiters-section's ending char (a repeat of the primary-delimiter) not found.");

            }
        }


        //validate if the char can be a valid encoding char
        private void ThrowIfNotInRange(char v)
        {
            if (((v >= 33 && v <= 47) || (v >= 58 && v <= 64) || (v >= 91 && v <= 96) || (v >= 123 && v <= 126)) == false)
            {
                throw new Exception($"Delimiter '{v}' is invalid - a delimiter must be printable, non-white-space and non-alpha-numeric.");
            }
        }

        public override string ToString()
        {
            string header = $"{new string(Delimiters)}{EscapeChar}{Delimiters[0]}";
            return $"{header}{Value}";
        }

        /* 
         * Helper: replace the addressed section value.
         */
        public void SetValue(int[] indexAtEachLevel, string sectionNewValue)
        {
            var section = GetSection(indexAtEachLevel);
            if (section != null) { section.Value = sectionNewValue; }
        }

        /// <summary>
        /// Return empty (not null) if indexed child does not exists. NULL if the index-level is invalid
        /// </summary>
        /// <param name="indexAtEachLevel"></param>
        /// <returns></returns>
        public string GetValue(int[] indexAtEachLevel)
        {
            var section = GetSection(indexAtEachLevel);
            return section?.Value ?? null;
        }

        //helper
        public string[] GetChildValues(int[] indexesToParentSection)
        {
            List<string> result = new List<string>();
            var parent = GetSection(indexesToParentSection);
            if(parent != null)
            {
                if(parent.ChildSections.Count == 0)
                {
                    result.Add(parent.Value);
                }
                else
                {
                    foreach(var child in parent.ChildSections)
                    {
                        result.Add(child?.Value ?? string.Empty);
                    }
                }
            }

            return result.ToArray();
        }
    }
}
