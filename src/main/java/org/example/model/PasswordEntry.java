package org.example.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa UMA credencial salva (uma linha da tabela): o serviço
 * (Netflix, Google...), a conta/e-mail, a senha, e quais opções de geração
 * foram usadas pra criar essa senha (maiúsculas, símbolos, tamanho...).
 *
 * Guardamos essas opções junto com a senha para poder "lembrar" as
 * preferências da última vez que o usuário gerou uma senha pra esse mesmo
 * serviço (ver MainApp.loadPreferencesForService()).
 *
 * Essa classe é basicamente um "molde de dados": só tem construtor e
 * getters, sem nenhuma lógica. Não tem setters de propósito — para "editar"
 * uma credencial, o resto do app cria um PasswordEntry NOVO em vez de mudar
 * um existente (ver MainApp.onSave()).
 */
public class PasswordEntry {

    private String service;
    private String account;
    private String password;
    private boolean useUpper;
    private boolean useLower;
    private boolean useNumbers;
    private boolean useSymbols;
    private int length;

    public PasswordEntry(String service, String account, String password,
                          boolean useUpper, boolean useLower, boolean useNumbers, boolean useSymbols,
                          int length) {
        this.service = service;
        this.account = account;
        this.password = password;
        this.useUpper = useUpper;
        this.useLower = useLower;
        this.useNumbers = useNumbers;
        this.useSymbols = useSymbols;
        this.length = length;
    }

    public String getService() {
        return service;
    }

    public String getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }

    public boolean isUseUpper() {
        return useUpper;
    }

    public boolean isUseLower() {
        return useLower;
    }

    public boolean isUseNumbers() {
        return useNumbers;
    }

    public boolean isUseSymbols() {
        return useSymbols;
    }

    public int getLength() {
        return length;
    }

    /**
     * Converte essa credencial em UMA linha de texto, pronta para gravar no
     * cofre. Os campos ficam separados por "|", na ordem:
     * serviço|conta|senha|maiúsculas|minúsculas|números|símbolos|tamanho
     *
     * Só que existe um problema clássico aqui: e se a própria SENHA contiver
     * um "|"? A linha ficaria com campos demais e a leitura quebraria. Para
     * resolver isso usamos ESCAPE (ver o método escape() abaixo): antes de
     * montar a linha, todo "|" que faz parte do conteúdo é marcado com uma
     * barra invertida na frente, para não ser confundido com o separador.
     */
    public String toLine() {
        return String.join("|",
                escape(service),
                escape(account),
                escape(password),
                String.valueOf(useUpper),
                String.valueOf(useLower),
                String.valueOf(useNumbers),
                String.valueOf(useSymbols),
                String.valueOf(length));
    }

    /**
     * Faz o caminho inverso de toLine(): recebe uma linha do arquivo e
     * reconstrói o objeto PasswordEntry a partir dela.
     */
    public static PasswordEntry fromLine(String line) {
        List<String> parts = splitEscaped(line);
        if (parts.size() < 8) {
            throw new IllegalArgumentException("Linha do cofre em formato inválido: " + parts.size() + " campos");
        }
        return new PasswordEntry(
                parts.get(0),
                parts.get(1),
                parts.get(2),
                Boolean.parseBoolean(parts.get(3)),
                Boolean.parseBoolean(parts.get(4)),
                Boolean.parseBoolean(parts.get(5)),
                Boolean.parseBoolean(parts.get(6)),
                Integer.parseInt(parts.get(7)));
    }

    /**
     * "Protege" os caracteres que teriam significado especial no arquivo:
     *
     *   \  vira  \\   (a própria barra precisa ser protegida primeiro,
     *                  senão bagunçaria as substituições seguintes)
     *   |  vira  \|   (para não ser lido como separador de campos)
     *   quebra de linha vira \n  (para a credencial não virar duas linhas)
     */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Separa a linha nos "|" que são REALMENTE separadores, ignorando os que
     * foram protegidos por escape(), e já desfaz a proteção de cada caractere.
     *
     * Não dá para usar split() aqui porque ele não entende escape — por isso
     * percorremos a linha caractere por caractere:
     *
     * - se o caractere anterior foi uma barra invertida, este caractere é
     *   conteúdo literal (ou um código como "n" = quebra de linha);
     * - se for uma barra invertida, ligamos a "flag" de escape e seguimos;
     * - se for um "|" solto, terminamos o campo atual e começamos o próximo;
     * - qualquer outro caractere é acrescentado ao campo atual.
     */
    private static List<String> splitEscaped(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (char c : line.toCharArray()) {
            if (escaping) {
                switch (c) {
                    case 'n' -> current.append('\n');
                    case 'r' -> current.append('\r');
                    default -> current.append(c); // cobre \\ e \|
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '|') {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString()); // o último campo não é seguido de "|"
        return parts;
    }

    @Override
    public String toString() {
        return service + " (" + account + ")";
    }
}
