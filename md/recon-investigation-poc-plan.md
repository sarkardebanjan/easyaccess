# Recon Investigation Assistant — Proof of Concept
## Design and Delivery Plan

| Field | Value |
|---|---|
| Document type | POC design and delivery plan |
| Status | Draft for review |
| Audience | Engineering, Architecture, Delivery Management |
| Date | 08 August 2026 |

---

## 1. Executive Summary

This document defines the design, dependency set, constraints and delivery estimate for a Proof of Concept (POC) that enables an engineer to investigate execution reconciliation breaks through natural language.

The POC connects an existing in-house reconciliation service and a corpus of internal policy and operations documentation to a Large Language Model (LLM), using the Model Context Protocol (MCP). The engineer poses a question in the GitHub Copilot chat panel inside their IDE. The LLM determines which of two available tools to invoke, retrieves grounded information, and returns a natural-language answer.

The POC deliberately excludes the end-state user interface and the LLM chat client. GitHub Copilot, already licensed and deployed within the organisation, serves as the interim client. This removes the need for an external LLM API key and materially reduces time to a demonstrable outcome.

Two environmental constraints shape the design. The target host runs Red Hat Enterprise Linux 8, and container runtimes are not permitted under current organisational policy. The design responds to both by eliminating the external vector database server entirely: the knowledge base is held in-process by the application and distributed as a build artifact. This is a deliberate POC-scope simplification with a defined upgrade path, described in Section 8.

Estimated delivery effort is **35 working days for a single developer**, equivalent to approximately **seven to eight calendar weeks**, inclusive of development, testing and documentation.

---

## 2. Scope

### 2.1 In Scope

- A Spring Boot MCP server exposing two tools to the LLM.
- A retrieval-augmented generation (RAG) tool performing semantic search over internal policy and operations documentation.
- An application tool invoking the existing reconciliation REST service.
- An offline ingestion utility that converts source PDF documentation into a searchable knowledge base artifact.
- Deployment onto a RHEL 8 host without container runtimes.
- Functional testing, retrieval quality evaluation, technical documentation and a management-facing summary.

### 2.2 Out of Scope

The following are explicitly deferred and form part of the subsequent product phase:

- The end-state application user interface.
- A dedicated LLM chat client and associated API key management.
- Authentication, authorisation and multi-tenancy on the MCP server.
- High availability, horizontal scaling and disaster recovery.
- Production-grade observability and alerting.
- Migration to Apache Lucene (planned; see Section 8).

---

## 3. Architecture

### 3.1 Component Overview

```
   Engineer
      |
      | natural-language question + execution identifier
      v
   GitHub Copilot Chat  (VS Code or IntelliJ IDEA, agent mode)
      |
      | MCP protocol over STDIO
      v
   Recon MCP Server  (Spring Boot, Java)
      |
      +---- search_knowledge_base ------> SimpleVectorStore (in-process, heap-resident)
      |                                        ^
      |                                        | loaded at startup
      |                                   policy-kb.json  (file on disk)
      |
      +---- investigate_execution_break --> Recon REST Service (Spring Boot, existing)
```

An offline ingestion utility produces `policy-kb.json` from source PDF documentation. It runs independently of the live query path and is executed only when source documentation changes.

### 3.2 Runtime Sequence

1. The engineer submits a question in the Copilot chat panel.
2. GitHub Copilot launches the MCP server as a subprocess and communicates over standard input and output.
3. The LLM evaluates the question against the published tool descriptions and selects one or both tools.
4. `search_knowledge_base` performs a cosine similarity search against the in-memory vector store and returns the most relevant documentation excerpts, each attributed to a source document and page.
5. `investigate_execution_break` issues an HTTP request to the reconciliation REST service and returns structured investigation data for the supplied execution identifier.
6. The LLM synthesises the tool results into a natural-language response.

### 3.3 Design Decisions

**Transport: STDIO.** The MCP server is launched by the IDE as a subprocess. This avoids network port allocation, firewall exceptions, TLS provisioning and authentication for the POC. It also avoids a known defect in the GitHub Copilot IntelliJ plugin whereby custom HTTP headers configured in `mcp.json` are not forwarded to the server, which would impair any header-based authentication over HTTP transport.

**Tool count: two, with distinct responsibilities.** Tool descriptions are the sole routing mechanism available to the LLM; no external classifier is used. The two tools are therefore scoped so that their descriptions are unambiguous — one addresses documented policy and procedure, the other addresses live state for a specific execution. Refining these descriptions is a recognised development activity, not an afterthought (see Section 6, Phase 5).

