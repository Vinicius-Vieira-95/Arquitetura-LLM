# Spring Data JPA

O Spring Data JPA reduz drasticamente a quantidade de codigo necessario para acessar dados relacionais. Em vez de escrever implementacoes de repositorio manualmente, voce declara uma interface e o Spring gera a implementacao em tempo de execucao.

A peca central e a interface de repositorio. Ao estender JpaRepository, uma interface ganha metodos prontos para operacoes comuns como salvar, buscar por identificador, listar todos e remover registros, sem nenhuma implementacao escrita a mao.

Os query methods permitem definir consultas apenas pelo nome do metodo. O Spring Data interpreta nomes como findByNomeAndAtivoTrue e gera a consulta correspondente automaticamente, seguindo uma convencao de nomenclatura.

Para consultas mais complexas, e possivel usar a anotacao Query com JPQL ou SQL nativo. O gerenciamento de transacoes e integrado: operacoes de escrita normalmente rodam dentro de uma transacao, garantindo consistencia.
