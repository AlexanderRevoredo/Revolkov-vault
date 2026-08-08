package org.example;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.example.model.PasswordEntry;
import org.example.service.MasterPasswordService;
import org.example.service.PasswordGenerator;
import org.example.service.PasswordStorage;
import org.example.service.PasswordStrengthEvaluator;
import org.example.service.VaultCrypto;

import java.io.InputStream;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Classe principal (e única tela) do REVOLKOV VAULT.
 *
 * Em JavaFX, toda aplicação com interface gráfica estende {@link Application}
 * e implementa o método start(), que é chamado automaticamente pelo framework
 * assim que a aplicação termina de inicializar (o método main() lá embaixo só
 * dá o "start" no motor do JavaFX através de launch(args)).
 *
 * Essa classe é dividida em duas partes, na ordem em que aparecem no arquivo:
 * 1. Métodos "build..." que MONTAM a interface (criam botões, campos, tabela).
 * 2. Métodos "on..." que REAGEM a cliques e ações do usuário (a lógica em si).
 */
public class MainApp extends Application {

    // Texto usado no lugar da senha real quando ela está "escondida" na tabela.
    private static final String MASKED_PASSWORD = "••••••••";
    // Nome do app, centralizado aqui para não repetir a string em vários lugares.
    private static final String APP_NAME = "REVOLKOV VAULT";

    // "Serviços" (classes que fazem o trabalho pesado). A tela só chama esses
    // objetos e mostra o resultado na interface — ela não sabe COMO cada coisa
    // é feita (gerar senha, salvar em arquivo, etc.), só QUEM faz.
    private final PasswordGenerator generator = new PasswordGenerator();
    private final MasterPasswordService masterPasswordService = new MasterPasswordService();
    // Só existe depois que o usuário digita a senha mestre: sem a chave
    // derivada dela, não há como ler nem gravar o cofre criptografado.
    private PasswordStorage storage;
    private final PasswordStrengthEvaluator strengthEvaluator = new PasswordStrengthEvaluator();

    // Lista "observável": é como um ArrayList normal, mas a TableView consegue
    // "escutar" mudanças nela (adicionar/remover) e se atualiza sozinha na tela,
    // sem precisarmos chamar table.refresh() toda vez que a lista muda.
    private final ObservableList<PasswordEntry> entries = FXCollections.observableArrayList();

    // Guarda quais credenciais o usuário já revelou (clicou em "Ver senha" e
    // digitou a senha mestre certa) durante essa sessão do app, para que a
    // tabela mostre a senha em texto puro só para essas linhas.
    // Usamos IdentityHashMap (via newSetFromMap) porque queremos comparar os
    // objetos por IDENTIDADE (é exatamente esse objeto na memória?) e não por
    // igualdade de conteúdo — assim, duas senhas iguais mas de linhas diferentes
    // não seriam confundidas.
    private final Set<PasswordEntry> revealedEntries = Collections.newSetFromMap(new IdentityHashMap<>());

    // FilteredList "envolve" a lista `entries` e deixa esconder itens que não
    // batem com o texto digitado na busca, sem apagar nada da lista original.
    private FilteredList<PasswordEntry> filteredEntries;

    // Campos da interface guardados como atributos da classe porque precisam
    // ser lidos/alterados por vários métodos diferentes (não só por quem os criou).
    private TextField serviceField;
    private TextField accountField;
    private TextField passwordField;   // guarda a senha em TEXTO PURO (é a "fonte da verdade")
    private PasswordField passwordMask; // versão visual mascarada (••••), espelha o campo acima
    private Button togglePasswordBtn;   // botão do "olho" que alterna entre os dois campos acima
    private ProgressBar strengthBar;
    private Label strengthLabel;
    private boolean passwordVisible;
    private CheckBox upperCheck;
    private CheckBox lowerCheck;
    private CheckBox numberCheck;
    private CheckBox symbolCheck;
    private Spinner<Integer> lengthSpinner;
    private TableView<PasswordEntry> table;
    private Label statusLabel;
    // Quando não é nulo, significa que o formulário está EDITANDO essa credencial
    // (em vez de criar uma nova). Ver onEdit() e onSave().
    private PasswordEntry editingEntry;
    private Label titleLabel;
    private Label subtitleLabel;
    private String titleFontFamily;

