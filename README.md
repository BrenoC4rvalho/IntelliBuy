# IntelliBuy - E-commerce Platform with AI Product Assistant

IntelliBuy is a Spring Boot-based e-commerce platform that includes a powerful AI-driven product assistant. This assistant leverages PostgreSQL with `pgvector` for similarity search and Ollama for embedding generation and natural language processing, allowing users to ask questions about products in natural language.

## Table of Contents

- [Features](#features)
- [Technologies Used](#technologies-used)
- [Prerequisites](#prerequisites)
- [Setup and Installation](#setup-and-installation)
    - [1. Database Setup (Docker Compose)](#1-database-setup-docker-compose)
    - [2. Ollama Setup](#2-ollama-setup)
    - [3. Spring Boot Application Configuration](#3-spring-boot-application-configuration)
    - [4. Running the Application](#4-running-the-application)
- [API Endpoints](#api-endpoints)
- [AI Product Assistant Usage](#ai-product-assistant-usage)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [License](#license)

## Features

* **Product Management:** CRUD operations for products (name, description, price, stock).
* **Customer Management:** CRUD operations for customer data.
* **Purchase Management:** Handle customer purchases and purchase items.
* **AI Product Assistant:**
    * Generates embeddings for product data (name, description, price).
    * Stores embeddings in PostgreSQL using `pgvector` for efficient similarity search.
    * Processes natural language queries from users.
    * Retrieves most relevant products based on query similarity.
    * Generates accurate answers using an Ollama LLM, strictly based on the retrieved product information.

## Technologies Used

* **Spring Boot:** Framework for building robust Java applications.
* **Spring Data JPA:** For database interaction and persistence.
* **PostgreSQL:** Relational database.
* **pgvector:** PostgreSQL extension for efficient similarity search on vector embeddings.
* **Ollama:** Local large language model (LLM) serving for:
    * **Embedding Generation:** Transforming text into numerical vectors.
    * **Text Generation:** Generating natural language responses.
* **Lombok:** Reduces boilerplate code in Java.
* **Docker & Docker Compose:** For easy setup and management of the PostgreSQL database.

## Prerequisites

Before you begin, ensure you have the following installed:

* **Java 17+**
* **Maven**
* **Docker & Docker Compose**
* **Ollama**: Download and install Ollama from [ollama.com](https://ollama.com/).

## Setup and Installation

Follow these steps to get IntelliBuy up and running.

### 1. Database Setup (Docker Compose)

The PostgreSQL database with `pgvector` is managed via Docker Compose.

1.  **Create `docker-compose.yml`**:
    In the root directory of your project, create a file named `docker-compose.yml` with the following content:

    ```yaml
    version:

docker compose exec db psql -U your_db_user -d intellibuy_db
-- Dentro do psql:
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE IF NOT EXISTS product_embeddings_ai (
id VARCHAR(255) PRIMARY KEY,
content TEXT,
metadata JSONB,
embedding VECTOR(768) -- Ajuste esta dimensão!
);
\q