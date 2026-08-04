package com.example.ragdemo.ingest;

import com.example.ragdemo.store.Chunk;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Divide um documento em chunks.
 *
 * <p>Estrategia: quebra por paragrafos (linhas em branco) e agrupa paragrafos
 * consecutivos ate atingir um tamanho-alvo em caracteres. E uma estrategia
 * "consciente de estrutura": evita cortar no meio de uma frase. Variacoes comuns
 * (overlap entre chunks, split por tokens, por cabecalhos markdown) entrariam aqui.
 *
 * <p>O chunking importa muito no RAG: chunks grandes demais diluem a relevancia;
 * pequenos demais perdem contexto.
 */
@Component
public class Chunker {

    private final int maxChars;

    public Chunker(@Value("${rag.chunk-max-chars:600}") int maxChars) {
        this.maxChars = maxChars;
    }

    public List<Chunk> chunk(String source, String text) {
        List<Chunk> chunks = new ArrayList<>();
        String[] paragraphs = text.strip().split("\\n\\s*\\n");

        StringBuilder buffer = new StringBuilder();
        int index = 0;
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Se adicionar este paragrafo estourar o alvo e ja houver conteudo, fecha o chunk atual.
            if (buffer.length() > 0 && buffer.length() + trimmed.length() > maxChars) {
                chunks.add(new Chunk(source + "#" + index++, source, buffer.toString().strip()));
                buffer.setLength(0);
            }
            if (buffer.length() > 0) {
                buffer.append("\n\n");
            }
            buffer.append(trimmed);
        }
        if (buffer.length() > 0) {
            chunks.add(new Chunk(source + "#" + index, source, buffer.toString().strip()));
        }
        return chunks;
    }
}
