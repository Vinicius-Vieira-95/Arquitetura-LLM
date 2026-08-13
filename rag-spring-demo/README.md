# rag-spring-demo

Mini-RAG em **Spring Boot** que deixa visíveis os princípios de *Retrieval-Augmented Generation*:
**chunking → embeddings → busca por similaridade → prompt aumentado → geração** (via Anthropic API).

Roda com **apenas uma chave**: `ANTHROPIC_API_KEY`. O *embedder* é implementado do zero
(TF-IDF + cosseno), porque a Anthropic não oferece API de embeddings — e fica atrás de uma
interface, pronto pra ser trocado por um modelo semântico de verdade.

---

## As duas fases do RAG

**Indexação (offline, uma vez no startup)** — `CorpusIndexer`
```
docs/*.md ──► Chunker ──► Embedder.fit() ──► Embedder.embed() ──► InMemoryVectorStore
```

**Consulta (online, a cada pergunta)** — `RagService`
```
pergunta ─► embed ─► busca top-k (cosseno) ─► monta prompt c/ contexto ─► Anthropic ─► resposta
              RETRIEVE                              AUGMENT                  GENERATE
```

## Onde cada princípio mora no código

| Princípio do RAG            | Arquivo                                   |
|-----------------------------|-------------------------------------------|
| Dividir documentos (chunk)  | `ingest/Chunker.java`                     |
| Vetorizar texto (embeddings)| `embedding/Embedder.java` + `TfidfEmbedder.java` |
| Armazenar/buscar vetores    | `store/InMemoryVectorStore.java`          |
| Indexação ponta a ponta     | `ingest/CorpusIndexer.java`               |
| Recuperar + aumentar + gerar| `rag/RagService.java`                     |
| Chamar o LLM                | `llm/AnthropicClient.java`                |
| API HTTP                    | `web/RagController.java`                  |

## Como rodar

```bash
export ANTHROPIC_API_KEY="sua-chave-aqui"
mvn spring-boot:run
```

Perguntar:
```bash
curl -s http://localhost:8080/ask \
  -H "content-type: application/json" \
  -d '{"question": "Para que serve o endpoint de health do Actuator?"}'
```

Resposta (formato):
```json
{
  "question": "...",
  "answer": "O endpoint de health informa se a aplicacao esta saudavel... [1]",
  "retrieved": [
    { "chunk": { "id": "spring-boot-actuator.md#0", "source": "spring-boot-actuator.md", "text": "..." },
      "score": 0.68 }
  ]
}
```

O campo `retrieved` mostra exatamente quais trechos foram injetados no prompt e com que score —
útil pra entender (e depurar) por que o modelo respondeu daquele jeito.

## Configuração (`application.yml`)

| Propriedade            | Padrão                        | O que faz                          |
|------------------------|-------------------------------|------------------------------------|
| `anthropic.model`      | `claude-haiku-4-5`            | modelo de geração                  |
| `rag.top-k`            | `3`                           | quantos chunks recuperar           |
| `rag.chunk-max-chars`  | `600`                         | tamanho-alvo de cada chunk         |

## Limitação importante (de propósito)

O `TfidfEmbedder` faz busca **léxica**: casa palavras exatas, sem entender semântica. Sinônimos e
até singular/plural ("profile" vs "profiles") podem não casar. Isso é justamente o que motiva
embeddings **semânticos**. Trocar é trivial: crie uma classe que implemente `Embedder` chamando,
por exemplo, a Voyage AI (recomendada pela Anthropic) ou a OpenAI, e registre-a como bean. Nada
mais no projeto muda — `RagService`, `VectorStore` e `CorpusIndexer` continuam iguais.

## Próximos passos (se quiser evoluir)

- Trocar o embedder por um semântico (Voyage/OpenAI/modelo local via ONNX).
- Trocar o store em memória por pgvector / Qdrant (índice ANN: HNSW/IVF).
- Adicionar *overlap* entre chunks e *re-ranking* dos resultados.
- Comparar com **Function Calling** como padrão alternativo de integração (tema do seu TCC).

## Validação rápida da lógica de recuperação

O arquivo `RagCoreCheck.java` (na raiz) replica TF-IDF + cosseno isolados e pode ser rodado sem
Maven, só com o JDK 11+:
```bash
java RagCoreCheck.java
```
