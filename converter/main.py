from typing import Annotated
from fastapi import FastAPI, File, UploadFile, HTTPException
from io import BytesIO
from pathlib import Path

from docling.document_converter import DocumentConverter, PdfFormatOption
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import PdfPipelineOptions, RapidOcrOptions

app = FastAPI()

pipeline_options = PdfPipelineOptions(
    generate_page_images=True,
    images_scale=1.0,
    do_ocr=True,
    ocr_options=RapidOcrOptions(),
)

converter = DocumentConverter(
    format_options={InputFormat.PDF: PdfFormatOption(pipeline_options=pipeline_options)}
)

UPLOAD_DIR = "uploads"
upload_directory = Path(__file__).parent.joinpath(UPLOAD_DIR)
upload_directory.mkdir(exist_ok=True)

RESULT_DIR = "results"
result_directory = Path(__file__).parent.joinpath(RESULT_DIR)
result_directory.mkdir(exist_ok=True)


@app.post("/convert/")
async def convert_file(file: Annotated[UploadFile, File()]):
    file_name = file.filename or "uploaded_file.pdf"
    file_bytes = await file.read()

    document = converter.convert(BytesIO(file_bytes)).document
    total_pages = len(document.pages)

    for page_index in range(total_pages):
        page_no = page_index + 1
        page = document.pages[page_index]

        if page.image is None or page.image.pil_image is None:
            raise HTTPException(
                status_code=500,
                detail=f"Image generation failed for page {page_no}",
            )

        markdown_path = result_directory.joinpath(f"page_{page_no}.md")
        image_path = result_directory.joinpath(f"page_{page_no}.png")

        markdown = document.export_to_markdown(page_no=page_no)
        image = page.image.pil_image

        markdown_path.write_text(markdown, encoding="utf-8")
        image.save(image_path, format="PNG")

    return {
        "file_name": file_name,
        "file_size": len(file_bytes),
        "total_pages": total_pages,
    }
