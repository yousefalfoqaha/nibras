from typing import Annotated, TypedDict, List
from fastapi import FastAPI, File, UploadFile, HTTPException
import os
import base64
from io import BytesIO

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
    format_options={
        InputFormat.PDF: PdfFormatOption(pipeline_options=pipeline_options)
    }
)

UPLOAD_DIR = "uploads"
os.makedirs(UPLOAD_DIR, exist_ok=True)

class Page(TypedDict):
    content: str
    image_base64: str

def pil_to_base64_png(pil_image) -> str:
    buf = BytesIO()
    pil_image.save(buf, format="PNG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")

@app.post("/convert/")
async def convert_file(file: Annotated[UploadFile, File()]):
    file_name = file.filename or "uploaded_file.pdf"
    file_path = os.path.join(UPLOAD_DIR, file_name)

    with open(file_path, "wb") as f:
        f.write(await file.read())

    try:
        document = converter.convert(file_path).document
        total_pages = len(document.pages)

        pages: List[Page] = []

        for page_index in range(total_pages):
            page_no = page_index + 1
            page = document.pages[page_index]

            if page.image is None or page.image.pil_image is None:
                raise HTTPException(
                    status_code=500,
                    detail=f"Image generation failed for page {page_no}",
                )

            pages.append(
                {
                    "content": document.export_to_markdown(page_no=page_no),
                    "image_base64": pil_to_base64_png(page.image.pil_image),
                }
            )

        return {
            "file_name": file_name,
            "file_size": os.path.getsize(file_path),
            "total_pages": total_pages,
            "pages": pages,
        }

    finally:
        if os.path.exists(file_path):
            os.remove(file_path)