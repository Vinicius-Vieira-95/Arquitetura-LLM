# Análise comparativa dos 5 cenários (execução completa, 50+50 perguntas)

Dados brutos: [`comparison.md`](./results/comparison.md) (100 linhas: 50
perguntas de ferramentas x 4 cenários + 50 perguntas de RAG x 3 cenários).
Como `benchmark/results/` está no `.gitignore` (é saída regenerada a cada
rodada), esse arquivo bruto não fica versionado — só este resumo.

- **Data da execução**: 2026-09-03 22:46
- **Chamadas reais à API**: 350 (nenhuma falhou)
- **Modelo**: `claude-sonnet-5` (FC, ReAct, RAG+FC, RAG+ReAct) / `claude-haiku-4-5` (RAG puro, `rag-spring-demo`)

---

## 1. Seção Ferramentas — FC vs ReAct vs RAG+FC vs RAG+ReAct

50 perguntas: clima (13), calculadora (13), câmbio (12), conhecimento geral
sem ferramenta (12). RAG puro fica fora desta seção (não tem ferramentas).

### Latência média geral

| Cenário | Latência média |
|---|---|
| ReAct | 3515 ms |
| Function Calling | 3751 ms |
| RAG + ReAct | 3789 ms |
| RAG + Function Calling | 3950 ms |

Os 4 cenários ficam num intervalo estreito (~3500–4000 ms). Acrescentar RAG a
um ciclo de ferramentas custa pouco quando a pergunta não precisa de RAG:
**+199 ms** (ReAct → RAG+ReAct) e **+199 ms** (FC → RAG+FC) — a diferença é
só o overhead de publicar a spec de `search_documents` ao modelo, já que ela
não chega a ser chamada nessas perguntas.

### Latência média por categoria

| Categoria | FC | ReAct | RAG+FC | RAG+ReAct |
|---|---|---|---|---|
| Clima (n=13) | 3997 ms | 4269 ms | 4422 ms | 4276 ms |
| Calculadora (n=13) | 2883 ms | 3395 ms | 3287 ms | 3339 ms |
| Câmbio (n=12) | 3463 ms | 3606 ms | 3902 ms | 3912 ms |
| Sem ferramenta (n=12) | 4714 ms | **2739 ms** | 4207 ms | 3623 ms |

**Achado**: na categoria "sem ferramenta" (conhecimento geral, sem tool call),
o **ReAct é visivelmente mais rápido** que os outros 3 (~2,7s vs ~3,6–4,7s).
Hipótese: o ciclo ReAct manda uma única mensagem de texto livre e recebe uma
completion de texto; o Function Calling (FC e RAG+FC) sempre inclui o JSON
Schema completo das ferramentas no payload de request/response mesmo quando
nenhuma é chamada, o que pesa mais em tokens de entrada/processamento do que
o texto livre do ReAct.

### Iterações: todos os 4 convergem, com uma exceção

Em 49 das 50 perguntas, os 4 cenários usam o mesmo número de iterações (2 com
ferramenta, 1 sem). A exceção:

| Pergunta | FC | ReAct | RAG+FC | RAG+ReAct |
|---|---|---|---|---|
| Quanto é 144 / 12? | 2 it., `calculate` | 3 it., `calculate, calculate` | **1 it., nenhuma ferramenta** | 2 it., `calculate` |

- **ReAct gastou uma iteração a mais**, chamando `calculate` duas vezes
  (provável parsing/formatação do Action Input que não convenceu o modelo na
  primeira Observation).
- **RAG+FC respondeu de cabeça**, sem chamar `calculate` — para uma divisão
  exata (144/12=12) o modelo decidiu que não precisava da ferramenta. É um
  ponto relevante para a discussão de acurácia: nem sempre a ferramenta
  disponível é usada, mesmo em uma demo pensada para forçar seu uso.

---

## 2. Seção RAG — RAG vs RAG+FC vs RAG+ReAct

50 perguntas sobre o Edital 2027.1 (Uece), todas respondíveis a partir do
corpus indexado. FC/ReAct puros ficam fora desta seção (não têm RAG).

### Latência média geral

| Cenário | Latência média | Δ vs. RAG puro |
|---|---|---|
| RAG puro | 3037 ms | — |
| RAG + ReAct | 4813 ms | **+58%** |
| RAG + Function Calling | 5549 ms | **+83%** |

Diferença bem mais acentuada que na seção de ferramentas: aqui **RAG+FC
custa 83% mais latência** que RAG puro, e RAG+ReAct 58% mais.

### Por que a diferença é maior aqui: taxa de refinamento

