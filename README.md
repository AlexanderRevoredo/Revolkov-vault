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
  <img src="https://img.shields.io/badge/AES-256--GCM-22C55E" alt="AES-256-GCM">
  <img src="https://img.shields.io/badge/vers%C3%A3o-1.3.0-3AA8FF" alt="Versão 1.3.0">
  <img src="https://img.shields.io/badge/Status-Em%20desenvolvimento-3AA8FF" alt="Status do projeto">
  <img src="https://img.shields.io/badge/Projeto-Portf%C3%B3lio-8B5CF6" alt="Projeto de portfólio">
</p>

---

## Sobre o projeto

O **Revolkov Vault** é uma aplicação desktop para geração, organização e consulta de credenciais armazenadas localmente.

O projeto começou como um gerador de senhas e evoluiu para um pequeno cofre de credenciais com identidade visual própria, persistência local criptografada, pesquisa, edição, avaliação de força e proteção por senha mestre.

O objetivo principal é aplicar e demonstrar conhecimentos de:

- programação orientada a objetos;
- separação de responsabilidades;
- desenvolvimento de interfaces com JavaFX;
- persistência de dados em arquivos;
- validação e feedback ao usuário;
- geração aleatória criptograficamente adequada;
- hash de senhas com salt;
- criptografia autenticada de dados (AES-GCM);
- construção e evolução de um produto de software.

> [!IMPORTANT]
> Esta versão foi desenvolvida para fins educacionais e de portfólio.
>
> As credenciais são criptografadas com AES-256-GCM, usando uma chave derivada da senha mestre. **Não existe recuperação de senha mestre**: se você esquecê-la, o cofre não poderá ser aberto e as credenciais serão perdidas.

---

## Funcionalidades

### Geração de senhas

- Geração com `SecureRandom`.
- Tamanho configurável entre 4 e 64 caracteres.
- Inclusão opcional de:
  - letras maiúsculas;
  - letras minúsculas;
  - números;
  - símbolos.
- Bloqueio da geração caso nenhuma categoria esteja marcada.
- Indicador visual de força da senha (5 níveis: muito fraca → muito forte).
- Opção para mostrar ou ocultar a senha digitada.
- Cópia da senha para a área de transferência.

### Gerenciamento de credenciais

- Cadastro de serviço, conta e senha.
- Substituição de credenciais com o mesmo serviço e conta.
- Edição de credenciais cadastradas (mantém a senha atual se o campo for deixado em branco).
- Exclusão com confirmação.
- Pesquisa pelo nome do serviço.
- Senhas mascaradas na tabela.
- Revelação condicionada à confirmação da senha mestre.
- Recuperação das preferências de geração usadas anteriormente para um serviço.
- Feedback visual para operações de sucesso e erro.

### Interface

- Tema escuro com identidade visual própria (fundo `#0b0f1a`, detalhes em azul `#3aa8ff`).
- Tabela com linhas em zebra striping e botão de exclusão destacado em vermelho.
- Logo e ícones adaptados para diferentes tamanhos.
- Fonte customizada (Ethnocentric) no título, com tamanho responsivo ao redimensionamento da janela.
- Botões de edição, visualização e exclusão desabilitados quando nenhuma credencial está selecionada.
- Diálogos e alertas personalizados com o mesmo tema da aplicação.

---

## Tecnologias utilizadas

| Tecnologia                           | Utilização                                                            |
| ------------------------------------ | --------------------------------------------------------------------- |
| Java 21                              | Linguagem principal                                                   |
| JavaFX 21.0.2                        | Interface gráfica                                                     |
| Maven                                | Gerenciamento de dependências e execução                              |
| JavaFX CSS                           | Estilização da aplicação                                              |
| SecureRandom                         | Geração de senhas, salts e vetores de inicialização (IV)              |
| PBKDF2WithHmacSHA256                 | Hash de verificação da senha mestre e derivação da chave AES do cofre |
| AES-256/GCM/NoPadding (javax.crypto) | Criptografia autenticada do arquivo do cofre                          |
| Java NIO                             | Leitura e gravação dos arquivos locais                                |

O projeto não depende de frameworks externos de persistência ou de banco de dados.

---

## Estrutura do projeto

```
src/
└── main/
    ├── java/
    │   └── org/
    │       └── example/
    │           ├── Launcher.java
    │           ├── MainApp.java
    │           ├── model/
    │           │   └── PasswordEntry.java
    │           └── service/
    │               ├── MasterPasswordService.java
    │               ├── PasswordGenerator.java
    │               ├── PasswordStorage.java
    │               ├── PasswordStrengthEvaluator.java
    │               └── VaultCrypto.java
    └── resources/
        ├── fonts/
        ├── icons/
        └── style.css

```

