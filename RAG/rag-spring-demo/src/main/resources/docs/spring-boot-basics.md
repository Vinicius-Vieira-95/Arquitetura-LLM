# Fundamentos do Spring Boot

O Spring Boot e um framework que facilita a criacao de aplicacoes Spring prontas para producao com o minimo de configuracao. Ele parte do principio de "convencao sobre configuracao": em vez de definir tudo manualmente, voce aceita padroes sensatos e so ajusta o que precisa mudar.

O coracao do Spring Boot e a auto-configuracao. Ao detectar bibliotecas no classpath, o framework configura automaticamente os beans correspondentes. Por exemplo, se o driver de um banco de dados esta presente, o Spring Boot tenta configurar uma fonte de dados automaticamente.

Os "starters" sao dependencias agregadoras que trazem, de uma vez, tudo o que e necessario para um determinado recurso. O starter spring-boot-starter-web, por exemplo, inclui o Spring MVC, o Jackson para serializacao JSON e um servidor Tomcat embutido.

Uma aplicacao Spring Boot tipica e empacotada como um JAR executavel com servidor embutido, o que permite roda-la com um simples comando java -jar, sem precisar instalar um servidor de aplicacao separado.
