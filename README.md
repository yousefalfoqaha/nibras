# <div align="center"><img src="/assets/nibras.png" width="100"><br/>Nibras</div>

Nibras is an AI agent for the German Jordanian University (GJU), focused on accurately retrieving relevant university documents to answer student questions regarding study plans, fees, and academic calendars (for now).

→ Try out [Nibras](https://nibrasgju.com)

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

3. Add a `document-types.yml` file besides the `compose.yml` file (check out the configuration section for more details on document types)
4. Run `docker compose up -d` 

That's it, have fun with Nibras!

## Document types

Include a special `document-types.yml` file besides the `compose.yml` file that can be used to declare document types that both the **document processing pipeline** and the **search tool** can use.

**Document processing pipeline**: when a document is being processed to be persisted in a vector store, the pipeline identifies the `document_type`, and then checks to see if all `required_attributes` are present. If a required attribute is missing, the user is informed to provide that detail.
<br/><br/>
**Search tool**: when asked a question, the search tool identifies the relevant `document_types` to search for, and tries to identify `required_attributes`, if a `required_attribute` is missing, it will try to auto-complete the attribute if only one exists in the vector store, otherwise it will pause the search.

Here is an example `document-types.yml`:

```yaml
document_types:
  study_plan:

    # descriptions are very useful for the AI agent to recognize if a document type is worth searching or not, you can freely apply context engineering techniques here
    description: |
      The study plan includes school, curriculum, admission requirements, placement tests (aka remedials), learning outcomes, and program objectives.
      The framework categorizes courses by level (university, school, program) and type (compulsory, elective). All track courses and electives are listed here.
      The study plan guide organizes courses by semester, providing a recommended sequence.
      Courses offered section Lists all courses by department and other schools, with full descriptions.
      Some extra notes,
        - The study plan year indicates the first cohort it serves and remains in effect until superseded.
        - The academic level reflects the student’s level within the program.

    # in all cases, the document must mention a year to be saved into the vector store
    # if no year was mentioned in the conversation, the search tool will provide the latest year
    # if a year was mentioned, but no document is available for that year, recommend the nearest year
    requires_year: true 
    
    # attributes are extra "filters" that help narrow down documents, such as separating masters and bachelor documents, or computer science and computer engineering majors
    # the document MUST include all the required attributes inorder to be saved into the vector store
    attributes:
      required: [academic_level, program]
      optional: [department]

  academic_calendar:
    description: "Events for all semesters (first, second, summer)."

    # useful for documents that require up-to-date information
    # even if the user mentions a different year, this always fetches the document with the latest year
    prefer_latest_year: true

  fees:
    description: "Tuition, registration, fees for each academic a"
    attributes:
      required: [academic_level]

# list of allowed values per-attribute
attributes:
  program:
    values:
      - computer science
      - mechatronics engineering
      - computer engineering

  academic_level:
    values:
      - undergraduate
      - graduate

  department:
    values:
      - computer science
      - mechatronics engineering
      - computer engineering
```

## Configuration

Nibras can be configured using the following environment variables:

| Variable                               | Default Value(s) | Description                                                            |
| -------------------------------------- | ---------------- | ---------------------------------------------------------------------- |
| `DB_NAME`, `DB_USER` and `DB_PASSWORD` | `chatbot`        | Name, user, and password to the PGVector store.                        |
| `APP_USER`, `APP_PASSWORD`             | `chatbot`        | User and password to restricted endpoints (e.g. `POST /files/process`) |
| `OPENAI_API_KEY`                       |                  | Required. Your API key to OpenAI for chat completions.                 |

