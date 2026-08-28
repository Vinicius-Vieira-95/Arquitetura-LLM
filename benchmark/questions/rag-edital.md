# Questionário — RAG / rag-spring-demo (50 perguntas)

Perguntas dentro do escopo do corpus indexado: o documento
`rag-spring-demo/src/main/resources/docs/Edital 2027.1.pdf`, o **Edital Nº
04/2026-CEV/UECE**, que trata da isenção da taxa de inscrição do Vestibular
2027.1 da UECE. Todas as perguntas abaixo têm resposta no texto do edital —
cada uma referencia o item de origem entre colchetes, para facilitar a
checagem manual de fidelidade (grounding) das respostas geradas.

Versao em texto puro (uma pergunta por linha, para uso automatizado pelo
harness) em `rag-edital.txt`.

> Lembrete: o `rag-spring-demo` não tem modo mock — o `/ask` sempre chama a
> API real da Anthropic. Rode com `ANTHROPIC_API_KEY` definido para usar este
> questionário.

## Categorias de isenção — 10 perguntas [itens 3 a 4.2]

1. Quais são as categorias de isenção da taxa de inscrição previstas no edital? [item 3]
2. O que caracteriza um candidato da Categoria C? [item 3.1]
3. Quantas doações de sangue são exigidas para a Categoria C, e em qual período? [item 8.1.2]
4. O que define a Categoria E1? [item 3.2]
5. Qual a diferença entre as categorias E1 e E2? [itens 3.2 e 3.3]
6. O que é exigido para um candidato se enquadrar na Categoria F1? [item 3.4]
7. O que é exigido para um candidato se enquadrar na Categoria F2? [item 3.5]
8. Qual o critério de renda para a Categoria G? [item 3.6]
9. Quais leis fundamentam o enquadramento na Categoria H (PcD)? [item 3.7]
10. Quantas isenções podem ser concedidas para candidatos das categorias E1, E2, F1 ou F2? [item 4]

## Documentação exigida — 10 perguntas [itens 8 a 8.7]

11. Qual documentação é exigida para a Categoria C (doador de sangue)? [item 8.1]
12. Qual documentação é exigida para a Categoria E1? [item 8.2]
13. Que documentos comprovam a trajetória escolar de um candidato da Categoria E2? [item 8.3]
14. Qual documentação é exigida para a Categoria F1? [item 8.4]
15. Qual documentação é exigida para a Categoria F2? [item 8.5]
16. Quais documentos comprovam a renda familiar para a Categoria G? [item 8.6]
17. Que tipo de laudo médico é exigido para a Categoria H? [item 8.7.2]
18. Existe um prazo de validade para o laudo médico da Categoria H? [item 8.7.2]
19. O Certificado de Conclusão do Ensino Médio substitui o Histórico Escolar? [item 8.2.2]
20. Em que formato os documentos devem ser enviados no processo de isenção? [item 7]

## Cronograma — 6 perguntas [item 5]

21. Quando começa e termina o período de solicitação de isenção da taxa de inscrição? [item 5.1/5.2]
22. Até quando pode ser enviada a documentação da isenção? [item 5.4/5.5]
23. Quando será divulgado o Resultado Preliminar da isenção? [item 5]
24. Qual o prazo para interpor recurso contra o indeferimento da isenção? [item 11]
25. Quando será divulgado o Resultado Definitivo da isenção? [item 5]
26. A partir de que horário o site é bloqueado no último dia de solicitação? [item 7.1]

## Vagas e cotas — 6 perguntas [itens 1 a 2.3]

27. Qual o percentual de vagas reservado para cotistas? [item 2]
28. Quais segmentos de concorrência compõem a política de cotas? [item 1]
29. Qual o percentual de vagas reservado para PcD entre as vagas não destinadas a cotas? [item 2.3]
30. Como são definidos os percentuais de cotistas pretos, pardos, indígenas e quilombolas? [item 2.1]
31. O que caracteriza um candidato Cotista Social? [item 2.2]
32. Quantos segmentos de concorrência existem no total? [item 1]

## Indeferimento — 6 perguntas [item 9]

33. Em quais situações a isenção não será concedida por problemas na documentação? [item 9.2]
34. Documentos com emendas ou rasuras são aceitos no processo de isenção? [item 9.2-d]
35. O que acontece se o histórico escolar enviado não estiver assinado? [item 9.2-g]
36. Um candidato que estudou em escola privada pode se enquadrar nas categorias E1, E2, F1 ou F2? [item 9.2-i]
37. Participar de programas sociais do governo garante a isenção da taxa de inscrição? [item 9.3]
38. O que acontece se o arquivo digital enviado estiver corrompido ou protegido por senha? [item 9.2-e]

## Recursos — 4 perguntas [itens 10 a 15]

39. Quem julga os recursos contra o indeferimento da isenção? [item 15]
40. O que não pode constar no texto de um recurso? [item 13]
41. Como deve ser apresentado o recurso contra o indeferimento? [item 11]
42. É possível apresentar recurso fora do prazo estabelecido? [item 14]

## Disposições finais e documentos de identificação — 8 perguntas [itens 16 a 28]

43. Quais documentos são aceitos como documento de identificação neste edital? [item 22]
44. A carteira de estudante é aceita como documento de identificação? [item 22.1-e]
45. O CPF pode ser usado como documento de identificação? [item 22.1-c]
46. A isenção concedida neste edital vale para outros vestibulares além do 2027.1? [item 16]
47. Obter a isenção garante automaticamente a inscrição no curso pretendido? [item 19]
48. É possível entregar a documentação da isenção presencialmente na sede da CEV/Uece? [item 24]
49. Quem pode ter a isenção negada por já estar cursando o ensino médio em determinada etapa? [item 26]
50. Quem resolve os casos omissos ou duvidosos deste edital? [item 28]

## O que observar ao rodar

- **Fidelidade (grounding)**: a resposta bate com o item do edital indicado
  entre colchetes? É o principal critério de qualidade em RAG — mais
  importante que "soa bem escrito".
- **Citações**: o `RagService` instrui o modelo a citar os trechos numerados
  (`[1]`, `[2]`...); verifique se a citação aponta para o chunk certo.
- **Chunks recuperados** (`retrieved` na resposta): com `rag.top-k` padrão em
  5, perguntas muito especificas podem não trazer o chunk certo entre os
  top-k se o chunking cortou a informação no meio — vale registrar esses
  casos como limitação do RAG "ingênuo" (TF-IDF + chunking por tamanho fixo).
- Este questionário é só de perguntas *dentro* do escopo do corpus. Para medir
  o comportamento de recusa (a instrução do `RagService` pede para dizer
  quando a informação não está no contexto), vale complementar depois com um
  pequeno lote de perguntas *fora* do escopo do edital.
