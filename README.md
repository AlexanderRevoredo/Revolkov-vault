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
  <img src="https://img.shields.io/badge/Projeto-Portf%C3%B3lio-22C55E" alt="Projeto de portfólio">
</p>

---

## Sobre o projeto

O **Revolkov Vault** é uma aplicação desktop para geração, organização e consulta de credenciais armazenadas localmente.

O projeto começou como um gerador de senhas e evoluiu para um pequeno cofre de credenciais com identidade visual própria, persistência local, pesquisa, edição, avaliação de força e proteção da visualização por senha mestre.

O objetivo principal é aplicar e demonstrar conhecimentos de:

- programação orientada a objetos;
- separação de responsabilidades;
- desenvolvimento de interfaces com JavaFX;
- persistência de dados em arquivos;
- validação e feedback ao usuário;
- geração aleatória criptograficamente adequada;
- hash de senhas com salt;
- construção e evolução de um produto de software.

> [!IMPORTANT]
> Esta versão foi desenvolvida para fins educacionais e de portfólio.
>
> As credenciais ainda são armazenadas em texto puro no arquivo local. Portanto, o aplicativo não deve ser utilizado para guardar senhas reais ou informações sensíveis.

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
- Indicador visual de força da senha.
- Opção para mostrar ou ocultar a senha digitada.
- Cópia da senha para a área de transferência.

### Gerenciamento de credenciais

- Cadastro de serviço, conta e senha.
- Substituição de credenciais com o mesmo serviço e conta.
- Edição de credenciais cadastradas.
- Exclusão com confirmação.
- Pesquisa pelo nome do serviço.
- Senhas mascaradas na tabela.
- Revelação condicionada à confirmação da senha mestre.
- Recuperação das preferências de geração usadas anteriormente para um serviço.
- Feedback visual para operações de sucesso e erro.

### Interface

- Tema escuro com identidade visual própria.
- Logo e ícones adaptados para diferentes tamanhos.
- Layout adaptável ao redimensionamento da janela.
- Botões de edição, visualização e exclusão desabilitados quando nenhuma credencial está selecionada.
- Diálogos e alertas personalizados com o mesmo tema da aplicação.

---

## Tecnologias utilizadas

| Tecnologia | Utilização |
|---|---|
| Java 21 | Linguagem principal |
| JavaFX 21.0.2 | Interface gráfica |
| Maven | Gerenciamento de dependências e execução |
| JavaFX CSS | Estilização da aplicação |
| SecureRandom | Geração de senhas e salts |
| PBKDF2WithHmacSHA256 | Derivação do hash da senha mestre |
| Java NIO | Leitura e gravação dos arquivos locais |

---

## Estrutura do projeto

```text
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
    │               └── PasswordStrengthEvaluator.java
    └── resources/
        ├── fonts/
        ├── icons/
        └── style.css
```

### Responsabilidades das classes

#### `MainApp`

Responsável pela montagem da interface e pela coordenação das ações realizadas pelo usuário.

#### `PasswordEntry`

Representa uma credencial armazenada, incluindo serviço, conta, senha e preferências de geração.

#### `PasswordGenerator`

Gera senhas de acordo com o tamanho e os grupos de caracteres selecionados.

#### `PasswordStrengthEvaluator`

Avalia a força da senha utilizando uma heurística baseada no tamanho e na variedade de caracteres.

#### `PasswordStorage`

Realiza a leitura e a gravação das credenciais no armazenamento local.

#### `MasterPasswordService`

Cadastra e valida a senha mestre utilizando PBKDF2 com salt aleatório.

---

## Como executar

### Pré-requisitos

Certifique-se de possuir:

- JDK 21;
- Maven;
- Git.

### Clonar o repositório

```bash
git clone https://github.com/AlexanderRevoredo/Revolkov-vault.git
cd Revolkov-vault
```

### Executar com Maven

```bash
mvn clean javafx:run
```

Na primeira execução, o aplicativo solicitará a criação de uma senha mestre.

Essa senha será exigida sempre que o usuário tentar revelar uma credencial armazenada.

---

## Armazenamento local

Os arquivos são criados fora da pasta do projeto, dentro do diretório pessoal do usuário:

```text
~/password-manager/
├── master.key
└── passwords.txt
```

No Windows, o caminho normalmente será:

```text
C:\Users\<usuario>\password-manager\
```

### `master.key`

Armazena:

- salt aleatório;
- hash derivado da senha mestre.

A senha mestre original não é armazenada.

### `passwords.txt`

Armazena:

- serviço;
- conta;
- senha;
- preferências de geração;
- tamanho configurado.

---

## Segurança implementada

A senha mestre não é salva em texto puro.

A implementação utiliza:

- `PBKDF2WithHmacSHA256`;
- salt aleatório de 16 bytes;
- 65.536 iterações;
- chave derivada de 256 bits;
- comparação com `MessageDigest.isEqual`;
- `SecureRandom` para geração de senhas e salts.

### Limitação atual

> [!WARNING]
> A senha mestre protege a visualização das credenciais dentro da interface, mas ainda não protege diretamente o arquivo `passwords.txt`.
>
> As credenciais são armazenadas em texto puro no disco e podem ser lidas por alguém que tenha acesso ao arquivo.
>
> A implementação de criptografia autenticada do cofre está planejada para uma próxima versão.

---

## Roadmap

- [ ] Criptografar o arquivo de credenciais com AES-GCM.
- [ ] Derivar a chave de criptografia a partir da senha mestre.
- [ ] Limpar automaticamente a senha da área de transferência.
- [ ] Adicionar testes unitários para geração e avaliação de força.
- [ ] Adicionar testes para persistência e validação da senha mestre.
- [ ] Melhorar o tratamento de arquivos ausentes ou corrompidos.
- [ ] Criar instalador para Windows.
- [ ] Publicar versões executáveis na seção Releases.
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
- persistência em arquivos;
- hash de senha com salt;
- geração aleatória com `SecureRandom`;
- evolução incremental baseada em testes e feedback.

---

## Próxima evolução técnica

O principal objetivo da próxima versão é transformar a senha mestre em uma proteção real para o conteúdo do cofre.

A estratégia planejada é:

1. derivar uma chave a partir da senha mestre;
2. criptografar as credenciais com AES-GCM;
3. armazenar apenas o conteúdo criptografado;
4. autenticar o arquivo para detectar alterações ou corrupção;
5. reduzir o tempo de exposição das senhas na memória e na área de transferência.

---

## Autor

Desenvolvido por **Alexander Revoredo**.

- GitHub: [@AlexanderRevoredo](https://github.com/AlexanderRevoredo)

---

<p align="center">
  Desenvolvido como projeto de portfólio para demonstrar evolução em Java, JavaFX e segurança de aplicações.
</p>
