package com.academix.document.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Small hand-rolled layout helper over PDFBox's low-level content stream API — this codebase
 * has no templating engine, and these three document types don't warrant adding one.
 * Standard-14 fonts don't reliably cover accented glyphs, so text is stripped of diacritics
 * before being drawn (source strings keep proper French spelling for readability).
 */
class PdfDocumentBuilder implements AutoCloseable {

	static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
	static final PDFont ITALIC = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

	private static final float MARGIN = 56f;
	private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
	private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
	private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

	private final PDDocument document = new PDDocument();
	private PDPageContentStream stream;
	private float cursorY;

	PdfDocumentBuilder() {
		newPage();
	}

	PdfDocumentBuilder title(String text) {
		writeLine(text, BOLD, 15, 22);
		return this;
	}

	PdfDocumentBuilder heading(String text) {
		writeLine(text, BOLD, 11.5f, 16);
		return this;
	}

	PdfDocumentBuilder paragraph(String text) {
		for (String line : wrap(text, REGULAR, 11)) {
			writeLine(line, REGULAR, 11, 16);
		}
		return this;
	}

	PdfDocumentBuilder keyValue(String label, String value) {
		writeLine(label + " : " + value, REGULAR, 11, 15);
		return this;
	}

	PdfDocumentBuilder italic(String text) {
		writeLine(text, ITALIC, 10, 14);
		return this;
	}

	PdfDocumentBuilder tableRow(String[] values, float[] columnOffsets, PDFont font, float size) {
		ensureSpace(18);
		try {
			stream.beginText();
			stream.setFont(font, size);
			stream.newLineAtOffset(MARGIN, cursorY);
			float previousOffset = 0;
			for (int i = 0; i < values.length; i++) {
				float offset = columnOffsets[i];
				stream.newLineAtOffset(offset - previousOffset, 0);
				stream.showText(clean(values[i]));
				previousOffset = offset;
			}
			stream.endText();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write PDF table row", e);
		}
		cursorY -= 18;
		return this;
	}

	PdfDocumentBuilder rule() {
		ensureSpace(10);
		try {
			stream.setLineWidth(0.6f);
			stream.moveTo(MARGIN, cursorY);
			stream.lineTo(MARGIN + CONTENT_WIDTH, cursorY);
			stream.stroke();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not draw PDF rule", e);
		}
		cursorY -= 10;
		return this;
	}

	PdfDocumentBuilder spacer(float height) {
		cursorY -= height;
		return this;
	}

	byte[] build() {
		closeStream();
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			document.save(out);
			document.close();
			return out.toByteArray();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not finalize PDF document", e);
		}
	}

	@Override
	public void close() {
		closeStream();
	}

	private void newPage() {
		closeStream();
		PDPage page = new PDPage(PDRectangle.A4);
		document.addPage(page);
		try {
			stream = new PDPageContentStream(document, page);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not start PDF page", e);
		}
		cursorY = PAGE_HEIGHT - MARGIN;
	}

	private void closeStream() {
		if (stream != null) {
			try {
				stream.close();
			} catch (IOException e) {
				throw new UncheckedIOException("Could not finalize PDF page", e);
			}
			stream = null;
		}
	}

	private void ensureSpace(float needed) {
		if (cursorY - needed < MARGIN) {
			newPage();
		}
	}

	private void writeLine(String text, PDFont font, float size, float leading) {
		ensureSpace(leading);
		try {
			stream.beginText();
			stream.setFont(font, size);
			stream.newLineAtOffset(MARGIN, cursorY);
			stream.showText(clean(text));
			stream.endText();
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write PDF text", e);
		}
		cursorY -= leading;
	}

	private List<String> wrap(String text, PDFont font, float size) {
		List<String> lines = new ArrayList<>();
		for (String paragraph : text.split("\n")) {
			StringBuilder current = new StringBuilder();
			for (String word : paragraph.split(" ")) {
				String candidate = current.isEmpty() ? word : current + " " + word;
				if (width(candidate, font, size) > CONTENT_WIDTH && !current.isEmpty()) {
					lines.add(current.toString());
					current = new StringBuilder(word);
				} else {
					current = new StringBuilder(candidate);
				}
			}
			lines.add(current.toString());
		}
		return lines;
	}

	private float width(String text, PDFont font, float size) {
		try {
			return font.getStringWidth(clean(text)) / 1000 * size;
		} catch (IOException e) {
			throw new UncheckedIOException("Could not measure PDF text", e);
		}
	}

	private String clean(String text) {
		String withoutLigatures = text.replace("œ", "oe").replace("Œ", "OE");
		return DIACRITICS.matcher(Normalizer.normalize(withoutLigatures, Normalizer.Form.NFD)).replaceAll("");
	}
}
