FROM pytorch/pytorch:2.9.1-cuda12.6-cudnn9-runtime

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  build-essential \
  libxml2-dev \
  libxslt1-dev \
  libgl1 \
  libglib2.0-0 && \
  rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir --prefer-binary -r requirements.txt
