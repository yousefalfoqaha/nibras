from typing import Annotated
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import StreamingResponse
from io import BytesIO
import zipfile

from docling.document_converter import DocumentConverter, PdfFormatOption
from docling.datamodel.base_models import InputFormat, DocumentStream
from docling.datamodel.pipeline_options import PdfPipelineOptions, ThreadedPdfPipelineOptions
from docling.datamodel.accelerator_options import AcceleratorDevice, AcceleratorOptions
from docling.pipeline.threaded_standard_pdf_pipeline import ThreadedStandardPdfPipeline

app = FastAPI()

pipeline_options = ThreadedPdfPipelineOptions(
    generate_page_images=True,
    images_scale=1.0,
    do_ocr=False,
    accelerator_options=AcceleratorOptions(
        device=AcceleratorDevice.CUDA,
    ),
    ocr_batch_size=4,
    layout_batch_size=64,
    table_batch_size=4,
)

converter = DocumentConverter(
    format_options={
        InputFormat.PDF: PdfFormatOption(
            pipeline_cls=ThreadedStandardPdfPipeline,
            pipeline_options=pipeline_options,
        )
    }
)


@app.post("/convert/")
async def convert_file(file: Annotated[UploadFile, File()]):
    file_name = file.filename or "uploaded_file.pdf"
    file_bytes = await file.read()
    source = DocumentStream(name=file_name, stream=BytesIO(file_bytes))

    document = converter.convert(source).document

    zip_buffer = BytesIO()

    with zipfile.ZipFile(zip_buffer, mode="w") as archive:
        for page_no, page in document.pages.items():
            if page.image is None or page.image.pil_image is None:
                raise HTTPException(
                    status_code=500,
                    detail=f"Image generation failed for page {page_no}",
                )

            markdown = document.export_to_markdown(page_no=page_no)
            archive.writestr(f"{page_no}.md", markdown)

            image_buffer = BytesIO()
            page.image.pil_image.save(image_buffer, format="JPEG")
            archive.writestr(f"{page_no}.jpeg", image_buffer.getvalue())

    zip_buffer.seek(0)

    return StreamingResponse(
        zip_buffer,
        media_type="application/zip",
        headers={"Content-Disposition": "attachment; filename=result.zip"},
    )
