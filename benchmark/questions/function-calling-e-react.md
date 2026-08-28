# Questionário — Function Calling vs ReAct (50 perguntas)

Perguntas dentro do escopo comum aos dois projetos: eles compartilham
exatamente as mesmas ferramentas (`get_weather`, `calculate`,
`convert_currency`) e o mesmo comportamento de responder direto quando
nenhuma ferramenta e' necessaria. Por isso o questionario e' unico — rodar as
mesmas 50 perguntas nos dois da' uma comparacao direta de iteracoes, latencia
e formato de interacao (JSON estruturado vs. texto livre parseado).

Versao em texto puro (uma pergunta por linha, para uso automatizado pelo
harness) em `function-calling-e-react.txt`.

## Clima (`get_weather`) — 13 perguntas

1. Qual o clima em Fortaleza?
2. Qual a temperatura em São Paulo agora?
3. Como está o tempo em Recife?
4. Está chovendo em Manaus?
5. Qual o clima em Salvador?
6. Quantos graus está fazendo em Belo Horizonte?
7. Como está o tempo em Curitiba hoje?
8. Qual a previsão do tempo para Brasília?
9. Está fazendo frio em Porto Alegre?
10. Qual o clima em Lisboa?
11. Como está o tempo em Nova York?
12. Qual a temperatura em Tóquio?
13. Está ensolarado em Natal?

## Calculadora (`calculate`) — 13 perguntas

14. Quanto é (12 + 7) * 3?
15. Quanto é 25 * 4 - 10?
16. Quanto é (100 - 40) / 3?
17. Quanto é 2 + 3 * 4?
18. Quanto é -10 + 5?
19. Quanto é 9 * (3 + 2) - 7?
20. Quanto é 144 / 12?
21. Quanto é (8 + 2) * (3 - 1)?
22. Quanto é 7.5 + 2.3?
23. Quanto é 1000 / (4 * 5)?
24. Quanto é 3 * 3 * 3?
25. Quanto é (50 - 20) / (2 + 3)?
26. Quanto é 18 + 24 - 6?

## Câmbio (`convert_currency`) — 12 perguntas

27. Converta 100 dólares em reais.
28. Quanto é 50 euros em reais?
29. Converta 200 reais em dólares.
30. Quanto vale 1000 reais em euros?
31. Converta 75 dólares em euros.
32. Quanto é 500 euros em dólares?
33. Converta 30 reais em dólares.
34. Quanto vale 250 dólares em reais?
35. Converta 10 euros em reais.
36. Quanto é 1500 reais em dólares?
37. Converta 60 dólares em reais.
38. Quanto vale 400 euros em reais?

## Conhecimento geral (sem ferramenta) — 12 perguntas

39. Quem descobriu o Brasil?
40. Qual é a capital da França?
41. Quem escreveu Dom Casmurro?
42. Em que ano começou a Segunda Guerra Mundial?
43. Qual é o maior planeta do sistema solar?
44. Quantos continentes existem?
45. Quem pintou a Mona Lisa?
46. Qual é o rio mais longo do mundo?
47. O que é fotossíntese?
48. Quem foi Albert Einstein?
49. Qual é a fórmula química da água?
50. Quantos ossos tem o corpo humano adulto?

## O que observar ao rodar

- **Iterações**: perguntas com ferramenta devem fechar em 2 (uma para pedir a
  ferramenta, uma para responder com o resultado); sem ferramenta, em 1.
- **Ferramenta/ação escolhida**: confirma se o roteamento (JSON schema no FC,
  parsing de texto no ReAct) acertou a ferramenta certa.
- **Divergências entre FC e ReAct na mesma pergunta** são o dado mais
  interessante para o TCC — por exemplo, o ReAct pode falhar em extrair um
  `Action Input` bem formatado onde o FC não erra (ou vice-versa), já que um
  depende de JSON Schema validado e o outro de regex sobre texto livre.
- No modo `mock`, os dois devem se comportar de forma praticamente idêntica
  (o roteamento por palavra-chave é equivalente nos dois `MockLlmClient`); as
  diferenças relevantes aparecem no modo `anthropic`, com o modelo de verdade.
