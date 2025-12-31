from typing import Annotated
from fastapi import FastAPI, File, UploadFile
import os
from docling.document_converter import DocumentConverter

app = FastAPI()
converter = DocumentConverter()

UPLOAD_DIR = "uploads"
MARKDOWN_DIR = "markdown_output"
os.makedirs(UPLOAD_DIR, exist_ok=True)
os.makedirs(MARKDOWN_DIR, exist_ok=True)

@app.post("/convert/")
async def convert_file(file: Annotated[UploadFile, File()]):
    file_name = file.filename or "uploaded_file"
    file_path = os.path.join(UPLOAD_DIR, file_name)
    
    with open(file_path, "wb") as f:
        f.write(await file.read())
    
    try:
        document = converter.convert(file_path).document
        markdown_content = document.export_to_markdown()

        return {
            "file_name": file_name,
            "file_size": os.path.getsize(file_path),
            "content": markdown_content
        }

    finally:
        if os.path.exists(file_path):
            os.remove(file_path)