**Vector store: in-process, file-backed.** Described in Section 4.

**Embedding model: local ONNX inference.** Embeddings are computed in-process using a locally executed ONNX model. No external API call is made and no API key is required. This satisfies data residency expectations for internal policy documentation.

---

## 4. Response to Environmental Constraints

### 4.1 Constraint: Container Runtimes Unavailable

The reference architecture for this class of system uses ChromaDB, a vector database, deployed as a container alongside the application. Organisational policy does not permit container runtimes, which removes the standard deployment path.

### 4.2 Constraint: RHEL 8 Platform

RHEL 8 ships SQLite 3.26. ChromaDB's Python distribution requires SQLite 3.35 or later and fails at import time on this platform. The system SQLite library cannot be upgraded through supported channels without affecting other packages that link against it. Available workarounds — installing a bundled SQLite replacement and patching the library at runtime, or installing a standalone Rust binary distribution — are viable but introduce an additional installed service, an additional operational surface, and an installation procedure that is difficult to reproduce reliably in a restricted network environment.

### 4.3 Resolution

For the POC, the external vector database is removed from the architecture. The knowledge base is held in-process using Spring AI's `SimpleVectorStore`, an implementation of the framework's standard `VectorStore` interface that stores vectors in a concurrent map, performs exhaustive cosine similarity search, and serialises to and from a JSON file.

The consequences are favourable at POC scale:

- No service to install, configure, monitor or restart on the RHEL 8 host.
- The knowledge base becomes a build artifact. It can be generated on a machine with network access and transferred to the target host by file copy, requiring no installation on the target at all.
- Exhaustive search yields complete recall, whereas approximate nearest-neighbour indexes trade recall for speed.

The material limitation is that search cost grows linearly with corpus size and the entire corpus is resident in heap memory. At the anticipated POC corpus size this is not a constraint; at product scale it becomes one. Section 8 addresses the transition.

The vendor documents `SimpleVectorStore` as unsuitable for production use. Its adoption here is scoped to the POC and is not a recommendation for the product build.

---

## 5. Dependencies

### 5.1 Software and Tooling

| Dependency | Version / Detail | Purpose |
|---|---|---|
| GitHub Copilot | Current plugin, agent mode enabled | LLM chat client and MCP host. Licences already held. |
| VS Code or IntelliJ IDEA | Current release | Development environment and Copilot host |
| Java Development Kit | 21 LTS | Runtime and build |
| Apache Maven | 3.9.x | Build and dependency management |
| Spring Boot | 3.4.x | Application framework |
| Spring AI | 1.1.x GA, pinned via BOM | AI abstractions, MCP server, vector store |
| Red Hat Enterprise Linux | 8.x | Target deployment host |

### 5.2 Spring AI Modules

| Artifact | Purpose |
|---|---|
| `spring-ai-starter-mcp-server` | MCP server with STDIO transport; annotation-driven tool registration |
| `spring-ai-vector-store` | `VectorStore` interface and `SimpleVectorStore` implementation |
| `spring-ai-starter-model-transformers` | Local ONNX embedding inference |
| `spring-ai-pdf-document-reader` | PDF text extraction (Apache PDFBox) |

### 5.3 Transitive and Data Dependencies

| Dependency | Detail |
|---|---|
| ONNX Runtime (Java) | Resolved transitively; executes the embedding model |
| `all-MiniLM-L6-v2` model artifacts | Approximately 90 MB. Model and tokeniser files must be available offline. |
| Apache PDFBox | Resolved transitively via the PDF document reader |

### 5.4 External Systems

| System | Detail |
|---|---|
| Reconciliation REST service | Existing Spring Boot service. Assumed to expose, or to be extended to expose, an endpoint returning investigation data for a supplied execution identifier. |
| Internal artifact repository | Must mirror Spring AI and ONNX Runtime artifacts. See Section 7, Risk R1. |

### 5.5 Testing

| Dependency | Purpose |
|---|---|
| JUnit 5, Spring Boot Test | Unit and integration testing |
| WireMock or equivalent | Stubbing the reconciliation REST service |

---

## 6. Sizing and Resource Requirements

### 6.1 Corpus Assumptions

The estimates below assume ten source PDF documents averaging fifty pages, predominantly text.

| Measure | Estimate |
|---|---|
| Total pages | 500 |
| Approximate token count | 300,000 |
| Resulting chunks (220-token chunks, 60-token overlap) | 1,300 – 1,800 |
| Knowledge base artifact size (`policy-kb.json`) | 10 – 20 MB |

