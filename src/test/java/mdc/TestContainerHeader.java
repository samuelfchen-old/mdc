package mdc;

import org.junit.Test;

import static org.junit.Assert.*;

public class TestContainerHeader {
    // String json = 
    String header = null;
    MultiDimensionalCsv mdvHeader = null;

    @Test
    public void test() {
        header = "|;,\\|sec1|sec2";
        try {
            mdvHeader = new MultiDimensionalCsv(header);
        } catch (Exception e) { e.printStackTrace(); }

        assertNotNull(mdvHeader.GetSection(new int[] { 0 }).getValue());

        assertEquals("sec1", mdvHeader.GetSection(new int[] { 0 }).getValue());
        assertEquals("sec2", mdvHeader.GetSection(new int[] { 1 }).getValue());

        assertEquals("sec1", mdvHeader.GetSection(new int[] { 0, 0 }).getValue());
        assertEquals("sec1", mdvHeader.GetSection(new int[] { 0, 0, 0 }).getValue());
        assertEquals("sec2", mdvHeader.GetSection(new int[] { 1, 0 }).getValue());

        assertNull(mdvHeader.GetSection(new int[] { 1, 0, 0, 0 })); //this is different
        assertEquals("", mdvHeader.GetSection(new int[] { 0, 0, 1 }).getValue());
        assertEquals("", mdvHeader.GetSection(new int[] { 5, 0 }).getValue());

        mdvHeader.SetValue(new int[] { 0 }, "SEC1");
        mdvHeader.SetValue(new int[] { 1 }, "SEC2");
        mdvHeader.SetValue(new int[] { 2 }, "SEC3");    

        mdvHeader.SetValue(new int[] { 0, 1 }, "SEC1b");
        assertEquals("|;,\\|SEC1;SEC1b|SEC2|SEC3", mdvHeader.toString());

        mdvHeader.SetValue(new int[] { 0, 0, 1 }, "SEC1c");
        assertEquals("|;,\\|SEC1,SEC1c;SEC1b|SEC2|SEC3", mdvHeader.toString());

        mdvHeader.SetValue(new int[] { 0, 1, 1 }, "SEC1d");
        assertEquals("|;,\\|SEC1,SEC1c;SEC1b,SEC1d|SEC2|SEC3", mdvHeader.toString());
        assertEquals("SEC1,SEC1c;SEC1b,SEC1d", mdvHeader.GetSection(new int[] { 0 }).getValue());
        assertEquals("SEC1,SEC1c", mdvHeader.GetSection(new int[] { 0, 0 }).getValue());
        assertEquals("SEC1d", mdvHeader.GetSection(new int[] { 0, 1, 1 }).getValue());

        mdvHeader.SetValue(new int[] { 0, 1, 4 }, mdvHeader.Escape("SE;|C1d4"));
        assertEquals("|;,\\|SEC1,SEC1c;SEC1b,SEC1d,,,SE\\;\\|C1d4|SEC2|SEC3", mdvHeader.toString());
        assertEquals("SE;|C1d4", mdvHeader.UnEscape(mdvHeader.GetSection(new int[] { 0, 1, 4 }).getValue()));

        DelimitedSection section = mdvHeader.GetSection(new int[] { 0 });
        assertEquals("SEC1,SEC1c;SEC1b,SEC1d,,,SE\\;\\|C1d4", section.getValue());
        assertEquals(';', section.Delimiters[0]);
        assertEquals(',', section.Delimiters[1]);
        assertEquals(2, section.Delimiters.length);
        assertEquals('\\', section.EscapeChar);

        assertEquals("", mdvHeader.GetSection(new int[] { 0, 1, 3 }).getValue());
        assertEquals("", mdvHeader.GetSection(new int[] { 0, 1, 8 }).getValue());
        assertNull(mdvHeader.GetSection(new int[] { 0, 1, 0, 0 })); // missed null check? thing
        assertEquals("SEC1d", mdvHeader.GetSection(new int[] { 0, 1, 1 }).getValue());
        assertEquals("SEC1d", mdvHeader.GetValue(new int[] { 0, 1, 1 }));

        String[] r1 = mdvHeader.GetChildValues(new int[] { 0, 1, 1 }/*parent addr*/);
        assertEquals(1, r1.length);
        assertEquals("SEC1d", r1[0]);

        String[] r2 = mdvHeader.GetChildValues(new int[] { 0 });
        assertEquals(2, r2.length);
        assertEquals("SEC1,SEC1c", r2[0]);
        assertEquals("SEC1b,SEC1d,,,SE\\;\\|C1d4", r2[1]);

        String[] r2a = mdvHeader.GetChildValues(new int[] { 1 });
        assertEquals(1, r2a.length);
        assertEquals("SEC2", r2a[0]);

        String[] r3 = mdvHeader.GetChildValues(new int[] { 0, 1 });
        assertEquals(5, r3.length);
        assertEquals("SEC1b", r3[0]);
        assertEquals("SEC1d", r3[1]);
        assertEquals("", r3[3]);
        assertEquals("SE\\;\\|C1d4", r3[4]);
    }
}
