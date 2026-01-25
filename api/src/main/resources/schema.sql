CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
	id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
	content TEXT,
	metadata JSON,
	embedding vector(1536)
);

CREATE INDEX ON vector_store USING HNSW (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS metadata_field (
  id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  name VARCHAR(255),
  description TEXT
);

CREATE TABLE IF NOT EXISTS metadata_allowed_value (
  id INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  metadata_field_id INT NOT NULL,
  value VARCHAR(255) NOT NULL,
  description TEXT
);
