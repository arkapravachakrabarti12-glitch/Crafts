package com.mpin.app.core;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XlsxWriterTest {

    @Test
    public void writesValidWorkbook() throws Exception {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{1, "SBI <mobile> & \"UPI\"", "0471", "4 digit"});
        rows.add(new Object[]{2, "Locker\u0001", "905317", "6 digit"});

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        XlsxWriter.write(out, "MPINs", new String[]{"#", "Label / Purpose", "MPIN", "Length"}, rows,
                new int[]{6, 32, 12, 10}, new int[]{XlsxWriter.STYLE_CENTER, 0, XlsxWriter.STYLE_TEXT_CENTER, 3});

        Map<String, String> files = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                files.put(e.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        assertTrue(files.keySet().containsAll(List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
                "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml")));

        // every part must be well-formed XML
        for (String xml : files.values()) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        }
        String sheet = files.get("xl/worksheets/sheet1.xml");
        assertTrue(sheet.contains("SBI &lt;mobile&gt; &amp; &quot;UPI&quot;"));
        assertTrue(sheet.contains(">0471<"));
        assertTrue(sheet.contains("<v>2</v>"));
    }

    @Test
    public void columnNames() {
        assertEquals("A", XlsxWriter.columnName(0));
        assertEquals("Z", XlsxWriter.columnName(25));
        assertEquals("AA", XlsxWriter.columnName(26));
    }
}
