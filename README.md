# <div align="center"><img src="/assets/nibras.png" width="100"><br/>Nibras</div>

Nibras is an AI agent for the German Jordanian University (GJU), focused on accurately retrieving relevant university documents to answer student questions regarding study plans, fees, and academic calendars (for now).

→ Try out [Nibras](nibrasgju.com)

## Overview
The main point of Nibras is to develop a document-based search tool that specializes in searching GJU documents for later retrieval based on a user's query, and intelligently stops searching if the student's question is too ambiguous to perform a confident search, prompting Nibras to ask the student clarifying questions. Nibras uses this search tool to answer student questions (or ask appropriate questions himself) in a conversational manner.

<div align="center">  
<img src="/assets/overview.png" width="auto">  
</div><br/>

> If you want a deeper dive, check out [this sick diagram](/assets/diagram.png)

## Features
- RAG techniques to search for relevant sections across pages without losing context
- Expand relevant search results to contain full document sections as additional context
- LLM-assisted document processing pipeline for rich data (helpful for later retrieval)
- Multi-step search tool to ensure correct documents are found based on the conversation topic
- Ability to stop and ask clarifying questions to continue a more detailed search

## Installation (using Docker)

> **IMPORTANT!** Nibras is currently in beta, expect breaking changes before v1.0

### Setup

1. Download the `compose.yml` file
2. Change the `caddy` service to point your domain to the app:

```yaml 
caddy:
  image: caddy:2
  depends_on:
    - app
  ports:
    - "80:80"
    - "443:443"
  command: >
    caddy reverse-proxy --from example.com --to app:8080  # change example.com 
  volumes:
    - caddy_data:/data
    - caddy_config:/config
  restart: always
```

3. Run `docker compose up -d` 

That's it, have fun with Nibras!

## Configuration

Nibras can be configured using the following environment variables:

| Variable                               | Default Value(s) | Description                                                            |
| -------------------------------------- | ---------------- | ---------------------------------------------------------------------- |
| `DB_NAME`, `DB_USER` and `DB_PASSWORD` | `chatbot`        | Name, user, and password to the PGVector store.                        |
| `APP_USER`, `APP_PASSWORD`             | `chatbot`        | User and password to restricted endpoints (e.g. `POST /files/process`) |
| `OPENAI_API_KEY`                       |                  | Required. Your API key to OpenAI for chat completions.                 |