### Responsabilidades das classes

#### `Launcher`

Ponto de entrada alternativo, usado apenas pelo executável empacotado com jpackage. Não estende `Application`; apenas repassa a chamada para `MainApp.main()`. Existe para contornar uma limitação do empacotamento do JavaFX (rodar o `.jar`/`.exe` com a própria `Application` como classe principal falha com o erro "JavaFX runtime components are missing").

#### `MainApp`

Responsável pela montagem da interface e pela coordenação das ações realizadas pelo usuário. Também conduz o fluxo de criação/desbloqueio do cofre logo na inicialização, antes de qualquer tela ser exibida.

#### `PasswordEntry`

Representa uma credencial armazenada, incluindo serviço, conta, senha e preferências de geração. Sabe se converter para uma linha de texto (`toLine()`) e se reconstruir a partir dela (`fromLine()`), com escape de caracteres especiais.

#### `MasterPasswordService`

Cadastra e valida a senha mestre utilizando PBKDF2 com salt aleatório. Guarda apenas o hash da senha em `master.key` — nunca a senha em si.

#### `PasswordGenerator`

Gera senhas de acordo com o tamanho e os grupos de caracteres selecionados.

#### `PasswordStorage`

Lê e grava o cofre criptografado (`vault.dat`) em disco, delegando a criptografia para `VaultCrypto`. Também cuida da migração automática do antigo `passwords.txt` em texto puro para o novo formato, na primeira execução após a atualização.

#### `PasswordStrengthEvaluator`

Avalia a força da senha utilizando uma heurística baseada no tamanho e na variedade de caracteres.

#### `VaultCrypto`

Deriva a chave AES-256 a partir da senha mestre (PBKDF2WithHmacSHA256) e realiza a criptografia/descriptografia autenticada (AES/GCM/NoPadding) do conteúdo do cofre. Lança uma exceção dedicada quando a senha está incorreta ou o arquivo foi corrompido.

---

## Download (Windows)

Para usar o aplicativo pronto, **não é necessário instalar Java nem qualquer outra ferramenta** — o instalador já inclui tudo.

