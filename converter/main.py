from typing import Annotated

from fastapi import FastAPI, File, UploadFile

from io import BytesIO
import zipfile

from docling.document_converter import DocumentConverter, PdfFormatOption
from docling.datamodel.base_models import InputFormat, DocumentStream
from docling.datamodel.pipeline_options import PdfPipelineOptions, ThreadedPdfPipelineOptions
from docling.datamodel.accelerator_options import AcceleratorDevice, AcceleratorOptions
from docling.pipeline.threaded_standard_pdf_pipeline import ThreadedStandardPdfPipeline

app = FastAPI()

pipeline_options = ThreadedPdfPipelineOptions(
    do_ocr=False,
    accelerator_options=AcceleratorOptions(
        device=AcceleratorDevice.CUDA,
    ),
    ocr_batch_size=1,
    layout_batch_size=8,
    table_batch_size=1,
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
    markdown = document.export_to_markdown()

    return {
        "content": markdown
    }
