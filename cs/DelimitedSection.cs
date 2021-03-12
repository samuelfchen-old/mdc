using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace FolddaConnect.Codec
{

    public class DelimitedSection
    {
        public static readonly char[] DEFAULT_DELIMITER_CHARS = new char[] { '|', ';', ',', '^', ':', '~', '$', '&', '#', '=', '?', '"', '\'', '@', '_' };
        public const char DEFAULT_ESCAPE_CHAR = '\\';

        public char[] Delimiters { get; protected set; } = DEFAULT_DELIMITER_CHARS;
        public char EscapeChar { get; protected set; } = DEFAULT_ESCAPE_CHAR;

        public List<DelimitedSection> ChildSections { get; protected set; } = new List<DelimitedSection>();
        private string _value = string.Empty;

        //payload value (excluding encoder chars section)
        public string Value
        {
            get
            {
                //remove trailing empty child-sections
                while (ChildSections.Count > 0 && string.IsNullOrEmpty(ChildSections[ChildSections.Count - 1].Value /* recurrsion */))
                {
                    ChildSections.RemoveAt(ChildSections.Count - 1);
                }

                //join children with "next level" separator
                return ChildSections.Count == 0 ? _value
                    : string.Join(Delimiters[0].ToString(), ChildSections.Select(s => s.Value /* recurrsion */).ToArray());
            }

            set
            {
                // this statement below incurs recurrsion:
                // ParseChildSections() => MakeChildSection() => DimensionalSection(value,) => Value (this method)
                ChildSections = ParseChildSections(value, Delimiters, EscapeChar);

                if (ChildSections.Count == 0)
                {
                    _value = value ?? string.Empty;
                }
                else
                {
                    _value = string.Empty;
                }

                //ChildSections.Clear();
            }
        }

        protected DelimitedSection() { }

        public DelimitedSection(char[] delimiters, char escapeChar)
        {
            Delimiters = delimiters;
            EscapeChar = escapeChar;
        }

        public DelimitedSection(string value, char[] delimiters, char escapeChar) : this(delimiters, escapeChar)
        {
            Value = value;
        }

        public DelimitedSection this[int i]
        {
            get
            {
                if (Delimiters == null || Delimiters.Length == 0)
                {
                    throw new Exception($"No delimiter char defined at this level. Child-section dimension accessing invalid.");
                }

                //turns a "leaf" node to a "composite" node - that is, a node that have children that can be indexed.
                if (!string.IsNullOrEmpty(_value))
                {
                    ChildSections = ParseChildSections(_value, Delimiters, EscapeChar);
                    _value = string.Empty;
                }

                //extend the children elements if over-indexing is required
                while (ChildSections.Count <= i)
                {
                    ChildSections.Add(MakeChildSection(string.Empty, Delimiters, EscapeChar));
                }

                return ChildSections[i];
            }
        }

        private static DelimitedSection MakeChildSection(string value, char[] parentDelimiters, char parentEscapeChar)
        {
            if (parentDelimiters == null || parentDelimiters.Length == 0)
            {
                throw new System.Exception($"No delimiter char defined. Child-section dimension accessing invalid.");
            }
            else
            {
                //children delimiters range is 1-less of the parent
                char[] delimiters = new char[parentDelimiters.Length - 1];
                int i = delimiters.Length;
                while (--i >= 0) { delimiters[i] = parentDelimiters[i + 1]; }

                return new DelimitedSection(value, delimiters, parentEscapeChar);
            }
        }


        internal static List<DelimitedSection> ParseChildSections(string parentValue, char[] delimiters, char escapeChar)
        {
            List<DelimitedSection> result = new List<DelimitedSection>();
            if (delimiters?.Length > 0)
            {
                foreach (string value in ParseChildSectionValues(parentValue, delimiters[0], escapeChar))
                {
                    result.Add(MakeChildSection(value, delimiters, escapeChar));
                }
            }

            return result;
        }

        //helper  - implements the escaping logic
        private static List<string> ParseChildSectionValues(string parentValue, char childDelimiter, char escapeChar)
        {
            List<string> result = new List<string>();
            bool escaping = false;
            int childSectionStartIndex = 0;
            char[] valueCharArray = parentValue.ToCharArray();
            for (int currCharIndex = childSectionStartIndex; currCharIndex < valueCharArray.Length; currCharIndex++)
            {
                if (valueCharArray[currCharIndex] == escapeChar)
                {
                    //set escape-mode based on encounting the escape char
                    escaping = !escaping;  //note it flips when escape-char is hit again
                }
                else
                {
                    //.. if it's a non-escape char 
                    //if we encounter a separator-char and not in escaping-mode, we break it as a child-section ...
                    if (!escaping && valueCharArray[currCharIndex] == childDelimiter)
                    {
                        int sectionLength = currCharIndex - childSectionStartIndex;
                        string sectionValue = new string(valueCharArray, childSectionStartIndex, sectionLength);

                        /* Note: Value-setter may call ParseSections(), i.e. causes recursion */
                        result.Add(sectionValue);

                        childSectionStartIndex = currCharIndex + 1;    //next section start position
                    }

                    //reset escape-mode regardless (cos it's non-escape char)
                    escaping = false;
                }
            }

            //get the last token, that is, all chars after the last-encountered separator-char
            if(valueCharArray.Length > childSectionStartIndex)
            {
                string lastSectionValue =
                    new string(valueCharArray, childSectionStartIndex, valueCharArray.Length - childSectionStartIndex);
                result.Add(lastSectionValue);
            }

            return result;
        }

        //helper: used for storing a section-value that conatins delimiters chars and/or escape char
        //but you want these chars to be ignored during encoding and decoding, so the stored value
        //can be retrived as one-piece rather than being fragmented through child-indexing
        public string Escape(string sectionValue)
        {
            StringBuilder escaped = new StringBuilder();
            foreach (char c in sectionValue.ToCharArray())
            {
                if (Delimiters.Contains(c) || EscapeChar == c)
                {
                    escaped.Append(EscapeChar);
                }
                escaped.Append(c);
            }
            return escaped.ToString();   //escaped section value
        }

        //helper: undo the above Escape() effect and restores to its intended value, when an escaped string is retrieved. 
        public string UnEscape(string escapedSectionValue)
        {
            StringBuilder buffer = new StringBuilder();

            if (!string.IsNullOrEmpty(escapedSectionValue))   
            {
                char[] valueCharArray = escapedSectionValue.ToCharArray();
                for (int i = 0; i < valueCharArray.Length; i++)
                {
                    char curr = valueCharArray[i];
                    if (curr == EscapeChar && i < valueCharArray.Length - 1)
                    {
                        char next = valueCharArray[i + 1];
                        if (Delimiters.Contains(next) || next == EscapeChar)
                        {
                            continue;   //skip the escape-char
                        }
                    }

                    buffer.Append(curr);
                }
            }

            return buffer.ToString();   //un-escaped section value
        }
        
        //helper
        public DelimitedSection GetSection(int[] sectionIndexAddress)
        {
            if (sectionIndexAddress == null || sectionIndexAddress.Length == 0)
            {
                return this;
            }
            else if (Delimiters.Length > 0)
            {
                int[] nextLevelSectionIndexAddress = new int[sectionIndexAddress.Length - 1];
                Array.Copy(sectionIndexAddress, 1, nextLevelSectionIndexAddress, 0, nextLevelSectionIndexAddress.Length);

                return this[sectionIndexAddress[0]]?        /* auto extend children if required */
                        .GetSection(nextLevelSectionIndexAddress) ?? null;  //recurrsion
            }
            else
            {
                return null;
            }
        }

        public override string ToString()
        {
            return Value;
        }
    }
}