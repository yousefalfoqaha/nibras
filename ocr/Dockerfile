FROM pytorch/pytorch:2.10.0-cuda13.0-cudnn9-runtime

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  python3.12-venv \
  build-essential \
  libxml2-dev \
  libxslt1-dev \
  libgl1 \
  libglib2.0-0 && \
  rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .

RUN python3 -m venv /opt/venv \
  && /opt/venv/bin/pip install --upgrade pip \
  && /opt/venv/bin/pip install --no-cache-dir --prefer-binary -r requirements.txt

ENV PATH="/opt/venv/bin:$PATH"