Chunk size is constrained by the embedding model, which accepts a maximum sequence length of 256 tokens and silently truncates longer input. Chunk configuration is set below this threshold to prevent undetected content loss.

### 6.2 Processing Time

| Operation | Estimate | Frequency |
|---|---|---|
| PDF text extraction (500 pages) | 5 – 20 seconds | Per ingestion run |
| Embedding generation (~1,500 chunks, CPU) | 15 – 45 seconds | Per ingestion run |
| Serialisation to artifact | 1 – 2 seconds | Per ingestion run |
| **Total ingestion** | **Under two minutes** | Only when documentation changes |
| Knowledge base load at server startup | 1 – 3 seconds | Every server start |
| ONNX model initialisation at startup | 2 – 4 seconds | Every server start |
| Similarity search per query | Under 10 milliseconds | Per tool invocation |

Server startup is incurred each time GitHub Copilot initiates an agent session, as the MCP server runs as a subprocess under STDIO transport. A cold start of five to seven seconds is expected and acceptable.

### 6.3 Hardware

| Resource | Minimum | Recommended |
|---|---|---|
| CPU | 2 vCPU | 4 vCPU — embedding generation is CPU-bound |
| RAM | 8 GB | 16 GB |
| JVM heap (MCP server) | `-Xmx1g` | `-Xmx2g` |
| JVM heap (ingestion run) | `-Xmx2g` | `-Xmx4g` |
| Disk | 2 GB | 5 GB |

Memory consumption is modest at POC scale. The knowledge base occupies a low single-digit number of megabytes in heap; the ONNX model and runtime account for the majority of the footprint. Memory scales linearly with corpus size, and this is the primary technical driver for the Lucene migration described in Section 8.

---

## 7. Delivery Plan and Effort Estimate

### 7.1 Estimating Basis

- One developer allocated full-time.
- Five productive hours per developer-day; three hours per day reserved for organisational commitments and meetings.
- Twenty working days per month.
- Estimates include development, testing and documentation.
- A twenty per cent contingency is applied, reflecting the restricted network environment and the exploratory nature of tool-routing behaviour.

### 7.2 Work Breakdown

| Phase | Description | Days | Hours |
|---|---|---:|---:|
| 0 | **Environment and access.** JDK and build tooling on RHEL 8. Verification that Spring AI and ONNX artifacts resolve through the internal repository. Offline provisioning of embedding model files. Copilot agent mode and MCP configuration confirmed. | 4 | 20 |
| 1 | **Connectivity spike.** Minimal MCP server with a single trivial tool, registered in Copilot and successfully invoked end to end. Establishes that the full loop functions before further investment. | 3 | 15 |
| 2 | **Ingestion pipeline.** PDF reading, chunking, embedding, artifact generation. Source document metadata attribution. Repeatable execution procedure. | 4 | 20 |
| 3 | **Knowledge base tool.** `search_knowledge_base` implementation, result formatting with source attribution, similarity threshold gating. | 3 | 15 |
| 4 | **Application tool.** `investigate_execution_break` implementation, REST client, response mapping, error handling. | 3 | 15 |
| 5 | **Routing and retrieval tuning.** Iterative refinement of tool descriptions to achieve reliable tool selection. Chunking and threshold adjustment against a curated question set. | 5 | 25 |
| 6 | **Testing and hardening.** Unit and integration tests. Retrieval quality evaluation against expected answers. Startup, logging and failure-mode handling under STDIO transport. | 4 | 20 |
| 7 | **Documentation and presentation.** Technical documentation, operational runbook, management summary. | 3 | 15 |
| | **Subtotal** | **29** | **145** |
| | Contingency (20%) | 6 | 30 |
| | **Total** | **35** | **175** |

### 7.3 Summary

**35 working days — approximately seven to eight calendar weeks — for one developer at full allocation.**

Phase 1 is the critical early milestone. If the Copilot-to-MCP loop cannot be established, the delivery approach requires reassessment before further effort is committed. This phase is deliberately sequenced early and kept minimal for that reason.

Phase 5 carries the widest variance. Tool selection accuracy depends on how the LLM interprets tool descriptions, and convergence is empirical rather than deterministic. The five-day allocation reflects experience that this activity is consistently underestimated.

---

## 8. Risks and Mitigations

