# -*- coding: utf-8 -*-
"""Kézikönyv PDF renderelő - közös sablon mind a 6 nyelvi változathoz.
Használat: python render.py content_HU.json ../PLC-szimulator_kezikonyv_HU.pdf
"""
import sys
import json
import os

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from reportlab.lib import colors
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle,
    ListFlowable, ListItem, PageBreak, KeepTogether, HRFlowable
)
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont

FONT_DIR = r"C:\Windows\Fonts"
pdfmetrics.registerFont(TTFont("Arial", os.path.join(FONT_DIR, "arial.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Bold", os.path.join(FONT_DIR, "arialbd.ttf")))
pdfmetrics.registerFont(TTFont("Arial-Italic", os.path.join(FONT_DIR, "ariali.ttf")))

NAVY = colors.HexColor("#1F4E79")
DARKRED = colors.HexColor("#A6192E")
GREY = colors.HexColor("#444444")
LIGHTRULE = colors.HexColor("#1F4E79")

PAGE_W, PAGE_H = A4
MARGIN = 20 * mm


def build_styles():
    styles = {}
    styles["DocTitle"] = ParagraphStyle(
        "DocTitle", fontName="Arial-Bold", fontSize=28, leading=32,
        textColor=NAVY, spaceAfter=6,
    )
    styles["DocSubtitle"] = ParagraphStyle(
        "DocSubtitle", fontName="Arial", fontSize=14, leading=18,
        textColor=GREY, spaceAfter=18,
    )
    styles["Author"] = ParagraphStyle(
        "Author", fontName="Arial", fontSize=11, leading=15,
        textColor=colors.black, spaceAfter=8,
    )
    styles["Disclaimer"] = ParagraphStyle(
        "Disclaimer", fontName="Arial-Bold", fontSize=10, leading=14,
        textColor=DARKRED,
    )
    styles["Version"] = ParagraphStyle(
        "Version", fontName="Arial", fontSize=9, leading=12,
        textColor=GREY,
    )
    styles["TOCHeading"] = ParagraphStyle(
        "TOCHeading", fontName="Arial-Bold", fontSize=18, leading=22,
        textColor=NAVY, spaceAfter=14,
    )
    styles["TOCEntry"] = ParagraphStyle(
        "TOCEntry", fontName="Arial", fontSize=11, leading=18,
        textColor=colors.black, leftIndent=0,
    )
    styles["TOCEntrySub"] = ParagraphStyle(
        "TOCEntrySub", fontName="Arial", fontSize=10, leading=15,
        textColor=GREY, leftIndent=14 * mm,
    )
    styles["SectionHeading"] = ParagraphStyle(
        "SectionHeading", fontName="Arial-Bold", fontSize=14, leading=18,
        textColor=colors.white,
    )
    styles["SubHeading"] = ParagraphStyle(
        "SubHeading", fontName="Arial-Bold", fontSize=11.5, leading=15,
        textColor=NAVY, spaceBefore=10, spaceAfter=4,
    )
    styles["Body"] = ParagraphStyle(
        "Body", fontName="Arial", fontSize=9.7, leading=14,
        textColor=colors.black, spaceAfter=6, alignment=TA_LEFT,
    )
    styles["Bullet"] = ParagraphStyle(
        "Bullet", fontName="Arial", fontSize=9.7, leading=13.6,
        textColor=colors.black, spaceAfter=3,
    )
    styles["SubBullet"] = ParagraphStyle(
        "SubBullet", fontName="Arial", fontSize=9.3, leading=13,
        textColor=colors.HexColor("#222222"), spaceAfter=2,
    )
    styles["TableHeader"] = ParagraphStyle(
        "TableHeader", fontName="Arial-Bold", fontSize=9, leading=12,
        textColor=colors.white,
    )
    styles["TableCell"] = ParagraphStyle(
        "TableCell", fontName="Arial", fontSize=8.8, leading=12,
        textColor=colors.black,
    )
    styles["Note"] = ParagraphStyle(
        "Note", fontName="Arial-Italic", fontSize=9, leading=13,
        textColor=DARKRED, spaceBefore=3, spaceAfter=6, leftIndent=4 * mm,
    )
    styles["Footer"] = ParagraphStyle(
        "Footer", fontName="Arial", fontSize=8, leading=10,
        textColor=GREY, alignment=TA_CENTER,
    )
    return styles


def md_inline(text):
    """**bold** -> <b>bold</b>, kódformátum `x` -> <font name=Arial-Bold>x</font>"""
    import re
    text = re.sub(r"\*\*(.+?)\*\*", r"<b>\1</b>", text)
    text = re.sub(r"`(.+?)`", r"<font face='Arial-Bold'>\1</font>", text)
    return text


def make_footer(footer_title):
    def _footer(canvas, doc):
        canvas.saveState()
        canvas.setFont("Arial", 8)
        canvas.setFillColor(GREY)
        text = "%s | %s" % (footer_title, canvas.getPageNumber())
        canvas.drawCentredString(PAGE_W / 2.0, 12 * mm, text)
        canvas.restoreState()
    return _footer


def render_bullets(items, styles, level=0):
    flow = []
    for it in items:
        if isinstance(it, str):
            text = it
            sub = None
        else:
            text = it.get("text", "")
            sub = it.get("sub")
        style = styles["Bullet"] if level == 0 else styles["SubBullet"]
        bullet_char = "\u25CF" if level == 0 else "\u2013"
        indent = 4 * mm + level * 6 * mm
        p = Paragraph(md_inline(text), style)
        flow.append(ListFlowable(
            [ListItem(p, value=bullet_char, leftIndent=indent)],
            bulletType="bullet", start=bullet_char,
            leftIndent=indent, bulletFontName="Arial", bulletFontSize=8,
        ))
        if sub:
            flow.extend(render_bullets(sub, styles, level=level + 1))
    return flow


def render_table(tbl, styles):
    header = tbl["header"]
    rows = tbl["rows"]
    data = [[Paragraph(md_inline(h), styles["TableHeader"]) for h in header]]
    for row in rows:
        data.append([Paragraph(md_inline(c), styles["TableCell"]) for c in row])
    col_count = len(header)
    avail_width = PAGE_W - 2 * MARGIN
    col_widths = tbl.get("col_widths")
    if col_widths:
        col_widths = [avail_width * w for w in col_widths]
    else:
        col_widths = [avail_width / col_count] * col_count
    t = Table(data, colWidths=col_widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#BBBBBB")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F2F6FA")]),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
    ]))
    return t


