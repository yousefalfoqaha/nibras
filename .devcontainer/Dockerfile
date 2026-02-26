FROM mcr.microsoft.com/devcontainers/base:ubuntu

RUN apt-get update && \
  apt-get install -y --no-install-recommends \
  build-essential \
  libxml2-dev \
  libxslt1-dev \
  libgl1 \
  libglib2.0-0 && \
  rm -rf /var/lib/apt/lists/*
