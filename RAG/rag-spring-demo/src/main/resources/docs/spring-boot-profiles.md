# Profiles e Configuracao no Spring Boot

Os profiles do Spring permitem manter configuracoes diferentes para ambientes diferentes, como desenvolvimento, homologacao e producao. Um bean ou um arquivo de configuracao pode ser associado a um profile especifico e so entra em vigor quando aquele profile esta ativo.

Para ativar um profile, define-se a propriedade spring.profiles.active, seja via variavel de ambiente, argumento de linha de comando ou arquivo de configuracao. E possivel ativar mais de um profile ao mesmo tempo.

A configuracao externalizada e um principio central: os valores que mudam entre ambientes ficam fora do codigo. O Spring Boot le configuracoes de varias fontes seguindo uma ordem de precedencia, incluindo arquivos application.properties ou application.yml, variaveis de ambiente e argumentos de linha de comando.

Arquivos especificos de profile, como application-prod.yml, sobrescrevem os valores do arquivo base quando o profile correspondente esta ativo. Isso evita duplicar configuracao e mantem cada ambiente isolado.