1. Acesse a página de [**Releases**](https://github.com/AlexanderRevoredo/Revolkov-vault/releases).
2. Baixe o arquivo `REVOLKOV.VAULT-<versão>.exe` (ex.: `REVOLKOV.VAULT-1.3.0.exe`).
3. Execute o instalador e siga o assistente.
4. Abra o aplicativo pelo atalho criado no menu Iniciar ou na área de trabalho.

Na primeira execução, será solicitada a criação de uma **senha mestre**. Ela protege todo o cofre e será pedida sempre que o aplicativo for aberto, além de ser exigida novamente antes de revelar qualquer credencial salva.

> [!NOTE]
> O instalador ainda não tem assinatura digital (certificado de code signing), então o Windows SmartScreen deve mostrar um aviso de "aplicativo não reconhecido" ao executá-lo. Isso é esperado: clique em **"Mais informações"** e depois em **"Executar assim mesmo"** para prosseguir.

> [!CAUTION]
> Guarde a senha mestre em local seguro. Não existe recuperação: sem ela, as credenciais salvas não poderão ser recuperadas por ninguém.

---

## Como executar (desenvolvimento)

### Pré-requisitos

Certifique-se de possuir:

- JDK 21;
- Maven;
- Git.

### Clonar o repositório

```
git clone https://github.com/AlexanderRevoredo/Revolkov-vault.git
cd Revolkov-vault

```

### Executar com Maven

```
mvn clean javafx:run

```

Na primeira execução, o aplicativo solicitará a criação de uma senha mestre. Nas execuções seguintes, a senha mestre será exigida para desbloquear o cofre na inicialização e novamente antes de revelar uma credencial armazenada.

---

## Armazenamento local

Os arquivos são criados fora da pasta do projeto, dentro do diretório pessoal do usuário:

```
~/password-manager/
├── master.key
└── vault.dat

```

No Windows, o caminho normalmente será:

```
C:\Users\<usuario>\password-manager\

```

### `master.key`

Arquivo de texto no formato `salt:hash`, com os dois valores em Base64:

- salt aleatório de 16 bytes;
- hash PBKDF2 derivado da senha mestre.

A senha mestre original não é armazenada.

### `vault.dat`

Arquivo binário criptografado que armazena todas as credenciais (serviço, conta, senha e preferências de geração).

```
┌──────────────────┬────────────────┬─────────────────────────────────────┐
│  salt (16 bytes)  │  IV (12 bytes) │  dados criptografados + tag de auth  │
└──────────────────┴────────────────┴─────────────────────────────────────┘

```

Depois de descriptografado em memória, o conteúdo é uma credencial por linha, com os campos serializados como:

```
serviço|conta|senha|maiúsculas|minúsculas|números|símbolos|tamanho

```

Caracteres especiais (`|`, `\`, quebras de linha) recebem escape antes da serialização, para não quebrar a leitura do arquivo.

### Migração de versões anteriores

Versões anteriores do projeto gravavam as credenciais em `passwords.txt`, em texto puro. Ao abrir o app pela primeira vez após a atualização (com uma senha mestre já cadastrada, mas ainda sem `vault.dat`), a migração acontece automaticamente:

1. a senha mestre digitada é validada;
2. uma chave de criptografia é derivada a partir dela;
3. as credenciais do `passwords.txt` antigo são importadas;
4. um novo `vault.dat` criptografado é criado com essas credenciais;
5. o `passwords.txt` original é apagado.

---

## Segurança implementada

A senha mestre não é salva em texto puro, e as credenciais não ficam legíveis em disco.

**Autenticação da senha mestre:**

- `PBKDF2WithHmacSHA256`;
- salt aleatório de 16 bytes;
- 65.536 iterações;
- chave derivada de 256 bits;
- comparação com `MessageDigest.isEqual`.

**Criptografia do cofre:**

- `AES-256/GCM/NoPadding` (criptografia autenticada);
- chave derivada da senha mestre via PBKDF2, com salt próprio (independente do salt da senha mestre);
- IV de 12 bytes gerado a cada gravação;
- tag de autenticação de 128 bits, que detecta senha incorreta e adulteração do arquivo;
- `SecureRandom` para geração de senhas, salts e IVs.

O cofre é descriptografado apenas na memória, após o desbloqueio na abertura do aplicativo.

### Limitações conhecidas

> [!WARNING]
>
> - Não há recuperação de senha mestre. Esquecer a senha significa perder o acesso às credenciais.
> - Enquanto o aplicativo está aberto, as credenciais ficam descriptografadas na memória do processo.
> - A senha copiada permanece na área de transferência até ser substituída por outro conteúdo.
> - O aplicativo não possui bloqueio automático por inatividade.
> - Não existe mecanismo de backup do cofre.

---

## Roadmap

**Segurança**

- [x] Criptografar o arquivo de credenciais com AES-GCM.
- [x] Derivar a chave de criptografia a partir da senha mestre.
- [x] Migrar automaticamente o armazenamento legado em texto puro.
- [ ] Limpar automaticamente a senha da área de transferência.
- [ ] Bloqueio automático por inatividade.
- [ ] Reduzir o tempo de exposição de dados sensíveis em memória.
- [ ] Melhorar o tratamento de arquivos ausentes ou corrompidos.

**Qualidade**

- [ ] Testes unitários para geração de senhas e avaliação de força.
- [ ] Testes para persistência, migração e validação da senha mestre.
- [ ] Testes para criptografia/descriptografia do cofre.

**Distribuição**

- [ ] Automatizar o empacotamento (ex.: `jpackage-maven-plugin`).
- [x] Publicar versões executáveis na seção Releases.
- [ ] Adicionar demonstração em GIF ou vídeo.
- [ ] Adicionar captura atualizada da interface ao README.

---

## Aprendizados demonstrados

Durante o desenvolvimento foram aplicados conceitos como:

- encapsulamento;
- organização em serviços;
- separação entre interface, modelo e persistência;
- coleções observáveis do JavaFX;
- filtragem com `FilteredList`;
- bindings para controle automático de componentes;
- validação de formulários;
- diálogos modais;
- personalização de interface com CSS;
- persistência em arquivos (texto e binário);
- hash de senha com salt (PBKDF2);
- criptografia autenticada de dados (AES-GCM);
- geração aleatória com `SecureRandom`;
- migração de dados entre formatos de armazenamento;
- evolução incremental baseada em testes manuais e feedback.

---

## Autor

Desenvolvido por **Alexander Revoredo**.

- GitHub: [@AlexanderRevoredo](https://github.com/AlexanderRevoredo)

---

<p align="center">
  Desenvolvido como projeto de portfólio para demonstrar evolução em Java, JavaFX e segurança de aplicações.
</p>