De 50 perguntas, **7 (14%)** levaram tanto o RAG+FC quanto o RAG+ReAct a
chamar `search_documents` de novo para refinar a busca — as mesmas 7 para os
dois cenários (sinal de que a decisão de "o contexto inicial não basta" é
relativamente consistente entre os dois formatos de tool-calling):

| Pergunta | RAG (baseline) | RAG+FC | RAG+ReAct |
|---|---|---|---|
| Quais são as categorias de isenção da taxa de inscrição previstas no edital? | 1 it., 3846 ms | 3 it., **16519 ms** | 3 it., **14849 ms** |
| Qual documentação é exigida para a Categoria C (doador de sangue)? | 1 it., 3724 ms | 2 it., 8895 ms | 2 it., 9513 ms |
| Qual documentação é exigida para a Categoria F2? | 1 it., 3786 ms | 3 it., **11385 ms** | 3 it., **10123 ms** |
| Quando começa e termina o período de solicitação de isenção da taxa de inscrição? | 1 it., 2451 ms | 2 it., 7583 ms | 2 it., 6595 ms |
| Quais segmentos de concorrência compõem a política de cotas? | 1 it., 2930 ms | 2 it., 7867 ms | 2 it., 8023 ms |
| Quantos segmentos de concorrência existem no total? | 1 it., 2416 ms | 2 it., 7769 ms | 2 it., 7851 ms |
| Quem pode ter a isenção negada por já estar cursando o ensino médio em determinada etapa? | 1 it., 2727 ms | 2 it., 9317 ms | 2 it., **12856 ms** |

Padrão: perguntas **amplas/agregadoras** ("quais são **todas** as
categorias...", "**quantos** segmentos existem no total") são as que mais
disparam refinamento — faz sentido, já que top-5 chunks por similaridade de
cosseno tende a não cobrir uma pergunta que exige juntar informação
espalhada por várias seções do edital.

Nas outras 43 perguntas (86%), RAG+FC e RAG+ReAct tiveram 1 iteração só —
mesmo comportamento do RAG puro em conteúdo, só mais lentos pelo overhead de
publicar a spec de `search_documents` (e as demais ferramentas do registry)
ao modelo mesmo sem usá-la.

---

## 3. Síntese para o capítulo de resultados

1. **Ferramentas sem RAG**: acrescentar `search_documents` ao registry custa
   pouco (~200 ms) quando a pergunta não precisa dele — o overhead é
   majoritariamente de payload, não de raciocínio extra do modelo.
2. **RAG com ferramentas**: acrescentar Function Calling/ReAct a um ciclo de
   RAG custa bem mais (58–83%) porque, diferente do caso anterior, o modelo
   **de fato usa** a ferramenta extra em 14% das perguntas — e quando usa,
   paga o preço de 1–2 idas a mais ao modelo (latências de 7–16s vs. ~3s do
   RAG puro).
3. **Trade-off central**: RAG puro é mais rápido e suficiente para 86% das
   perguntas testadas; RAG+FC/RAG+ReAct trocam latência por uma chance de
   recuperar contexto melhor em perguntas amplas/agregadoras — exatamente o
   tipo de pergunta onde top-k fixo por similaridade de cosseno tende a
   falhar.
4. **FC vs ReAct** (estrutural, não latência): convergem em iterações/ações
   escolhidas em quase todas as perguntas, com uma divergência pontual (144/12)
   que ilustra os dois modos de falha diferentes de cada arquitetura — ReAct
   repetindo uma Action, Function Calling pulando a ferramenta.
5. **Ferramenta sem uso** (144/12, RAG+FC): mesmo com a ferramenta disponível
   e apropriada, o modelo pode optar por responder "de cabeça" — vale
   mencionar como limite observado, não hipotético, da abordagem.

---

## 4. Limitações desta execução

- Uma única rodada por pergunta (sem repetição) — as latências são pontos,
  não médias com desvio-padrão. Para o capítulo de resultados, considere
  rodar N vezes e reportar P50/P95.
- RAG puro usa `claude-haiku-4-5` (padrão do `rag-spring-demo`), enquanto os
  outros 4 usam `claude-sonnet-5` — a comparação de latência entre RAG puro e
  os demais não é apples-to-apples por causa do modelo, só a comparação RAG
  vs. RAG+FC vs. RAG+ReAct (mesmo modelo nos 3) é.
- Sem medição de tokens/custo em USD — só latência e contagem de
  iterações/ações. `comparison.md` tem a resposta completa de cada
  pergunta, então dá para reprocessar e extrair outras métricas depois.
