package com.mpin.app.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a single-sheet .xlsx file. Apache POI is too heavy for Android, and an
 * .xlsx is just a zip of a few XML files, so this builds them directly.
 *
 * Cells are Strings (stored as text, so MPINs keep leading zeros) or Numbers.
 */
public final class XlsxWriter {

    /** Per-column cell styles. */
    public static final int STYLE_NORMAL = 0;
    public static final int STYLE_TEXT_CENTER = 2;
    public static final int STYLE_CENTER = 3;
    private static final int STYLE_HEADER = 1;

    private XlsxWriter() {
    }

    public static void write(OutputStream out, String sheetName, String[] headers, List<Object[]> rows,
                             int[] columnWidths, int[] columnStyles) throws IOException {
        ZipOutputStream zip = new ZipOutputStream(out);
        entry(zip, "[Content_Types].xml", CONTENT_TYPES);
        entry(zip, "_rels/.rels", ROOT_RELS);
        entry(zip, "xl/workbook.xml", workbook(sheetName));
        entry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
        entry(zip, "xl/styles.xml", STYLES);
        entry(zip, "xl/worksheets/sheet1.xml", sheet(headers, rows, columnWidths, columnStyles));
        zip.finish();
        zip.flush();
    }

    private static void entry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String workbook(String sheetName) {
        return XML_DECL
                + "<workbook xmlns=\"" + NS_MAIN + "\" xmlns:r=\"" + NS_REL + "\">"
                + "<sheets><sheet name=\"" + escape(sheetName) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
                + "</workbook>";
    }

    private static String sheet(String[] headers, List<Object[]> rows, int[] widths, int[] styles) {
        StringBuilder sb = new StringBuilder(XML_DECL);
        sb.append("<worksheet xmlns=\"").append(NS_MAIN).append("\" xmlns:r=\"").append(NS_REL).append("\">");
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews>");
        sb.append("<sheetFormatPr defaultRowHeight=\"15\"/>");
        sb.append("<cols>");
        for (int i = 0; i < widths.length; i++) {
            sb.append("<col min=\"").append(i + 1).append("\" max=\"").append(i + 1)
                    .append("\" width=\"").append(widths[i]).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        sb.append("<row r=\"1\">");
        for (int c = 0; c < headers.length; c++) {
            appendCell(sb, c, 1, headers[c], STYLE_HEADER);
        }
        sb.append("</row>");

        int r = 2;
        for (Object[] row : rows) {
            sb.append("<row r=\"").append(r).append("\">");
            for (int c = 0; c < row.length; c++) {
                int style = styles != null && c < styles.length ? styles[c] : STYLE_NORMAL;
                appendCell(sb, c, r, row[c], style);
            }
            sb.append("</row>");
            r++;
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private static void appendCell(StringBuilder sb, int col, int row, Object value, int style) {
        String ref = columnName(col) + row;
        if (value instanceof Number) {
            sb.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\"><v>")
                    .append(value).append("</v></c>");
        } else {
            sb.append("<c r=\"").append(ref).append("\" s=\"").append(style).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                    .append(escape(value == null ? "" : value.toString())).append("</t></is></c>");
        }
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        int n = index + 1;
        while (n > 0) {
            int rem = (n - 1) % 26;
            name.insert(0, (char) ('A' + rem));
            n = (n - 1) / 26;
        }
        return name.toString();
    }

    /** XML-escapes text and drops characters XML 1.0 cannot contain. */
    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                default:
                    if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                        out.append(ch);
                    }
            }
        }
        return out.toString();
    }

    private static final String XML_DECL = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";

    private static final String CONTENT_TYPES = XML_DECL
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String ROOT_RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK_RELS = XML_DECL
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    // 0 normal, 1 header (bold white on teal), 2 text + centered, 3 centered
    private static final String STYLES = XML_DECL
            + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
            + "<fonts count=\"2\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>"
            + "</fonts>"
            + "<fills count=\"3\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF0F766E\"/><bgColor indexed=\"64\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"4\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"49\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";
}
