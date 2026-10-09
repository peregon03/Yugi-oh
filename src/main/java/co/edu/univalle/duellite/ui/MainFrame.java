package co.edu.univalle.duellite.ui;

import co.edu.univalle.duellite.api.ApiException;
import co.edu.univalle.duellite.api.YgoApiClient;
import co.edu.univalle.duellite.logic.BattleListener;
import co.edu.univalle.duellite.logic.Duel;
import co.edu.univalle.duellite.model.Card;
import co.edu.univalle.duellite.model.Position;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Ventana principal del juego Yu-Gi-Oh! Duel Lite.
 *
 * Implementa {@link BattleListener} para recibir los eventos del duelo
 * y actualizar la interfaz de forma desacoplada de la lógica.
 *
 * Uso de hilos:
 * - Las peticiones HTTP y la descarga de imágenes se hacen en SwingWorker.
 * - Los métodos de BattleListener son llamados desde SwingUtilities.invokeLater
 *   (dentro de Duel.jugarTurno, que corre en el EDT al ser disparado por botones).
 */
public class MainFrame extends JFrame implements BattleListener {

    // ---- Paleta de colores del tema ----
    private static final Color COLOR_FONDO         = new Color(15, 15, 30);
    private static final Color COLOR_PANEL_JUGADOR  = new Color(20, 40, 20);
    private static final Color COLOR_PANEL_MAQUINA  = new Color(40, 20, 20);
    private static final Color COLOR_TEXTO          = new Color(220, 220, 255);
    private static final Color COLOR_LOG_FONDO      = new Color(10, 10, 20);
    private static final Color COLOR_LOG_TEXTO      = new Color(180, 255, 180);

    // Colores para botones
    private static final Color COLOR_BTN_INICIAR       = new Color(185, 125, 0);  // Ámbar
    private static final Color COLOR_BTN_INICIAR_HOVER = new Color(220, 160, 0);  // Ámbar claro
    private static final Color COLOR_BTN_JUGAR         = new Color(120, 60, 200); // Púrpura
    private static final Color COLOR_BTN_JUGAR_HOVER   = new Color(150, 90, 230); // Púrpura claro
    private static final Color COLOR_BTN_TEXTO         = Color.BLACK;             // Negro para todos

    // Colores para los diálogos JOptionPane
    private static final Color COLOR_DIALOGO_FONDO  = new Color(20, 20, 45);
    private static final Color COLOR_DIALOGO_TEXTO  = new Color(220, 220, 255);
    private static final Color COLOR_DIALOGO_BTN    = new Color(55, 25, 100);
    private static final Color COLOR_DIALOGO_BORDE  = new Color(110, 70, 190);

    // ---- Componentes de la UI ----
    private JButton btnIniciar;
    private JButton btnJugar;
    private JLabel lblMarcador;
    private JLabel lblTurno;
    private JLabel lblEstado;
    private JProgressBar progressBar;
    private JTextArea areaLog;

    private JRadioButton rbAtaque;
    private JRadioButton rbDefensa;

    // Paneles de cartas: índices 0, 1, 2
    private CardPanel[] panelsJugador;
    private CardPanel[] panelsMaquina;

    // ---- Estado del juego ----
    private Duel duelActual;
    private List<Card> cartasJugador;
    private List<Card> cartasMaquina;
    private int cartaSeleccionadaIndex = -1;

    private final YgoApiClient apiClient;

    // ---- Constructor ----

    public MainFrame() {
        super("Yu-Gi-Oh! Duel Lite - Laboratorio #1");
        this.apiClient = new YgoApiClient();
        inicializarUI();
        configurarVentana();
    }

    // ====================================================================
    // Inicialización de la UI
    // ====================================================================