    /**
     * Ponto de entrada da interface gráfica. O JavaFX chama esse método sozinho
     * depois de launch(args), passando o "Stage" (a janela do sistema operacional).
     */
    @Override
    public void start(Stage stage) {
        // Antes de mostrar qualquer coisa, o cofre precisa ser criado (na
        // primeira execução) ou desbloqueado com a senha mestre. Se o usuário
        // fechar essa etapa, encerramos o app: sem a chave derivada da senha,
        // não há como descriptografar nada para exibir.
        if (!setUpVault()) {
            Platform.exit();
            return;
        }

        // Descriptografa o cofre e carrega as credenciais para a lista em memória.
        entries.addAll(storage.loadAll());

        // BorderPane divide a janela em regiões: topo, centro, esquerda, direita, baixo.
        // Usamos só "topo" (cabeçalho + formulário) e "centro" (busca + tabela).
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setTop(new VBox(15, buildHeader(), buildForm()));
        root.setCenter(buildTable());

        // Scene é o "conteúdo" da janela (tamanho inicial 700x620) e é nela que
        // aplicamos a folha de estilos (o tema escuro/azul do app.css).
        Scene scene = new Scene(root, 780, 620);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        // Sempre que a largura da janela mudar (usuário redimensiona/maximiza),
        // recalculamos o tamanho da fonte do título para ele ficar responsivo.
        scene.widthProperty().addListener((obs, oldWidth, newWidth) -> updateHeaderFontSize(newWidth.doubleValue()));
        updateHeaderFontSize(scene.getWidth());

        stage.setScene(scene);
        stage.setTitle(APP_NAME);
        loadIcon(stage);
        stage.show();
    }

    // Tamanhos de ícone que o Windows pode pedir dependendo do contexto
    // (barra de título, barra de tarefas, Alt+Tab, atalho na área de trabalho...).
    private static final int[] ICON_SIZES = {16, 24, 32, 48, 64, 128, 256};

