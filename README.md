<p align="center">
  <img src="src/main/resources/icons/icon-128.png" width="120" alt="Logo do Revolkov Vault">
</p>

<h1 align="center">REVOLKOV VAULT</h1>

<p align="center">
  Cofre local de credenciais e gerador de senhas desenvolvido em Java com JavaFX.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/JavaFX-21.0.2-2C54A3?logo=java&logoColor=white" alt="JavaFX 21.0.2">
  <img src="https://img.shields.io/badge/Maven-Build-C71A36?logo=apachemaven&logoColor=white" alt="Maven">
  <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-3AA8FF" alt="Status do projeto">
</p>

## Sobre o projeto

O **Revolkov Vault** é uma aplicação desktop criada para gerar, organizar e consultar credenciais armazenadas localmente.

O projeto nasceu como um gerador de senhas e evoluiu para um pequeno cofre de credenciais com identidade visual própria, busca, edição, avaliação de força e proteção da visualização por senha mestre.

Este é um projeto de portfólio desenvolvido com foco em:

- programação orientada a objetos;
- separação de responsabilidades;
- construção de interfaces com JavaFX;
- persistência local de dados;
- validação e feedback ao usuário;
- aplicação de conceitos iniciais de segurança.

## Funcionalidades

- Geração de senhas com `SecureRandom`.
- Escolha entre letras maiúsculas, minúsculas, números e símbolos.
- Tamanho configurável entre 4 e 64 caracteres.
- Indicador visual de força da senha.
- Cadastro de serviço, conta e senha.
- Edição e exclusão de credenciais.
- Confirmação antes de operações destrutivas.
- Busca de credenciais pelo nome do serviço.
- Senhas mascaradas na tabela.
- Exibição de senha condicionada à validação da senha mestre.
- Recuperação das preferências de geração utilizadas anteriormente para cada serviço.
- Tema escuro responsivo com identidade visual própria.
- Ícones adaptados para diferentes tamanhos de janela e sistema operacional.

## Tecnologias utilizadas

- **Java 21**
- **JavaFX 21.0.2**
- **Maven**
- **JavaFX CSS**
- **PBKDF2WithHmacSHA256** para derivação do hash da senha mestre
- **SecureRandom** para geração de senhas e salt

## Estrutura do projeto

```text
src/main/java/org/example/
├── Launcher.java
├── MainApp.java
├── model/
│   └── PasswordEntry.java
└── service/
    ├── MasterPasswordService.java
    ├── PasswordGenerator.java
    ├── PasswordStorage.java
    └── PasswordStrengthEvaluator.java

src/main/resources/
├── fonts/
├── icons/
└── style.css
```

### Responsabilidades principais

- `MainApp`: monta a interface e coordena as ações do usuário.
- `PasswordEntry`: representa uma credencial salva.
- `PasswordGenerator`: gera senhas de acordo com as opções selecionadas.
- `PasswordStrengthEvaluator`: classifica a força da senha por tamanho e variedade de caracteres.
- `PasswordStorage`: realiza a leitura e a escrita das credenciais no armazenamento local.
- `MasterPasswordService`: cadastra e valida a senha mestre por meio de hash com salt.

## Como executar

### Pré-requisitos

- JDK 21 instalado.
- Maven instalado e disponível no terminal.
- Git instalado para clonar o repositório.

### Clonando o projeto

```bash
git clone https://github.com/AlexanderRevoredo/Revolkov-vault.git
cd Revolkov-vault
```

### Executando com Maven

```bash
mvn clean javafx:run
```

Na primeira execução, o aplicativo solicitará a criação de uma senha mestre.

Ela será exigida sempre que o usuário tentar revelar uma credencial salva.

## Armazenamento local

Os dados são armazenados fora da pasta do projeto, no diretório pessoal do usuário:

```text
~/password-manager/
├── master.key
└── passwords.txt
```

No Windows, o caminho normalmente corresponde a:

```text
C:\Users\<usuario>\password-manager\
```

- `master.key` armazena o salt e o hash derivado da senha mestre.
- `passwords.txt` armazena as credenciais e as preferências de geração.

## Segurança

A senha mestre **não é salva em texto puro**.

O projeto utiliza:

- PBKDF2 com HMAC-SHA-256;
- salt aleatório de 16 bytes;
- 65.536 iterações;
- chave derivada de 256 bits;
- comparação de hashes com `MessageDigest.isEqual`.

A geração de senhas utiliza `SecureRandom`, adequado para valores que não devem ser facilmente previsíveis.

> [!WARNING]
> Nesta versão, as credenciais ainda são gravadas em texto puro no arquivo `passwords.txt`.
>
> A senha mestre protege a visualização dentro da interface, mas não criptografa o arquivo armazenado no disco.
>
> Por isso, a versão atual deve ser considerada educacional e de portfólio, não um gerenciador de senhas pronto para uso com credenciais reais.

## Roadmap

- [ ] Criptografar o cofre local com AES-GCM.
- [ ] Derivar a chave de criptografia a partir da senha mestre.
- [ ] Limpar automaticamente a senha da área de transferência.
- [ ] Adicionar testes unitários para geração, força e persistência.
- [ ] Melhorar o tratamento de arquivos corrompidos.
- [ ] Criar instalador para Windows.
- [ ] Publicar versões executáveis na seção Releases.
- [ ] Adicionar demonstração em GIF ou vídeo ao README.

## Aprendizados demonstrados

O projeto aplica conceitos como:

- organização em camadas simples;
- encapsulamento da lógica em serviços;
- coleções observáveis e filtradas do JavaFX;
- bindings para controle automático do estado dos botões;
- validação de formulários e diálogos modais;
- personalização visual com CSS;
- persistência em arquivos;
- hashing de senha com salt;
- geração aleatória criptograficamente mais segura.

## Autor

Desenvolvido por **Alexander Revoredo**.

GitHub: [@AlexanderRevoredo](https://github.com/AlexanderRevoredo)