| Ref | Risk | Impact | Mitigation |
|---|---|---|---|
| R1 | Spring AI artifacts or the ONNX embedding model are unavailable through the internal artifact repository. | High — blocks all development | Verify in Phase 0, before any other work. Raise mirroring requests immediately. Model files may alternatively be provisioned as a controlled internal artifact. |
| R2 | The Copilot MCP integration behaves differently from documented behaviour in the organisation's plugin version. | High — invalidates the client approach | Phase 1 connectivity spike is designed to surface this within the first week. |
| R3 | Source PDFs convey material information through diagrams and images rather than text. | Medium — silent retrieval gaps | Text extraction does not process images. Assess source documents during Phase 2. Where diagrams carry procedural content, transcribe the affected sections manually for the POC. |
| R4 | Tool selection proves unreliable for compound questions. | Medium — degrades demonstrated value | Allocated iteration time in Phase 5. Fallback is to narrow the demonstrated question set to well-characterised categories. |
| R5 | STDIO transport requires that no output is written to the console, as this corrupts protocol messages. | Medium — intermittent, hard-to-diagnose failures | Configure file-based logging and suppress console output at project inception. Documented in the runbook. |
| R6 | The reconciliation REST service does not expose a suitable endpoint. | Medium — schedule impact | Confirm endpoint availability during Phase 0. Effort to extend the service is not included in this estimate. |

---

## 9. Path to Product

The POC establishes the interaction pattern and validates the business case. Three changes are anticipated for the product build.

**Vector store migration to Apache Lucene.** `SimpleVectorStore` holds the entire corpus in heap and searches it exhaustively; both properties scale linearly and become limiting as documentation coverage grows. Apache Lucene provides Hierarchical Navigable Small World indexing for approximate nearest-neighbour search, is memory-mapped rather than heap-resident, and requires no server process — preserving the deployment simplicity that the container restriction demands. Lucene additionally provides lexical search over the same index, enabling hybrid retrieval. This is materially valuable for reconciliation documentation, where queries frequently reference exact identifiers such as status and error codes that semantic similarity alone matches poorly.

Because both implementations sit behind Spring AI's `VectorStore` interface, the migration is confined to a single component. Tool implementations are unaffected.

**Client replacement.** GitHub Copilot is an interim client. The product introduces the application user interface and a dedicated LLM chat client authenticating to a model endpoint with a managed API key. The MCP server is expected to move from STDIO to HTTP transport at this point, requiring authentication and network controls.

**Operational hardening.** Authentication, authorisation, audit logging, observability and a defined re-ingestion trigger for source documentation are required for production deployment and are out of POC scope.

---

## 10. Success Criteria

The POC is considered successful when all of the following are demonstrated:

1. An engineer poses a policy question in Copilot chat and receives an accurate answer attributed to the correct source document and page.
2. An engineer poses an investigation question with an execution identifier and receives an answer grounded in live data from the reconciliation service.
3. A compound question causes both tools to be invoked within a single interaction, with results correctly combined.
4. The knowledge base is regenerated from updated source documentation and deployed to the RHEL 8 host without installing additional services.
5. Technical documentation and an operational runbook are complete and independently reproducible.

---

## Appendix A — Configuration Reference

**MCP server, STDIO transport**

```properties
spring.ai.mcp.server.stdio=true
spring.ai.mcp.server.name=recon-mcp
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.file.name=/var/log/recon-mcp/recon-mcp.log
logging.pattern.console=
kb.file=/var/lib/recon-mcp/policy-kb.json
```

**Copilot MCP registration** (`mcp.json`)

```json
{
  "servers": {
    "recon-mcp": {
      "type": "stdio",
      "command": "/usr/lib/jvm/java-21/bin/java",
      "args": ["-Xmx2g", "-jar", "/opt/recon-mcp/recon-mcp.jar"]
    }
  }
}
```

Absolute paths are required; the plugin does not reliably resolve executables from the shell environment.

**Ingestion execution**

```
java -Xmx4g -jar recon-mcp.jar \
     --spring.profiles.active=ingest \
     --kb.file=/var/lib/recon-mcp/policy-kb.json \
     /path/to/policy/*.pdf
```

---

## Appendix B — Glossary

| Term | Definition |
|---|---|
| MCP | Model Context Protocol. An open standard by which an LLM client discovers and invokes external tools. |
| RAG | Retrieval-Augmented Generation. Supplying an LLM with retrieved source material so that answers are grounded in authoritative content rather than model recall. |
| Embedding | A numerical vector representation of text, enabling comparison by semantic similarity. |
| Chunk | A segment of a source document sized for embedding and retrieval. |
| STDIO transport | An MCP communication mode in which the client launches the server as a subprocess and exchanges messages over standard input and output. |
| HNSW | Hierarchical Navigable Small World. A graph-based approximate nearest-neighbour search algorithm. |