    /**
     * Carrega todos os tamanhos de ícone disponíveis e entrega pro Stage.
     * O sistema operacional escolhe sozinho qual tamanho usar em cada lugar.
     */
    private void loadIcon(Stage stage) {
        for (int size : ICON_SIZES) {
            // getResourceAsStream lê um arquivo que foi empacotado dentro do
            // próprio .jar (fica em src/main/resources durante o desenvolvimento).
            try (InputStream iconStream = getClass().getResourceAsStream("/icons/icon-" + size + ".png")) {
                if (iconStream != null) {
                    stage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception ignored) {
                // ícone opcional; segue sem ele se não existir
            }
        }
    }

    /**
     * Monta o cabeçalho: logo do lobo + "REVOLKOV VAULT" + subtítulo.
     */
    private VBox buildHeader() {
        ImageView logoView = new ImageView();
        try (InputStream iconStream = getClass().getResourceAsStream("/icons/icon-128.png")) {
            if (iconStream != null) {
                logoView.setImage(new Image(iconStream));
                logoView.setFitHeight(56);
                logoView.setFitWidth(56);
                logoView.setPreserveRatio(true); // não deixa a imagem esticar/deformar
            }
        } catch (Exception ignored) {
            // logo opcional; segue sem ela se não existir
        }

        titleLabel = new Label(APP_NAME);
        titleLabel.getStyleClass().add("app-title"); // liga esse Label à regra .app-title do CSS
        titleFontFamily = loadTitleFontFamily();

        // HBox organiza os filhos lado a lado (logo à esquerda do texto).
        HBox titleRow = new HBox(14, logoView, titleLabel);
        titleRow.setAlignment(Pos.CENTER);

        subtitleLabel = new Label("COFRE LOCAL DE CREDENCIAIS");
        subtitleLabel.getStyleClass().add("app-subtitle");

        // VBox organiza os filhos um embaixo do outro (título em cima, subtítulo embaixo).
        VBox header = new VBox(4, titleRow, subtitleLabel);
        header.setAlignment(Pos.CENTER);
        return header;
    }

    /**
     * Carrega a fonte customizada (Ethnocentric) a partir do arquivo .otf
     * empacotado no projeto e devolve o NOME da família da fonte, para que
     * outros métodos possam usar Font.font(nomeDaFonte, ...) em qualquer tamanho.
     * Se o arquivo não existir, devolve null e o app usa a fonte padrão do sistema.
     */
    private String loadTitleFontFamily() {
        try (InputStream fontStream = getClass().getResourceAsStream("/fonts/Ethnocentric-Regular.otf")) {
            if (fontStream != null) {
                // O tamanho passado aqui (12) não importa muito: só precisamos
                // registrar a fonte no JavaFX pra descobrir o nome dela.
                Font loaded = Font.loadFont(fontStream, 12);
                if (loaded != null) {
                    return loaded.getFamily();
                }
            }
        } catch (Exception ignored) {
            // fonte opcional; usa o fallback do CSS se não for encontrada
        }
        return null;
    }

    /**
     * Recalcula o tamanho da fonte do título/subtítulo com base na largura
     * atual da janela, para o texto crescer/encolher de forma responsiva.
     */
    private void updateHeaderFontSize(double width) {
        double titleSize = clamp(width / 18.0, 30, 72);
        double subtitleSize = clamp(titleSize * 0.32, 12, 22);

        if (titleFontFamily != null) {
            titleLabel.setFont(Font.font(titleFontFamily, FontWeight.BOLD, titleSize));
        } else {
            titleLabel.setStyle("-fx-font-size: " + titleSize + "px; -fx-font-weight: bold;");
        }
        subtitleLabel.setStyle("-fx-font-size: " + subtitleSize + "px;");
    }

    /** Limita um valor entre um mínimo e um máximo (evita fonte gigante ou minúscula demais). */
    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Monta o formulário de cima: campos de Serviço/Conta/Senha, checkboxes
     * de geração, tamanho da senha, e os botões de ação (Gerar, Salvar, etc.).
     */
    private VBox buildForm() {
        serviceField = new TextField();
        serviceField.setPromptText("Ex: Netflix, Google, Amazon...");
        // "focusedProperty" avisa quando o campo GANHA ou PERDE o foco (o cursor
        // saiu dele). Aqui, quando o usuário sai do campo de Serviço, tentamos
        // carregar as preferências de geração usadas da última vez pra esse serviço.
        serviceField.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                loadPreferencesForService(serviceField.getText());
            }
        });

        accountField = new TextField();
        accountField.setPromptText("email ou usuário");

        // --- Campo de senha com opção de mostrar/ocultar ---
        // O JavaFX não tem um "TextField que pode virar PasswordField". Então o
        // truque é ter DOIS campos ocupando o mesmo espaço (um StackPane empilha
        // os filhos um sobre o outro) e mostrar só um deles por vez:
        //   - passwordField: TextField normal, mostra a senha em texto puro.
        //   - passwordMask:  PasswordField, mostra bolinhas (•••).
        // Os dois ficam "grudados" através de bindBidirectional: quando um muda
        // de texto, o outro muda junto automaticamente. O resto do código só
        // precisa mexer em passwordField (é o campo "oficial").
        passwordField = new TextField();
        passwordField.setPromptText("gere ou digite uma senha");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        passwordMask = new PasswordField();
        passwordMask.setPromptText("gere ou digite uma senha");
        passwordMask.setMaxWidth(Double.MAX_VALUE);
        passwordMask.textProperty().bindBidirectional(passwordField.textProperty());

        togglePasswordBtn = new Button();
        togglePasswordBtn.getStyleClass().add("button-eye");
        togglePasswordBtn.setOnAction(e -> togglePasswordVisibility());

        StackPane passwordStack = new StackPane(passwordField, passwordMask);
        HBox.setHgrow(passwordStack, Priority.ALWAYS); // deixa o campo esticar e ocupar o espaço disponível
        HBox passwordRow = new HBox(6, passwordStack, togglePasswordBtn);
        setPasswordVisible(false); // começa escondida, como um campo de senha normal

        // --- Indicador de força da senha ---
        strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(strengthBar, Priority.ALWAYS);
        strengthLabel = new Label();
        strengthLabel.setMinWidth(90);
        HBox strengthRow = new HBox(8, strengthBar, strengthLabel);
        strengthRow.setAlignment(Pos.CENTER_LEFT);
        // Toda vez que o texto da senha mudar (digitando ou gerando), recalcula a força.
        passwordField.textProperty().addListener((obs, oldV, newV) -> updateStrengthIndicator(newV));
        updateStrengthIndicator(""); // estado inicial, com o campo vazio

        upperCheck = new CheckBox("Maiúsculas");
        lowerCheck = new CheckBox("Minúsculas");
        numberCheck = new CheckBox("Números");
        symbolCheck = new CheckBox("Símbolos");
        upperCheck.setSelected(true);
        lowerCheck.setSelected(true);
        numberCheck.setSelected(true);
        symbolCheck.setSelected(true);

        // Spinner<Integer> é um campo numérico com setinhas pra cima/baixo.
        // Os parâmetros são: valor mínimo, valor máximo, valor inicial.
        lengthSpinner = new Spinner<>(4, 64, 12);
        lengthSpinner.setEditable(true); // permite digitar o número direto, não só usar as setinhas

        Button generateBtn = new Button("Gerar senha");
        generateBtn.setOnAction(e -> onGenerate());

        Button saveBtn = new Button("Salvar");
        saveBtn.setOnAction(e -> onSave());

        Button copyBtn = new Button("Copiar senha");
        copyBtn.setOnAction(e -> onCopy());

        Button clearBtn = new Button("Limpar");
        clearBtn.setOnAction(e -> clearForm());

        // GridPane organiza os campos em linhas e colunas, tipo uma tabela.
        // Aqui usamos 2 colunas: uma para os rótulos ("Serviço:", "Conta:"...)
        // e outra para os campos em si.
        GridPane grid = new GridPane();
        grid.setHgap(10); // espaço horizontal entre colunas
        grid.setVgap(8);  // espaço vertical entre linhas

        // ColumnConstraints define o COMPORTAMENTO de cada coluna. Aqui dizemos:
        // a coluna dos rótulos tem largura mínima fixa, e a coluna dos campos
        // deve CRESCER (Priority.ALWAYS) para ocupar todo o espaço extra da
        // janela — é isso que faz os campos esticarem quando a janela é maior.
        ColumnConstraints labelCol = new ColumnConstraints();
        labelCol.setMinWidth(70);
        ColumnConstraints fieldCol = new ColumnConstraints();
        fieldCol.setHgrow(Priority.ALWAYS);
        fieldCol.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelCol, fieldCol);

        // addRow(linha, ...colunas) adiciona vários nós de uma vez na mesma linha.
        grid.addRow(0, new Label("Serviço:"), serviceField);
        grid.addRow(1, new Label("Conta:"), accountField);
        grid.addRow(2, new Label("Senha:"), passwordRow);
        grid.addRow(3, new Label(), strengthRow); // label vazio só pra manter o alinhamento das colunas

        serviceField.setMaxWidth(Double.MAX_VALUE);
        accountField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(serviceField, Priority.ALWAYS);
        GridPane.setHgrow(accountField, Priority.ALWAYS);
        GridPane.setHgrow(passwordRow, Priority.ALWAYS);
        GridPane.setHgrow(strengthRow, Priority.ALWAYS);

        HBox checkboxes = new HBox(12, upperCheck, lowerCheck, numberCheck, symbolCheck);
        HBox lengthBox = new HBox(8, new Label("Tamanho:"), lengthSpinner);
        lengthBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttons = new HBox(10, generateBtn, saveBtn, copyBtn, clearBtn);

        // Label usada para dar feedback rápido ao usuário ("Senha gerada.",
        // "Senha copiada!", mensagens de erro leves, etc.).
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-neutral");

        VBox form = new VBox(10, grid, checkboxes, lengthBox, buttons, statusLabel);
        return form;
    }

    /**
     * Alterna entre mostrar a senha em texto puro (passwordField visível) ou
     * mascarada (passwordMask visível). Só um dos dois fica visível por vez.
     * "managed(false)" tira o nó do cálculo de layout também, não só da tela
     * — sem isso, o campo invisível ainda ocuparia espaço "fantasma".
     */
    private void setPasswordVisible(boolean visible) {
        passwordVisible = visible;
        passwordField.setVisible(visible);
        passwordField.setManaged(visible);
        passwordMask.setVisible(!visible);
        passwordMask.setManaged(!visible);
        if (togglePasswordBtn != null) {
            togglePasswordBtn.setText(visible ? "🙈" : "👁");
        }
    }

    private void togglePasswordVisibility() {
        setPasswordVisible(!passwordVisible);
    }

    /**
     * Pede pro avaliador de força calcular o resultado e atualiza a barra
     * (cor + preenchimento) e o texto ("Fraca", "Forte"...) na tela.
     */
    private void updateStrengthIndicator(String password) {
        PasswordStrengthEvaluator.Result result = strengthEvaluator.evaluate(password);
        strengthBar.setProgress(result.fraction());
        // -fx-accent controla a cor de preenchimento padrão de uma ProgressBar.
        strengthBar.setStyle("-fx-accent: " + result.color() + ";");
        strengthLabel.setText(result.label());
        strengthLabel.setStyle("-fx-text-fill: " + result.color() + ";");
    }

    /**
     * Monta a parte de baixo da tela: campo de busca, tabela de credenciais
     * salvas e os botões de ação (Editar / Ver senha / Excluir).
     */
    private VBox buildTable() {
        // FilteredList "envolve" a lista real (entries). Ele não copia os dados,
        // só decide quais itens aparecem, de acordo com o predicate (uma função
        // que devolve true/false para cada item).
        filteredEntries = new FilteredList<>(entries, e -> true);

        TextField searchField = new TextField();
        searchField.setPromptText("Buscar por serviço...");
        searchField.textProperty().addListener((obs, old, text) -> {
            String query = text == null ? "" : text.trim().toLowerCase();
            // Troca o predicate toda vez que o texto de busca muda. A tabela,
            // que está "olhando" para filteredEntries, se atualiza sozinha.
            filteredEntries.setPredicate(entry -> query.isEmpty() || entry.getService().toLowerCase().contains(query));
        });

        table = new TableView<>(filteredEntries);
        // Texto mostrado quando a tabela está vazia (em vez do padrão em inglês
        // "No content in table").
        Label emptyPlaceholder = new Label("Nenhuma credencial cadastrada.");
        emptyPlaceholder.getStyleClass().add("status-neutral");
        table.setPlaceholder(emptyPlaceholder);

        // Cada TableColumn precisa saber DE ONDE tirar o valor a mostrar.
        // PropertyValueFactory("service") usa reflection para chamar
        // getService() em cada PasswordEntry da lista automaticamente.
        TableColumn<PasswordEntry, String> serviceCol = new TableColumn<>("Serviço");
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("service"));
        serviceCol.setPrefWidth(150);

        TableColumn<PasswordEntry, String> accountCol = new TableColumn<>("Conta");
        accountCol.setCellValueFactory(new PropertyValueFactory<>("account"));
        accountCol.setPrefWidth(200);

        // A coluna de senha é diferente: em vez de usar PropertyValueFactory
        // (que sempre mostraria a senha real), escrevemos manualmente a regra:
        // se essa linha estiver no conjunto "revealedEntries" (o usuário já
        // confirmou a senha mestre pra ela), mostra a senha de verdade; senão,
        // mostra a máscara.
        TableColumn<PasswordEntry, String> passwordCol = new TableColumn<>("Senha");
        passwordCol.setCellValueFactory(data ->
                new ReadOnlyStringWrapper(revealedEntries.contains(data.getValue()) ? data.getValue().getPassword() : MASKED_PASSWORD));
        passwordCol.setPrefWidth(340);
        passwordCol.setMinWidth(220);

        table.getColumns().addAll(List.of(serviceCol, accountCol, passwordCol));
        // CONSTRAINED_RESIZE_POLICY faz as colunas dividirem toda a largura
        // disponível da tabela (em vez de deixar espaço vazio sobrando à direita).
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button editBtn = new Button("Editar selecionado");
        editBtn.setOnAction(e -> onEdit());

        Button revealBtn = new Button("👁 Ver senha");
        revealBtn.getStyleClass().add("button-eye");
        revealBtn.setOnAction(e -> onReveal());

        Button deleteBtn = new Button("Excluir selecionado");
        deleteBtn.getStyleClass().add("button-danger"); // fica vermelho, por ser uma ação destrutiva
        deleteBtn.setOnAction(e -> onDelete());

        // Binding: em vez de ficar checando "tem algo selecionado?" toda hora,
        // a gente PRENDE a propriedade "disabled" dos botões a uma condição.
        // O JavaFX cuida de reavaliar isso sozinho sempre que a seleção mudar.
        BooleanBinding nothingSelected = Bindings.isNull(table.getSelectionModel().selectedItemProperty());
        editBtn.disableProperty().bind(nothingSelected);
        revealBtn.disableProperty().bind(nothingSelected);
        deleteBtn.disableProperty().bind(nothingSelected);

        HBox actions = new HBox(10, editBtn, revealBtn, deleteBtn);

        VBox box = new VBox(8, searchField, table, actions);
        return box;
    }

    // ------------------------------------------------------------------
    // A partir daqui: métodos "on..." — o que acontece quando o usuário
    // clica em cada botão. Essa é a LÓGICA do app.
    // ------------------------------------------------------------------

    /** Botão "Gerar senha": pede uma senha nova pro PasswordGenerator, seguindo as opções marcadas. */
    private void onGenerate() {
        try {
            String password = generator.generate(
                    lengthSpinner.getValue(),
                    upperCheck.isSelected(),
                    lowerCheck.isSelected(),
                    numberCheck.isSelected(),
                    symbolCheck.isSelected());
            passwordField.setText(password);
            setPasswordVisible(true); // mostra a senha recém-gerada, pro usuário conferir
            setStatus("Senha gerada.", true);
        } catch (IllegalArgumentException ex) {
            // O PasswordGenerator lança essa exceção se nenhuma caixinha
            // estiver marcada (não dá pra gerar senha sem nenhum tipo de caractere).
            showError(ex.getMessage());
        }
    }

    /**
     * Botão "Salvar": cria (ou atualiza, se estiver editando) uma credencial
     * e grava tudo de novo no arquivo .txt.
     */
    private void onSave() {
        String service = serviceField.getText();
        String account = accountField.getText();
        String typedPassword = passwordField.getText();

        if (service == null || service.isBlank() || account == null || account.isBlank()) {
            showError("Preencha serviço e conta antes de salvar.");
            return;
        }

        // Decide qual senha usar:
        // - Se está editando e o campo de senha ficou em branco, mantém a senha antiga.
        // - Se não está editando, o campo de senha é obrigatório.
        // - Caso contrário, usa o que foi digitado/gerado.
        String finalPassword;
        if (editingEntry != null && (typedPassword == null || typedPassword.isBlank())) {
            finalPassword = editingEntry.getPassword();
        } else if (typedPassword == null || typedPassword.isBlank()) {
            showError("Gere ou digite uma senha antes de salvar.");
            return;
        } else {
            finalPassword = typedPassword;
        }

        // PasswordEntry não tem "setters" (é um objeto praticamente imutável),
        // então pra "editar" uma credencial a gente na verdade cria um objeto
        // NOVO com os dados atualizados e substitui o antigo na lista.
        PasswordEntry newEntry = new PasswordEntry(
                service.trim(), account.trim(), finalPassword,
                upperCheck.isSelected(), lowerCheck.isSelected(),
                numberCheck.isSelected(), symbolCheck.isSelected(),
                lengthSpinner.getValue());

        if (editingEntry != null) {
            // Estamos editando: remove o objeto antigo específico da lista.
            entries.remove(editingEntry);
            revealedEntries.remove(editingEntry);
        } else {
            // Não estamos editando: se já existir uma credencial pra esse
            // mesmo serviço+conta, ela é substituída (evita duplicar linhas).
            entries.removeIf(existing -> existing.getService().equalsIgnoreCase(service.trim())
                    && existing.getAccount().equalsIgnoreCase(account.trim()));
        }
        entries.add(newEntry);
        storage.saveAll(entries); // reescreve o arquivo .txt inteiro com a lista atualizada

        setStatus("Credencial " + service.trim() + " salva com sucesso", true);
        editingEntry = null;
        clearForm();
    }

    /**
     * Botão "Editar selecionado": copia os dados da linha escolhida para o
     * formulário, deixando o campo de senha em branco (a senha real só
     * aparece se o usuário clicar em "Ver senha" antes, ou digitar uma nova).
     */
    private void onEdit() {
        PasswordEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecione uma senha na lista para editar.");
            return;
        }
        editingEntry = selected; // marca "modo edição" — ver onSave()
        serviceField.setText(selected.getService());
        accountField.setText(selected.getAccount());
        passwordField.clear();
        passwordField.setPromptText("Deixe em branco para manter a senha atual, ou gere uma nova");
        setPasswordVisible(false);
        upperCheck.setSelected(selected.isUseUpper());
        lowerCheck.setSelected(selected.isUseLower());
        numberCheck.setSelected(selected.isUseNumbers());
        symbolCheck.setSelected(selected.isUseSymbols());
        lengthSpinner.getValueFactory().setValue(selected.getLength());
        setStatus("Editando " + selected.getService() + "...", false);
    }

    /**
     * Botão "Ver senha": só revela a senha real depois de confirmar a senha
     * mestre. Depois de confirmada, a senha aparece tanto no formulário
     * quanto na própria linha da tabela.
     */
    private void onReveal() {
        PasswordEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecione uma senha na lista para revelar.");
            return;
        }
        if (!askMasterPassword()) {
            return; // usuário cancelou ou errou a senha mestre
        }
        passwordField.setText(selected.getPassword());
        setPasswordVisible(true);
        revealedEntries.add(selected);
        table.refresh(); // força a tabela a recalcular as células (senão a linha não atualizaria sozinha)
        setStatus("Senha revelada para " + selected.getService() + ".", true);
    }

    /**
     * Botão "Excluir selecionado": pede confirmação antes (ação destrutiva e
     * irreversível) e só então remove a credencial da lista e do arquivo.
     */
    private void onDelete() {
        PasswordEntry selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Selecione uma senha na lista para excluir.");
            return;
        }
        if (!confirmDelete(selected)) {
            return;
        }
        entries.remove(selected);
        revealedEntries.remove(selected);
        storage.saveAll(entries);
        setStatus("Credencial removida.", true);
    }

    /** Mostra um alerta de confirmação e devolve true só se o usuário clicar em "Excluir". */
    private boolean confirmDelete(PasswordEntry entry) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar exclusão");
        alert.setHeaderText("Excluir a credencial de " + entry.getService() + "?");
        alert.setContentText("Essa ação não pode ser desfeita.");
        applyTheme(alert.getDialogPane());

        // Trocamos os botões padrão (que viriam em inglês) por versões em
        // português, cada uma com seu próprio ButtonType.
        ButtonType deleteType = new ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(deleteType, cancelType);

        Optional<ButtonType> result = alert.showAndWait(); // pausa aqui até o usuário responder
        return result.isPresent() && result.get() == deleteType;
    }

    /** Botão "Copiar senha": joga o conteúdo do campo de senha na área de transferência do Windows. */
    private void onCopy() {
        if (passwordField.getText() == null || passwordField.getText().isBlank()) {
            showError("Não há senha para copiar.");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(passwordField.getText());
        Clipboard.getSystemClipboard().setContent(content);
        setStatus("✓ Senha copiada!", true);
    }

    /**
     * Chamado quando o usuário sai do campo "Serviço". Se já existe uma
     * credencial salva pra esse mesmo serviço, copia as opções de geração
     * usadas da última vez (maiúsculas, símbolos, tamanho...), pra manter
     * consistência sem o usuário precisar lembrar/reconfigurar tudo de novo.
     */
    private void loadPreferencesForService(String service) {
        if (service == null || service.isBlank()) {
            return;
        }
        // Stream + filter + findFirst: percorre a lista procurando a primeira
        // credencial cujo serviço bate (ignorando maiúsculas/minúsculas).
        Optional<PasswordEntry> existing = entries.stream()
                .filter(entry -> entry.getService().equalsIgnoreCase(service.trim()))
                .findFirst();
        // ifPresent só executa o bloco se realmente encontrou alguma coisa.
        existing.ifPresent(entry -> {
            upperCheck.setSelected(entry.isUseUpper());
            lowerCheck.setSelected(entry.isUseLower());
            numberCheck.setSelected(entry.isUseNumbers());
            symbolCheck.setSelected(entry.isUseSymbols());
            lengthSpinner.getValueFactory().setValue(entry.getLength());
            setStatus("Preferências carregadas para " + entry.getService() + ".", false);
        });
    }

    /** Limpa o formulário e sai do "modo edição", se estiver nele. */
    private void clearForm() {
        serviceField.clear();
        accountField.clear();
        passwordField.clear();
        passwordField.setPromptText("gere ou digite uma senha");
        setPasswordVisible(false);
        editingEntry = null;
        table.getSelectionModel().clearSelection();
    }

    /**
     * Atualiza a mensagem de status embaixo dos botões, trocando a classe CSS
     * pra deixar o texto verde (sucesso) ou cinza (mensagem neutra/informativa).
     */
    private void setStatus(String message, boolean success) {
        statusLabel.getStyleClass().removeAll("status-success", "status-neutral");
        statusLabel.getStyleClass().add(success ? "status-success" : "status-neutral");
        statusLabel.setText(message);
    }

    /**
     * Prepara o cofre antes da tela principal aparecer. Existem três caminhos:
     *
     * 1. PRIMEIRA EXECUÇÃO (não há senha mestre cadastrada):
     *    pede para criar uma senha mestre e cria um cofre criptografado vazio.
     *
     * 2. ATUALIZAÇÃO DA VERSÃO ANTIGA (existe senha mestre, mas ainda não
     *    existe cofre criptografado): pede a senha e converte o antigo
     *    passwords.txt em texto puro para o novo formato criptografado.
     *
     * 3. USO NORMAL (cofre criptografado já existe): pede a senha mestre e
     *    desbloqueia o cofre.
     *
     * Devolve false se o usuário fechar/cancelar a janela — nesse caso o app
     * simplesmente não abre.
     */
    private boolean setUpVault() {
        if (!masterPasswordService.isRegistered()) {
            String newPassword = askNewMasterPassword();
            if (newPassword == null) {
                return false;
            }
            masterPasswordService.register(newPassword);
            storage = PasswordStorage.createNew(newPassword.toCharArray());
            return true;
        }

        String password = askMasterPasswordToUnlock();
        if (password == null) {
            return false;
        }

        if (!PasswordStorage.vaultExists()) {
            // Caminho 2: primeira abertura depois de atualizar o app. O
            // createNew() cuida de importar as credenciais antigas e apagar
            // o arquivo em texto puro.
            storage = PasswordStorage.createNew(password.toCharArray());
            return true;
        }

        try {
            storage = PasswordStorage.unlock(password.toCharArray());
            return true;
        } catch (VaultCrypto.WrongPasswordException e) {
            // A senha já foi conferida contra o hash antes de chegar aqui, então
            // isso indica que o arquivo do cofre foi alterado ou corrompido.
            showError(e.getMessage());
            return false;
        }
    }

    /**
     * Janela da primeira execução: pede para o usuário criar uma senha mestre
     * (digitada duas vezes, para evitar erro de digitação). Devolve a senha
     * escolhida, ou null se o usuário fechar a janela sem cadastrar.
     */
    private String askNewMasterPassword() {
        // Dialog<String> é uma janela modal (trava o resto do app até fechar)
        // que devolve um valor do tipo String quando o usuário confirma.
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(APP_NAME + " - Configuração inicial");
        dialog.setHeaderText("Crie uma senha mestre para proteger suas senhas salvas.\n"
                + "Ela será pedida sempre que você quiser revelar uma senha.");
        applyTheme(dialog.getDialogPane());

        PasswordField pass1 = new PasswordField();
        pass1.setPromptText("Senha mestre");
        PasswordField pass2 = new PasswordField();
        pass2.setPromptText("Confirmar senha mestre");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        VBox content = new VBox(10, pass1, pass2, error);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("Cadastrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(okType);

        // Isso aqui é o "truque" pra VALIDAR antes de deixar a janela fechar:
        // pegamos o botão real (Node) que representa okType e adicionamos um
        // "filtro" de evento. Se chamarmos event.consume(), o clique é
        // cancelado e a janela continua aberta — é assim que mostramos o erro
        // sem fechar o diálogo.
        Node okButton = dialog.getDialogPane().lookupButton(okType);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String p1 = pass1.getText();
            String p2 = pass2.getText();
            if (p1 == null || p1.isBlank()) {
                error.setText("Digite uma senha mestre.");
                event.consume();
            } else if (!p1.equals(p2)) {
                error.setText("As senhas não coincidem.");
                event.consume();
            }
        });

        // resultConverter decide o que o showAndWait() vai devolver quando a
        // janela fechar, dependendo de qual botão foi clicado.
        dialog.setResultConverter(bt -> bt == okType ? pass1.getText() : null);
        Optional<String> result = dialog.showAndWait(); // bloqueia até o usuário fechar a janela
        return result.orElse(null);
    }

    /**
     * Janela mostrada toda vez que o app abre (a partir da segunda execução):
     * pede a senha mestre para desbloquear o cofre. Devolve a senha digitada,
     * ou null se o usuário cancelar.
     *
     * A senha é conferida contra o hash salvo (MasterPasswordService) antes de
     * a janela fechar, então quem sai daqui com uma senha em mãos tem certeza
     * de que ela é a correta.
     */
    private String askMasterPasswordToUnlock() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(APP_NAME);
        dialog.setHeaderText("Digite a senha mestre para abrir o cofre.");
        applyTheme(dialog.getDialogPane());

        PasswordField field = new PasswordField();
        field.setPromptText("Senha mestre");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        VBox content = new VBox(10, field, error);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("Abrir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        Node okButton = dialog.getDialogPane().lookupButton(okType);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!masterPasswordService.verify(field.getText())) {
                error.setText("Senha mestre incorreta.");
                event.consume(); // cancela o clique: a janela continua aberta
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        return (result.isPresent() && result.get() == okType) ? field.getText() : null;
    }

    /**
     * Mostra a janela "digite a senha mestre" usada antes de revelar uma senha.
     * Devolve true só se a senha digitada bater com a cadastrada.
     *
     * Sim, isso é redundante com o desbloqueio da abertura — e é de propósito:
     * protege o caso de o app ficar aberto e sem supervisão, para que ninguém
     * consiga ver as senhas apenas clicando em "Ver senha".
     */
    private boolean askMasterPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmação necessária");
        dialog.setHeaderText("Digite a senha mestre para revelar a senha.");
        applyTheme(dialog.getDialogPane());

        PasswordField field = new PasswordField();
        field.setPromptText("Senha mestre");
        Label error = new Label();
        error.getStyleClass().add("error-label");
        VBox content = new VBox(10, field, error);
        dialog.getDialogPane().setContent(content);

        ButtonType okType = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(okType, cancelType);

        // Mesmo truque do askNewMasterPassword(): se a senha estiver errada,
        // consumimos o evento e a janela continua aberta mostrando o erro.
        Node okButton = dialog.getDialogPane().lookupButton(okType);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!masterPasswordService.verify(field.getText())) {
                error.setText("Senha mestre incorreta.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        return result.isPresent() && result.get() == okType;
    }

    /** Aplica o mesmo tema escuro (style.css) em janelas de diálogo/alerta. */
    private void applyTheme(DialogPane pane) {
        pane.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
    }

    /** Atalho pra mostrar uma janelinha de erro simples, já com o tema aplicado. */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    /**
     * Ponto de entrada "de verdade" do programa (o que o Java chama primeiro).
     * launch(args) é um método herdado de Application que prepara o motor
     * gráfico do JavaFX e, quando tudo está pronto, chama o nosso start(stage).
     */
    public static void main(String[] args) {
        launch(args);
    }
}