def render_intro(intro, styles):
    flow = []
    if isinstance(intro, list):
        for p in intro:
            flow.append(Paragraph(md_inline(p), styles["Body"]))
    elif intro:
        flow.append(Paragraph(md_inline(intro), styles["Body"]))
    return flow


def section_heading_flow(num, title, styles):
    t = Table(
        [[Paragraph(num, ParagraphStyle(
            "num", fontName="Arial-Bold", fontSize=13, leading=16,
            textColor=colors.white, alignment=TA_CENTER)),
          Paragraph(title, styles["SectionHeading"])]],
        colWidths=[10 * mm, (PAGE_W - 2 * MARGIN - 10 * mm)],
    )
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (0, 0), NAVY),
        ("BACKGROUND", (1, 0), (1, 0), colors.HexColor("#2E6DA4")),
        ("VALIGN", (0, 0), (-1, -1), "MIDDLE"),
        ("ALIGN", (0, 0), (0, 0), "CENTER"),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
        ("LEFTPADDING", (1, 0), (1, 0), 8),
    ]))
    return t


def build_pdf(content, out_path):
    styles = build_styles()
    doc = SimpleDocTemplate(
        out_path, pagesize=A4,
        leftMargin=MARGIN, rightMargin=MARGIN,
        topMargin=18 * mm, bottomMargin=18 * mm,
        title=content["doc_title"] + " - " + content["doc_subtitle"],
        author="Sisak Laszlo",
    )
    footer_title = content["footer_title"]
    story = []

    # --- Title page ---
    story.append(Spacer(1, 70 * mm))
    story.append(Paragraph(content["doc_title"], styles["DocTitle"]))
    story.append(Paragraph(content["doc_subtitle"], styles["DocSubtitle"]))
    story.append(HRFlowable(width="55%", thickness=1.1, color=NAVY,
                             spaceBefore=4, spaceAfter=14, hAlign="LEFT"))
    story.append(Paragraph(content["author_line"], styles["Author"]))
    story.append(Spacer(1, 4))
    story.append(Paragraph(content["disclaimer"], styles["Disclaimer"]))
    if content.get("version_line"):
        story.append(Spacer(1, 10))
        story.append(Paragraph(content["version_line"], styles["Version"]))
    story.append(PageBreak())

    # --- TOC ---
    story.append(Paragraph(content["toc_heading"], styles["TOCHeading"]))
    for sec in content["sections"]:
        story.append(Paragraph(
            "%s.&nbsp;&nbsp;%s" % (sec["num"], md_inline(sec["title"])),
            styles["TOCEntry"]))
        for sub in sec.get("subsections", []):
            story.append(Paragraph("%s" % md_inline(sub["title"]), styles["TOCEntrySub"]))
    story.append(PageBreak())

    # --- Sections ---
    for sec in content["sections"]:
        head_block = [section_heading_flow(sec["num"], sec["title"], styles), Spacer(1, 6)]
        head_block.extend(render_intro(sec.get("intro"), styles))
        story.append(KeepTogether(head_block))
        if sec.get("items"):
            story.extend(render_bullets(sec["items"], styles))
        if sec.get("table"):
            story.append(Spacer(1, 4))
            story.append(render_table(sec["table"], styles))
            story.append(Spacer(1, 4))
        if sec.get("note"):
            story.append(Paragraph(md_inline(sec["note"]), styles["Note"]))
        for sub in sec.get("subsections", []):
            sub_block = [Paragraph(md_inline(sub["title"]), styles["SubHeading"])]
            sub_block.extend(render_intro(sub.get("intro"), styles))
            story.append(KeepTogether(sub_block))
            if sub.get("items"):
                story.extend(render_bullets(sub["items"], styles))
            if sub.get("table"):
                story.append(Spacer(1, 4))
                story.append(render_table(sub["table"], styles))
                story.append(Spacer(1, 4))
            if sub.get("note"):
                story.append(Paragraph(md_inline(sub["note"]), styles["Note"]))
        story.append(Spacer(1, 10))

    footer_fn = make_footer(footer_title)
    doc.build(story, onFirstPage=footer_fn, onLaterPages=footer_fn)


if __name__ == "__main__":
    content_path = sys.argv[1]
    out_path = sys.argv[2]
    with open(content_path, "r", encoding="utf-8") as f:
        content = json.load(f)
    build_pdf(content, out_path)
    print("OK:", out_path)