    private void inicializarUI() {
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(COLOR_FONDO);

        // Estilizar los diálogos JOptionPane para que sigan la paleta del juego.
        // Solo afecta a paneles que NO tienen setBackground() propio (como el interior
        // de los JOptionPane), no a nuestros paneles que sí lo tienen definido.
        UIManager.put("OptionPane.background",        COLOR_DIALOGO_FONDO);
        UIManager.put("Panel.background",             COLOR_DIALOGO_FONDO);
        UIManager.put("OptionPane.messageForeground", COLOR_DIALOGO_TEXTO);
        UIManager.put("Button.background",            COLOR_DIALOGO_BTN);
        UIManager.put("Button.foreground",            Color.BLACK);
        UIManager.put("Button.select",                COLOR_DIALOGO_BTN.darker());
        UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_DIALOGO_BORDE, 1),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)
        ));

        // ---- Panel superior: marcador y turno ----
        JPanel panelSuperior = crearPanelSuperior();
        add(panelSuperior, BorderLayout.NORTH);

        // ---- Panel central: cartas de ambos jugadores ----
        JPanel panelCentral = crearPanelCentral();
        add(panelCentral, BorderLayout.CENTER);

        // ---- Panel inferior: controles y log ----
        JPanel panelInferior = crearPanelInferior();
        add(panelInferior, BorderLayout.SOUTH);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 4, 4));
        panel.setBackground(new Color(25, 25, 50));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        lblMarcador = new JLabel("Jugador 0  -  0 Máquina", SwingConstants.CENTER);
        lblMarcador.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblMarcador.setForeground(new Color(255, 215, 0));  // Dorado

        lblTurno = new JLabel("Presiona 'Iniciar Duelo' para comenzar", SwingConstants.CENTER);
        lblTurno.setFont(new Font("SansSerif", Font.ITALIC, 14));
        lblTurno.setForeground(COLOR_TEXTO);

        panel.add(lblMarcador);
        panel.add(lblTurno);
        return panel;
    }

    private JPanel crearPanelCentral() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 10, 0));
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Panel del jugador (izquierda)
        JPanel panelJugador = new JPanel(new BorderLayout(4, 4));
        panelJugador.setBackground(COLOR_PANEL_JUGADOR);
        panelJugador.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0, 150, 0), 2),
                "Tu Campo",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(0, 200, 0)
        ));

        panelsJugador = new CardPanel[3];
        JPanel filaCJ = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        filaCJ.setBackground(COLOR_PANEL_JUGADOR);
        for (int i = 0; i < 3; i++) {
            panelsJugador[i] = new CardPanel(false);
            final int idx = i;
            panelsJugador[i].setOnSeleccionado(() -> seleccionarCarta(idx));
            filaCJ.add(panelsJugador[i]);
        }
        panelJugador.add(filaCJ, BorderLayout.CENTER);

        // Panel de la máquina (derecha)
        JPanel panelMaquina = new JPanel(new BorderLayout(4, 4));
        panelMaquina.setBackground(COLOR_PANEL_MAQUINA);
        panelMaquina.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 0, 0), 2),
                "Campo de la Máquina",
                javax.swing.border.TitledBorder.CENTER,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12),
                new Color(200, 0, 0)
        ));

        panelsMaquina = new CardPanel[3];
        JPanel filaCM = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        filaCM.setBackground(COLOR_PANEL_MAQUINA);
        for (int i = 0; i < 3; i++) {
            panelsMaquina[i] = new CardPanel(true);  // Boca abajo
            filaCM.add(panelsMaquina[i]);
        }
        panelMaquina.add(filaCM, BorderLayout.CENTER);

        panel.add(panelJugador);
        panel.add(panelMaquina);
        return panel;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(COLOR_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        // ---- Subpanel de controles (botones + posición) ----
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        panelControles.setBackground(new Color(20, 20, 40));
        panelControles.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 100), 1));

        btnIniciar = new JButton("⚔  Iniciar Duelo");
        estilizarBoton(btnIniciar, COLOR_BTN_INICIAR, COLOR_BTN_INICIAR_HOVER, COLOR_BTN_TEXTO);
        btnIniciar.addActionListener(e -> iniciarDuelo());

        btnJugar = new JButton("▶  Jugar Carta Seleccionada");
        estilizarBoton(btnJugar, COLOR_BTN_JUGAR, COLOR_BTN_JUGAR_HOVER, COLOR_BTN_TEXTO);
        btnJugar.setEnabled(false);
        btnJugar.addActionListener(e -> jugarCartaSeleccionada());

        // Selector de posición
        JLabel lblPosicion = new JLabel("Tu posición:");
        lblPosicion.setForeground(COLOR_TEXTO);
        rbAtaque  = new JRadioButton("Ataque",  true);
        rbDefensa = new JRadioButton("Defensa", false);
        estilizarRadioButton(rbAtaque);
        estilizarRadioButton(rbDefensa);
        ButtonGroup grupoPosicion = new ButtonGroup();
        grupoPosicion.add(rbAtaque);
        grupoPosicion.add(rbDefensa);

        // Barra de progreso e indicador de estado
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(120, 14));
        progressBar.setBackground(new Color(30, 30, 60));
        progressBar.setForeground(new Color(140, 90, 0));  // Ámbar, igual que btnIniciar
        progressBar.setBorderPainted(false);

        lblEstado = new JLabel(" ");
        lblEstado.setForeground(new Color(255, 200, 100));
        lblEstado.setFont(new Font("SansSerif", Font.ITALIC, 11));

        panelControles.add(btnIniciar);
        panelControles.add(new JSeparator(SwingConstants.VERTICAL));
        panelControles.add(lblPosicion);
        panelControles.add(rbAtaque);
        panelControles.add(rbDefensa);
        panelControles.add(new JSeparator(SwingConstants.VERTICAL));
        panelControles.add(btnJugar);
        panelControles.add(progressBar);
        panelControles.add(lblEstado);

        // ---- Log de batalla ----
        areaLog = new JTextArea(8, 50);
        areaLog.setEditable(false);
        areaLog.setBackground(COLOR_LOG_FONDO);
        areaLog.setForeground(COLOR_LOG_TEXTO);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaLog.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(50, 50, 100), 1),
                "Log de Batalla",
                javax.swing.border.TitledBorder.LEFT,
                javax.swing.border.TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 11),
                new Color(150, 150, 255)
        ));

        panel.add(panelControles, BorderLayout.NORTH);
        panel.add(scrollLog, BorderLayout.CENTER);
        return panel;
    }

    private void configurarVentana() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 700));
        pack();
        setLocationRelativeTo(null);  // Centrar en la pantalla
    }

    // ====================================================================
    // Lógica de inicio del duelo
    // ====================================================================

    /**
     * Inicia un nuevo duelo: descarga 6 cartas Monster aleatorias (3 por jugador)
     * usando SwingWorker para no bloquear el EDT.
     */
    private void iniciarDuelo() {
        // Deshabilitar controles mientras carga
        btnIniciar.setEnabled(false);
        btnJugar.setEnabled(false);
        cartaSeleccionadaIndex = -1;

        // Resetear paneles de cartas a su estado inicial
        for (int i = 0; i < 3; i++) {
            panelsJugador[i].resetear(false);
            panelsMaquina[i].resetear(true);
        }

        // Mostrar indicador de carga
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
        lblEstado.setText("Cargando cartas...");
        lblMarcador.setText("Jugador 0  -  0 Máquina");
        lblTurno.setText("Cargando cartas...");

        registrarLog("═══════════════════════════════════");
        registrarLog("  ¡Nuevo Duelo iniciado!");
        registrarLog("  Descargando cartas de la API...");
        registrarLog("═══════════════════════════════════");

        cartasJugador = new ArrayList<>();
        cartasMaquina = new ArrayList<>();

        // SwingWorker que descarga las 6 cartas en segundo plano
        SwingWorker<List<Card>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Card> doInBackground() throws Exception {
                List<Card> todasLasCartas = new ArrayList<>();
                // Descargar 3 cartas para el jugador y 3 para la máquina
                for (int i = 0; i < 6; i++) {
                    todasLasCartas.add(apiClient.fetchRandomMonster());
                }
                return todasLasCartas;
            }

            @Override
            protected void done() {
                // Este método corre en el EDT
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);

                try {
                    List<Card> cartas = get();
                    cartasJugador = cartas.subList(0, 3);
                    cartasMaquina = cartas.subList(3, 6);

                    // Asignar cartas a los paneles
                    for (int i = 0; i < 3; i++) {
                        panelsJugador[i].setCarta(cartasJugador.get(i));
                        panelsMaquina[i].setCarta(cartasMaquina.get(i));
                    }

                    lblEstado.setText("Cartas cargadas. ¡Listo para duelo!");
                    registrarLog("✓ Cartas del jugador cargadas.");
                    registrarLog("✓ Cartas de la máquina cargadas (boca abajo).");

                    // Descargar imágenes en segundo plano
                    descargarImagenesAsync();

                    // Crear el duelo
                    duelActual = new Duel(cartasJugador, cartasMaquina, MainFrame.this);
                    duelActual.notificarInicioTurno();

                    btnIniciar.setEnabled(true);
                    btnJugar.setEnabled(false); // Se habilita al seleccionar carta

                } catch (Exception ex) {
                    manejarErrorCarga(ex);
                }
            }
        };

        worker.execute();
    }

    /**
     * Descarga las imágenes de las 6 cartas de forma asíncrona.
     * Actualiza cada CardPanel cuando su imagen esté lista.
     */
    private void descargarImagenesAsync() {
        List<Card> todasLasCartas = new ArrayList<>();
        todasLasCartas.addAll(cartasJugador);
        todasLasCartas.addAll(cartasMaquina);

        // Un SwingWorker por cada carta para máxima paralelización
        for (int i = 0; i < todasLasCartas.size(); i++) {
            final Card carta = todasLasCartas.get(i);
            final boolean esDelJugador = i < 3;
            final int indexPanel = esDelJugador ? i : i - 3;

            SwingWorker<ImageIcon, Void> imgWorker = new SwingWorker<>() {
                @Override
                protected ImageIcon doInBackground() {
                    if (carta.getImageUrl() == null || carta.getImageUrl().isEmpty()) {
                        return null;
                    }
                    try {
                        URL url = new URL(carta.getImageUrl());
                        return new ImageIcon(url);
                    } catch (Exception e) {
                        // Si falla la imagen, la carta sigue funcionando con placeholder
                        return null;
                    }
                }

                @Override
                protected void done() {
                    try {
                        ImageIcon icono = get();
                        if (icono != null) {
                            carta.setImagen(icono);
                            // Solo actualizar la imagen si es del jugador (la máquina está boca abajo)
                            if (esDelJugador) {
                                panelsJugador[indexPanel].actualizarImagen();
                            }
                        }
                    } catch (Exception e) {
                        // Imagen no disponible; el placeholder ya está mostrándose
                    }
                }
            };
            imgWorker.execute();
        }
    }

    /**
     * Maneja errores durante la carga de cartas.
     */
    private void manejarErrorCarga(Exception ex) {
        String mensaje;
        if (ex.getCause() instanceof ApiException) {
            mensaje = ex.getCause().getMessage();
        } else {
            mensaje = "Error inesperado: " + ex.getMessage();
        }
        registrarLog("✗ Error: " + mensaje);
        lblEstado.setText("Error al cargar. Intente de nuevo.");
        JOptionPane.showMessageDialog(
                this,
                "No se pudieron cargar las cartas:\n" + mensaje,
                "Error de carga",
                JOptionPane.ERROR_MESSAGE
        );
        btnIniciar.setEnabled(true);
    }

    // ====================================================================
    // Lógica de selección y juego de cartas
    // ====================================================================

    /**
     * Maneja la selección de una carta del jugador por clic en el panel.
     */
    private void seleccionarCarta(int index) {
        // Solo se puede seleccionar si el duelo está activo y es turno del jugador
        if (duelActual == null || duelActual.isDuelTerminado()) return;

        // Ignorar cartas ya usadas
        if (panelsJugador[index].isUsada()) return;

        // Deseleccionar la anterior
        if (cartaSeleccionadaIndex >= 0) {
            panelsJugador[cartaSeleccionadaIndex].setSeleccionada(false);
        }

        // Seleccionar la nueva
        cartaSeleccionadaIndex = index;
        panelsJugador[index].setSeleccionada(true);

        // Habilitar el botón de jugar
        btnJugar.setEnabled(true);
    }

    /**
     * Ejecuta el turno con la carta seleccionada por el jugador.
     */
    private void jugarCartaSeleccionada() {
        if (duelActual == null || duelActual.isDuelTerminado()) return;
        if (cartaSeleccionadaIndex < 0) {
            JOptionPane.showMessageDialog(this,
                    "Debes seleccionar una carta primero.",
                    "Sin selección",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Determinar la posición elegida por el jugador
        Position posicion = rbAtaque.isSelected() ? Position.ATAQUE : Position.DEFENSA;

        // Deshabilitar el botón mientras se procesa
        btnJugar.setEnabled(false);

        // Ejecutar el turno (esto dispara los métodos de BattleListener)
        try {
            duelActual.jugarTurno(cartaSeleccionadaIndex, posicion);
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            btnJugar.setEnabled(true);
        }
    }

    // ====================================================================
    // Implementación de BattleListener
    // ====================================================================

    @Override
    public void onTurnStarted(String quienAtaca) {
        lblTurno.setText("Turno de: " + quienAtaca + " (ATACA)");
        registrarLog("──────────────────────────────────");
        registrarLog("► Turno de: " + quienAtaca);

        // Si le toca a la máquina atacar, le habilitamos el botón de igual forma
        // porque el jugador también debe elegir su carta (y su posición lo define como defensor)
        if (!duelActual.isDuelTerminado()) {
            btnJugar.setEnabled(cartaSeleccionadaIndex >= 0);
        }
    }

    @Override
    public void onTurn(String descripcionJugador, String descripcionMaquina, String ganadorTurno) {
        // Obtener el índice de la carta de la máquina que se acaba de jugar
        List<Integer> usadasMaquina = duelActual.getUsadasMaquina();
        int ultimaUsadaMaquina = usadasMaquina.get(usadasMaquina.size() - 1);

        // Revelar la carta de la máquina
        panelsMaquina[ultimaUsadaMaquina].revelar();
        panelsMaquina[ultimaUsadaMaquina].actualizarImagen();

        // Marcar cartas como usadas en los paneles
        panelsJugador[cartaSeleccionadaIndex].setUsada(true);
        panelsJugador[cartaSeleccionadaIndex].setSeleccionada(false);
        panelsMaquina[ultimaUsadaMaquina].setUsada(true);

        // Resetear selección
        cartaSeleccionadaIndex = -1;
        btnJugar.setEnabled(false);

        // Registrar en el log
        registrarLog("  Jugador: " + descripcionJugador);
        registrarLog("  Máquina: " + descripcionMaquina);
        registrarLog("  ★ Ganador del turno: " + ganadorTurno);
    }

    @Override
    public void onScoreChanged(int puntosJugador, int puntosMaquina) {
        lblMarcador.setText("Jugador " + puntosJugador + "  -  " + puntosMaquina + " Máquina");
        registrarLog("  Marcador: Jugador " + puntosJugador + " - " + puntosMaquina + " Máquina");
    }

    @Override
    public void onDuelEnded(String ganador) {
        registrarLog("═══════════════════════════════════");
        registrarLog("  ¡DUELO TERMINADO! Ganador: " + ganador.toUpperCase());
        registrarLog("═══════════════════════════════════");

        lblTurno.setText("¡Duelo terminado! Ganador: " + ganador);
        btnJugar.setEnabled(false);

        // Revelar todas las cartas de la máquina no usadas
        List<Integer> usadasMaquina = duelActual.getUsadasMaquina();
        for (int i = 0; i < 3; i++) {
            if (!usadasMaquina.contains(i)) {
                panelsMaquina[i].revelar();
                panelsMaquina[i].actualizarImagen();
            }
        }

        String mensajeDialogo = "¡DUELO TERMINADO!\n\n"
                + "Ganador: " + ganador + "\n\n"
                + "Marcador final:\n"
                + "Jugador: " + duelActual.getPuntosJugador() + " victorias\n"
                + "Máquina: " + duelActual.getPuntosMaquina() + " victorias\n\n"
                + "¿Deseas iniciar un nuevo duelo?";

        int respuesta = JOptionPane.showConfirmDialog(
                this,
                mensajeDialogo,
                "Fin del Duelo",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );

        if (respuesta == JOptionPane.YES_OPTION) {
            iniciarDuelo();
        }
    }

    // ====================================================================
    // Utilidades
    // ====================================================================

    /**
     * Agrega una línea al log de batalla y hace scroll automático al final.
     */
    private void registrarLog(String mensaje) {
        areaLog.append(mensaje + "\n");
        // Autoscroll al final
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    /**
     * Aplica el estilo temático a un botón con efecto hover.
     *
     * @param btn        El botón a estilizar
     * @param colorBase  Color de fondo en reposo
     * @param colorHover Color de fondo al pasar el mouse
     * @param colorTexto Color del texto
     */
    private void estilizarBoton(JButton btn, Color colorBase, Color colorHover, Color colorTexto) {
        btn.setBackground(colorBase);
        btn.setForeground(colorTexto);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);           // Necesario para que el fondo se pinte en todos los SO
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
<<<<<<< HEAD
                BorderFactory.createLineBorder(colorHover, 1),          // Borde con el color hover
                BorderFactory.createEmptyBorder(7, 16, 7, 16)           // Relleno interior
=======
            BorderFactory.createLineBorder(colorHover, 1),          // Borde con el color hover
            BorderFactory.createEmptyBorder(7, 16, 7, 16)           // Relleno interior
>>>>>>> 1d7e935 (ui: actualizar colores de botones y README con integrantes y capturas)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Efecto hover: cambia el fondo al entrar y salir con el mouse
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(colorHover);
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(colorBase);
            }
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(colorBase.darker());
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(colorHover);
            }
        });
    }

    private void estilizarRadioButton(JRadioButton rb) {
        rb.setBackground(new Color(20, 20, 40));
        rb.setForeground(COLOR_TEXTO);
        rb.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }
}