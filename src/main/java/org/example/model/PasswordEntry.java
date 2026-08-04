package org.example.model;

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
     * Converte essa credencial em UMA linha de texto, pronta para salvar no
     * arquivo passwords.txt. Os campos ficam separados por "|", na ordem:
     * serviço|conta|senha|maiúsculas|minúsculas|números|símbolos|tamanho
     */
    public String toLine() {
        return String.join("|",
                service,
                account,
                password,
                String.valueOf(useUpper),
                String.valueOf(useLower),
                String.valueOf(useNumbers),
                String.valueOf(useSymbols),
                String.valueOf(length));
    }

    /**
     * Faz o caminho inverso de toLine(): recebe uma linha do arquivo .txt e
     * reconstrói o objeto PasswordEntry a partir dela.
     * O "-1" no split faz o Java manter campos vazios no final da linha
     * (sem isso, "a|b|" viraria só ["a","b"] em vez de ["a","b",""]).
     */
    public static PasswordEntry fromLine(String line) {
        String[] parts = line.split("\\|", -1);
        return new PasswordEntry(
                parts[0],
                parts[1],
                parts[2],
                Boolean.parseBoolean(parts[3]),
                Boolean.parseBoolean(parts[4]),
                Boolean.parseBoolean(parts[5]),
                Boolean.parseBoolean(parts[6]),
                Integer.parseInt(parts[7]));
    }

    @Override
    public String toString() {
        return service + " (" + account + ")";
    }
}
