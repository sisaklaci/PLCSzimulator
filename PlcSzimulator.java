import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * PLC-szimulátor: létradiagrammal (ladder diagram) szerkeszthető és
 * futtatható programozható logikai vezérlő (PLC) szimulációja.
 *
 * A program hálózatokból (rungs) épül fel. Minden hálózat egy
 * "sorba kapcsolt - párhuzamos - sorba kapcsolt" (előtag / párhuzamos ágak /
 * utótag) érintkező-lánc, amely egy kimenő elemet (tekercs, set, reset,
 * időzítő vagy számláló) vezérel. Indítás után a program ciklikusan
 * (scan-ciklus) kiértékeli a hálózatokat, és a teljesítményáramlást
 * valós időben, színezve jeleníti meg a létradiagramon.
 */
public class PlcSzimulator extends JFrame {

    static final String PROGRAM = "PLC-szimulátor";

    // ================================================================ többnyelvűség (i18n)

    static String nyelv = "hu";
    static final Map<String, Map<String, String>> FORDITASOK = new LinkedHashMap<>();

    static void ford(String hu, String en, String de, String es, String fr, String pl) {
        FORDITASOK.computeIfAbsent("en", k -> new HashMap<>()).put(hu, en);
        FORDITASOK.computeIfAbsent("de", k -> new HashMap<>()).put(hu, de);
        FORDITASOK.computeIfAbsent("es", k -> new HashMap<>()).put(hu, es);
        FORDITASOK.computeIfAbsent("fr", k -> new HashMap<>()).put(hu, fr);
        FORDITASOK.computeIfAbsent("pl", k -> new HashMap<>()).put(hu, pl);
    }

    /** Lefordítja a magyar forrásszöveget az aktuális nyelvre; ha nincs fordítás, a magyar szöveget adja vissza. */
    static String tr(String hu) {
        if (hu == null || "hu".equals(nyelv)) return hu;
        Map<String, String> m = FORDITASOK.get(nyelv);
        if (m == null) return hu;
        String t = m.get(hu);
        return t != null ? t : hu;
    }

    static {
        ford("Magyar", "Hungarian", "Ungarisch", "Húngaro", "Hongrois", "Węgierski");
        ford("Angol", "English", "Englisch", "Inglés", "Anglais", "Angielski");
        ford("Német", "German", "Deutsch", "Alemán", "Allemand", "Niemiecki");
        ford("Spanyol", "Spanish", "Spanisch", "Español", "Espagnol", "Hiszpański");
        ford("Francia", "French", "Französisch", "Francés", "Français", "Francuski");
        ford("Lengyel", "Polish", "Polnisch", "Polaco", "Polonais", "Polski");
        ford("Nyelv", "Language", "Sprache", "Idioma", "Langue", "Język");

        // menü
        ford("Fájl", "File", "Datei", "Archivo", "Fichier", "Plik");
        ford("Új program", "New program", "Neues Programm", "Nuevo programa", "Nouveau programme", "Nowy program");
        ford("Megnyitás...", "Open...", "Öffnen...", "Abrir...", "Ouvrir...", "Otwórz...");
        ford("Mentés", "Save", "Speichern", "Guardar", "Enregistrer", "Zapisz");
        ford("Mentés másként...", "Save as...", "Speichern als...", "Guardar como...", "Enregistrer sous...", "Zapisz jako...");
        ford("Kilépés", "Exit", "Beenden", "Salir", "Quitter", "Zamknij");
        ford("Szerkesztés", "Edit", "Bearbeiten", "Editar", "Édition", "Edycja");
        ford("Címkék (tag-ek) szerkesztése...", "Edit tags...", "Tags bearbeiten...", "Editar etiquetas...", "Modifier les étiquettes...", "Edytuj tagi...");
        ford("Gépi kód", "Machine code", "Maschinencode", "Código máquina", "Code machine", "Kod maszynowy");
        ford("IL / STL megtekintése...", "View IL / STL...", "IL / STL anzeigen...", "Ver IL / STL...", "Afficher IL / STL...", "Zobacz IL / STL...");
        ford("Ellenőrzés", "Check", "Prüfung", "Comprobación", "Vérification", "Sprawdzanie");
        ford("Program ellenőrzése...", "Check program...", "Programm prüfen...", "Comprobar programa...", "Vérifier le programme...", "Sprawdź program...");
        ford("Hivatkozások keresése...", "Find references...", "Verweise suchen...", "Buscar referencias...", "Rechercher des références...", "Znajdź odwołania...");
        ford("Súgó", "Help", "Hilfe", "Ayuda", "Aide", "Pomoc");
        ford("Használat", "Usage", "Bedienung", "Instrucciones", "Utilisation", "Instrukcja obsługi");

        // súgó ablak
        ford("Létradiagram szerkesztése", "Editing the ladder diagram", "Bearbeiten des Kontaktplans", "Edición del diagrama de contactos", "Modification du schéma à contacts", "Edycja schematu drabinkowego");
        ford("Egy érintkező-helyre (szaggatott '+' négyzet) kattintva új érintkezőt adhat hozzá.",
                "Click a contact slot (dashed '+' box) to add a new contact.",
                "Klicken Sie auf einen Kontaktplatz (gestricheltes '+'-Feld), um einen neuen Kontakt hinzuzufügen.",
                "Haga clic en una posición de contacto (casilla punteada '+') para añadir un nuevo contacto.",
                "Cliquez sur un emplacement de contact (case en pointillés '+') pour ajouter un nouveau contact.",
                "Kliknij miejsce kontaktu (przerywany kwadrat '+'), aby dodać nowy kontakt.");
        ford("Meglévő érintkezőre kattintva szerkesztheti vagy törölheti azt.",
                "Click an existing contact to edit or delete it.",
                "Klicken Sie auf einen vorhandenen Kontakt, um ihn zu bearbeiten oder zu löschen.",
                "Haga clic en un contacto existente para editarlo o eliminarlo.",
                "Cliquez sur un contact existant pour le modifier ou le supprimer.",
                "Kliknij istniejący kontakt, aby go edytować lub usunąć.");
        ford("A kimenetre (tekercs / időzítő / számláló) kattintva szerkesztheti a kimenetet.",
                "Click the output (coil / timer / counter) to edit it.",
                "Klicken Sie auf den Ausgang (Spule / Zeitglied / Zähler), um ihn zu bearbeiten.",
                "Haga clic en la salida (bobina / temporizador / contador) para editarla.",
                "Cliquez sur la sortie (bobine / temporisateur / compteur) pour la modifier.",
                "Kliknij wyjście (cewka / timer / licznik), aby je edytować.");
        ford("A hálózat alatti szaggatott '+ ág' sorra kattintva párhuzamos ágat vehet fel.",
                "Click the dashed '+ branch' row below the network to add a parallel branch.",
                "Klicken Sie auf die gestrichelte Zeile '+ Zweig' unter dem Netzwerk, um einen Parallelzweig hinzuzufügen.",
                "Haga clic en la fila punteada '+ rama' debajo de la red para añadir una rama paralela.",
                "Cliquez sur la ligne en pointillés '+ branche' sous le réseau pour ajouter une branche parallèle.",
                "Kliknij przerywany wiersz '+ odgałęzienie' pod siecią, aby dodać równoległą gałąź.");
        ford("Egy ág elején lévő '✕'-re kattintva törölheti az adott ágat.",
                "Click the '✕' at the start of a branch to delete that branch.",
                "Klicken Sie auf das '✕' am Anfang eines Zweigs, um diesen Zweig zu löschen.",
                "Haga clic en la '✕' al principio de una rama para eliminar esa rama.",
                "Cliquez sur le '✕' au début d'une branche pour supprimer cette branche.",
                "Kliknij '✕' na początku gałęzi, aby ją usunąć.");
        ford("A hálózat bal oldali sávjára kattintva kijelölheti a hálózatot (eszköztár: + Hálózat / Hálózat törlése).",
                "Click the network's left-hand strip to select it (toolbar: + Network / Delete network).",
                "Klicken Sie auf den linken Streifen des Netzwerks, um es auszuwählen (Symbolleiste: + Netzwerk / Netzwerk löschen).",
                "Haga clic en la franja izquierda de la red para seleccionarla (barra de herramientas: + Red / Eliminar red).",
                "Cliquez sur la bande de gauche du réseau pour le sélectionner (barre d'outils : + Réseau / Supprimer le réseau).",
                "Kliknij lewy pasek sieci, aby ją wybrać (pasek narzędzi: + Sieć / Usuń sieć).");
        ford("A hálózat sávjára duplán kattintva szerkesztheti a hálózat megjegyzését (ezt új hálózat létrehozásakor is megkérdezi a program).",
                "Double-click the network's strip to edit its comment (also asked when creating a new network).",
                "Doppelklicken Sie auf den Netzwerkstreifen, um dessen Kommentar zu bearbeiten (wird auch beim Erstellen eines neuen Netzwerks abgefragt).",
                "Haga doble clic en la franja de la red para editar su comentario (también se pregunta al crear una nueva red).",
                "Double-cliquez sur la bande du réseau pour modifier son commentaire (également demandé lors de la création d'un nouveau réseau).",
                "Kliknij dwukrotnie pasek sieci, aby edytować jej komentarz (program pyta o to także przy tworzeniu nowej sieci).");
        ford("A kimenet szerkesztésénél a cél lehet Kimenet (Q) vagy Belső relé (M) is – Tekercs, Set vagy Reset típussal egyaránt (pl. M-re is lehet Set/Reset kapcsolást építeni).",
                "When editing an output, the target can be an Output (Q) or Internal relay (M) – with Coil, Set or Reset type alike (e.g. Set/Reset can also be built on M).",
                "Beim Bearbeiten eines Ausgangs kann das Ziel ein Ausgang (Q) oder ein internes Relais (M) sein – jeweils mit dem Typ Spule, Set oder Reset (z. B. kann Set/Reset auch auf M aufgebaut werden).",
                "Al editar una salida, el destino puede ser una Salida (Q) o un Relé interno (M), con tipo Bobina, Set o Reset por igual (por ejemplo, Set/Reset también se puede usar en M).",
                "Lors de la modification d'une sortie, la cible peut être une Sortie (Q) ou un Relais interne (M) – avec le type Bobine, Set ou Reset (par ex. Set/Reset peut aussi être construit sur M).",
                "Podczas edycji wyjścia celem może być Wyjście (Q) lub Przekaźnik wewnętrzny (M) – zarówno z typem Cewka, Set, jak i Reset (np. Set/Reset można zbudować także na M).");
        ford("Egy meglévő kontaktusra Ctrl+kattintással vágólapra másolhatja; egy üres '+' helyre Ctrl+kattintással beillesztheti a vágólapon lévő kontaktust.",
                "Ctrl+click an existing contact to copy it to the clipboard; Ctrl+click an empty '+' slot to paste the clipboard contact there.",
                "Strg+Klick auf einen vorhandenen Kontakt kopiert ihn in die Zwischenablage; Strg+Klick auf einen leeren '+'-Platz fügt den Kontakt aus der Zwischenablage dort ein.",
                "Ctrl+clic en un contacto existente lo copia al portapapeles; Ctrl+clic en una posición '+' vacía pega allí el contacto del portapapeles.",
                "Ctrl+clic sur un contact existant le copie dans le presse-papiers ; Ctrl+clic sur un emplacement '+' vide y colle le contact du presse-papiers.",
                "Ctrl+kliknięcie istniejącego kontaktu kopiuje go do schowka; Ctrl+kliknięcie pustego miejsca '+' wkleja tam kontakt ze schowka.");
        ford("Az eszköztár '▲ Fel' / '▼ Le' gombjaival mozgathatja a kijelölt hálózatot; a '⧉ Hálózat másolása' / 'Hálózat beillesztése' gombokkal egész hálózatot másolhat.",
                "Use the toolbar's '▲ Up' / '▼ Down' buttons to move the selected network; use '⧉ Copy network' / 'Paste network' to copy an entire network.",
                "Mit den Symbolleisten-Schaltflächen '▲ Hoch' / '▼ Runter' verschieben Sie das ausgewählte Netzwerk; mit '⧉ Netzwerk kopieren' / 'Netzwerk einfügen' kopieren Sie ein ganzes Netzwerk.",
                "Con los botones '▲ Arriba' / '▼ Abajo' de la barra de herramientas puede mover la red seleccionada; con '⧉ Copiar red' / 'Pegar red' puede copiar una red completa.",
                "Utilisez les boutons '▲ Haut' / '▼ Bas' de la barre d'outils pour déplacer le réseau sélectionné ; utilisez '⧉ Copier le réseau' / 'Coller le réseau' pour copier un réseau entier.",
                "Przyciskami '▲ Góra' / '▼ Dół' na pasku narzędzi przesuwasz wybraną sieć; przyciskami '⧉ Kopiuj sieć' / 'Wklej sieć' możesz skopiować całą sieć.");
        ford("Ctrl+Z / Ctrl+Y (vagy az eszköztár gombjai): visszavonás / ismét – a létra, a címkék, a HMI és az FBD szerkesztésére egyaránt vonatkozik.",
                "Ctrl+Z / Ctrl+Y (or the toolbar buttons): undo / redo – applies to editing the ladder, tags, HMI and FBD alike.",
                "Strg+Z / Strg+Y (oder die Symbolleisten-Schaltflächen): Rückgängig / Wiederholen – gilt gleichermaßen für die Bearbeitung von Kontaktplan, Tags, HMI und FBD.",
                "Ctrl+Z / Ctrl+Y (o los botones de la barra de herramientas): deshacer / rehacer – se aplica igualmente a la edición del diagrama de contactos, las etiquetas, el HMI y el FBD.",
                "Ctrl+Z / Ctrl+Y (ou les boutons de la barre d'outils) : annuler / rétablir – s'applique aussi bien à la modification du schéma à contacts, des étiquettes, du HMI et du FBD.",
                "Ctrl+Z / Ctrl+Y (lub przyciski na pasku narzędzi): Undo / Redo – dotyczy edycji schematu drabinkowego, tagów, HMI i FBD.");
        ford("Szimuláció", "Simulation", "Simulation", "Simulación", "Simulation", "Symulacja");
        ford("<b>Indítás</b>: elindítja a ciklikus kiértékelést (scan-ciklus) a megadott ciklusidővel.",
                "<b>Start</b>: starts the cyclic evaluation (scan cycle) with the given cycle time.",
                "<b>Start</b>: startet die zyklische Auswertung (Scan-Zyklus) mit der angegebenen Zykluszeit.",
                "<b>Inicio</b>: inicia la evaluación cíclica (ciclo de escaneo) con el tiempo de ciclo indicado.",
                "<b>Démarrer</b> : démarre l'évaluation cyclique (cycle de scrutation) avec le temps de cycle indiqué.",
                "<b>Start</b>: uruchamia cykliczną ewaluację (cykl skanowania) z podanym czasem cyklu.");
        ford("<b>Lépés</b>: leállított állapotban egyetlen scan-ciklust hajt végre.",
                "<b>Step</b>: executes a single scan cycle while stopped.",
                "<b>Schritt</b>: führt im angehaltenen Zustand einen einzelnen Scan-Zyklus aus.",
                "<b>Paso</b>: ejecuta un único ciclo de escaneo mientras está detenido.",
                "<b>Pas</b> : exécute un seul cycle de scrutation à l'arrêt.",
                "<b>Krok</b>: wykonuje jeden cykl skanowania w stanie zatrzymania.");
        ford("<b>Nullázás</b>: törli a kimenetek, belső relék, időzítők és számlálók állapotát.",
                "<b>Reset</b>: clears the state of outputs, internal relays, timers and counters.",
                "<b>Zurücksetzen</b>: löscht den Zustand von Ausgängen, internen Relais, Zeitgliedern und Zählern.",
                "<b>Reiniciar</b>: borra el estado de las salidas, relés internos, temporizadores y contadores.",
                "<b>Réinitialiser</b> : efface l'état des sorties, relais internes, temporisateurs et compteurs.",
                "<b>Reset</b>: czyści stan wyjść, przekaźników wewnętrznych, timerów i liczników.");
        ford("A bemenet gombokra (I) kattintva kapcsolhatja be/ki a bemeneteket, futás közben is.",
                "Click the input buttons (I) to switch inputs on/off, even while running.",
                "Klicken Sie auf die Eingangstasten (I), um Eingänge ein-/auszuschalten, auch während des Betriebs.",
                "Haga clic en los botones de entrada (I) para activar/desactivar las entradas, incluso durante la ejecución.",
                "Cliquez sur les boutons d'entrée (I) pour activer/désactiver les entrées, même en cours d'exécution.",
                "Kliknij przyciski wejść (I), aby włączać/wyłączać wejścia, nawet podczas działania programu.");
        ford("Funkcióblokk (FBD)", "Function block (FBD)", "Funktionsbaustein (FBD)", "Bloque de función (FBD)", "Bloc fonctionnel (FBD)", "Blok funkcyjny (FBD)");
        ford("A \"Funkcióblokk (FBD)\" fülön AND/OR/NOT/XOR, SR-tároló, TON, CTU, valamint felfutó/lefutó él (P/N) blokkokból építhet logikát, a létrával és a HMI-vel közös I/Q/M/T/C memórián.",
                "On the \"Function block (FBD)\" tab you can build logic from AND/OR/NOT/XOR, SR-latch, TON, CTU and rising/falling edge (P/N) blocks, sharing the same I/Q/M/T/C memory with the ladder and the HMI.",
                "Auf der Registerkarte „Funktionsbaustein (FBD)“ können Sie Logik aus AND/OR/NOT/XOR-, SR-Speicher-, TON-, CTU- sowie steigenden/fallenden Flanken-Bausteinen (P/N) aufbauen, die sich den I/Q/M/T/C-Speicher mit dem Kontaktplan und dem HMI teilen.",
                "En la pestaña «Bloque de función (FBD)» puede construir lógica con bloques AND/OR/NOT/XOR, biestable SR, TON, CTU y flanco ascendente/descendente (P/N), compartiendo la misma memoria I/Q/M/T/C con el diagrama de contactos y el HMI.",
                "Dans l'onglet « Bloc fonctionnel (FBD) », vous pouvez construire une logique à partir de blocs AND/OR/NOT/XOR, bascule SR, TON, CTU et front montant/descendant (P/N), partageant la même mémoire I/Q/M/T/C avec le schéma à contacts et le HMI.",
                "Na karcie „Blok funkcyjny (FBD)” można budować logikę z bloków AND/OR/NOT/XOR, zatrzasku SR, TON, CTU oraz detektora narastającego/opadającego zbocza (P/N), współdzieląc tę samą pamięć I/Q/M/T/C z drabinką i HMI.");
        ford("\"✎ FBD szerkesztés\" módban a legördülőből válassza az elhelyezendő blokk típusát, majd kattintson a vászonra.",
                "In \"✎ FBD edit\" mode, choose the block type to place from the dropdown, then click the canvas.",
                "Wählen Sie im Modus „✎ FBD bearbeiten“ den zu platzierenden Bausteintyp aus der Dropdown-Liste und klicken Sie dann auf die Zeichenfläche.",
                "En el modo «✎ Editar FBD», elija en el desplegable el tipo de bloque a colocar y luego haga clic en el lienzo.",
                "En mode « ✎ Modifier FBD », choisissez dans la liste déroulante le type de bloc à placer, puis cliquez sur la zone de dessin.",
                "W trybie „✎ Edycja FBD” wybierz z listy rozwijanej typ bloku do umieszczenia, a następnie kliknij na płótnie.");
        ford("Huzalozáshoz kattintson egy blokk kimeneti pontjára (jobb oldali kör), majd a célbemenetre (bal oldali kör).",
                "To wire, click a block's output pin (circle on the right), then the target input pin (circle on the left).",
                "Zum Verdrahten klicken Sie auf den Ausgangspin eines Blocks (Kreis rechts) und dann auf den Ziel-Eingangspin (Kreis links).",
                "Para cablear, haga clic en el pin de salida de un bloque (círculo a la derecha) y luego en el pin de entrada de destino (círculo a la izquierda).",
                "Pour câbler, cliquez sur la broche de sortie d'un bloc (cercle à droite), puis sur la broche d'entrée cible (cercle à gauche).",
                "Aby połączyć, kliknij pin wyjściowy bloku (kółko po prawej), a następnie docelowy pin wejściowy (kółko po lewej).");
        ford("Blokkra duplán kattintva szerkesztheti a tulajdonságait (típus, bemenetek száma, kötött cím, időzítő/számláló beállítás).",
                "Double-click a block to edit its properties (type, number of inputs, bound address, timer/counter setting).",
                "Doppelklicken Sie auf einen Block, um seine Eigenschaften zu bearbeiten (Typ, Anzahl der Eingänge, gebundene Adresse, Zeitglied-/Zähler-Einstellung).",
                "Haga doble clic en un bloque para editar sus propiedades (tipo, número de entradas, dirección vinculada, ajuste de temporizador/contador).",
                "Double-cliquez sur un bloc pour modifier ses propriétés (type, nombre d'entrées, adresse liée, réglage du temporisateur/compteur).",
                "Kliknij dwukrotnie blok, aby edytować jego właściwości (typ, liczba wejść, powiązany adres, ustawienie timera/licznika).");

        // eszköztár
        ford("▶ Indítás", "▶ Start", "▶ Start", "▶ Iniciar", "▶ Démarrer", "▶ Start");
        ford("■ Leállítás", "■ Stop", "■ Stopp", "■ Detener", "■ Arrêter", "■ Stop");
        ford("⏭ Lépés", "⏭ Step", "⏭ Schritt", "⏭ Paso", "⏭ Pas", "⏭ Krok");
        ford("⟲ Nullázás", "⟲ Reset", "⟲ Zurücksetzen", "⟲ Reiniciar", "⟲ Réinitialiser", "⟲ Reset");
        ford("↶ Visszavon", "↶ Undo", "↶ Rückgängig", "↶ Deshacer", "↶ Annuler", "↶ Wstecz");
        ford("↷ Ismét", "↷ Redo", "↷ Wiederholen", "↷ Rehacer", "↷ Rétablir", "↷ Dalej");
        ford("Ciklusidő: ", "Cycle time: ", "Zykluszeit: ", "Tiempo de ciclo: ", "Temps de cycle : ", "Czas cyklu: ");
        ford("+ Hálózat", "+ Network", "+ Netzwerk", "+ Red", "+ Réseau", "+ Sieć");
        ford("Hálózat törlése", "Delete network", "Netzwerk löschen", "Eliminar red", "Supprimer le réseau", "Usuń sieć");
        ford("▲ Fel", "▲ Up", "▲ Hoch", "▲ Arriba", "▲ Haut", "▲ Góra");
        ford("A kijelölt hálózat mozgatása egy pozícióval feljebb", "Move the selected network one position up", "Das ausgewählte Netzwerk eine Position nach oben verschieben", "Mover la red seleccionada una posición hacia arriba", "Déplacer le réseau sélectionné d'une position vers le haut", "Przesuń wybraną sieć o jedną pozycję w górę");
        ford("▼ Le", "▼ Down", "▼ Runter", "▼ Abajo", "▼ Bas", "▼ Dół");
        ford("A kijelölt hálózat mozgatása egy pozícióval lejjebb", "Move the selected network one position down", "Das ausgewählte Netzwerk eine Position nach unten verschieben", "Mover la red seleccionada una posición hacia abajo", "Déplacer le réseau sélectionné d'une position vers le bas", "Przesuń wybraną sieć o jedną pozycję w dół");
        ford("⧉ Hálózat másolása", "⧉ Copy network", "⧉ Netzwerk kopieren", "⧉ Copiar red", "⧉ Copier le réseau", "⧉ Kopiuj sieć");
        ford("Hálózat beillesztése", "Paste network", "Netzwerk einfügen", "Pegar red", "Coller le réseau", "Wklej sieć");
        ford("Nagyítás: ", "Zoom: ", "Zoom: ", "Zoom: ", "Zoom : ", "Powiększenie: ");

        // I/O panel
        ford("Bemenetek (I)", "Inputs (I)", "Eingänge (I)", "Entradas (I)", "Entrées (I)", "Wejścia (I)");
        ford("Kattintson a bemenet állapotának váltásához", "Click to toggle the input's state", "Klicken Sie, um den Zustand des Eingangs umzuschalten", "Haga clic para cambiar el estado de la entrada", "Cliquez pour basculer l'état de l'entrée", "Kliknij, aby zmienić stan wejścia");
        ford("Kimenetek (Q)", "Outputs (Q)", "Ausgänge (Q)", "Salidas (Q)", "Sorties (Q)", "Wyjścia (Q)");
        ford("Belső relék (M)", "Internal relays (M)", "Interne Relais (M)", "Relés internos (M)", "Relais internes (M)", "Przekaźniki wewnętrzne (M)");
        ford("Időzítők (T) / Számlálók (C)", "Timers (T) / Counters (C)", "Zeitglieder (T) / Zähler (C)", "Temporizadores (T) / Contadores (C)", "Temporisateurs (T) / Compteurs (C)", "Timery (T) / Liczniki (C)");
        ford("Számláló nullázása", "Reset counter", "Zähler zurücksetzen", "Reiniciar contador", "Réinitialiser le compteur", "Zeruj licznik");

        // létra vászon
        ford("Hálózat megjegyzése (opcionális):", "Network comment (optional):", "Netzwerkkommentar (optional):", "Comentario de la red (opcional):", "Commentaire du réseau (facultatif) :", "Komentarz sieci (opcjonalnie):");
        ford("Új hálózat", "New network", "Neues Netzwerk", "Nueva red", "Nouveau réseau", "Nowa sieć");
        ford("Hálózat megjegyzése:", "Network comment:", "Netzwerkkommentar:", "Comentario de la red:", "Commentaire du réseau :", "Komentarz sieci:");
        ford("Hálózat", "Network", "Netzwerk", "Red", "Réseau", "Sieć");
        ford("+ Új hálózat", "+ New network", "+ Neues Netzwerk", "+ Nueva red", "+ Nouveau réseau", "+ Nowa sieć");
        ford("+ ág", "+ branch", "+ Zweig", "+ rama", "+ branche", "+ gałąź");

        // közös gombok és érintkező-szerkesztő
        ford("OK", "OK", "OK", "OK", "OK", "OK");
        ford("Törlés", "Delete", "Löschen", "Eliminar", "Supprimer", "Usuń");
        ford("Mégse", "Cancel", "Abbrechen", "Cancelar", "Annuler", "Anuluj");
        ford("Index:", "Index:", "Index:", "Índice:", "Index :", "Indeks:");
        ford("Bemenet (I)", "Input (I)", "Eingang (I)", "Entrada (I)", "Entrée (I)", "Wejście (I)");
        ford("Kimenet (Q)", "Output (Q)", "Ausgang (Q)", "Salida (Q)", "Sortie (Q)", "Wyjście (Q)");
        ford("Belső relé (M)", "Internal relay (M)", "Internes Relais (M)", "Relé interno (M)", "Relais interne (M)", "Przekaźnik wewnętrzny (M)");
        ford("Időzítő kész (T.DN)", "Timer done (T.DN)", "Zeitglied fertig (T.DN)", "Temporizador terminado (T.DN)", "Temporisateur terminé (T.DN)", "Timer zakończony (T.DN)");
        ford("Számláló kész (C.DN)", "Counter done (C.DN)", "Zähler fertig (C.DN)", "Contador terminado (C.DN)", "Compteur terminé (C.DN)", "Licznik zakończony (C.DN)");
        ford("Záró (NO)", "Normally open (NO)", "Schließer (NO)", "Normalmente abierto (NO)", "Normalement ouvert (NO)", "Normalnie otwarty (NO)");
        ford("Nyitó (NC)", "Normally closed (NC)", "Öffner (NC)", "Normalmente cerrado (NC)", "Normalement fermé (NC)", "Normalnie zamknięty (NC)");
        ford("Érintkező típusa:", "Contact type:", "Kontakttyp:", "Tipo de contacto:", "Type de contact :", "Typ kontaktu:");
        ford("Operandus típusa:", "Operand type:", "Operandentyp:", "Tipo de operando:", "Type d'opérande :", "Typ operandu:");
        ford("Érintkező szerkesztése", "Edit contact", "Kontakt bearbeiten", "Editar contacto", "Modifier le contact", "Edytuj kontakt");

        // címke-szerkesztő
        ford("Cím", "Address", "Adresse", "Dirección", "Adresse", "Adres");
        ford("Név", "Name", "Name", "Nombre", "Nom", "Nazwa");
        ford("Megjegyzés", "Comment", "Kommentar", "Comentario", "Commentaire", "Komentarz");
        ford("Adjon nevet és/vagy megjegyzést a bemeneteknek, kimeneteknek, belső reléknek, időzítőknek és számlálóknak:",
                "Give a name and/or comment to the inputs, outputs, internal relays, timers and counters:",
                "Geben Sie den Eingängen, Ausgängen, internen Relais, Zeitgliedern und Zählern einen Namen und/oder Kommentar:",
                "Asigne un nombre y/o comentario a las entradas, salidas, relés internos, temporizadores y contadores:",
                "Donnez un nom et/ou un commentaire aux entrées, sorties, relais internes, temporisateurs et compteurs :",
                "Podaj nazwę i/lub komentarz dla wejść, wyjść, przekaźników wewnętrznych, timerów i liczników:");
        ford("Címkék (tag-ek) szerkesztése", "Edit tags", "Tags bearbeiten", "Editar etiquetas", "Modifier les étiquettes", "Edytuj tagi");

        // kimenet-szerkesztő
        ford("Tekercs (OTE)", "Coil (OTE)", "Spule (OTE)", "Bobina (OTE)", "Bobine (OTE)", "Cewka (OTE)");
        ford("Set (bekapcsolás)", "Set (turn on)", "Set (Einschalten)", "Set (activar)", "Set (activation)", "Set (włączenie)");
        ford("Reset (kikapcsolás)", "Reset (turn off)", "Reset (Ausschalten)", "Reset (desactivar)", "Reset (désactivation)", "Reset (wyłączenie)");
        ford("Időzítő (TON)", "Timer (TON)", "Zeitglied (TON)", "Temporizador (TON)", "Temporisateur (TON)", "Timer (TON)");
        ford("Számláló (CTU)", "Counter (CTU)", "Zähler (CTU)", "Contador (CTU)", "Compteur (CTU)", "Licznik (CTU)");
        ford("Negált (NOT) tekercs – csak Tekercs típusnál érvényes",
                "Negated (NOT) coil – only valid for Coil type",
                "Negierte (NOT) Spule – gilt nur für den Typ Spule",
                "Bobina negada (NOT) – solo válida para el tipo Bobina",
                "Bobine négative (NOT) – valable uniquement pour le type Bobine",
                "Negowana (NOT) cewka – dotyczy tylko typu Cewka");
        ford("Cél:", "Target:", "Ziel:", "Destino:", "Cible :", "Cel:");
        ford("Negálás:", "Negation:", "Negierung:", "Negación:", "Négation :", "Negacja:");
        ford("Időzítő index (T):", "Timer index (T):", "Zeitglied-Index (T):", "Índice de temporizador (T):", "Index du temporisateur (T) :", "Indeks timera (T):");
        ford("Beállított idő (ms):", "Preset time (ms):", "Voreingestellte Zeit (ms):", "Tiempo preestablecido (ms):", "Temps préréglé (ms) :", "Czas ustawiony (ms):");
        ford("Számláló index (C):", "Counter index (C):", "Zähler-Index (C):", "Índice de contador (C):", "Index du compteur (C) :", "Indeks licznika (C):");
        ford("Beállított érték:", "Preset value:", "Voreingestellter Wert:", "Valor preestablecido:", "Valeur préréglée :", "Wartość ustawiona:");
        ford("Kimenet típusa:", "Output type:", "Ausgangstyp:", "Tipo de salida:", "Type de sortie :", "Typ wyjścia:");
        ford("Kimenet szerkesztése", "Edit output", "Ausgang bearbeiten", "Editar salida", "Modifier la sortie", "Edytuj wyjście");

        // HMI widget-szerkesztő
        ford("Lámpa", "Lamp", "Lampe", "Lámpara", "Lampe", "Lampka");
        ford("Gomb", "Button", "Taste", "Botón", "Bouton", "Przycisk");
        ford("Kijelző", "Display", "Anzeige", "Pantalla", "Afficheur", "Wyświetlacz");
        ford("Felirat", "Label", "Beschriftung", "Etiqueta", "Étiquette", "Etykieta");
        ford("Téglalap", "Rectangle", "Rechteck", "Rectángulo", "Rectangle", "Prostokąt");
        ford("Időzítő (T)", "Timer (T)", "Zeitglied (T)", "Temporizador (T)", "Temporisateur (T)", "Timer (T)");
        ford("Számláló (C)", "Counter (C)", "Zähler (C)", "Contador (C)", "Compteur (C)", "Licznik (C)");
        ford("Számérték (T/C aktuális értéke) a kész-bit helyett",
                "Numeric value (T/C current value) instead of the done bit",
                "Zahlenwert (aktueller Wert von T/C) statt des Fertig-Bits",
                "Valor numérico (valor actual de T/C) en lugar del bit de terminado",
                "Valeur numérique (valeur actuelle de T/C) au lieu du bit terminé",
                "Wartość liczbowa (aktualna wartość T/C) zamiast bitu zakończenia");
        ford("Váltó – kattintásra kapcsol", "Toggle – switches on click", "Wechselnd – schaltet bei Klick", "Conmutador – cambia al hacer clic", "Bascule – commute au clic", "Przełącznik – zmienia stan po kliknięciu");
        ford("Pillanat – csak lenyomva aktív", "Momentary – active only while pressed", "Taster – nur aktiv solange gedrückt", "Momentáneo – activo solo mientras se mantiene pulsado", "Momentané – actif uniquement si maintenu", "Chwilowy – aktywny tylko podczas przytrzymania");
        ford("Szín: BE / kitöltés", "Color: ON / fill", "Farbe: EIN / Füllung", "Color: encendido / relleno", "Couleur : ON / remplissage", "Kolor: WŁ. / wypełnienie");
        ford("Szín kiválasztása", "Choose color", "Farbe wählen", "Elegir color", "Choisir la couleur", "Wybierz kolor");
        ford("Szín: KI / keret", "Color: OFF / border", "Farbe: AUS / Rahmen", "Color: apagado / borde", "Couleur : OFF / bordure", "Kolor: WYŁ. / ramka");
        ford("Widget típusa:", "Widget type:", "Widget-Typ:", "Tipo de widget:", "Type de widget :", "Typ widżetu:");
        ford("Felirat / szöveg:", "Label / text:", "Beschriftung / Text:", "Etiqueta / texto:", "Étiquette / texte :", "Etykieta / tekst:");
        ford("Kijelzőnél:", "For display:", "Für Anzeige:", "Para la pantalla:", "Pour l'afficheur :", "Dla wyświetlacza:");
        ford("Gombnál:", "For button:", "Für Taste:", "Para el botón:", "Pour le bouton :", "Dla przycisku:");
        ford("Szín 1 (lámpa BE / téglalap kitöltés):", "Color 1 (lamp ON / rectangle fill):", "Farbe 1 (Lampe EIN / Rechteck-Füllung):", "Color 1 (lámpara encendida / relleno del rectángulo):", "Couleur 1 (lampe ON / remplissage du rectangle) :", "Kolor 1 (lampka WŁ. / wypełnienie prostokąta):");
        ford("Szín 2 (lámpa KI / téglalap-felirat szín):", "Color 2 (lamp OFF / rectangle label color):", "Farbe 2 (Lampe AUS / Rechteck-Beschriftungsfarbe):", "Color 2 (lámpara apagada / color de etiqueta del rectángulo):", "Couleur 2 (lampe OFF / couleur de l'étiquette du rectangle) :", "Kolor 2 (lampka WYŁ. / kolor etykiety prostokąta):");
        ford("Betűméret:", "Font size:", "Schriftgröße:", "Tamaño de fuente:", "Taille de police :", "Rozmiar czcionki:");
        ford("HMI elem szerkesztése", "Edit HMI element", "HMI-Element bearbeiten", "Editar elemento HMI", "Modifier l'élément HMI", "Edytuj element HMI");

        // HMI eszköztár / képernyők
        ford("Képernyő: ", "Screen: ", "Bildschirm: ", "Pantalla: ", "Écran : ", "Ekran: ");
        ford("Képernyő", "Screen", "Bildschirm", "Pantalla", "Écran", "Ekran");
        ford("+ Új képernyő", "+ New screen", "+ Neuer Bildschirm", "+ Nueva pantalla", "+ Nouvel écran", "+ Nowy ekran");
        ford("Új képernyő neve:", "New screen name:", "Name des neuen Bildschirms:", "Nombre de la nueva pantalla:", "Nom du nouvel écran :", "Nazwa nowego ekranu:");
        ford("Átnevezés...", "Rename...", "Umbenennen...", "Renombrar...", "Renommer...", "Zmień nazwę...");
        ford("Képernyő neve:", "Screen name:", "Bildschirmname:", "Nombre de la pantalla:", "Nom de l'écran :", "Nazwa ekranu:");
        ford("Képernyő törlése", "Delete screen", "Bildschirm löschen", "Eliminar pantalla", "Supprimer l'écran", "Usuń ekran");
        ford("Legalább egy képernyőnek maradnia kell.", "At least one screen must remain.", "Mindestens ein Bildschirm muss übrig bleiben.", "Debe quedar al menos una pantalla.", "Au moins un écran doit rester.", "Musi pozostać przynajmniej jeden ekran.");
        ford("Biztosan törli a", "Are you sure you want to delete", "Möchten Sie wirklich", "¿Seguro que desea eliminar", "Voulez-vous vraiment supprimer", "Czy na pewno usunąć");
        ford("képernyőt és minden elemét?", "screen and all its elements?", "den Bildschirm und alle seine Elemente löschen?", "la pantalla y todos sus elementos?", "l'écran et tous ses éléments ?", "ekran i wszystkie jego elementy?");
        ford("✎ HMI szerkesztés", "✎ Edit HMI", "✎ HMI bearbeiten", "✎ Editar HMI", "✎ Modifier HMI", "✎ Edycja HMI");
        ford("Szerkesztés mód: elemek elhelyezése, mozgatása, átméretezése és tulajdonságainak módosítása",
                "Edit mode: placing, moving, resizing elements and changing their properties",
                "Bearbeitungsmodus: Elemente platzieren, verschieben, Größe ändern und Eigenschaften bearbeiten",
                "Modo de edición: colocar, mover, redimensionar elementos y modificar sus propiedades",
                "Mode édition : placer, déplacer, redimensionner les éléments et modifier leurs propriétés",
                "Tryb edycji: umieszczanie, przesuwanie, zmiana rozmiaru elementów i ich właściwości");
        ford("Új elem: ", "New element: ", "Neues Element: ", "Nuevo elemento: ", "Nouvel élément : ", "Nowy element: ");
        ford("Kiválasztás / mozgatás", "Select / move", "Auswählen / Verschieben", "Seleccionar / mover", "Sélectionner / déplacer", "Wybór / przesuwanie");
        ford("Kijelölt elem törlése", "Delete selected element", "Ausgewähltes Element löschen", "Eliminar elemento seleccionado", "Supprimer l'élément sélectionné", "Usuń wybrany element");
        ford("Futás közben (szerkesztés nélkül) a gombok és lámpák élőben reagálnak.",
                "While running (not in edit mode), buttons and lamps respond live.",
                "Während des Betriebs (ohne Bearbeitungsmodus) reagieren Tasten und Lampen live.",
                "Durante la ejecución (sin modo de edición), los botones y lámparas responden en vivo.",
                "En cours d'exécution (hors mode édition), les boutons et lampes réagissent en direct.",
                "Podczas działania (poza trybem edycji) przyciski i lampki reagują na żywo.");
        ford("⧉ Külön ablakban", "⧉ In separate window", "⧉ In separatem Fenster", "⧉ En ventana separada", "⧉ Dans une fenêtre séparée", "⧉ W osobnym oknie");
        ford("A HMI megjelenítése külön, mozgatható ablakban – így a létradiagrammal egymás mellett is látható",
                "Show the HMI in a separate, movable window – so it can be viewed side by side with the ladder diagram",
                "Zeigt das HMI in einem separaten, verschiebbaren Fenster an – so lässt es sich neben dem Kontaktplan anzeigen",
                "Muestra el HMI en una ventana separada y móvil, para poder verlo junto al diagrama de contactos",
                "Affiche le HMI dans une fenêtre séparée et déplaçable – pour le voir côte à côte avec le schéma à contacts",
                "Wyświetla HMI w osobnym, przesuwalnym oknie – dzięki czemu można je oglądać razem ze schematem drabinkowym");
        ford("Kattintson a vászonra az új elem elhelyezéséhez...",
                "Click the canvas to place the new element...",
                "Klicken Sie auf die Zeichenfläche, um das neue Element zu platzieren...",
                "Haga clic en el lienzo para colocar el nuevo elemento...",
                "Cliquez sur la zone de dessin pour placer le nouvel élément...",
                "Kliknij na płótnie, aby umieścić nowy element...");
        ford("Kattintson a vászonra az új blokk elhelyezéséhez...",
                "Click the canvas to place the new block...",
                "Klicken Sie auf die Zeichenfläche, um den neuen Block zu platzieren...",
                "Haga clic en el lienzo para colocar el nuevo bloque...",
                "Cliquez sur la zone de dessin pour placer le nouveau bloc...",
                "Kliknij na płótnie, aby umieścić nowy blok...");
        ford("Kattintson egy bemenet pontra a huzal befejezéséhez (üres helyre kattintva megszakítja)...",
                "Click an input pin to finish the wire (click empty space to cancel)...",
                "Klicken Sie auf einen Eingangspin, um die Verdrahtung abzuschließen (Klick auf eine leere Stelle bricht ab)...",
                "Haga clic en un pin de entrada para terminar el cable (haga clic en un espacio vacío para cancelar)...",
                "Cliquez sur une broche d'entrée pour terminer le câblage (cliquez sur un espace vide pour annuler)...",
                "Kliknij pin wejściowy, aby zakończyć połączenie (kliknięcie w puste miejsce przerywa)...");

        // FBD blokk-szerkesztő
        ford("Bemenet (I/Q/M/T.DN/C.DN olvasása)", "Input (reads I/Q/M/T.DN/C.DN)", "Eingang (liest I/Q/M/T.DN/C.DN)", "Entrada (lee I/Q/M/T.DN/C.DN)", "Entrée (lit I/Q/M/T.DN/C.DN)", "Wejście (odczytuje I/Q/M/T.DN/C.DN)");
        ford("Kimenet (Q/M írása)", "Output (writes Q/M)", "Ausgang (schreibt Q/M)", "Salida (escribe Q/M)", "Sortie (écrit Q/M)", "Wyjście (zapisuje Q/M)");
        ford("AND (ÉS)", "AND", "AND (UND)", "AND (Y)", "AND (ET)", "AND (I)");
        ford("OR (VAGY)", "OR", "OR (ODER)", "OR (O)", "OR (OU)", "OR (LUB)");
        ford("NOT (NEM)", "NOT", "NOT (NICHT)", "NOT (NO)", "NOT (NON)", "NOT (NIE)");
        ford("XOR (KIZÁRÓ VAGY)", "XOR (exclusive OR)", "XOR (Exklusiv-Oder)", "XOR (O exclusivo)", "XOR (OU exclusif)", "XOR (wyłączne OR)");
        ford("SR tároló (Reset-domináns)", "SR latch (reset-dominant)", "SR-Speicher (rücksetzdominant)", "Biestable SR (dominante en reset)", "Bascule SR (dominante Reset)", "Zatrzask SR (dominacja resetu)");
        ford("TON (bekapcsolás-késleltetés)", "TON (on-delay timer)", "TON (Einschaltverzögerung)", "TON (temporizador de retardo a la conexión)", "TON (temporisation à l'enclenchement)", "TON (timer z opóźnieniem załączenia)");
        ford("CTU (felfelé számláló)", "CTU (count up)", "CTU (Vorwärtszähler)", "CTU (contador ascendente)", "CTU (compteur incrémental)", "CTU (licznik w górę)");
        ford("R_TRIG (felfutó él, P)", "R_TRIG (rising edge, P)", "R_TRIG (steigende Flanke, P)", "R_TRIG (flanco ascendente, P)", "R_TRIG (front montant, P)", "R_TRIG (zbocze narastające, P)");
        ford("F_TRIG (lefutó él, N)", "F_TRIG (falling edge, N)", "F_TRIG (fallende Flanke, N)", "F_TRIG (flanco descendente, N)", "F_TRIG (front descendant, N)", "F_TRIG (zbocze opadające, N)");
        ford("Bemenetek száma:", "Number of inputs:", "Anzahl der Eingänge:", "Número de entradas:", "Nombre d'entrées :", "Liczba wejść:");
        ford("Blokk típusa:", "Block type:", "Blocktyp:", "Tipo de bloque:", "Type de bloc :", "Typ bloku:");
        ford("Felirat / megjegyzés:", "Label / comment:", "Beschriftung / Kommentar:", "Etiqueta / comentario:", "Étiquette / commentaire :", "Etykieta / komentarz:");
        ford("FBD blokk szerkesztése", "Edit FBD block", "FBD-Block bearbeiten", "Editar bloque FBD", "Modifier le bloc FBD", "Edytuj blok FBD");

        // FBD eszköztár
        ford("✎ FBD szerkesztés", "✎ Edit FBD", "✎ FBD bearbeiten", "✎ Editar FBD", "✎ Modifier FBD", "✎ Edycja FBD");
        ford("Szerkesztés mód: blokkok elhelyezése, mozgatása, huzalozása és tulajdonságainak módosítása",
                "Edit mode: placing, moving, wiring blocks and changing their properties",
                "Bearbeitungsmodus: Blöcke platzieren, verschieben, verdrahten und Eigenschaften bearbeiten",
                "Modo de edición: colocar, mover, cablear bloques y modificar sus propiedades",
                "Mode édition : placer, déplacer, câbler les blocs et modifier leurs propriétés",
                "Tryb edycji: umieszczanie, przesuwanie, łączenie bloków i zmiana ich właściwości");
        ford("Új blokk: ", "New block: ", "Neuer Block: ", "Nuevo bloque: ", "Nouveau bloc : ", "Nowy blok: ");
        ford("Bemenet", "Input", "Eingang", "Entrada", "Entrée", "Wejście");
        ford("Kimenet", "Output", "Ausgang", "Salida", "Sortie", "Wyjście");
        ford("SR tároló", "SR latch", "SR-Speicher", "Biestable SR", "Bascule SR", "Zatrzask SR");
        ford("Kijelölt blokk törlése", "Delete selected block", "Ausgewählten Block löschen", "Eliminar bloque seleccionado", "Supprimer le bloc sélectionné", "Usuń wybrany blok");
        ford("Huzalozás: kattintson egy kimeneti pontra, majd a célbemenetre.",
                "Wiring: click an output pin, then the target input.",
                "Verdrahtung: Klicken Sie auf einen Ausgangspin, dann auf den Ziel-Eingang.",
                "Cableado: haga clic en un pin de salida y luego en la entrada de destino.",
                "Câblage : cliquez sur une broche de sortie, puis sur l'entrée cible.",
                "Łączenie: kliknij pin wyjściowy, a następnie docelowe wejście.");

        // gépi kód export ablak
        ford("Vágólapra másolás", "Copy to clipboard", "In die Zwischenablage kopieren", "Copiar al portapapeles", "Copier dans le presse-papiers", "Kopiuj do schowka");
        ford("Gépi kód a vágólapra másolva.", "Machine code copied to clipboard.", "Maschinencode in die Zwischenablage kopiert.", "Código máquina copiado al portapapeles.", "Code machine copié dans le presse-papiers.", "Kod maszynowy skopiowany do schowka.");
        ford("Mentés .txt fájlba...", "Save to .txt file...", "Als .txt-Datei speichern...", "Guardar en archivo .txt...", "Enregistrer dans un fichier .txt...", "Zapisz do pliku .txt...");
        ford("Szöveges fájl (*.txt)", "Text file (*.txt)", "Textdatei (*.txt)", "Archivo de texto (*.txt)", "Fichier texte (*.txt)", "Plik tekstowy (*.txt)");
        ford("Gépi kód elmentve:", "Machine code saved:", "Maschinencode gespeichert:", "Código máquina guardado:", "Code machine enregistré :", "Kod maszynowy zapisany:");
        ford("Nem sikerült menteni:", "Failed to save:", "Speichern fehlgeschlagen:", "No se pudo guardar:", "Échec de l'enregistrement :", "Nie udało się zapisać:");
        ford("Hiba", "Error", "Fehler", "Error", "Erreur", "Błąd");
        ford("Gépi kód (IL / STL) megtekintése", "View machine code (IL / STL)", "Maschinencode anzeigen (IL / STL)", "Ver código máquina (IL / STL)", "Afficher le code machine (IL / STL)", "Zobacz kod maszynowy (IL / STL)");

        // program ellenőrzése
        ford("Figyelmeztetés", "Warning", "Warnung", "Advertencia", "Avertissement", "Ostrzeżenie");
        ford("Több hely írja közvetlenül a", "Multiple places directly write to the", "Mehrere Stellen schreiben direkt auf den", "Varios lugares escriben directamente en la", "Plusieurs endroits écrivent directement sur la", "Wiele miejsc zapisuje bezpośrednio do");
        ford("kimenetet:", "output:", "Ausgang:", "salida:", "sortie :", "wyjścia:");
        ford("scan-ciklusonként az utoljára kiértékelt felülírja a többit.", "each scan cycle, the last one evaluated overwrites the others.", "pro Scan-Zyklus überschreibt die zuletzt ausgewertete Stelle die anderen.", "en cada ciclo de escaneo, el último evaluado sobrescribe a los demás.", "à chaque cycle de scrutation, le dernier évalué écrase les autres.", "w każdym cyklu skanowania ostatnio obliczone miejsce nadpisuje pozostałe.");
        ford("Több hely vezérli ugyanazt az időzítőt/számlálót", "Multiple places control the same timer/counter", "Mehrere Stellen steuern denselben Zeitgeber/Zähler", "Varios lugares controlan el mismo temporizador/contador", "Plusieurs endroits contrôlent le même temporisateur/compteur", "Wiele miejsc kontroluje ten sam timer/licznik");
        ford("önmagára hivatkozik a", "references itself at", "verweist auf sich selbst am", "se referencia a sí mismo en la", "se référence lui-même à", "odwołuje się do samego siebie w");
        ford("bemeneten.", "input.", "Eingang.", "entrada.", "entrée.", "wejściu.");
        ford("nyitott (nem kötött) bemenete(i):", "open (unconnected) input(s):", "offene(r) (nicht verbundene(r)) Eingang/Eingänge:", "entrada(s) abierta(s) (no conectada(s)):", "entrée(s) ouverte(s) (non connectée(s)) :", "otwarte (niepodłączone) wejście/wejścia:");
        ford("futás közben hamis (0) értéket vesz fel.", "will take the false (0) value while running.", "nimmt im Betrieb den Wert falsch (0) an.", "tomará el valor falso (0) durante la ejecución.", "prendra la valeur faux (0) pendant l'exécution.", "podczas działania przyjmie wartość fałsz (0).");
        ford("Nem találtam hivatkozási problémát.", "No reference problems found.", "Keine Verweisprobleme gefunden.", "No se encontraron problemas de referencia.", "Aucun problème de référence trouvé.", "Nie znaleziono problemów z odwołaniami.");
        ford("Program ellenőrzése", "Check program", "Programm prüfen", "Comprobar programa", "Vérifier le programme", "Sprawdź program");

        // kereszthivatkozás-keresés
        ford("Cím:", "Address:", "Adresse:", "Dirección:", "Adresse :", "Adres:");
        ford("előtag", "prefix", "Vorkette", "prefijo", "préfixe", "przedczłon");
        ford("ág", "branch", "Zweig", "rama", "branche", "gałąź");
        ford("utótag", "suffix", "Nachkette", "sufijo", "suffixe", "przyczłon");
        ford("kimenet", "output", "Ausgang", "salida", "sortie", "wyjście");
        ford("TON időzítő", "TON timer", "TON-Zeitglied", "Temporizador TON", "Temporisateur TON", "Timer TON");
        ford("CTU számláló", "CTU counter", "CTU-Zähler", "Contador CTU", "Compteur CTU", "Licznik CTU");
        ford("Létra", "Ladder", "Kontaktplan", "Diagrama de contactos", "Schéma à contacts", "Schemat drabinkowy");
        ford("kontaktus", "contact", "Kontakt", "contacto", "contact", "kontakt");
        ford("Bemenet blokk", "Input block", "Eingangsblock", "Bloque de entrada", "Bloc d'entrée", "Blok wejściowy");
        ford("Kimenet blokk", "Output block", "Ausgangsblock", "Bloque de salida", "Bloc de sortie", "Blok wyjściowy");
        ford("TON blokk", "TON block", "TON-Block", "Bloque TON", "Bloc TON", "Blok TON");
        ford("CTU blokk", "CTU block", "CTU-Block", "Bloque CTU", "Bloc CTU", "Blok CTU");
        ford("képernyő:", "screen:", "Bildschirm:", "pantalla:", "écran :", "ekran:");
        ford("Nincs hivatkozás.", "No references.", "Keine Verweise.", "Sin referencias.", "Aucune référence.", "Brak odwołań.");
        ford("Hivatkozások keresése", "Find references", "Verweise suchen", "Buscar referencias", "Rechercher des références", "Znajdź odwołania");

        // státuszsor és fájlműveletek
        ford("Fut – ciklusidő:", "Running – cycle time:", "Läuft – Zykluszeit:", "En ejecución – tiempo de ciclo:", "En cours – temps de cycle :", "Działa – czas cyklu:");
        ford("Leállítva. Scan-ciklusok:", "Stopped. Scan cycles:", "Angehalten. Scan-Zyklen:", "Detenido. Ciclos de escaneo:", "Arrêté. Cycles de scrutation :", "Zatrzymano. Cykle skanowania:");
        ford("Egy lépés végrehajtva. Scan-ciklusok:", "One step executed. Scan cycles:", "Ein Schritt ausgeführt. Scan-Zyklen:", "Un paso ejecutado. Ciclos de escaneo:", "Un pas exécuté. Cycles de scrutation :", "Wykonano jeden krok. Cykle skanowania:");
        ford("Nullázva.", "Reset.", "Zurückgesetzt.", "Reiniciado.", "Réinitialisé.", "Zresetowano.");
        ford("Scan-ciklusok:", "Scan cycles:", "Scan-Zyklen:", "Ciclos de escaneo:", "Cycles de scrutation :", "Cykle skanowania:");
        ford("Visszaállítási hiba:", "Restore error:", "Wiederherstellungsfehler:", "Error de restauración:", "Erreur de restauration :", "Błąd przywracania:");
        ford("Visszavonva.", "Undone.", "Rückgängig gemacht.", "Deshecho.", "Annulé.", "Wycofano.");
        ford("Ismét végrehajtva.", "Redone.", "Wiederholt.", "Rehecho.", "Rétabli.", "Powtórzono.");
        ford("Előbb jelöljön ki egy hálózatot (kattintson a bal oldali sávjára).", "First select a network (click its left-hand strip).", "Wählen Sie zuerst ein Netzwerk aus (klicken Sie auf seinen linken Streifen).", "Primero seleccione una red (haga clic en su franja izquierda).", "Sélectionnez d'abord un réseau (cliquez sur sa bande de gauche).", "Najpierw wybierz sieć (kliknij jej lewy pasek).");
        ford("Hálózat vágólapra másolva.", "Network copied to clipboard.", "Netzwerk in die Zwischenablage kopiert.", "Red copiada al portapapeles.", "Réseau copié dans le presse-papiers.", "Sieć skopiowana do schowka.");
        ford("Nincs vágólapra másolt hálózat.", "No network on the clipboard.", "Kein Netzwerk in der Zwischenablage.", "No hay ninguna red en el portapapeles.", "Aucun réseau dans le presse-papiers.", "Brak sieci w schowku.");
        ford("Hálózat beillesztve a vágólapról.", "Network pasted from the clipboard.", "Netzwerk aus der Zwischenablage eingefügt.", "Red pegada desde el portapapeles.", "Réseau collé depuis le presse-papiers.", "Wklejono sieć ze schowka.");
        ford("Biztosan töröl minden hálózatot?", "Are you sure you want to delete all networks?", "Möchten Sie wirklich alle Netzwerke löschen?", "¿Seguro que desea eliminar todas las redes?", "Voulez-vous vraiment supprimer tous les réseaux ?", "Czy na pewno usunąć wszystkie sieci?");
        ford("PLC program (*.plc)", "PLC program (*.plc)", "SPS-Programm (*.plc)", "Programa de PLC (*.plc)", "Programme automate (*.plc)", "Program PLC (*.plc)");
        ford("Nem sikerült megnyitni:", "Failed to open:", "Öffnen fehlgeschlagen:", "No se pudo abrir:", "Échec de l'ouverture :", "Nie udało się otworzyć:");
        ford("Mentve:", "Saved:", "Gespeichert:", "Guardado:", "Enregistré :", "Zapisano:");
        ford("Készenlét.", "Ready.", "Bereit.", "Listo.", "Prêt.", "Gotowy.");
        ford("Kontaktus vágólapra másolva.", "Contact copied to clipboard.", "Kontakt in die Zwischenablage kopiert.", "Contacto copiado al portapapeles.", "Contact copié dans le presse-papiers.", "Kontakt skopiowany do schowka.");
        ford("Kontaktus beillesztve a vágólapról.", "Contact pasted from the clipboard.", "Kontakt aus der Zwischenablage eingefügt.", "Contacto pegado desde el portapapeles.", "Contact collé depuis le presse-papiers.", "Wklejono kontakt ze schowka.");
        ford("Létradiagram", "Ladder diagram", "Kontaktplan", "Diagrama de contactos", "Schéma à contacts", "Schemat drabinkowy");
        ford("Címke", "Label", "Beschriftung", "Etiqueta", "Étiquette", "Etykieta");
        ford("Ismeretlen fájlformátum.", "Unknown file format.", "Unbekanntes Dateiformat.", "Formato de archivo desconocido.", "Format de fichier inconnu.", "Nieznany format pliku.");
        ford("Motor indítás/leállítás (önfenntartó kapcsolás)", "Motor start/stop (self-retaining circuit)", "Motor Start/Stopp (Selbsthalteschaltung)", "Arranque/paro del motor (circuito autorretenido)", "Démarrage/arrêt du moteur (circuit à auto-maintien)", "Start/stop silnika (układ samopodtrzymujący)");
        ford("3 másodperces késleltetés a motor indítása után", "3-second delay after the motor starts", "3 Sekunden Verzögerung nach dem Motorstart", "Retardo de 3 segundos tras el arranque del motor", "Délai de 3 secondes après le démarrage du moteur", "3-sekundowe opóźnienie po starcie silnika");
        ford("Jelzőlámpa a késleltetés lejártakor", "Indicator lamp when the delay expires", "Meldeleuchte bei Ablauf der Verzögerung", "Lámpara indicadora al expirar el retardo", "Voyant lorsque le délai est écoulé", "Lampka sygnalizacyjna po upływie opóźnienia");
        ford("Indítások számlálása", "Counting the starts", "Zählen der Starts", "Conteo de arranques", "Comptage des démarrages", "Zliczanie startów");
        ford("Jelzés 5 indítás után", "Signal after 5 starts", "Meldung nach 5 Starts", "Señal tras 5 arranques", "Signal après 5 démarrages", "Sygnał po 5 startach");

        // gépi kód export – új dialektusok és gyártói táblázat
        ford("Mitsubishi FX/Q utasításlista", "Mitsubishi FX/Q instruction list", "Mitsubishi FX/Q-Befehlsliste", "Lista de instrucciones Mitsubishi FX/Q", "Liste d'instructions Mitsubishi FX/Q", "Lista instrukcji Mitsubishi FX/Q");
        ford("Allen-Bradley (SLC-500) utasításkészlet", "Allen-Bradley (SLC-500) instruction set", "Allen-Bradley (SLC-500)-Befehlssatz", "Conjunto de instrucciones Allen-Bradley (SLC-500)", "Jeu d'instructions Allen-Bradley (SLC-500)", "Zestaw instrukcji Allen-Bradley (SLC-500)");
        ford("Gépi kód megtekintése...", "View machine code...", "Maschinencode anzeigen...", "Ver código máquina...", "Afficher le code machine...", "Zobacz kod maszynowy...");
        ford("Gépi kód megtekintése", "View machine code", "Maschinencode anzeigen", "Ver código máquina", "Afficher le code machine", "Zobacz kod maszynowy");
        ford("Gyártói kompatibilitás...", "Vendor compatibility...", "Herstellerkompatibilität...", "Compatibilidad de fabricantes...", "Compatibilité des fabricants...", "Zgodność z producentami...");
        ford("Gyártói kompatibilitás", "Vendor compatibility", "Herstellerkompatibilität", "Compatibilidad de fabricantes", "Compatibilité des fabricants", "Zgodność z producentami");
        ford("Teljes körűen támogatott", "Fully supported", "Vollständig unterstützt", "Totalmente compatible", "Entièrement pris en charge", "W pełni obsługiwany");
        ford("Instruction List (IEC 61131-3) nézettel kompatibilis", "Compatible with the Instruction List (IEC 61131-3) view", "Kompatibel mit der Ansicht Instruction List (IEC 61131-3)", "Compatible con la vista Instruction List (IEC 61131-3)", "Compatible avec la vue Instruction List (IEC 61131-3)", "Zgodny z widokiem Instruction List (IEC 61131-3)");
        ford("gyártóspecifikus", "vendor-specific", "herstellerspezifisch", "específico del fabricante", "spécifique au fabricant", "specyficzny dla producenta");
        ford("Nem támogatott – egyedi formátum", "Not supported – proprietary format", "Nicht unterstützt – herstellereigenes Format", "No compatible – formato propietario", "Non pris en charge – format propriétaire", "Nieobsługiwany – format własnościowy");
        ford("saját vezérlési nyelvek", "proprietary control languages", "herstellereigene Steuerungssprachen", "lenguajes de control propios", "langages de commande propriétaires", "własne języki sterowania");
        ford("Nem támogatott – robotnyelv, nem PLC-logika", "Not supported – robot language, not PLC logic", "Nicht unterstützt – Robotersprache, keine SPS-Logik", "No compatible – lenguaje de robot, no lógica de PLC", "Non pris en charge – langage robot, pas une logique automate", "Nieobsługiwany – język robota, nie logika PLC");
        ford("robotvezérlők", "robot controllers", "Robotersteuerungen", "controladores de robot", "contrôleurs de robot", "sterowniki robotów");
        ford("nyelv", "language", "Sprache", "lenguaje", "langage", "język");
        ford("Gyártó", "Manufacturer", "Hersteller", "Fabricante", "Fabricant", "Producent");
        ford("Jellemző PLC-k", "Typical PLCs", "Typische SPS", "PLC típicos", "Automates typiques", "Typowe sterowniki PLC");
        ford("Alacsony szintű / utasítás jellegű nyelv", "Low-level / instruction-type language", "Low-Level- / befehlsorientierte Sprache", "Lenguaje de bajo nivel / tipo instrucción", "Langage bas niveau / de type instruction", "Język niskopoziomowy / typu instrukcyjnego");
        ford("Állapot ebben a szimulátorban", "Status in this simulator", "Status in diesem Simulator", "Estado en este simulador", "État dans ce simulateur", "Status w tym symulatorze");

        // névjegy
        ford("Névjegy", "About", "Info", "Acerca de", "À propos", "O programie");
        ford("A programot Sisak László (helmetsoft.com) készítette a PLC oktatási célú felhasználására.",
                "This program was created by László Sisak (helmetsoft.com) for educational use in PLC training.",
                "Dieses Programm wurde von László Sisak (helmetsoft.com) für den Einsatz in der SPS-Ausbildung erstellt.",
                "Este programa fue creado por László Sisak (helmetsoft.com) para uso educativo en la formación de PLC.",
                "Ce programme a été créé par László Sisak (helmetsoft.com) à des fins éducatives pour la formation aux automates (PLC).",
                "Program został stworzony przez László Sisaka (helmetsoft.com) do celów edukacyjnych w zakresie szkolenia PLC.");
        ford("INGYENES", "FREE", "KOSTENLOS", "GRATUITO", "GRATUIT", "BEZPŁATNY");
        ford("kereskedelmi forgalomba nem hozható.", "may not be sold or distributed commercially.", "darf nicht kommerziell vertrieben werden.", "no puede comercializarse.", "ne peut pas être commercialisé.", "nie może być sprzedawany ani wykorzystywany komercyjnie.");

        // GRAFCET
        ford("✎ GRAFCET szerkesztés", "✎ Edit GRAFCET", "✎ GRAFCET bearbeiten", "✎ Editar GRAFCET", "✎ Modifier GRAFCET", "✎ Edycja GRAFCET");
        ford("Szerkesztés mód: lépések és átmenetek elhelyezése, mozgatása, huzalozása és tulajdonságainak módosítása",
                "Edit mode: placing, moving, wiring steps and transitions and changing their properties",
                "Bearbeitungsmodus: Schritte und Transitionen platzieren, verschieben, verdrahten und Eigenschaften bearbeiten",
                "Modo de edición: colocar, mover, cablear pasos y transiciones y modificar sus propiedades",
                "Mode édition : placer, déplacer, câbler les étapes et transitions et modifier leurs propriétés",
                "Tryb edycji: umieszczanie, przesuwanie, łączenie kroków i przejść oraz zmiana ich właściwości");
        ford("Lépés", "Step", "Schritt", "Paso", "Étape", "Krok");
        ford("Átmenet", "Transition", "Transition", "Transición", "Transition", "Przejście");
        ford("Huzalozás: kattintson egy lépés/átmenet kimeneti pontjára, majd a célbemenetre.",
                "Wiring: click a step's/transition's output pin, then the target input.",
                "Verdrahtung: Klicken Sie auf den Ausgangspin eines Schritts/einer Transition und dann auf den Ziel-Eingang.",
                "Cableado: haga clic en el pin de salida de un paso/transición y luego en la entrada de destino.",
                "Câblage : cliquez sur la broche de sortie d'une étape/transition, puis sur l'entrée cible.",
                "Łączenie: kliknij pin wyjściowy kroku/przejścia, a następnie docelowe wejście.");
        ford("Létra generálása...", "Generate ladder...", "Kontaktplan generieren...", "Generar diagrama de contactos...", "Générer le schéma à contacts...", "Generuj drabinkę...");
        ford("Akció szerkesztése", "Edit action", "Aktion bearbeiten", "Editar acción", "Modifier l'action", "Edytuj akcję");
        ford("Akció típusa:", "Action type:", "Aktionstyp:", "Tipo de acción:", "Type d'action :", "Typ akcji:");
        ford("Folyamatos (N)", "Continuous (N)", "Dauernd (N)", "Continua (N)", "Continue (N)", "Ciągła (N)");
        ford("Set (S)", "Set (S)", "Set (S)", "Set (S)", "Set (S)", "Set (S)");
        ford("Reset (R)", "Reset (R)", "Reset (R)", "Reset (R)", "Reset (R)", "Reset (R)");
        ford("Kezdeti lépés", "Initial step", "Anfangsschritt", "Paso inicial", "Étape initiale", "Krok początkowy");
        ford("Felirat:", "Label:", "Beschriftung:", "Etiqueta:", "Étiquette :", "Etykieta:");
        ford("+ Hozzáad", "+ Add", "+ Hinzufügen", "+ Añadir", "+ Ajouter", "+ Dodaj");
        ford("Akciók (amíg a lépés aktív)", "Actions (while the step is active)", "Aktionen (solange der Schritt aktiv ist)", "Acciones (mientras el paso está activo)", "Actions (tant que l'étape est active)", "Akcje (dopóki krok jest aktywny)");
        ford("Lépés szerkesztése", "Edit step", "Schritt bearbeiten", "Editar paso", "Modifier l'étape", "Edytuj krok");
        ford("Feltétel (receptivitás) – ÉS kapcsolatban lévő kontaktusok",
                "Condition (receptivity) – contacts in AND relationship",
                "Bedingung (Transitionsbedingung) – Kontakte in UND-Verknüpfung",
                "Condición (receptividad) – contactos en relación AND",
                "Condition (réceptivité) – contacts en relation ET",
                "Warunek (receptywność) – styki w relacji AND");
        ford("Átmenet szerkesztése", "Edit transition", "Transition bearbeiten", "Editar transición", "Modifier la transition", "Edytuj przejście");
        ford("A GRAFCET diagram üres – adjon hozzá legalább egy lépést.",
                "The GRAFCET diagram is empty – add at least one step.",
                "Das GRAFCET-Diagramm ist leer – fügen Sie mindestens einen Schritt hinzu.",
                "El diagrama GRAFCET está vacío – añada al menos un paso.",
                "Le diagramme GRAFCET est vide – ajoutez au moins une étape.",
                "Diagram GRAFCET jest pusty – dodaj co najmniej jeden krok.");
        ford("Nincs elég szabad belső relé (M) a létra generálásához:",
                "Not enough free internal relays (M) to generate the ladder:",
                "Nicht genügend freie interne Relais (M) zum Generieren des Kontaktplans:",
                "No hay suficientes relés internos (M) libres para generar el diagrama de contactos:",
                "Pas assez de relais internes (M) libres pour générer le schéma à contacts :",
                "Za mało wolnych przekaźników wewnętrznych (M) do wygenerowania drabinki:");
        ford("lépéshez csak", "steps, only", "Schritten stehen nur", "pasos, solo hay", "étapes, seulement", "kroków dostępnych jest tylko");
        ford("szabad M bit áll rendelkezésre (összesen 16 van). Csökkentse a lépések számát, vagy szabadítson fel M biteket.",
                "free M bits are available (16 total). Reduce the number of steps, or free up some M bits.",
                "freie M-Bits verfügbar (insgesamt 16). Verringern Sie die Anzahl der Schritte, oder geben Sie M-Bits frei.",
                "bits M libres disponibles (16 en total). Reduzca el número de pasos o libere bits M.",
                "bits M libres disponibles (16 au total). Réduisez le nombre d'étapes, ou libérez des bits M.",
                "wolnych bitów M (łącznie jest 16). Zmniejsz liczbę kroków lub zwolnij bity M.");
        ford("A következő belső relék (M) kerülnek felhasználásra:",
                "The following internal relays (M) will be used:",
                "Die folgenden internen Relais (M) werden verwendet:",
                "Se utilizarán los siguientes relés internos (M):",
                "Les relais internes (M) suivants seront utilisés :",
                "Zostaną użyte następujące przekaźniki wewnętrzne (M):");
        ford("Ez új hálózatokat hoz létre (aktiválás + deaktiválás lépésenként, plusz az akció-hálózatok).",
                "This will create new networks (activation + deactivation per step, plus the action networks).",
                "Dadurch werden neue Netzwerke erstellt (Aktivierung + Deaktivierung pro Schritt, plus die Aktions-Netzwerke).",
                "Esto creará nuevas redes (activación + desactivación por paso, además de las redes de acción).",
                "Cela créera de nouveaux réseaux (activation + désactivation par étape, plus les réseaux d'action).",
                "Spowoduje to utworzenie nowych sieci (aktywacja + dezaktywacja dla każdego kroku, plus sieci akcji).");
        ford("Ettől kezdve a tényleges kimeneteket a generált létra vezérli; a GRAFCET fül csak vizuális előnézetként animálja a lépéseket tovább.",
                "From now on, the actual outputs are controlled by the generated ladder; the GRAFCET tab continues to animate the steps only as a visual preview.",
                "Ab jetzt steuert der generierte Kontaktplan die tatsächlichen Ausgänge; die Registerkarte GRAFCET animiert die Schritte weiterhin nur als visuelle Vorschau.",
                "A partir de ahora, las salidas reales están controladas por el diagrama de contactos generado; la pestaña GRAFCET sigue animando los pasos solo como vista previa visual.",
                "Désormais, les sorties réelles sont contrôlées par le schéma à contacts généré ; l'onglet GRAFCET continue d'animer les étapes uniquement à titre d'aperçu visuel.",
                "Od teraz rzeczywiste wyjścia są sterowane przez wygenerowany schemat drabinkowy; zakładka GRAFCET nadal animuje kroki wyłącznie jako podgląd wizualny.");
        ford("Folytatja?", "Continue?", "Fortfahren?", "¿Continuar?", "Continuer ?", "Kontynuować?");
        ford("Létra generálása GRAFCET-ből", "Generate ladder from GRAFCET", "Kontaktplan aus GRAFCET generieren", "Generar diagrama de contactos desde GRAFCET", "Générer le schéma à contacts à partir du GRAFCET", "Generuj drabinkę z GRAFCET");
        ford("lépés aktiválása", "step activation", "Schrittaktivierung", "activación del paso", "activation de l'étape", "aktywacja kroku");
        ford("lépés deaktiválása", "step deactivation", "Schrittdeaktivierung", "desactivación del paso", "désactivation de l'étape", "dezaktywacja kroku");
        ford("lépés akciója", "step's action", "Schrittaktion", "acción del paso", "action de l'étape", "akcja kroku");
        ford("hálózat hozzáadva a létraprogramhoz a GRAFCET alapján.",
                "network(s) added to the ladder program based on the GRAFCET.",
                "Netzwerk(e) zum Kontaktplanprogramm basierend auf dem GRAFCET hinzugefügt.",
                "red(es) añadida(s) al programa de contactos basándose en el GRAFCET.",
                "réseau(x) ajouté(s) au programme à contacts à partir du GRAFCET.",
                "sieć/sieci dodano do programu drabinkowego na podstawie GRAFCET.");

        // ---- minta programok (programkönyvtár) ----
        ford("Minta programok", "Sample programs", "Beispielprogramme", "Programas de ejemplo", "Programmes d'exemple", "Programy przykładowe");
        ford("Motorindítás (alapértelmezett demó)", "Motor start (default demo)", "Motorstart (Standarddemo)", "Arranque de motor (demo predeterminada)", "Démarrage moteur (démo par défaut)", "Rozruch silnika (domyślne demo)");
        ford("Csillag-delta motorindítás", "Star-delta motor starter", "Stern-Dreieck-Motoranlauf", "Arranque de motor estrella-triángulo", "Démarrage moteur étoile-triangle", "Rozruch silnika gwiazda-trójkąt");
        ford("Szállítószalag sorrendindítás", "Conveyor sequential start", "Förderband-Sequenzstart", "Arranque secuencial de cintas transportadoras", "Démarrage séquentiel de convoyeurs", "Sekwencyjny rozruch przenośników");
        ford("Közlekedési lámpa (állapotgép)", "Traffic light (state machine)", "Verkehrsampel (Zustandsautomat)", "Semáforo (máquina de estados)", "Feu de circulation (machine à états)", "Sygnalizacja świetlna (automat stanów)");
        ford("A jelenlegi program elvetésre kerül. Biztosan betölti a mintát?",
                "The current program will be discarded. Are you sure you want to load the sample?",
                "Das aktuelle Programm wird verworfen. Möchten Sie das Beispiel wirklich laden?",
                "El programa actual se descartará. ¿Seguro que desea cargar la muestra?",
                "Le programme actuel sera abandonné. Voulez-vous vraiment charger cet exemple ?",
                "Bieżący program zostanie odrzucony. Czy na pewno wczytać przykład?");
        ford("Minta program betöltve:", "Sample program loaded:", "Beispielprogramm geladen:", "Programa de ejemplo cargado:", "Exemple de programme chargé :", "Wczytano program przykładowy:");

        ford("Fő mágneskapcsoló indítás/leállítás (önfenntartó kapcsolás)",
                "Main contactor start/stop (self-sustaining circuit)",
                "Hauptschütz Start/Stopp (selbsthaltende Schaltung)",
                "Arranque/paro del contactor principal (circuito autoalimentado)",
                "Démarrage/arrêt du contacteur principal (circuit autoentretenu)",
                "Start/stop stycznika głównego (układ samopodtrzymujący)");
        ford("Csillag–delta átkapcsolási idő (3 s)", "Star-delta switchover time (3 s)", "Stern-Dreieck-Umschaltzeit (3 s)", "Tiempo de conmutación estrella-triángulo (3 s)", "Temps de commutation étoile-triangle (3 s)", "Czas przełączania gwiazda-trójkąt (3 s)");
        ford("Csillag kontaktor (az átkapcsolási idő lejártáig)", "Star contactor (until the switchover time elapses)", "Stern-Schütz (bis zum Ablauf der Umschaltzeit)", "Contactor estrella (hasta que transcurra el tiempo de conmutación)", "Contacteur étoile (jusqu'à l'écoulement du temps de commutation)", "Stycznik gwiazdy (do upływu czasu przełączania)");
        ford("Delta kontaktor (az átkapcsolási idő után)", "Delta contactor (after the switchover time)", "Dreieck-Schütz (nach der Umschaltzeit)", "Contactor triángulo (después del tiempo de conmutación)", "Contacteur triangle (après le temps de commutation)", "Stycznik trójkąta (po czasie przełączania)");

        ford("1. szállítószalag indítása/leállítása (önfenntartó kapcsolás)",
                "Conveyor 1 start/stop (self-sustaining circuit)",
                "Förderband 1 Start/Stopp (selbsthaltende Schaltung)",
                "Arranque/paro de la cinta 1 (circuito autoalimentado)",
                "Démarrage/arrêt du convoyeur 1 (circuit autoentretenu)",
                "Start/stop przenośnika 1 (układ samopodtrzymujący)");
        ford("Késleltetés a 2. szalag indítása előtt (2 s)", "Delay before conveyor 2 starts (2 s)", "Verzögerung vor dem Start von Förderband 2 (2 s)", "Retardo antes de que arranque la cinta 2 (2 s)", "Retard avant le démarrage du convoyeur 2 (2 s)", "Zwłoka przed startem przenośnika 2 (2 s)");
        ford("2. szállítószalag (2 s késleltetéssel)", "Conveyor 2 (with 2 s delay)", "Förderband 2 (mit 2 s Verzögerung)", "Cinta 2 (con 2 s de retardo)", "Convoyeur 2 (avec 2 s de retard)", "Przenośnik 2 (z 2 s zwłoki)");
        ford("Késleltetés a 3. szalag indítása előtt (további 2 s)", "Delay before conveyor 3 starts (another 2 s)", "Verzögerung vor dem Start von Förderband 3 (weitere 2 s)", "Retardo antes de que arranque la cinta 3 (otros 2 s)", "Retard avant le démarrage du convoyeur 3 (encore 2 s)", "Zwłoka przed startem przenośnika 3 (kolejne 2 s)");
        ford("3. szállítószalag (4 s késleltetéssel)", "Conveyor 3 (with 4 s delay)", "Förderband 3 (mit 4 s Verzögerung)", "Cinta 3 (con 4 s de retardo)", "Convoyeur 3 (avec 4 s de retard)", "Przenośnik 3 (z 4 s zwłoki)");

        ford("Kezdeti állapot: piros (ha még nincs aktív állapot)", "Initial state: red (if no state is active yet)", "Anfangszustand: Rot (wenn noch kein Zustand aktiv ist)", "Estado inicial: rojo (si aún no hay ningún estado activo)", "État initial : rouge (si aucun état n'est encore actif)", "Stan początkowy: czerwony (jeśli żaden stan nie jest jeszcze aktywny)");
        ford("Piros fázis időzítése (4 s)", "Red phase timing (4 s)", "Zeitsteuerung der Rotphase (4 s)", "Temporización de la fase roja (4 s)", "Minutage de la phase rouge (4 s)", "Czasowanie fazy czerwonej (4 s)");
        ford("Piros → sárga átkapcsolás", "Red → yellow transition", "Umschaltung Rot → Gelb", "Transición rojo → amarillo", "Transition rouge → jaune", "Przejście czerwony → żółty");
        ford("Piros → sárga átkapcsolás (piros állapot megszüntetése)", "Red → yellow transition (ending the red state)", "Umschaltung Rot → Gelb (Beenden des Rotzustands)", "Transición rojo → amarillo (fin del estado rojo)", "Transition rouge → jaune (fin de l'état rouge)", "Przejście czerwony → żółty (zakończenie stanu czerwonego)");
        ford("Sárga fázis időzítése (1 s)", "Yellow phase timing (1 s)", "Zeitsteuerung der Gelbphase (1 s)", "Temporización de la fase amarilla (1 s)", "Minutage de la phase jaune (1 s)", "Czasowanie fazy żółtej (1 s)");
        ford("Sárga → zöld átkapcsolás", "Yellow → green transition", "Umschaltung Gelb → Grün", "Transición amarillo → verde", "Transition jaune → vert", "Przejście żółty → zielony");
        ford("Sárga → zöld átkapcsolás (sárga állapot megszüntetése)", "Yellow → green transition (ending the yellow state)", "Umschaltung Gelb → Grün (Beenden des Gelbzustands)", "Transición amarillo → verde (fin del estado amarillo)", "Transition jaune → vert (fin de l'état jaune)", "Przejście żółty → zielony (zakończenie stanu żółtego)");
        ford("Zöld fázis időzítése (3 s)", "Green phase timing (3 s)", "Zeitsteuerung der Grünphase (3 s)", "Temporización de la fase verde (3 s)", "Minutage de la phase verte (3 s)", "Czasowanie fazy zielonej (3 s)");
        ford("Zöld → piros átkapcsolás (ciklus újrakezdése)", "Green → red transition (cycle restart)", "Umschaltung Grün → Rot (Zyklusneustart)", "Transición verde → rojo (reinicio del ciclo)", "Transition vert → rouge (redémarrage du cycle)", "Przejście zielony → czerwony (ponowny start cyklu)");
        ford("Zöld → piros átkapcsolás (zöld állapot megszüntetése)", "Green → red transition (ending the green state)", "Umschaltung Grün → Rot (Beenden des Grünzustands)", "Transición verde → rojo (fin del estado verde)", "Transition vert → rouge (fin de l'état vert)", "Przejście zielony → czerwony (zakończenie stanu zielonego)");
        ford("Piros lámpa", "Red light", "Rotes Licht", "Luz roja", "Feu rouge", "Światło czerwone");
        ford("Sárga lámpa", "Yellow light", "Gelbes Licht", "Luz amarilla", "Feu jaune", "Światło żółte");
        ford("Zöld lámpa", "Green light", "Grünes Licht", "Luz verde", "Feu vert", "Światło zielone");

        // ---- nyomtatás / PDF export / dokumentáció ----
        ford("Nyomtatás / PDF export...", "Print / PDF export...", "Drucken / PDF-Export...", "Imprimir / exportar a PDF...", "Imprimer / exporter en PDF...", "Drukuj / eksport do PDF...");
        ford("Nyomtatás / PDF export", "Print / PDF export", "Drucken / PDF-Export", "Imprimir / exportar a PDF", "Imprimer / exporter en PDF", "Drukuj / eksport do PDF");
        ford("Projekt dokumentáció nyomtatása...", "Print project documentation...", "Projektdokumentation drucken...", "Imprimir documentación del proyecto...", "Imprimer la documentation du projet...", "Drukuj dokumentację projektu...");
        ford("Projekt dokumentáció", "Project documentation", "Projektdokumentation", "Documentación del proyecto", "Documentation du projet", "Dokumentacja projektu");
        ford("Válassza ki, melyik ábrát nyomtatja. A megjelenő nyomtatási párbeszédablakban a \"Microsoft Print to PDF\" nyomtatót választva PDF fájlként is elmentheti.",
                "Choose which diagram to print. In the print dialog that appears, selecting the \"Microsoft Print to PDF\" printer also lets you save it as a PDF file.",
                "Wählen Sie aus, welches Diagramm Sie drucken möchten. Im erscheinenden Druckdialog können Sie durch Auswahl des Druckers „Microsoft Print to PDF“ auch als PDF-Datei speichern.",
                "Elija qué diagrama desea imprimir. En el cuadro de diálogo de impresión que aparece, al seleccionar la impresora «Microsoft Print to PDF» también puede guardarlo como archivo PDF.",
                "Choisissez le schéma à imprimer. Dans la boîte de dialogue d'impression qui s'affiche, en sélectionnant l'imprimante « Microsoft Print to PDF », vous pouvez également l'enregistrer en fichier PDF.",
                "Wybierz, który schemat wydrukować. W wyświetlonym oknie dialogowym drukowania, wybierając drukarkę „Microsoft Print to PDF”, można również zapisać go jako plik PDF.");
        ford("Létradiagram nyomtatása...", "Print ladder diagram...", "Kontaktplan drucken...", "Imprimir diagrama de contactos...", "Imprimer le schéma à contacts...", "Drukuj schemat drabinkowy...");
        ford("HMI nyomtatása...", "Print HMI...", "HMI drucken...", "Imprimir HMI...", "Imprimer le HMI...", "Drukuj HMI...");
        ford("Funkcióblokk (FBD) nyomtatása...", "Print function block diagram (FBD)...", "Funktionsbaustein (FBD) drucken...", "Imprimir bloque de función (FBD)...", "Imprimer le bloc fonctionnel (FBD)...", "Drukuj blok funkcyjny (FBD)...");
        ford("GRAFCET nyomtatása...", "Print GRAFCET...", "GRAFCET drucken...", "Imprimir GRAFCET...", "Imprimer le GRAFCET...", "Drukuj GRAFCET...");
        ford("Nyomtatás elindítva:", "Print job started:", "Druckauftrag gestartet:", "Trabajo de impresión iniciado:", "Impression lancée :", "Rozpoczęto drukowanie:");
        ford("Nyomtatási hiba:", "Printing error:", "Druckfehler:", "Error de impresión:", "Erreur d'impression :", "Błąd drukowania:");
        ford("(nincs elmentve)", "(not saved)", "(nicht gespeichert)", "(no guardado)", "(non enregistré)", "(niezapisany)");
        ford("Fájl:", "File:", "Datei:", "Archivo:", "Fichier :", "Plik:");
        ford("Létrehozva:", "Created:", "Erstellt:", "Creado:", "Créé le :", "Utworzono:");
        ford("Címke lista (I/Q/M/T/C)", "Tag list (I/Q/M/T/C)", "Tag-Liste (I/Q/M/T/C)", "Lista de etiquetas (I/Q/M/T/C)", "Liste des tags (I/Q/M/T/C)", "Lista tagów (I/Q/M/T/C)");
        ford("Nincs elnevezett címke.", "No tags have been named.", "Es sind keine Tags benannt.", "No se ha nombrado ninguna etiqueta.", "Aucun tag n'a été nommé.", "Nie nazwano żadnego tagu.");
        ford("A projekt dokumentáció nyomtatása elindítva.", "Printing of the project documentation has started.", "Der Druck der Projektdokumentation wurde gestartet.", "Se ha iniciado la impresión de la documentación del proyecto.", "L'impression de la documentation du projet a commencé.", "Rozpoczęto drukowanie dokumentacji projektu.");

        // ---- minta programok: HMI / FBD / GRAFCET tartalom ----
        ford("Motor indítás/leállítás", "Motor start/stop", "Motor Start/Stopp", "Arranque/paro del motor", "Démarrage/arrêt du moteur", "Start/stop silnika");
        ford("Indítás", "Start", "Start", "Arranque", "Démarrage", "Start");
        ford("Leállítás", "Stop", "Stopp", "Paro", "Arrêt", "Stop");
        ford("Motor", "Motor", "Motor", "Motor", "Moteur", "Silnik");
        ford("Jelzőlámpa", "Indicator light", "Anzeigelampe", "Luz indicadora", "Voyant", "Lampka sygnalizacyjna");
        ford("Késleltetés", "Delay", "Verzögerung", "Retardo", "Retard", "Zwłoka");
        ford("Indítások", "Starts", "Starts", "Arranques", "Démarrages", "Rozruchy");
        ford("Önfenntartó (SR)", "Self-sustaining (SR)", "Selbsthaltend (SR)", "Autoalimentado (SR)", "Autoentretenu (SR)", "Samopodtrzymujący (SR)");
        ford("Motor (FBD demo)", "Motor (FBD demo)", "Motor (FBD-Demo)", "Motor (demo FBD)", "Moteur (démo FBD)", "Silnik (demo FBD)");
        ford("Megállva", "Stopped", "Gestoppt", "Detenido", "Arrêté", "Zatrzymany");
        ford("Motor fut", "Motor running", "Motor läuft", "Motor en marcha", "Moteur en marche", "Silnik pracuje");
        ford("Fő", "Main", "Haupt", "Principal", "Principal", "Główny");
        ford("Csillag", "Star", "Stern", "Estrella", "Étoile", "Gwiazda");
        ford("Delta", "Delta", "Dreieck", "Triángulo", "Triangle", "Trójkąt");
        ford("Átkapcsolási idő", "Switchover time", "Umschaltzeit", "Tiempo de conmutación", "Temps de commutation", "Czas przełączania");
        ford("Fő (figyelő)", "Main (monitor)", "Haupt (Überwachung)", "Principal (monitor)", "Principal (surveillance)", "Główny (monitor)");
        ford("Átkapcsolási idő (figyelő)", "Switchover time (monitor)", "Umschaltzeit (Überwachung)", "Tiempo de conmutación (monitor)", "Temps de commutation (surveillance)", "Czas przełączania (monitor)");
        ford("Még nem telt le", "Not yet elapsed", "Noch nicht abgelaufen", "Aún no ha transcurrido", "Pas encore écoulé", "Jeszcze nie upłynął");
        ford("Csillag ellenőrzés", "Star check", "Stern-Prüfung", "Comprobación de estrella", "Vérification étoile", "Sprawdzenie gwiazdy");
        ford("Csillag jelzés (FBD demo)", "Star signal (FBD demo)", "Stern-Signal (FBD-Demo)", "Señal de estrella (demo FBD)", "Signal étoile (démo FBD)", "Sygnał gwiazdy (demo FBD)");
        ford("Állva", "Stopped", "Gestoppt", "Detenido", "Arrêté", "Zatrzymany");
        ford("Csillag fázis", "Star phase", "Stern-Phase", "Fase estrella", "Phase étoile", "Faza gwiazdy");
        ford("Delta fázis", "Delta phase", "Dreieck-Phase", "Fase triángulo", "Phase triangle", "Faza trójkąta");
        ford("1. szalag", "Conveyor 1", "Förderband 1", "Cinta 1", "Convoyeur 1", "Przenośnik 1");
        ford("2. szalag", "Conveyor 2", "Förderband 2", "Cinta 2", "Convoyeur 2", "Przenośnik 2");
        ford("3. szalag", "Conveyor 3", "Förderband 3", "Cinta 3", "Convoyeur 3", "Przenośnik 3");
        ford("1. késleltetés", "Delay 1", "Verzögerung 1", "Retardo 1", "Retard 1", "Zwłoka 1");
        ford("2. késleltetés", "Delay 2", "Verzögerung 2", "Retardo 2", "Retard 2", "Zwłoka 2");
        ford("1. szalag (figyelő)", "Conveyor 1 (monitor)", "Förderband 1 (Überwachung)", "Cinta 1 (monitor)", "Convoyeur 1 (surveillance)", "Przenośnik 1 (monitor)");
        ford("2. szalag (figyelő)", "Conveyor 2 (monitor)", "Förderband 2 (Überwachung)", "Cinta 2 (monitor)", "Convoyeur 2 (surveillance)", "Przenośnik 2 (monitor)");
        ford("3. szalag (figyelő)", "Conveyor 3 (monitor)", "Förderband 3 (Überwachung)", "Cinta 3 (monitor)", "Convoyeur 3 (surveillance)", "Przenośnik 3 (monitor)");
        ford("Mind a 3 szalag fut", "All 3 conveyors running", "Alle 3 Förderbänder laufen", "Las 3 cintas en marcha", "Les 3 convoyeurs en marche", "Wszystkie 3 przenośniki pracują");
        ford("Teljes sor üzemel (FBD demo)", "Entire line running (FBD demo)", "Gesamte Linie in Betrieb (FBD-Demo)", "Toda la línea en funcionamiento (demo FBD)", "Toute la ligne en fonctionnement (démo FBD)", "Cała linia działa (demo FBD)");
        ford("Sor fut", "Line running", "Linie läuft", "Línea en marcha", "Ligne en marche", "Linia pracuje");
        ford("Közlekedési lámpa", "Traffic light", "Verkehrsampel", "Semáforo", "Feu de circulation", "Sygnalizacja świetlna");
        ford("Piros", "Red", "Rot", "Rojo", "Rouge", "Czerwony");
        ford("Sárga", "Yellow", "Gelb", "Amarillo", "Jaune", "Żółty");
        ford("Zöld", "Green", "Grün", "Verde", "Vert", "Zielony");
        ford("Sárga (figyelő)", "Yellow (monitor)", "Gelb (Überwachung)", "Amarillo (monitor)", "Jaune (surveillance)", "Żółty (monitor)");
        ford("Felfutás-érzékelés", "Rising-edge detection", "Erkennung der steigenden Flanke", "Detección de flanco ascendente", "Détection de front montant", "Wykrywanie zbocza narastającego");
        ford("Sárga felfutás impulzus (FBD demo)", "Yellow rising-edge pulse (FBD demo)", "Gelb-Anstiegsimpuls (FBD-Demo)", "Impulso de flanco ascendente amarillo (demo FBD)", "Impulsion de front montant jaune (démo FBD)", "Impuls narastania żółtego (demo FBD)");
        ford("Piros (GRAFCET demo)", "Red (GRAFCET demo)", "Rot (GRAFCET-Demo)", "Rojo (demo GRAFCET)", "Rouge (démo GRAFCET)", "Czerwony (demo GRAFCET)");
        ford("Sárga (GRAFCET demo)", "Yellow (GRAFCET demo)", "Gelb (GRAFCET-Demo)", "Amarillo (demo GRAFCET)", "Jaune (démo GRAFCET)", "Żółty (demo GRAFCET)");
        ford("Zöld (GRAFCET demo)", "Green (GRAFCET demo)", "Grün (GRAFCET-Demo)", "Verde (demo GRAFCET)", "Vert (démo GRAFCET)", "Zielony (demo GRAFCET)");

        // ---- HMI: Zebra / Ajtó elemek ----
        ford("Zebra", "Zebra crossing", "Zebrastreifen", "Paso de peatones", "Passage piéton", "Przejście dla pieszych");
        ford("Ajtó", "Door", "Tür", "Puerta", "Porte", "Drzwi");

        // ---- HMI: szállítószalag / érzékelő / robot elemek ----
        ford("Szállítószalag", "Conveyor belt", "Förderband", "Cinta transportadora", "Convoyeur", "Przenośnik taśmowy");
        ford("Robotcella (motorblokk be- és kirakás)", "Robot cell (engine block load/unload)", "Roboterzelle (Motorblock Be-/Entladen)", "Célula robótica (carga/descarga de bloques de motor)", "Cellule robotisée (chargement/déchargement de blocs-moteurs)", "Cela robotyczna (załadunek/wyładunek bloków silnika)");
        ford("Érzékelő", "Sensor", "Sensor", "Sensor", "Capteur", "Czujnik");
        ford("Fanuc robot", "Fanuc robot", "Fanuc-Roboter", "Robot Fanuc", "Robot Fanuc", "Robot Fanuc");
        ford("Kuka robot", "Kuka robot", "Kuka-Roboter", "Robot Kuka", "Robot Kuka", "Robot Kuka");
        ford("Forgatás (érzékelőnél):", "Rotation (for sensor):", "Drehung (bei Sensor):", "Rotación (para el sensor):", "Rotation (pour le capteur) :", "Obrót (dla czujnika):");
        ford("Robotkarnál:", "For robot arm:", "Beim Roboterarm:", "Para el brazo robótico:", "Pour le bras robotisé :", "Dla ramienia robota:");
        ford("Tükrözve (ellentétes irányba néz)", "Mirrored (faces the opposite direction)", "Gespiegelt (zeigt in die entgegengesetzte Richtung)", "Reflejado (orientado en dirección opuesta)", "Inversé (orienté dans la direction opposée)", "Odbity (zwrócony w przeciwną stronę)");
        ford("Az érzékelők 0° / 90° / 180° / 270° forgatásban, a Kuka kar tükrözve látható.",
                "The sensors are shown rotated by 0° / 90° / 180° / 270°, and the Kuka arm is mirrored.",
                "Die Sensoren sind um 0° / 90° / 180° / 270° gedreht dargestellt, der Kuka-Arm ist gespiegelt.",
                "Los sensores se muestran rotados 0° / 90° / 180° / 270°, y el brazo Kuka está reflejado.",
                "Les capteurs sont affichés avec une rotation de 0° / 90° / 180° / 270°, et le bras Kuka est inversé.",
                "Czujniki są obrócone o 0° / 90° / 180° / 270°, a ramię Kuka jest odbite.");

        // ---- Lift (2 szintes) ----
        ford("Kezdeti állapot: földszinten, ajtó zárva (ha még nincs aktív állapot)",
                "Initial state: at ground floor, door closed (if no state is active yet)",
                "Anfangszustand: im Erdgeschoss, Tür geschlossen (wenn noch kein Zustand aktiv ist)",
                "Estado inicial: en planta baja, puerta cerrada (si aún no hay ningún estado activo)",
                "État initial : au rez-de-chaussée, porte fermée (si aucun état n'est encore actif)",
                "Stan początkowy: na parterze, drzwi zamknięte (jeśli żaden stan nie jest jeszcze aktywny)");
        ford("Földszinten állva: hívás emeletre -> indulás felfelé", "At ground floor: call to upper floor -> start moving up", "Im Erdgeschoss: Ruf zum Obergeschoss -> Start der Aufwärtsfahrt", "En planta baja: llamada al piso superior -> inicio de subida", "Au rez-de-chaussée : appel à l'étage -> démarrage de la montée", "Na parterze: wezwanie na piętro -> start jazdy w górę");
        ford("Földszinten állva: indulás felfelé (állapotváltás)", "At ground floor: start moving up (state change)", "Im Erdgeschoss: Start der Aufwärtsfahrt (Zustandswechsel)", "En planta baja: inicio de subida (cambio de estado)", "Au rez-de-chaussée : démarrage de la montée (changement d'état)", "Na parterze: start jazdy w górę (zmiana stanu)");
        ford("Földszinten állva: hívás földszintre -> ajtó nyitása", "At ground floor: call to ground floor -> open door", "Im Erdgeschoss: Ruf ins Erdgeschoss -> Tür öffnen", "En planta baja: llamada a planta baja -> abrir puerta", "Au rez-de-chaussée : appel au rez-de-chaussée -> ouverture de la porte", "Na parterze: wezwanie na parter -> otwarcie drzwi");
        ford("Földszinten állva: ajtó nyitása (állapotváltás)", "At ground floor: open door (state change)", "Im Erdgeschoss: Tür öffnen (Zustandswechsel)", "En planta baja: abrir puerta (cambio de estado)", "Au rez-de-chaussée : ouverture de la porte (changement d'état)", "Na parterze: otwarcie drzwi (zmiana stanu)");
        ford("Ajtónyitás ideje földszinten (3 s)", "Door-open duration at ground floor (3 s)", "Türöffnungsdauer im Erdgeschoss (3 s)", "Duración de apertura de puerta en planta baja (3 s)", "Durée d'ouverture de la porte au rez-de-chaussée (3 s)", "Czas otwarcia drzwi na parterze (3 s)");
        ford("Ajtó becsukódik földszinten, vissza alapállapotba", "Door closes at ground floor, back to idle state", "Tür schließt im Erdgeschoss, zurück in den Ruhezustand", "La puerta se cierra en planta baja, vuelve al estado de reposo", "La porte se ferme au rez-de-chaussée, retour à l'état de repos", "Drzwi zamykają się na parterze, powrót do stanu spoczynku");
        ford("Ajtó becsukódik földszinten (állapotváltás)", "Door closes at ground floor (state change)", "Tür schließt im Erdgeschoss (Zustandswechsel)", "La puerta se cierra en planta baja (cambio de estado)", "La porte se ferme au rez-de-chaussée (changement d'état)", "Drzwi zamykają się na parterze (zmiana stanu)");
        ford("Felfelé haladás ideje (2,5 s)", "Upward travel time (2.5 s)", "Aufwärtsfahrzeit (2,5 s)", "Tiempo de subida (2,5 s)", "Temps de montée (2,5 s)", "Czas jazdy w górę (2,5 s)");
        ford("Megérkezés az emeletre: ajtó automatikus nyitása", "Arrival at upper floor: door opens automatically", "Ankunft im Obergeschoss: Tür öffnet automatisch", "Llegada al piso superior: la puerta se abre automáticamente", "Arrivée à l'étage : la porte s'ouvre automatiquement", "Przyjazd na piętro: drzwi otwierają się automatycznie");
        ford("Megérkezés az emeletre (állapotváltás)", "Arrival at upper floor (state change)", "Ankunft im Obergeschoss (Zustandswechsel)", "Llegada al piso superior (cambio de estado)", "Arrivée à l'étage (changement d'état)", "Przyjazd na piętro (zmiana stanu)");
        ford("Emeleten állva: hívás földszintre -> indulás lefelé", "At upper floor: call to ground floor -> start moving down", "Im Obergeschoss: Ruf ins Erdgeschoss -> Start der Abwärtsfahrt", "En el piso superior: llamada a planta baja -> inicio de bajada", "À l'étage : appel au rez-de-chaussée -> démarrage de la descente", "Na piętrze: wezwanie na parter -> start jazdy w dół");
        ford("Emeleten állva: indulás lefelé (állapotváltás)", "At upper floor: start moving down (state change)", "Im Obergeschoss: Start der Abwärtsfahrt (Zustandswechsel)", "En el piso superior: inicio de bajada (cambio de estado)", "À l'étage : démarrage de la descente (changement d'état)", "Na piętrze: start jazdy w dół (zmiana stanu)");
        ford("Emeleten állva: hívás emeletre -> ajtó nyitása", "At upper floor: call to upper floor -> open door", "Im Obergeschoss: Ruf zum Obergeschoss -> Tür öffnen", "En el piso superior: llamada al piso superior -> abrir puerta", "À l'étage : appel à l'étage -> ouverture de la porte", "Na piętrze: wezwanie na piętro -> otwarcie drzwi");
        ford("Emeleten állva: ajtó nyitása (állapotváltás)", "At upper floor: open door (state change)", "Im Obergeschoss: Tür öffnen (Zustandswechsel)", "En el piso superior: abrir puerta (cambio de estado)", "À l'étage : ouverture de la porte (changement d'état)", "Na piętrze: otwarcie drzwi (zmiana stanu)");
        ford("Ajtónyitás ideje emeleten (3 s)", "Door-open duration at upper floor (3 s)", "Türöffnungsdauer im Obergeschoss (3 s)", "Duración de apertura de puerta en el piso superior (3 s)", "Durée d'ouverture de la porte à l'étage (3 s)", "Czas otwarcia drzwi na piętrze (3 s)");
        ford("Ajtó becsukódik emeleten, vissza alapállapotba", "Door closes at upper floor, back to idle state", "Tür schließt im Obergeschoss, zurück in den Ruhezustand", "La puerta se cierra en el piso superior, vuelve al estado de reposo", "La porte se ferme à l'étage, retour à l'état de repos", "Drzwi zamykają się na piętrze, powrót do stanu spoczynku");
        ford("Ajtó becsukódik emeleten (állapotváltás)", "Door closes at upper floor (state change)", "Tür schließt im Obergeschoss (Zustandswechsel)", "La puerta se cierra en el piso superior (cambio de estado)", "La porte se ferme à l'étage (changement d'état)", "Drzwi zamykają się na piętrze (zmiana stanu)");
        ford("Lefelé haladás ideje (2,5 s)", "Downward travel time (2.5 s)", "Abwärtsfahrzeit (2,5 s)", "Tiempo de bajada (2,5 s)", "Temps de descente (2,5 s)", "Czas jazdy w dół (2,5 s)");
        ford("Megérkezés a földszintre: ajtó automatikus nyitása", "Arrival at ground floor: door opens automatically", "Ankunft im Erdgeschoss: Tür öffnet automatisch", "Llegada a planta baja: la puerta se abre automáticamente", "Arrivée au rez-de-chaussée : la porte s'ouvre automatiquement", "Przyjazd na parter: drzwi otwierają się automatycznie");
        ford("Megérkezés a földszintre (állapotváltás)", "Arrival at ground floor (state change)", "Ankunft im Erdgeschoss (Zustandswechsel)", "Llegada a planta baja (cambio de estado)", "Arrivée au rez-de-chaussée (changement d'état)", "Przyjazd na parter (zmiana stanu)");
        ford("Jelzés: a kabin a földszinten van", "Indicator: cabin is at ground floor", "Anzeige: Kabine ist im Erdgeschoss", "Indicador: la cabina está en planta baja", "Indicateur : la cabine est au rez-de-chaussée", "Wskaźnik: kabina jest na parterze");
        ford("Jelzés: a kabin az emeleten van", "Indicator: cabin is at upper floor", "Anzeige: Kabine ist im Obergeschoss", "Indicador: la cabina está en el piso superior", "Indicateur : la cabine est à l'étage", "Wskaźnik: kabina jest na piętrze");
        ford("Ajtó nyitva jelzés (a HMI ajtó-animációnak)", "Door-open signal (for the HMI door animation)", "Signal „Tür offen“ (für die HMI-Türanimation)", "Señal de puerta abierta (para la animación de la puerta en el HMI)", "Signal de porte ouverte (pour l'animation de la porte dans le HMI)", "Sygnał otwartych drzwi (do animacji drzwi w HMI)");
        ford("Jelzés: a kabin mozgásban van", "Indicator: cabin is moving", "Anzeige: Kabine ist in Bewegung", "Indicador: la cabina está en movimiento", "Indicateur : la cabine est en mouvement", "Wskaźnik: kabina jest w ruchu");
        ford("Lift (2 szintes)", "Lift (2-floor)", "Aufzug (2 Stockwerke)", "Ascensor (2 plantas)", "Ascenseur (2 étages)", "Winda (2-piętrowa)");
        ford("Hívás: földszint", "Call: ground floor", "Ruf: Erdgeschoss", "Llamada: planta baja", "Appel : rez-de-chaussée", "Wezwanie: parter");
        ford("Hívás: emelet", "Call: upper floor", "Ruf: Obergeschoss", "Llamada: piso superior", "Appel : étage", "Wezwanie: piętro");
        ford("Emelet", "Upper floor", "Obergeschoss", "Piso superior", "Étage", "Piętro");
        ford("Földszint", "Ground floor", "Erdgeschoss", "Planta baja", "Rez-de-chaussée", "Parter");
        ford("Emeleten", "At upper floor", "Im Obergeschoss", "En el piso superior", "À l'étage", "Na piętrze");
        ford("Földszinten", "At ground floor", "Im Erdgeschoss", "En planta baja", "Au rez-de-chaussée", "Na parterze");
        ford("Mozgásban", "Moving", "In Bewegung", "En movimiento", "En mouvement", "W ruchu");
        ford("Ajtó nyitva (figyelő)", "Door open (monitor)", "Tür offen (Überwachung)", "Puerta abierta (monitor)", "Porte ouverte (surveillance)", "Drzwi otwarte (monitor)");
        ford("Mozgásban (figyelő)", "Moving (monitor)", "In Bewegung (Überwachung)", "En movimiento (monitor)", "En mouvement (surveillance)", "W ruchu (monitor)");
        ford("Egyszerre? (hibajelzés)", "Both at once? (fault signal)", "Beides gleichzeitig? (Fehlersignal)", "¿Ambos a la vez? (señal de fallo)", "Les deux à la fois ? (signal de défaut)", "Oba naraz? (sygnał błędu)");
        ford("Biztonsági hiba (FBD demo)", "Safety fault (FBD demo)", "Sicherheitsfehler (FBD-Demo)", "Fallo de seguridad (demo FBD)", "Défaut de sécurité (démo FBD)", "Błąd bezpieczeństwa (demo FBD)");
        ford("Földszinten (GRAFCET demo)", "At ground floor (GRAFCET demo)", "Im Erdgeschoss (GRAFCET-Demo)", "En planta baja (demo GRAFCET)", "Au rez-de-chaussée (démo GRAFCET)", "Na parterze (demo GRAFCET)");
        ford("Emeleten (GRAFCET demo)", "At upper floor (GRAFCET demo)", "Im Obergeschoss (GRAFCET-Demo)", "En el piso superior (demo GRAFCET)", "À l'étage (démo GRAFCET)", "Na piętrze (demo GRAFCET)");

        // ---- Útkereszteződés gyalogátkelővel ----
        ford("Útkereszteződés gyalogátkelővel", "Intersection with pedestrian crossing", "Kreuzung mit Fußgängerüberweg", "Intersección con paso de peatones", "Carrefour avec passage piéton", "Skrzyżowanie z przejściem dla pieszych");
        ford("Piros fázis időzítése (5 s)", "Red phase timing (5 s)", "Zeitsteuerung der Rotphase (5 s)", "Temporización de la fase roja (5 s)", "Minutage de la phase rouge (5 s)", "Czasowanie fazy czerwonej (5 s)");
        ford("Zöld fázis időzítése (4 s)", "Green phase timing (4 s)", "Zeitsteuerung der Grünphase (4 s)", "Temporización de la fase verde (4 s)", "Minutage de la phase verte (4 s)", "Czasowanie fazy zielonej (4 s)");
        ford("Piros lámpa (autós)", "Red light (vehicle)", "Rotes Licht (Fahrzeuge)", "Luz roja (vehículos)", "Feu rouge (véhicules)", "Światło czerwone (pojazdy)");
        ford("Sárga lámpa (autós)", "Yellow light (vehicle)", "Gelbes Licht (Fahrzeuge)", "Luz amarilla (vehículos)", "Feu jaune (véhicules)", "Światło żółte (pojazdy)");
        ford("Zöld lámpa (autós)", "Green light (vehicle)", "Grünes Licht (Fahrzeuge)", "Luz verde (vehículos)", "Feu vert (véhicules)", "Światło zielone (pojazdy)");
        ford("Gyalogos hívásgomb regisztrálása (bármikor nyomható)", "Pedestrian call-button registration (can be pressed any time)", "Registrierung des Fußgänger-Rufknopfes (kann jederzeit gedrückt werden)", "Registro del botón de llamada de peatones (se puede pulsar en cualquier momento)", "Enregistrement du bouton d'appel piéton (peut être pressé à tout moment)", "Rejestracja przycisku wezwania pieszych (można nacisnąć w każdej chwili)");
        ford("Gyalogos zöld: piros autós fázisban, ha volt hívás", "Pedestrian green: during the vehicle red phase, if a call was made", "Fußgänger grün: während der Fahrzeug-Rotphase, falls ein Ruf erfolgte", "Peatón verde: durante la fase roja de vehículos, si hubo una llamada", "Piéton vert : pendant la phase rouge des véhicules, si un appel a été fait", "Pieszy zielony: w fazie czerwonej dla pojazdów, jeśli było wezwanie");
        ford("Gyalogos piros: amikor nincs gyalogos zöld (negált kimenet)", "Pedestrian red: whenever pedestrian green is not active (negated coil)", "Fußgänger rot: wenn Fußgänger grün nicht aktiv ist (negierte Spule)", "Peatón rojo: cuando el verde de peatones no está activo (bobina negada)", "Piéton rouge : lorsque le vert piéton n'est pas actif (bobine négatée)", "Pieszy czerwony: gdy zielony dla pieszych nie jest aktywny (cewka zanegowana)");
        ford("Kiszolgált gyalogos hívás nyugtázása", "Acknowledging the served pedestrian call", "Quittierung des bedienten Fußgängerrufs", "Confirmación de la llamada de peatones atendida", "Acquittement de l'appel piéton traité", "Potwierdzenie obsłużonego wezwania pieszych");
        ford("Gyalogos hívás", "Pedestrian call", "Fußgängerruf", "Llamada de peatones", "Appel piéton", "Wezwanie pieszych");
        ford("Gyalogos piros", "Pedestrian red", "Fußgänger rot", "Peatón rojo", "Piéton rouge", "Pieszy czerwony");
        ford("Gyalogos zöld", "Pedestrian green", "Fußgänger grün", "Peatón verde", "Piéton vert", "Pieszy zielony");
        ford("Gyalogos hívás várakozik (figyelő)", "Pedestrian call waiting (monitor)", "Fußgängerruf wartet (Überwachung)", "Llamada de peatones en espera (monitor)", "Appel piéton en attente (surveillance)", "Wezwanie pieszych czeka (monitor)");
        ford("Lefutás-érzékelés (hívás kiszolgálva)", "Falling-edge detection (call served)", "Erkennung der fallenden Flanke (Ruf bedient)", "Detección de flanco descendente (llamada atendida)", "Détection de front descendant (appel traité)", "Wykrywanie zbocza opadającego (wezwanie obsłużone)");
        ford("Hívás kiszolgálva impulzus (FBD demo)", "Call-served pulse (FBD demo)", "Impuls „Ruf bedient“ (FBD-Demo)", "Impulso de llamada atendida (demo FBD)", "Impulsion d'appel traité (démo FBD)", "Impuls obsłużonego wezwania (demo FBD)");
        ford("Gyalogos tilos (GRAFCET demo)", "Pedestrian forbidden (GRAFCET demo)", "Fußgänger verboten (GRAFCET-Demo)", "Peatón prohibido (demo GRAFCET)", "Piéton interdit (démo GRAFCET)", "Pieszym zakaz (demo GRAFCET)");
        ford("Gyalogos szabad (GRAFCET demo)", "Pedestrian allowed (GRAFCET demo)", "Fußgänger frei (GRAFCET-Demo)", "Peatón permitido (demo GRAFCET)", "Piéton autorisé (démo GRAFCET)", "Pieszym wolno (demo GRAFCET)");

        // ---- óraimpulzus (clock bit) generátorok ----
        ford("Óraimpulzusok (clock bitek) szerkesztése...", "Edit clock pulses (clock bits)...", "Taktimpulse (Clock-Bits) bearbeiten...", "Editar impulsos de reloj (bits de reloj)...", "Modifier les impulsions d'horloge (bits d'horloge)...", "Edytuj impulsy zegarowe (bity zegarowe)...");
        ford("Óraimpulzusok (clock bitek) szerkesztése", "Edit clock pulses (clock bits)", "Taktimpulse (Clock-Bits) bearbeiten", "Editar impulsos de reloj (bits de reloj)", "Modifier les impulsions d'horloge (bits d'horloge)", "Edytuj impulsy zegarowe (bity zegarowe)");
        ford("Cél (M index):", "Target (M index):", "Ziel (M-Index):", "Destino (índice M):", "Cible (index M) :", "Cel (indeks M):");
        ford("Frekvencia (Hz):", "Frequency (Hz):", "Frequenz (Hz):", "Frecuencia (Hz):", "Fréquence (Hz) :", "Częstotliwość (Hz):");
        ford("Óraimpulzus szerkesztése", "Edit clock pulse", "Taktimpuls bearbeiten", "Editar impulso de reloj", "Modifier l'impulsion d'horloge", "Edytuj impuls zegarowy");
        ford("Szerkesztés...", "Edit...", "Bearbeiten...", "Editar...", "Modifier...", "Edytuj...");
        ford("Minden sor egy belső relét (M) adott frekvencián automatikusan kapcsol (50%-os kitöltéssel) – pl. vibráló jelzőlámpához. A bit bármely létra-, FBD- vagy HMI-elemben felhasználható kontaktusként.",
                "Each row automatically toggles an internal relay (M) at the given frequency (50% duty cycle) – e.g. for a flashing indicator light. The bit can be used as a contact in any ladder, FBD or HMI element.",
                "Jede Zeile schaltet ein internes Relais (M) automatisch mit der angegebenen Frequenz (50 % Tastverhältnis) – z. B. für eine blinkende Anzeigelampe. Das Bit kann als Kontakt in jedem Kontaktplan-, FBD- oder HMI-Element verwendet werden.",
                "Cada fila activa y desactiva automáticamente un relé interno (M) a la frecuencia indicada (ciclo de trabajo del 50%), por ejemplo para una luz indicadora intermitente. El bit puede usarse como contacto en cualquier elemento de diagrama de contactos, FBD o HMI.",
                "Chaque ligne fait basculer automatiquement un relais interne (M) à la fréquence indiquée (rapport cyclique de 50 %) – par exemple pour un voyant clignotant. Le bit peut être utilisé comme contact dans n'importe quel élément de schéma à contacts, FBD ou HMI.",
                "Każdy wiersz automatycznie przełącza przekaźnik wewnętrzny (M) z podaną częstotliwością (wypełnienie 50%) – np. do migającej lampki sygnalizacyjnej. Bit można wykorzystać jako kontakt w każdym elemencie schematu drabinkowego, FBD lub HMI.");
        ford("Két óraimpulzus nem célozhatja ugyanazt az M bitet:", "Two clock pulses cannot target the same M bit:", "Zwei Taktimpulse dürfen nicht dasselbe M-Bit ansteuern:", "Dos impulsos de reloj no pueden apuntar al mismo bit M:", "Deux impulsions d'horloge ne peuvent pas cibler le même bit M :", "Dwa impulsy zegarowe nie mogą celować w ten sam bit M:");
        ford("Órajel", "Clock pulse", "Taktimpuls", "Impulso de reloj", "Impulsion d'horloge", "Impuls zegarowy");

        // ---- Villogó lámpa / Futófény minták ----
        ford("Villogó lámpa (óraimpulzussal)", "Flashing lamp (with clock pulse)", "Blinklampe (mit Taktimpuls)", "Lámpara intermitente (con impulso de reloj)", "Lampe clignotante (avec impulsion d'horloge)", "Migająca lampka (z impulsem zegarowym)");
        ford("Futófény (óraimpulzussal)", "Chaser light (with clock pulse)", "Lauflicht (mit Taktimpuls)", "Luz secuencial (con impulso de reloj)", "Chenillard lumineux (avec impulsion d'horloge)", "Światło biegnące (z impulsem zegarowym)");
        ford("A Q0 lámpa közvetlenül az órajel (M15) állapotát követi", "Lamp Q0 directly follows the state of the clock pulse (M15)", "Die Lampe Q0 folgt direkt dem Zustand des Taktimpulses (M15)", "La lámpara Q0 sigue directamente el estado del impulso de reloj (M15)", "La lampe Q0 suit directement l'état de l'impulsion d'horloge (M15)", "Lampka Q0 bezpośrednio podąża za stanem impulsu zegarowego (M15)");
        ford("Futófény (óraimpulzussal vezérelve)", "Chaser light (controlled by a clock pulse)", "Lauflicht (gesteuert durch einen Taktimpuls)", "Luz secuencial (controlada por un impulso de reloj)", "Chenillard lumineux (piloté par une impulsion d'horloge)", "Światło biegnące (sterowane impulsem zegarowym)");
        ford("Lépés-zár feloldása két impulzus között (felkészülés a következő lépésre)", "Releasing the step lock between pulses (preparing for the next step)", "Freigabe der Schrittsperre zwischen den Impulsen (Vorbereitung auf den nächsten Schritt)", "Liberación del bloqueo de paso entre impulsos (preparación para el siguiente paso)", "Libération du verrou de pas entre deux impulsions (préparation du pas suivant)", "Zwolnienie blokady kroku między impulsami (przygotowanie do kolejnego kroku)");
        ford("Kezdeti pozíció beállítása (ha még semelyik sem aktív)", "Setting the initial position (if none is active yet)", "Einstellen der Startposition (wenn noch keine aktiv ist)", "Establecimiento de la posición inicial (si ninguna está aún activa)", "Réglage de la position initiale (si aucune n'est encore active)", "Ustawienie pozycji początkowej (jeśli żadna nie jest jeszcze aktywna)");
        ford("Pozíció", "Position", "Position", "Posición", "Position", "Pozycja");
        ford("lépés az órajel impulzusára", "step on the clock pulse", "Schritt bei Taktimpuls", "paso con el impulso de reloj", "pas sur l'impulsion d'horloge", "krok na impuls zegarowy");
        ford("Lépés-zár beállítása (impulzusonként csak egy lépés)", "Setting the step lock (only one step per pulse)", "Setzen der Schrittsperre (nur ein Schritt pro Impuls)", "Activación del bloqueo de paso (solo un paso por impulso)", "Activation du verrou de pas (un seul pas par impulsion)", "Ustawienie blokady kroku (tylko jeden krok na impuls)");
        ford("törlése a lépés után", "cleared after the step", "Löschen nach dem Schritt", "borrado tras el paso", "effacement après le pas", "kasowanie po kroku");
        ford("Kimeneti lámpa", "Output lamp", "Ausgangslampe", "Lámpara de salida", "Lampe de sortie", "Lampka wyjściowa");
        ford("Futás", "Run", "Lauf", "Marcha", "Marche", "Bieg");
        ford("Órajel (figyelő)", "Clock pulse (monitor)", "Taktimpuls (Überwachung)", "Impulso de reloj (monitor)", "Impulsion d'horloge (surveillance)", "Impuls zegarowy (monitor)");
        ford("Lépésimpulzus (FBD demo)", "Step pulse (FBD demo)", "Schrittimpuls (FBD-Demo)", "Impulso de paso (demo FBD)", "Impulsion de pas (démo FBD)", "Impuls kroku (demo FBD)");

        // ---- beépített súgó (Használat) bővítése: HMI, GRAFCET, óraimpulzusok, mintaprogramok, nyomtatás ----
        ford("A \"HMI\" fülön szabadon tervezhető kezelőfelület építhető: Lámpa, Gomb, Kijelző, Felirat, Téglalap, Trend, Zebra, Ajtó, Szállítószalag, Érzékelő, valamint Fanuc és Kuka robotkar elemekkel.",
                "The \"HMI\" tab lets you design a fully custom operator interface using Lamp, Button, Display, Label, Rectangle, Trend, Zebra crossing, Door, Conveyor belt, Sensor, and Fanuc/Kuka robot arm elements.",
                "Im Tab „HMI“ lässt sich eine frei gestaltbare Bedienoberfläche mit den Elementen Lampe, Taste, Anzeige, Beschriftung, Rechteck, Trend, Zebrastreifen, Tür, Förderband, Sensor sowie Fanuc- und Kuka-Roboterarm aufbauen.",
                "En la pestaña «HMI» puede diseñarse libremente una interfaz de operador con elementos Lámpara, Botón, Pantalla, Etiqueta, Rectángulo, Tendencia, Paso de peatones, Puerta, Cinta transportadora, Sensor y brazos robóticos Fanuc y Kuka.",
                "L'onglet « HMI » permet de concevoir librement une interface opérateur à l'aide des éléments Lampe, Bouton, Afficheur, Étiquette, Rectangle, Tendance, Passage piéton, Porte, Tapis roulant, Capteur, ainsi que des bras robotisés Fanuc et Kuka.",
                "Na karcie „HMI” można swobodnie zaprojektować interfejs operatora, korzystając z elementów Lampka, Przycisk, Wyświetlacz, Etykieta, Prostokąt, Trend, Przejście dla pieszych, Drzwi, Przenośnik taśmowy, Czujnik oraz ramion robotów Fanuc i Kuka.");
        ford("\"✎ HMI szerkesztés\" módban válassza ki a legördülőből az elhelyezendő elem típusát, majd kattintson a vászonra; duplán kattintva az elemre szerkesztheti a tulajdonságait.",
                "In \"✎ Edit HMI\" mode, choose the element type from the drop-down list and click on the canvas to place it; double-click an element to edit its properties.",
                "Wählen Sie im Modus „✎ HMI bearbeiten“ den gewünschten Elementtyp aus der Dropdown-Liste und klicken Sie auf die Zeichenfläche, um ihn zu platzieren; ein Doppelklick auf das Element öffnet seine Eigenschaften.",
                "En el modo «✎ Editar HMI», seleccione el tipo de elemento en la lista desplegable y haga clic en el lienzo para colocarlo; haga doble clic en un elemento para editar sus propiedades.",
                "En mode « ✎ Modifier HMI », choisissez le type d'élément dans la liste déroulante puis cliquez sur le canevas pour le placer ; double-cliquez sur un élément pour modifier ses propriétés.",
                "W trybie „✎ Edycja HMI” wybierz z listy rozwijanej typ elementu do umieszczenia, a następnie kliknij na płótnie; kliknij dwukrotnie element, aby edytować jego właściwości.");
        ford("Több HMI-képernyő is létrehozható és kezelhető a Képernyő legördülővel; a \"⧉ Külön ablakban\" gombbal a HMI önálló, mozgatható ablakba helyezhető ki.",
                "Multiple HMI screens can be created and managed with the Screen drop-down; the \"⧉ In separate window\" button detaches the HMI into its own movable window.",
                "Mehrere HMI-Bildschirme lassen sich über die Dropdown-Liste „Bildschirm“ erstellen und verwalten; mit der Schaltfläche „⧉ In separatem Fenster“ wird das HMI in ein eigenes, verschiebbares Fenster ausgelagert.",
                "Pueden crearse y gestionarse varias pantallas HMI con la lista desplegable Pantalla; el botón «⧉ En ventana separada» saca el HMI a una ventana propia y movible.",
                "Plusieurs écrans HMI peuvent être créés et gérés via la liste déroulante Écran ; le bouton « ⧉ Dans une fenêtre séparée » détache le HMI dans sa propre fenêtre déplaçable.",
                "Można utworzyć i obsługiwać wiele ekranów HMI za pomocą listy rozwijanej Ekran; przycisk „⧉ W osobnym oknie” wydziela HMI do własnego, przesuwalnego okna.");
        ford("Futás közben, szerkesztés mód nélkül a gombok élőben reagálnak a kattintásra, a lámpák és kijelzők pedig élőben követik a PLC logikáját.",
                "While running, without edit mode, buttons respond live to clicks, and lamps and displays follow the PLC logic in real time.",
                "Im laufenden Betrieb, außerhalb des Bearbeitungsmodus, reagieren die Schaltflächen live auf Klicks, während Lampen und Anzeigen die SPS-Logik in Echtzeit abbilden.",
                "Durante la ejecución, sin el modo de edición, los botones responden en vivo a los clics, y las lámparas y pantallas siguen la lógica del PLC en tiempo real.",
                "Pendant l'exécution, hors mode édition, les boutons réagissent en direct aux clics, et les lampes et afficheurs suivent la logique de l'automate en temps réel.",
                "Podczas pracy, poza trybem edycji, przyciski reagują na klikanie na żywo, a lampki i wyświetlacze na bieżąco odzwierciedlają logikę sterownika PLC.");
        ford("A \"GRAFCET\" fülön szekvenciális, lépésről lépésre haladó folyamatok tervezhetők: Lépésekből és a köztük lévő feltételes Átmenetekből épül fel a diagram.",
                "The \"GRAFCET\" tab lets you design sequential, step-by-step processes: the diagram is built from Steps and the conditional Transitions between them.",
                "Im Tab „GRAFCET“ lassen sich sequenzielle, schrittweise ablaufende Prozesse entwerfen: Das Diagramm besteht aus Schritten und den dazwischenliegenden, bedingten Transitionen.",
                "En la pestaña «GRAFCET» pueden diseñarse procesos secuenciales, paso a paso: el diagrama se compone de Pasos y las Transiciones condicionales entre ellos.",
                "L'onglet « GRAFCET » permet de concevoir des processus séquentiels, étape par étape : le diagramme se compose d'Étapes et des Transitions conditionnelles qui les relient.",
                "Na karcie „GRAFCET” można projektować procesy sekwencyjne, krok po kroku: diagram składa się z Kroków i łączących je warunkowych Przejść.");
        ford("\"✎ GRAFCET szerkesztés\" módban helyezze el a lépéseket és átmeneteket, majd kösse össze őket; minden lépéshez tetszőleges számú akció (Folyamatos, Set, Reset) rendelhető.",
                "In \"✎ Edit GRAFCET\" mode, place the steps and transitions and then connect them; any number of actions (Continuous, Set, Reset) can be assigned to each step.",
                "Platzieren Sie im Modus „✎ GRAFCET bearbeiten“ die Schritte und Transitionen und verbinden Sie sie; jedem Schritt können beliebig viele Aktionen (Kontinuierlich, Set, Reset) zugeordnet werden.",
                "En el modo «✎ Editar GRAFCET», coloque los pasos y las transiciones y conéctelos; a cada paso puede asignarse cualquier número de acciones (Continua, Set, Reset).",
                "En mode « ✎ Modifier GRAFCET », placez les étapes et les transitions puis reliez-les ; un nombre quelconque d'actions (Continue, Set, Reset) peut être affecté à chaque étape.",
                "W trybie „✎ Edycja GRAFCET” umieść kroki i przejścia, a następnie je połącz; do każdego kroku można przypisać dowolną liczbę akcji (Ciągła, Set, Reset).");
        ford("A \"Létra generálása...\" gombbal a megtervezett szekvencia automatikusan valódi, futtatható létradiagram-hálózatokká alakítható.",
                "The \"Generate ladder...\" button automatically converts the designed sequence into real, runnable ladder diagram networks.",
                "Mit der Schaltfläche „Kontaktplan generieren...“ wird die entworfene Sequenz automatisch in echte, lauffähige Kontaktplan-Netzwerke umgewandelt.",
                "El botón «Generar diagrama de contactos...» convierte automáticamente la secuencia diseñada en redes de diagrama de contactos reales y ejecutables.",
                "Le bouton « Générer le schéma à contacts... » convertit automatiquement la séquence conçue en réseaux de schéma à contacts réels et exécutables.",
                "Przycisk „Generuj drabinkę...” automatycznie przekształca zaprojektowaną sekwencję w rzeczywiste, uruchamialne sieci schematu drabinkowego.");
        ford("A Szerkesztés menü \"Óraimpulzusok (clock bitek) szerkesztése...\" pontjával tetszőleges frekvenciájú, 50%-os kitöltésű villogó belső relé (M) bitek hozhatók létre, villogtatáshoz és ütemezéshez.",
                "The Edit menu's \"Edit clock pulses (clock bits)...\" item lets you create internal relay (M) bits that flash at any frequency with a 50% duty cycle, for flashing lamps and timing.",
                "Mit dem Menüpunkt „Taktimpulse (Clock-Bits) bearbeiten...“ im Menü Bearbeiten lassen sich interne Relais-Bits (M) erzeugen, die mit beliebiger Frequenz und 50 % Tastverhältnis blinken – für Blinklampen und Taktgebung.",
                "Con la opción «Editar impulsos de reloj (bits de reloj)...» del menú Editar pueden crearse bits de relé interno (M) que parpadean a cualquier frecuencia con un ciclo de trabajo del 50%, para lámparas intermitentes y temporización.",
                "L'option « Modifier les impulsions d'horloge (bits d'horloge)... » du menu Édition permet de créer des bits de relais internes (M) clignotant à n'importe quelle fréquence avec un rapport cyclique de 50 %, pour des voyants clignotants ou une base de temps.",
                "Pozycja „Edytuj impulsy zegarowe (bity zegarowe)...” w menu Edycja pozwala utworzyć bity przekaźników wewnętrznych (M), które migają z dowolną częstotliwością przy wypełnieniu 50% - do migających lampek i taktowania.");
        ford("Ügyeljen rá, hogy a kiválasztott M-bitet más tekercs, Set/Reset vagy FBD Kimenet blokk ne írja, különben a bit nem fog látszólag villogni.",
                "Make sure the selected M bit is not also written by another coil, Set/Reset, or FBD Output block, otherwise it will appear not to flash.",
                "Achten Sie darauf, dass das gewählte M-Bit nicht auch von einer anderen Spule, einem Set/Reset oder einem FBD-Ausgangsblock beschrieben wird, sonst scheint es nicht zu blinken.",
                "Procure que el bit M seleccionado no sea escrito también por otra bobina, Set/Reset o bloque de Salida FBD, de lo contrario parecerá que no parpadea.",
                "Veillez à ce que le bit M choisi ne soit pas également écrit par une autre bobine, un Set/Reset ou un bloc Sortie FBD, sinon il semblera ne pas clignoter.",
                "Upewnij się, że wybranego bitu M nie zapisuje również inna cewka, Set/Reset ani blok Wyjście FBD, w przeciwnym razie będzie się wydawać, że nie miga.");
        ford("A Fájl → Minta programok almenüben kész, azonnal betölthető demonstrációs programok találhatók, köztük a clock bitek használatát bemutató \"Villogó lámpa\" és \"Futófény\" minták.",
                "The File → Sample programs submenu contains ready-made, instantly loadable demonstration programs, including the \"Villogó lámpa\" (flashing lamp) and \"Futófény\" (chaser light) samples that demonstrate clock bits.",
                "Das Untermenü Datei → Beispielprogramme enthält fertige, sofort ladbare Demonstrationsprogramme, darunter die Beispiele „Villogó lámpa“ (Blinklampe) und „Futófény“ (Lauflicht), die den Einsatz von Taktimpulsen zeigen.",
                "El submenú Archivo → Programas de ejemplo contiene programas de demostración listos para cargar al instante, incluidos los ejemplos «Villogó lámpa» (lámpara intermitente) y «Futófény» (luz secuencial), que muestran el uso de los bits de reloj.",
                "Le sous-menu Fichier → Programmes d'exemple contient des programmes de démonstration prêts à charger instantanément, dont les exemples « Villogó lámpa » (lampe clignotante) et « Futófény » (chenillard lumineux), qui illustrent l'utilisation des bits d'horloge.",
                "Podmenu Plik → Programy przykładowe zawiera gotowe, natychmiast wczytywane programy demonstracyjne, w tym przykłady „Villogó lámpa” (migająca lampka) i „Futófény” (światło biegnące), pokazujące wykorzystanie bitów zegarowych.");
        ford("A Fájl menü \"Nyomtatás / PDF export...\" pontjával a létradiagram, a HMI, az FBD vagy a GRAFCET nyomtatható ki; PDF-be mentéshez válassza a \"Microsoft Print to PDF\" nyomtatót.",
                "The File menu's \"Print / PDF export...\" item lets you print the ladder diagram, the HMI, the FBD, or the GRAFCET; to save as PDF, choose the \"Microsoft Print to PDF\" printer.",
                "Mit dem Menüpunkt „Drucken / PDF-Export...“ im Menü Datei können der Kontaktplan, das HMI, das FBD oder das GRAFCET gedruckt werden; wählen Sie zum Speichern als PDF den Drucker „Microsoft Print to PDF“.",
                "Con la opción «Imprimir / exportar a PDF...» del menú Archivo pueden imprimirse el diagrama de contactos, el HMI, el FBD o el GRAFCET; para guardar como PDF, elija la impresora «Microsoft Print to PDF».",
                "L'option « Imprimer / exporter en PDF... » du menu Fichier permet d'imprimer le schéma à contacts, le HMI, le FBD ou le GRAFCET ; pour enregistrer en PDF, choisissez l'imprimante « Microsoft Print to PDF ».",
                "Pozycja „Drukuj / eksport do PDF...” w menu Plik umożliwia wydrukowanie schematu drabinkowego, HMI, FBD lub GRAFCET; aby zapisać jako PDF, wybierz drukarkę „Microsoft Print to PDF”.");
        ford("HMI felület", "HMI interface", "HMI-Oberfläche", "Interfaz HMI", "Interface HMI", "Interfejs HMI");
        ford("Óraimpulzusok (clock bitek)", "Clock pulses (clock bits)", "Taktimpulse (Clock-Bits)", "Impulsos de reloj (bits de reloj)", "Impulsions d'horloge (bits d'horloge)", "Impulsy zegarowe (bity zegarowe)");
        ford("Nyomtatás és dokumentáció", "Printing and documentation", "Drucken und Dokumentation", "Impresión y documentación", "Impression et documentation", "Drukowanie i dokumentacja");
        ford("A \"Projekt dokumentáció nyomtatása...\" egy gombnyomással teljes, több lapos projektdokumentációt generál.",
                "\"Print project documentation...\" generates a complete, multi-page project documentation with a single click.",
                "„Projektdokumentation drucken...“ erzeugt mit einem Klick eine vollständige, mehrseitige Projektdokumentation.",
                "«Imprimir documentación del proyecto...» genera con un solo clic una documentación del proyecto completa y de varias páginas.",
                "« Imprimer la documentation du projet... » génère en un clic une documentation de projet complète, sur plusieurs pages.",
                "„Drukuj dokumentację projektu...” jednym kliknięciem generuje pełną, wielostronicową dokumentację projektu.");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new PlcSzimulator().setVisible(true);
        });
    }

    // ================================================================ operandusok

    enum OpTipus { I, Q, M, TDN, CDN }

    static class Operandus implements Serializable {
        OpTipus tipus;
        int index;

        Operandus(OpTipus tipus, int index) { this.tipus = tipus; this.index = index; }

        String cim() {
            switch (tipus) {
                case I: return "I" + (index / 8) + "." + (index % 8);
                case Q: return "Q" + (index / 8) + "." + (index % 8);
                case M: return "M" + (index / 8) + "." + (index % 8);
                case TDN: return "T" + index + ".DN";
                case CDN: return "C" + index + ".DN";
            }
            return "?";
        }

        boolean olvas(PlcAllapot a) {
            switch (tipus) {
                case I: return a.i[index];
                case Q: return a.q[index];
                case M: return a.m[index];
                case TDN: return a.t[index].kesz;
                case CDN: return a.c[index].kesz;
            }
            return false;
        }

        static int maxIndex(OpTipus t) { return (t == OpTipus.TDN || t == OpTipus.CDN) ? 7 : 15; }
    }

    // ================================================================ érintkezők

    static class Erintkezo implements Serializable {
        boolean no = true;      // true = záró (NO), false = nyitó (NC)
        Operandus op = new Operandus(OpTipus.I, 0);

        boolean zart(PlcAllapot a) {
            boolean b = op.olvas(a);
            return no ? b : !b;
        }

        String szoveg() { return op.cim(); }
    }

    // ================================================================ kimenet

    enum KimTipus { TEKERCS, SET, RESET, TON, CTU }

    static class Kimenet implements Serializable {
        KimTipus tipus = KimTipus.TEKERCS;
        Operandus cel = new Operandus(OpTipus.Q, 0);   // TEKERCS / SET / RESET esetén
        boolean negalt = false;  // TEKERCS esetén: Q/M = NEM(feltétel)
        int idx = 0;             // TON -> időzítő index, CTU -> számláló index
        long presetMs = 2000;    // TON
        int presetSzamlalo = 5;  // CTU

        String szoveg() {
            switch (tipus) {
                case TEKERCS: return cel.cim() + (negalt ? "  /" : "");
                case SET: return cel.cim() + "  S";
                case RESET: return cel.cim() + "  R";
                case TON: return "TON T" + idx;
                case CTU: return "CTU C" + idx;
            }
            return "?";
        }
    }

    // ================================================================ hálózat (rung)

    static class Sor implements Serializable {
        List<Erintkezo> elotag = new ArrayList<>();
        List<List<Erintkezo>> agak = new ArrayList<>();   // párhuzamos ágak (0 vagy >=2)
        List<Erintkezo> utotag = new ArrayList<>();
        Kimenet kimenet = new Kimenet();
        String megjegyzes = "";

        boolean kiertekel(PlcAllapot a) {
            for (Erintkezo e : elotag) if (!e.zart(a)) return false;
            if (!agak.isEmpty()) {
                boolean barmelyik = false;
                for (List<Erintkezo> ag : agak) {
                    boolean v = true;
                    for (Erintkezo e : ag) if (!e.zart(a)) { v = false; break; }
                    if (v) { barmelyik = true; break; }
                }
                if (!barmelyik) return false;
            }
            for (Erintkezo e : utotag) if (!e.zart(a)) return false;
            return true;
        }
    }

    // ================================================================ gépi kód (IL / STL) fordító

    enum KodMuvelet { KONTAKT, NYIT, ZAR, ST, S, R, TON, CTU, IGAZ, NEGAL }

    static class KodSor {
        KodMuvelet muvelet;
        boolean vagy;   // VAGY (OR) kapcsolódás – csak KONTAKT / NYIT esetén értelmezett
        boolean elso;   // ez az első elem/nyitás az adott (al)kontextusban -> LD / U lesz, nem AND / OR
        boolean neg;    // negált kontaktus (NC)
        Operandus op;   // KONTAKT, ST, S, R esetén
        long presetMs;  // TON
        int presetSzamlalo; // CTU
        int idx;        // TON/CTU T/C index
    }

    static KodSor ujKontakt(boolean vagy, boolean elso, Erintkezo e) {
        KodSor k = new KodSor();
        k.muvelet = KodMuvelet.KONTAKT;
        k.vagy = vagy; k.elso = elso; k.neg = !e.no; k.op = e.op;
        return k;
    }

    static KodSor ujNyit(boolean vagy, boolean elso) {
        KodSor k = new KodSor();
        k.muvelet = KodMuvelet.NYIT;
        k.vagy = vagy; k.elso = elso;
        return k;
    }

    static KodSor ujZar() { KodSor k = new KodSor(); k.muvelet = KodMuvelet.ZAR; return k; }

    /**
     * Egy hálózat (Sor) lefordítása egy egyszerű, dialektus-független "lépés" listára.
     * Zárójelet (NYIT/ZAR) csak ott tesz, ahol az a logika helyes kiértékeléséhez
     * valóban szükséges: egy VAGY-ágak csoportja csak akkor kap keretet, ha előtte
     * már volt egy védendő ÉS-lánc (elotag); egy önálló ág csak akkor kap saját
     * zárójelet, ha nem az első ág ÉS egynél több kontaktusból áll (egyébként a
     * szekvenciális ÉS/VAGY kiértékelés zárójel nélkül is helyes eredményt ad).
     */
    static List<KodSor> forditSor(Sor sor) {
        List<KodSor> ki = new ArrayList<>();
        boolean volt = false; // volt már korábbi utasítás -> a következő ÉS-sel kapcsolódik, nem LD/U "frissen"

        for (Erintkezo e : sor.elotag) {
            ki.add(ujKontakt(false, !volt, e));
            volt = true;
        }

        if (!sor.agak.isEmpty()) {
            boolean kellKeret = volt;
            if (kellKeret) ki.add(ujNyit(false, false));

            for (int bi = 0; bi < sor.agak.size(); bi++) {
                List<Erintkezo> ag = sor.agak.get(bi);
                boolean elsoAg = (bi == 0);
                if (ag.isEmpty()) {
                    KodSor igaz = new KodSor();
                    igaz.muvelet = KodMuvelet.IGAZ;
                    igaz.vagy = !elsoAg;
                    igaz.elso = elsoAg;
                    ki.add(igaz);
                } else if (elsoAg || ag.size() == 1) {
                    boolean agElso = true;
                    for (Erintkezo e : ag) {
                        ki.add(ujKontakt(!elsoAg, elsoAg && agElso, e));
                        agElso = false;
                    }
                } else {
                    ki.add(ujNyit(true, false));
                    boolean agElso = true;
                    for (Erintkezo e : ag) {
                        ki.add(ujKontakt(false, agElso, e));
                        agElso = false;
                    }
                    ki.add(ujZar());
                }
            }
            if (kellKeret) ki.add(ujZar());
            volt = true;
        }

        for (Erintkezo e : sor.utotag) {
            ki.add(ujKontakt(false, !volt, e));
            volt = true;
        }

        if (!volt) {
            KodSor igaz = new KodSor();
            igaz.muvelet = KodMuvelet.IGAZ;
            igaz.elso = true;
            ki.add(igaz);
        }

        Kimenet k = sor.kimenet;
        if (k.tipus == KimTipus.TEKERCS && k.negalt) {
            KodSor negal = new KodSor();
            negal.muvelet = KodMuvelet.NEGAL;
            ki.add(negal);
        }
        KodSor out = new KodSor();
        switch (k.tipus) {
            case TEKERCS: out.muvelet = KodMuvelet.ST; out.op = k.cel; break;
            case SET: out.muvelet = KodMuvelet.S; out.op = k.cel; break;
            case RESET: out.muvelet = KodMuvelet.R; out.op = k.cel; break;
            case TON: out.muvelet = KodMuvelet.TON; out.idx = k.idx; out.presetMs = k.presetMs; break;
            case CTU: out.muvelet = KodMuvelet.CTU; out.idx = k.idx; out.presetSzamlalo = k.presetSzamlalo; break;
        }
        ki.add(out);
        return ki;
    }

    // ================================================================ futásidejű állapot

    static class Idozito { long presetMs; long akkMs; boolean kesz; }
    static class Szamlalo { int preset; int ertek; boolean kesz; boolean elozo; }

    static class PlcAllapot {
        boolean[] i = new boolean[16];
        boolean[] q = new boolean[16];
        boolean[] m = new boolean[16];
        Idozito[] t = new Idozito[8];
        Szamlalo[] c = new Szamlalo[8];

        PlcAllapot() {
            for (int k = 0; k < 8; k++) { t[k] = new Idozito(); c[k] = new Szamlalo(); c[k].preset = 5; }
        }

        void nullaz() {
            Arrays.fill(q, false);
            Arrays.fill(m, false);
            for (Idozito x : t) { x.akkMs = 0; x.kesz = false; }
            for (Szamlalo x : c) { x.ertek = 0; x.kesz = false; x.elozo = false; }
        }
    }

    // ================================================================ HMI widgetek

    enum HmiTipus { LAMPA, GOMB, KIJELZO, FELIRAT, TEGLALAP, TREND, ZEBRA, AJTO, SZALLITOSZALAG, SZENZOR, ROBOT_FANUC, ROBOT_KUKA }
    static final int HMI_TREND_HOSSZ = 120;
    enum GombMod { PILLANAT, VALTO }

    static class HmiWidget implements Serializable {
        HmiTipus tipus = HmiTipus.LAMPA;
        int x = 40, y = 40, w = 70, h = 70;
        Operandus op = new Operandus(OpTipus.I, 0);
        boolean szamkent = false;      // KIJELZO: T/C aktuális értékét mutatja a DN/kész bit helyett
        GombMod gombMod = GombMod.VALTO;
        String felirat = tr("Címke");
        Color szinBe = new Color(0, 200, 0);
        Color szinKi = new Color(70, 70, 70);
        int fontMeret = 12;
        int forgatas = 0;          // SZENZOR: elforgatás foka (0 / 90 / 180 / 270)
        boolean tukrozve = false;  // ROBOT_FANUC / ROBOT_KUKA: függőleges tengelyre tükrözve (ellentétes irányban nyúl ki)
        transient Deque<Double> trendTortenet;
        transient double ajtoNyitottsag = 0;   // AJTO: 0 = zárva, 1 = nyitva (animált átmenet)
        transient double szalagFazis = 0;      // SZALLITOSZALAG: futó mintázat eltolási fázisa
        transient double szenzorPulzus = 0;    // SZENZOR: aktív érzékelés pulzáló jelzésének fázisa
        transient double robotFazis = 0;       // ROBOT_FANUC / ROBOT_KUKA: folyamatos pick&place mozgás fázisa
    }

    // ================================================================ funkcióblokk-diagram (FBD)

    enum FbdTipus { BEMENET, KIMENET, AND, OR, NOT, XOR, SR, TON, CTU, R_TRIG, F_TRIG }

    static class FbdBlock implements Serializable {
        int id;
        FbdTipus tipus = FbdTipus.AND;
        int x = 60, y = 60, w = 110, h = 70;
        int bemenetSzam = 2;
        int[] forrasId = {-1, -1};     // minden bemenethez a forrás blokk id-ja, vagy -1 ha nincs kötve
        Operandus op = new Operandus(OpTipus.I, 0); // BEMENET/KIMENET esetén melyik bitet olvassa/írja
        int idx = 0;                   // TON -> T index, CTU -> C index
        long presetMs = 2000;          // TON
        int presetSzamlalo = 5;        // CTU
        String felirat = "";           // szabad megjegyzés a blokkhoz

        // futásidejű (nem mentett) állapot
        transient boolean kimenetErtek = false;
        transient boolean elozoClk = false;
    }

    /** A FBD típushoz tartozó alapértelmezett/fix bemenetszám (AND/OR esetén ez csak kezdőérték, szabadon módosítható). */
    static int fbdAlapBemenetSzam(FbdTipus t) {
        switch (t) {
            case BEMENET: return 0;
            case KIMENET: return 1;
            case NOT: return 1;
            case XOR: return 2;
            case SR: return 2;
            case TON: return 1;
            case CTU: return 2;
            case R_TRIG: return 1;
            case F_TRIG: return 1;
            case AND: case OR: return 2;
        }
        return 2;
    }

    static boolean fbdVanKimenetPin(FbdTipus t) { return t != FbdTipus.KIMENET; }

    static String[] fbdBemenetFeliratok(FbdTipus t) {
        switch (t) {
            case SR: return new String[]{"S", "R"};
            case TON: return new String[]{"IN"};
            case CTU: return new String[]{"CU", "R"};
            case R_TRIG: case F_TRIG: return new String[]{"CLK"};
            default: return null;
        }
    }

    // ================================================================ GRAFCET (szekvenciális függvénykarton)

    enum GrafcetAkcioTipus { FOLYAMATOS, SET, RESET }

    static class GrafcetAkcio implements Serializable {
        GrafcetAkcioTipus tipus = GrafcetAkcioTipus.FOLYAMATOS;
        Operandus cel = new Operandus(OpTipus.Q, 0);
    }

    static class GrafcetLepes implements Serializable {
        int id;
        int x = 60, y = 60;
        int w = 56, h = 40;
        boolean kezdeti = false;
        String felirat = "";
        List<GrafcetAkcio> akciok = new ArrayList<>();
        transient boolean aktiv = false;
    }

    static class GrafcetAtmenet implements Serializable {
        int id;
        int x = 60, y = 160;
        int w = 54, h = 10;
        List<Integer> forrasLepesek = new ArrayList<>();
        List<Integer> celLepesek = new ArrayList<>();
        List<Erintkezo> feltetel = new ArrayList<>();
    }

    static class GrafcetDiagram implements Serializable {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        int kovetkezoLepesId = 1;
        int kovetkezoAtmenetId = 1;
    }

    // ================================================================ óraimpulzus (clock bit) generátorok

    static class OraImpulzus implements Serializable {
        int mIndex = 0;
        double frekvenciaHz = 1.0;
        transient long akkMs = 0;
    }

    // ================================================================ HMI képernyők (lapok)

    static class HmiKepernyo implements Serializable {
        String nev = tr("Képernyő") + " 1";
        List<HmiWidget> widgetek = new ArrayList<>();
    }

    static List<HmiKepernyo> ujHmiKepernyoLista() {
        List<HmiKepernyo> l = new ArrayList<>();
        l.add(new HmiKepernyo());
        return l;
    }

    // ================================================================ mentett projekt (program + címkék + HMI + FBD)

    static class ProjektAdat implements Serializable {
        List<Sor> program;
        Map<String, String> cimkeNev;
        Map<String, String> cimkeMegj;
        List<HmiWidget> hmiWidgetek;        // régi (egy képernyős) formátum – visszafelé kompatibilitáshoz
        List<HmiKepernyo> hmiKepernyok;     // új (több képernyős) formátum
        List<FbdBlock> fbdBlokkok;
        int fbdKovetkezoId;
        GrafcetDiagram grafcet;
        List<Integer> grafcetKezdetiMIndexek;
        List<OraImpulzus> oraImpulzusok;
    }

    // ================================================================ megjelenítési állandók

    static final Color ENERGIZALT = new Color(0, 0, 255 );
    static final Color SEMLEGES = new Color(100, 100, 100);
    static final Color HATTER = Color.WHITE;
    static final Color KIVALASZTVA = new Color(225, 240, 240);
    static final Color DOBOZ_HATTER_BE = new Color(190, 255, 0);
    static final Color DOBOZ_HATTER_KI = new Color(240, 240, 240);
    static final Font CIM_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
    static final Font HALO_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 11);

    static final int COL_W = 100;
    static final int OUT_COL_W = 150;
    static final int ROW_H = 52;
    static final int RAIL_MARGIN = 34;
    static final int NETWORK_LABEL_H = 20;
    static final int RUNG_GAP = 14;

    // ================================================================ állapot mezők

    List<Sor> program = uj_demoProgram();
    PlcAllapot allapot = new PlcAllapot();
    javax.swing.Timer scanTimer;
    javax.swing.Timer hmiAnimTimer;
    boolean fut = false;
    int ciklusIdoMs = 150;
    int scanSzamlalo = 0;
    Sor kivalasztottSor = null;
    File aktualisFajl = null;
    boolean demoProgram = true;   // igaz, amíg a program még a kezdeti demó-hálózatokat tartalmazza (szerkesztetlenül)
    String aktivMintaNev = null;  // melyik beépített minta program van betöltve (null = az alap demó)
    Sor vagolapHalozat = null;
    Erintkezo vagolapErintkezo = null;
    double nagyitas = 1.0;

    Point logikaiPont(MouseEvent e) {
        return new Point((int) Math.round(e.getX() / nagyitas), (int) Math.round(e.getY() / nagyitas));
    }

    Map<String, String> cimkeNev = new LinkedHashMap<>();
    Map<String, String> cimkeMegj = new LinkedHashMap<>();
    List<HmiKepernyo> hmiKepernyok = mintaHmiEpit(null);
    int hmiAktivKepernyo = 0;
    List<HmiWidget> hmiWidgetek = hmiKepernyok.get(0).widgetek;
    List<FbdBlock> fbdBlokkok = mintaFbdEpit(null);
    int fbdKovetkezoId = fbdKovetkezoIdSzamol(fbdBlokkok);
    GrafcetDiagram grafcet = mintaGrafcetEpit(null);
    List<Integer> grafcetKezdetiMIndexek = new ArrayList<>();
    List<OraImpulzus> oraImpulzusok = new ArrayList<>();

    LetraPanel letra;
    HmiPanel hmi;
    FbdPanel fbd;
    GrafcetPanel grafcetPanel;
    JToggleButton hmiSzerkesztesGomb;
    JComboBox<String> hmiEszkozCombo;
    JComboBox<String> hmiKepernyoCombo;
    JToggleButton fbdSzerkesztesGomb;
    JComboBox<String> fbdEszkozCombo;
    JToggleButton grafcetSzerkesztesGomb;
    JComboBox<String> grafcetEszkozCombo;

    JTabbedPane fulek;
    JPanel hmiLap;
    JFrame hmiAblak;
    boolean hmiLevalasztva = false;
    int hmiFulIndex = -1;
    JToggleButton hmiLevalasztGomb;
    JToggleButton[] beGombok = new JToggleButton[16];
    JLabel[] kiLedek = new JLabel[16];
    JLabel[] mLedek = new JLabel[16];
    JLabel[] idozitoLabel = new JLabel[8];
    JLabel[] szamlaloLabel = new JLabel[8];
    JLabel statusz;
    JButton indBtn, leaBtn, lepesBtn, nullazBtn;
    JScrollPane letraScroll;

    PlcSzimulator() {
        super(PROGRAM);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setJMenuBar(epitMenu());

        letra = new LetraPanel();
        JScrollPane scroll = new JScrollPane(letra);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        letraScroll = scroll;

        hmi = new HmiPanel();
        fbd = new FbdPanel();
        grafcetPanel = new GrafcetPanel();
        hmiLap = epitHmiLap();

        fulek = new JTabbedPane();
        fulek.addTab(tr("Létradiagram"), scroll);
        fulek.addTab("HMI", hmiLap);
        fulek.addTab(tr("Funkcióblokk (FBD)"), epitFbdLap());
        fulek.addTab("GRAFCET", epitGrafcetLap());

        JPanel felsoSav = new JPanel();
        felsoSav.setLayout(new BoxLayout(felsoSav, BoxLayout.Y_AXIS));
        felsoSav.add(epitNyelvValto());
        felsoSav.add(epitEszkoztar());
        add(felsoSav, BorderLayout.NORTH);
        add(fulek, BorderLayout.CENTER);
        add(epitIOPanel(), BorderLayout.EAST);
        statusz = new JLabel(" " + tr("Készenlét."));
        statusz.setBorder(new EmptyBorder(3, 8, 3, 8));
        add(statusz, BorderLayout.SOUTH);

        scanTimer = new javax.swing.Timer(ciklusIdoMs, e -> pordulEgyCiklus());
        hmiAnimTimer = new javax.swing.Timer(40, e -> hmiAnimLepes());
        hmiAnimTimer.start();

        setSize(1420, 880);
        setLocationRelativeTo(null);
        grafcetNullaz();
        frissitIOPanel();
        frissitUndoAllapot();
        bindUndoRedoGyorsgombok(getRootPane());
    }

    // ================================================================ nyelvválasztó zászlók

    static void rajzolZaszlo(Graphics2D g, String kod, int w, int h) {
        switch (kod) {
            case "hu":
                g.setColor(new Color(206, 42, 55)); g.fillRect(0, 0, w, h / 3);
                g.setColor(Color.WHITE); g.fillRect(0, h / 3, w, h / 3);
                g.setColor(new Color(67, 111, 72)); g.fillRect(0, 2 * h / 3, w, h - 2 * h / 3);
                break;
            case "de":
                g.setColor(Color.BLACK); g.fillRect(0, 0, w, h / 3);
                g.setColor(new Color(221, 0, 0)); g.fillRect(0, h / 3, w, h / 3);
                g.setColor(new Color(255, 206, 0)); g.fillRect(0, 2 * h / 3, w, h - 2 * h / 3);
                break;
            case "es":
                g.setColor(new Color(170, 21, 27)); g.fillRect(0, 0, w, h / 4);
                g.setColor(new Color(241, 191, 0)); g.fillRect(0, h / 4, w, h / 2);
                g.setColor(new Color(170, 21, 27)); g.fillRect(0, 3 * h / 4, w, h - 3 * h / 4);
                break;
            case "fr":
                g.setColor(new Color(0, 85, 164)); g.fillRect(0, 0, w / 3, h);
                g.setColor(Color.WHITE); g.fillRect(w / 3, 0, w / 3, h);
                g.setColor(new Color(239, 65, 53)); g.fillRect(2 * w / 3, 0, w - 2 * w / 3, h);
                break;
            case "pl":
                g.setColor(Color.WHITE); g.fillRect(0, 0, w, h / 2);
                g.setColor(new Color(220, 20, 60)); g.fillRect(0, h / 2, w, h - h / 2);
                break;
            case "en":
                rajzolUnionJack(g, w, h);
                break;
        }
    }

    static void rajzolUnionJack(Graphics2D g, int w, int h) {
        g.setColor(new Color(1, 33, 105));
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(Math.max(2f, h / 6f)));
        g.setColor(Color.WHITE);
        g.drawLine(0, 0, w, h);
        g.drawLine(w, 0, 0, h);
        g.setStroke(new BasicStroke(Math.max(1f, h / 10f)));
        g.setColor(new Color(200, 16, 46));
        g.drawLine(0, 0, w, h);
        g.drawLine(w, 0, 0, h);
        int vastagFeher = Math.max(2, h / 6);
        int vastagPiros = Math.max(1, h / 10);
        g.setColor(Color.WHITE);
        g.fillRect(0, h / 2 - vastagFeher / 2, w, vastagFeher);
        g.fillRect(w / 2 - Math.max(2, w / 6) / 2, 0, Math.max(2, w / 6), h);
        g.setColor(new Color(200, 16, 46));
        g.fillRect(0, h / 2 - vastagPiros / 2, w, vastagPiros);
        g.fillRect(w / 2 - Math.max(1, w / 10) / 2, 0, Math.max(1, w / 10), h);
    }

    static class ZaszloIcon implements Icon {
        String kod;
        int w, h;
        ZaszloIcon(String kod, int w, int h) { this.kod = kod; this.w = w; this.h = h; }
        public int getIconWidth() { return w; }
        public int getIconHeight() { return h; }
        public void paintIcon(Component c, Graphics g0, int x, int y) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(x, y);
            rajzolZaszlo(g, kod, w, h);
            g.setColor(new Color(90, 90, 90));
            g.drawRect(0, 0, w - 1, h - 1);
            g.dispose();
        }
    }

    static final String[][] NYELVEK = {{"hu", "Magyar"}, {"en", "Angol"}, {"de", "Német"}, {"es", "Spanyol"}, {"fr", "Francia"}, {"pl", "Lengyel"}};

    JToolBar epitNyelvValto() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);
        tb.add(new JLabel(tr("Nyelv") + ":  "));
        for (String[] ny : NYELVEK) {
            JButton b = new JButton(new ZaszloIcon(ny[0], 30, 20));
            b.setToolTipText(tr(ny[1]));
            b.setMargin(new Insets(3, 3, 3, 3));
            b.setFocusPainted(false);
            if (ny[0].equals(nyelv)) {
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(30, 120, 220), 2),
                        BorderFactory.createEmptyBorder(1, 1, 1, 1)));
            }
            String kod = ny[0];
            b.addActionListener(e -> nyelvValtas(kod));
            tb.add(b);
        }
        return tb;
    }

    // ================================================================ menü

    JMenuBar epitMenu() {
        JMenuBar mb = new JMenuBar();

        JMenu fajl = new JMenu(tr("Fájl"));
        fajl.add(menuElem(tr("Új program"), e -> ujProgram()));
        fajl.add(menuElem(tr("Megnyitás..."), e -> megnyit()));
        fajl.add(menuElem(tr("Mentés"), e -> mentes(false)));
        fajl.add(menuElem(tr("Mentés másként..."), e -> mentes(true)));
        fajl.addSeparator();
        JMenu minta = new JMenu(tr("Minta programok"));
        minta.add(menuElem(tr("Motorindítás (alapértelmezett demó)"), e -> betoltMintaProgram(null, tr("Motorindítás (alapértelmezett demó)"))));
        minta.add(menuElem(tr("Csillag-delta motorindítás"), e -> betoltMintaProgram("CSILLAGDELTA", tr("Csillag-delta motorindítás"))));
        minta.add(menuElem(tr("Szállítószalag sorrendindítás"), e -> betoltMintaProgram("SZALLITOSZALAG", tr("Szállítószalag sorrendindítás"))));
        minta.add(menuElem(tr("Robotcella (motorblokk be- és kirakás)"), e -> betoltMintaProgram("ROBOTCELLA", tr("Robotcella (motorblokk be- és kirakás)"))));
        minta.add(menuElem(tr("Közlekedési lámpa (állapotgép)"), e -> betoltMintaProgram("KOZLEKEDESILAMPA", tr("Közlekedési lámpa (állapotgép)"))));
        minta.add(menuElem(tr("Lift (2 szintes)"), e -> betoltMintaProgram("LIFT", tr("Lift (2 szintes)"))));
        minta.add(menuElem(tr("Útkereszteződés gyalogátkelővel"), e -> betoltMintaProgram("UTKERESZTEZODES", tr("Útkereszteződés gyalogátkelővel"))));
        minta.add(menuElem(tr("Villogó lámpa (óraimpulzussal)"), e -> betoltMintaProgram("VILLOGOLAMPA", tr("Villogó lámpa (óraimpulzussal)"))));
        minta.add(menuElem(tr("Futófény (óraimpulzussal)"), e -> betoltMintaProgram("FUTOFENY", tr("Futófény (óraimpulzussal)"))));
        fajl.add(minta);
        fajl.addSeparator();
        fajl.add(menuElem(tr("Nyomtatás / PDF export..."), e -> nyomtatasAblak()));
        fajl.add(menuElem(tr("Projekt dokumentáció nyomtatása..."), e -> dokumentacioNyomtatas()));
        fajl.addSeparator();
        fajl.add(menuElem(tr("Kilépés"), e -> System.exit(0)));
        mb.add(fajl);

        JMenu szerk = new JMenu(tr("Szerkesztés"));
        JMenuItem visszavonItem = new JMenuItem(visszavonAction);
        visszavonItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        JMenuItem ismetItem = new JMenuItem(ismetAction);
        ismetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        szerk.add(visszavonItem);
        szerk.add(ismetItem);
        szerk.addSeparator();
        szerk.add(menuElem(tr("Címkék (tag-ek) szerkesztése..."), e -> cimkeSzerkeszto()));
        szerk.add(menuElem(tr("Óraimpulzusok (clock bitek) szerkesztése..."), e -> oraImpulzusSzerkeszto()));
        mb.add(szerk);

        JMenu kod = new JMenu(tr("Gépi kód"));
        kod.add(menuElem(tr("Gépi kód megtekintése..."), e -> kodExportAblak()));
        mb.add(kod);

        JMenu ellenorzes = new JMenu(tr("Ellenőrzés"));
        ellenorzes.add(menuElem(tr("Program ellenőrzése..."), e -> ellenorzesAblak()));
        ellenorzes.add(menuElem(tr("Hivatkozások keresése..."), e -> kereszthivatkozasAblak()));
        mb.add(ellenorzes);

        JMenu sugo = new JMenu(tr("Súgó"));
        sugo.add(menuElem(tr("Használat"), e -> sugoAblak()));
        sugo.addSeparator();
        sugo.add(menuElem(tr("Névjegy"), e -> nevjegyAblak()));
        mb.add(sugo);
        return mb;
    }

    void nyelvValtas(String ujNyelv) {
        if (ujNyelv.equals(nyelv)) return;
        if (hmiLevalasztva) hmiLevalasztasValtas();
        nyelv = ujNyelv;
        PlcSzimulator uj = new PlcSzimulator();
        if (!this.demoProgram) {
            uj.program = this.program;   // valódi szerkesztés esetén átvesszük a meglévő (már esetleg vegyes nyelvű) programot
        } else if (this.aktivMintaNev != null) {
            uj.program = mintaProgramEpit(this.aktivMintaNev);   // betöltött minta program: újraépítjük, már a célnyelven
        }   // egyébként uj már a saját, célnyelven létrehozott alap demóját tartalmazza
        uj.demoProgram = this.demoProgram;
        uj.aktivMintaNev = this.aktivMintaNev;
        uj.cimkeNev = this.cimkeNev;
        uj.cimkeMegj = this.cimkeMegj;
        if (!this.demoProgram) {
            uj.hmiKepernyok = this.hmiKepernyok;
            uj.hmiAktivKepernyo = this.hmiAktivKepernyo;
            uj.hmiWidgetek = this.hmiKepernyok.get(this.hmiAktivKepernyo).widgetek;
            uj.fbdBlokkok = this.fbdBlokkok;
            uj.fbdKovetkezoId = this.fbdKovetkezoId;
            uj.grafcet = this.grafcet;
            uj.grafcetKezdetiMIndexek = this.grafcetKezdetiMIndexek;
            uj.oraImpulzusok = this.oraImpulzusok;
        } else if (this.aktivMintaNev != null) {
            uj.hmiKepernyok = mintaHmiEpit(this.aktivMintaNev);
            uj.hmiAktivKepernyo = 0;
            uj.hmiWidgetek = uj.hmiKepernyok.get(0).widgetek;
            uj.fbdBlokkok = mintaFbdEpit(this.aktivMintaNev);
            uj.fbdKovetkezoId = fbdKovetkezoIdSzamol(uj.fbdBlokkok);
            uj.grafcet = mintaGrafcetEpit(this.aktivMintaNev);
            uj.grafcetKezdetiMIndexek = new ArrayList<>();
            uj.oraImpulzusok = mintaOraImpulzusEpit(this.aktivMintaNev);
        }   // egyébként uj már a saját, célnyelven létrehozott alap HMI/FBD/GRAFCET demóját tartalmazza
        uj.grafcetNullaz();
        uj.aktualisFajl = this.aktualisFajl;
        uj.allapot = this.allapot;
        uj.ciklusIdoMs = this.ciklusIdoMs;
        uj.scanSzamlalo = this.scanSzamlalo;
        uj.nagyitas = this.nagyitas;
        uj.hmiKepernyoComboFrissit();
        uj.letra.ujraMeret(); uj.letra.repaint();
        uj.hmi.ujraMeret(); uj.hmi.repaint();
        uj.fbd.ujraMeret(); uj.fbd.repaint();
        uj.grafcetPanel.ujraMeret(); uj.grafcetPanel.repaint();
        uj.frissitIOPanel();
        uj.setTitle(aktualisFajl != null ? (PROGRAM + " – " + aktualisFajl.getName()) : PROGRAM);
        uj.setLocation(this.getLocation());
        uj.setSize(this.getSize());
        uj.setVisible(true);
        this.leallit();
        this.hmiAnimTimer.stop();
        this.dispose();
    }

    JMenuItem menuElem(String s, ActionListener l) { JMenuItem m = new JMenuItem(s); m.addActionListener(l); return m; }

    void nevjegyAblak() {
        String html = "<html><body style='width:320px;font-family:sans-serif;font-size:12px;text-align:center'>"
                + "<h2>" + PROGRAM + "</h2>"
                + "<p>" + tr("A programot Sisak László (helmetsoft.com) készítette a PLC oktatási célú felhasználására.") + "</p>"
                + "<p><b>" + tr("INGYENES") + "</b> – " + tr("kereskedelmi forgalomba nem hozható.") + "</p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), tr("Névjegy"), JOptionPane.PLAIN_MESSAGE);
    }

    void sugoAblak() {
        String html = "<html><body style='width:620px;font-family:sans-serif;font-size:11px'>"
                + "<h3>" + tr("Létradiagram szerkesztése") + "</h3>"
                + "<ul>"
                + "<li>" + tr("Egy érintkező-helyre (szaggatott '+' négyzet) kattintva új érintkezőt adhat hozzá.") + "</li>"
                + "<li>" + tr("Meglévő érintkezőre kattintva szerkesztheti vagy törölheti azt.") + "</li>"
                + "<li>" + tr("A kimenetre (tekercs / időzítő / számláló) kattintva szerkesztheti a kimenetet.") + "</li>"
                + "<li>" + tr("A hálózat alatti szaggatott '+ ág' sorra kattintva párhuzamos ágat vehet fel.") + "</li>"
                + "<li>" + tr("Egy ág elején lévő '✕'-re kattintva törölheti az adott ágat.") + "</li>"
                + "<li>" + tr("A hálózat bal oldali sávjára kattintva kijelölheti a hálózatot (eszköztár: + Hálózat / Hálózat törlése).") + "</li>"
                + "<li>" + tr("A hálózat sávjára duplán kattintva szerkesztheti a hálózat megjegyzését (ezt új hálózat létrehozásakor is megkérdezi a program).") + "</li>"
                + "<li>" + tr("A kimenet szerkesztésénél a cél lehet Kimenet (Q) vagy Belső relé (M) is – Tekercs, Set vagy Reset típussal egyaránt (pl. M-re is lehet Set/Reset kapcsolást építeni).") + "</li>"
                + "<li>" + tr("Egy meglévő kontaktusra Ctrl+kattintással vágólapra másolhatja; egy üres '+' helyre Ctrl+kattintással beillesztheti a vágólapon lévő kontaktust.") + "</li>"
                + "<li>" + tr("Az eszköztár '▲ Fel' / '▼ Le' gombjaival mozgathatja a kijelölt hálózatot; a '⧉ Hálózat másolása' / 'Hálózat beillesztése' gombokkal egész hálózatot másolhat.") + "</li>"
                + "<li>" + tr("Ctrl+Z / Ctrl+Y (vagy az eszköztár gombjai): visszavonás / ismét – a létra, a címkék, a HMI és az FBD szerkesztésére egyaránt vonatkozik.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("HMI felület") + "</h3>"
                + "<ul>"
                + "<li>" + tr("A \"HMI\" fülön szabadon tervezhető kezelőfelület építhető: Lámpa, Gomb, Kijelző, Felirat, Téglalap, Trend, Zebra, Ajtó, Szállítószalag, Érzékelő, valamint Fanuc és Kuka robotkar elemekkel.") + "</li>"
                + "<li>" + tr("\"✎ HMI szerkesztés\" módban válassza ki a legördülőből az elhelyezendő elem típusát, majd kattintson a vászonra; duplán kattintva az elemre szerkesztheti a tulajdonságait.") + "</li>"
                + "<li>" + tr("Több HMI-képernyő is létrehozható és kezelhető a Képernyő legördülővel; a \"⧉ Külön ablakban\" gombbal a HMI önálló, mozgatható ablakba helyezhető ki.") + "</li>"
                + "<li>" + tr("Futás közben, szerkesztés mód nélkül a gombok élőben reagálnak a kattintásra, a lámpák és kijelzők pedig élőben követik a PLC logikáját.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("Szimuláció") + "</h3>"
                + "<ul>"
                + "<li>" + tr("<b>Indítás</b>: elindítja a ciklikus kiértékelést (scan-ciklus) a megadott ciklusidővel.") + "</li>"
                + "<li>" + tr("<b>Lépés</b>: leállított állapotban egyetlen scan-ciklust hajt végre.") + "</li>"
                + "<li>" + tr("<b>Nullázás</b>: törli a kimenetek, belső relék, időzítők és számlálók állapotát.") + "</li>"
                + "<li>" + tr("A bemenet gombokra (I) kattintva kapcsolhatja be/ki a bemeneteket, futás közben is.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("Funkcióblokk (FBD)") + "</h3>"
                + "<ul>"
                + "<li>" + tr("A \"Funkcióblokk (FBD)\" fülön AND/OR/NOT/XOR, SR-tároló, TON, CTU, valamint felfutó/lefutó él (P/N) blokkokból építhet logikát, a létrával és a HMI-vel közös I/Q/M/T/C memórián.") + "</li>"
                + "<li>" + tr("\"✎ FBD szerkesztés\" módban a legördülőből válassza az elhelyezendő blokk típusát, majd kattintson a vászonra.") + "</li>"
                + "<li>" + tr("Huzalozáshoz kattintson egy blokk kimeneti pontjára (jobb oldali kör), majd a célbemenetre (bal oldali kör).") + "</li>"
                + "<li>" + tr("Blokkra duplán kattintva szerkesztheti a tulajdonságait (típus, bemenetek száma, kötött cím, időzítő/számláló beállítás).") + "</li>"
                + "</ul>"
                + "<h3>GRAFCET</h3>"
                + "<ul>"
                + "<li>" + tr("A \"GRAFCET\" fülön szekvenciális, lépésről lépésre haladó folyamatok tervezhetők: Lépésekből és a köztük lévő feltételes Átmenetekből épül fel a diagram.") + "</li>"
                + "<li>" + tr("\"✎ GRAFCET szerkesztés\" módban helyezze el a lépéseket és átmeneteket, majd kösse össze őket; minden lépéshez tetszőleges számú akció (Folyamatos, Set, Reset) rendelhető.") + "</li>"
                + "<li>" + tr("A \"Létra generálása...\" gombbal a megtervezett szekvencia automatikusan valódi, futtatható létradiagram-hálózatokká alakítható.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("Óraimpulzusok (clock bitek)") + "</h3>"
                + "<ul>"
                + "<li>" + tr("A Szerkesztés menü \"Óraimpulzusok (clock bitek) szerkesztése...\" pontjával tetszőleges frekvenciájú, 50%-os kitöltésű villogó belső relé (M) bitek hozhatók létre, villogtatáshoz és ütemezéshez.") + "</li>"
                + "<li>" + tr("Ügyeljen rá, hogy a kiválasztott M-bitet más tekercs, Set/Reset vagy FBD Kimenet blokk ne írja, különben a bit nem fog látszólag villogni.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("Minta programok") + "</h3>"
                + "<ul>"
                + "<li>" + tr("A Fájl → Minta programok almenüben kész, azonnal betölthető demonstrációs programok találhatók, köztük a clock bitek használatát bemutató \"Villogó lámpa\" és \"Futófény\" minták.") + "</li>"
                + "</ul>"
                + "<h3>" + tr("Nyomtatás és dokumentáció") + "</h3>"
                + "<ul>"
                + "<li>" + tr("A Fájl menü \"Nyomtatás / PDF export...\" pontjával a létradiagram, a HMI, az FBD vagy a GRAFCET nyomtatható ki; PDF-be mentéshez válassza a \"Microsoft Print to PDF\" nyomtatót.") + "</li>"
                + "<li>" + tr("A \"Projekt dokumentáció nyomtatása...\" egy gombnyomással teljes, több lapos projektdokumentációt generál.") + "</li>"
                + "</ul></body></html>";
        JScrollPane sugoScroll = new JScrollPane(new JLabel(html));
        sugoScroll.setBorder(BorderFactory.createEmptyBorder());
        sugoScroll.getVerticalScrollBar().setUnitIncrement(16);
        sugoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JDialog dlg = new JDialog(this, tr("Súgó"), true);
        JButton okGomb = new JButton(tr("OK"));
        okGomb.addActionListener(e -> dlg.dispose());
        JPanel gombSor = new JPanel(new FlowLayout(FlowLayout.CENTER));
        gombSor.add(okGomb);

        JPanel fo = new JPanel(new BorderLayout(0, 8));
        fo.setBorder(new EmptyBorder(8, 8, 8, 8));
        fo.add(sugoScroll, BorderLayout.CENTER);
        fo.add(gombSor, BorderLayout.SOUTH);
        dlg.setContentPane(fo);

        Dimension kepernyo = Toolkit.getDefaultToolkit().getScreenSize();
        int szelesseg = Math.min(820, kepernyo.width - 80);
        int magassag = Math.min(700, kepernyo.height - 120);
        dlg.setSize(szelesseg, magassag);
        dlg.setLocationRelativeTo(this);
        dlg.getRootPane().setDefaultButton(okGomb);
        dlg.setVisible(true);
    }

    // ================================================================ eszköztár

    JToolBar epitEszkoztar() {
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        indBtn = new JButton(tr("▶ Indítás"));
        leaBtn = new JButton(tr("■ Leállítás"));
        lepesBtn = new JButton(tr("⏭ Lépés"));
        nullazBtn = new JButton(tr("⟲ Nullázás"));
        indBtn.addActionListener(e -> indit());
        leaBtn.addActionListener(e -> leallit());
        lepesBtn.addActionListener(e -> lepes());
        nullazBtn.addActionListener(e -> nullaz());
        leaBtn.setEnabled(false);

        tb.add(indBtn); tb.add(leaBtn); tb.add(lepesBtn); tb.add(nullazBtn);
        tb.addSeparator();
        JButton visszavonBtn = new JButton(visszavonAction);
        visszavonBtn.setText(tr("↶ Visszavon"));
        JButton ismetBtn = new JButton(ismetAction);
        ismetBtn.setText(tr("↷ Ismét"));
        tb.add(visszavonBtn); tb.add(ismetBtn);
        tb.addSeparator();
        tb.add(new JLabel(tr("Ciklusidő: ")));
        JComboBox<Integer> ciklus = new JComboBox<>(new Integer[]{50, 100, 150, 250, 500, 1000});
        ciklus.setSelectedItem(ciklusIdoMs);
        ciklus.addActionListener(e -> {
            ciklusIdoMs = (Integer) ciklus.getSelectedItem();
            scanTimer.setDelay(ciklusIdoMs);
        });
        tb.add(ciklus);
        tb.add(new JLabel(" ms  "));

        tb.addSeparator();
        JButton ujSor = new JButton(tr("+ Hálózat"));
        ujSor.addActionListener(e -> { ujHalozat(); });
        JButton torlSor = new JButton(tr("Hálózat törlése"));
        torlSor.addActionListener(e -> torolHalozat());
        tb.add(ujSor); tb.add(torlSor);
        tb.addSeparator();
        JButton felGomb = new JButton(tr("▲ Fel"));
        felGomb.setToolTipText(tr("A kijelölt hálózat mozgatása egy pozícióval feljebb"));
        felGomb.addActionListener(e -> halozatFel());
        JButton leGomb = new JButton(tr("▼ Le"));
        leGomb.setToolTipText(tr("A kijelölt hálózat mozgatása egy pozícióval lejjebb"));
        leGomb.addActionListener(e -> halozatLe());
        tb.add(felGomb); tb.add(leGomb);
        tb.addSeparator();
        JButton masolGomb = new JButton(tr("⧉ Hálózat másolása"));
        masolGomb.addActionListener(e -> halozatMasolas());
        JButton beillesztGomb = new JButton(tr("Hálózat beillesztése"));
        beillesztGomb.addActionListener(e -> halozatBeillesztes());
        tb.add(masolGomb); tb.add(beillesztGomb);

        tb.addSeparator();
        tb.add(new JLabel(tr("Nagyítás: ")));
        JComboBox<String> zoomCombo = new JComboBox<>(new String[]{"50%", "75%", "100%", "125%", "150%", "200%"});
        zoomCombo.setSelectedItem("100%");
        zoomCombo.addActionListener(e -> {
            String s = (String) zoomCombo.getSelectedItem();
            nagyitas = Integer.parseInt(s.substring(0, s.length() - 1)) / 100.0;
            letra.ujraMeret(); letra.repaint();
            hmi.ujraMeret(); hmi.repaint();
            fbd.ujraMeret(); fbd.repaint();
        });
        tb.add(zoomCombo);

        return tb;
    }

    // ================================================================ I/O panel

    JScrollPane epitIOPanel() {
        JPanel fo = new JPanel();
        fo.setLayout(new BoxLayout(fo, BoxLayout.Y_AXIS));
        fo.setPreferredSize(new Dimension(230, 100));
        fo.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel be = new JPanel(new GridLayout(8, 2, 3, 3));
        be.setBorder(BorderFactory.createTitledBorder(tr("Bemenetek (I)")));
        for (int k = 0; k < 16; k++) {
            int idx = k;
            JToggleButton b = new JToggleButton(addrI(idx));
            b.setFont(CIM_FONT);
            b.setToolTipText(tr("Kattintson a bemenet állapotának váltásához"));
            b.addActionListener(e -> { allapot.i[idx] = b.isSelected(); letra.repaint(); hmi.repaint(); fbd.repaint(); grafcetPanel.repaint(); });
            beGombok[k] = b;
            be.add(b);
        }

        JPanel ki = new JPanel(new GridLayout(8, 2, 3, 3));
        ki.setBorder(BorderFactory.createTitledBorder(tr("Kimenetek (Q)")));
        for (int k = 0; k < 16; k++) {
            JLabel l = new JLabel(addrQ(k), SwingConstants.CENTER);
            l.setOpaque(true);
            l.setFont(CIM_FONT);
            l.setBackground(DOBOZ_HATTER_KI);
            l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            kiLedek[k] = l;
            ki.add(l);
        }

        JPanel mi = new JPanel(new GridLayout(8, 2, 3, 3));
        mi.setBorder(BorderFactory.createTitledBorder(tr("Belső relék (M)")));
        for (int k = 0; k < 16; k++) {
            JLabel l = new JLabel(addrM(k), SwingConstants.CENTER);
            l.setOpaque(true);
            l.setFont(CIM_FONT);
            l.setBackground(DOBOZ_HATTER_KI);
            l.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            mLedek[k] = l;
            mi.add(l);
        }

        JPanel tc = new JPanel();
        tc.setLayout(new GridLayout(16, 1, 2, 2));
        tc.setBorder(BorderFactory.createTitledBorder(tr("Időzítők (T) / Számlálók (C)")));
        for (int k = 0; k < 8; k++) {
            JLabel l = new JLabel("T" + k + ": 0 / 0 ms");
            l.setFont(CIM_FONT);
            idozitoLabel[k] = l;
            tc.add(l);
        }
        for (int k = 0; k < 8; k++) {
            JPanel sor = new JPanel(new BorderLayout());
            JLabel l = new JLabel("C" + k + ": 0 / 0");
            l.setFont(CIM_FONT);
            szamlaloLabel[k] = l;
            sor.add(l, BorderLayout.CENTER);
            JButton r = new JButton("0");
            r.setMargin(new Insets(0, 2, 0, 2));
            r.setToolTipText(tr("Számláló nullázása"));
            int idx = k;
            r.addActionListener(e -> { allapot.c[idx].ertek = 0; allapot.c[idx].kesz = false; frissitIOPanel(); letra.repaint(); hmi.repaint(); fbd.repaint(); grafcetPanel.repaint(); });
            sor.add(r, BorderLayout.EAST);
            tc.add(sor);
        }

        fo.add(be);
        fo.add(Box.createVerticalStrut(4));
        fo.add(ki);
        fo.add(Box.createVerticalStrut(4));
        fo.add(mi);
        fo.add(Box.createVerticalStrut(4));
        fo.add(new JScrollPane(tc));
        JScrollPane kulsoScroll = new JScrollPane(fo);
        kulsoScroll.setBorder(BorderFactory.createEmptyBorder());
        kulsoScroll.getVerticalScrollBar().setUnitIncrement(16);
        kulsoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return kulsoScroll;
    }

    static String addrI(int idx) { return "I" + (idx / 8) + "." + (idx % 8); }
    static String addrQ(int idx) { return "Q" + (idx / 8) + "." + (idx % 8); }
    static String addrM(int idx) { return "M" + (idx / 8) + "." + (idx % 8); }

    // ------------------------------------------------------------ címkék (tag-ek)

    String cimkeKulcs(OpTipus t, int idx) {
        switch (t) {
            case I: return "I" + (idx / 8) + "." + (idx % 8);
            case Q: return "Q" + (idx / 8) + "." + (idx % 8);
            case M: return "M" + (idx / 8) + "." + (idx % 8);
            case TDN: return "T" + idx;
            case CDN: return "C" + idx;
        }
        return "?";
    }

    String cimkeNeve(OpTipus t, int idx) {
        String n = cimkeNev.get(cimkeKulcs(t, idx));
        return (n == null || n.trim().isEmpty()) ? null : n.trim();
    }

    /** Rövid feliratot ad: a címke nevét, ha van, egyébként a nyers PLC-cím. */
    String rovidFelirat(Operandus op) {
        String n = cimkeNeve(op.tipus, op.index);
        return n != null ? n : op.cim();
    }

    /** Teljes feliratot ad: "Név (Cím)" formában, ha van címke, egyébként csak a cím. */
    String felirat(Operandus op) {
        String n = cimkeNeve(op.tipus, op.index);
        return n != null ? (n + " (" + op.cim() + ")") : op.cim();
    }

    /** Eszközleírás (tooltip) egy adott operandushoz: cím, név, megjegyzés. */
    String eszkozleiras(OpTipus t, int idx) {
        String k = cimkeKulcs(t, idx);
        String n = cimkeNev.get(k);
        String m = cimkeMegj.get(k);
        StringBuilder sb = new StringBuilder(k);
        if (n != null && !n.trim().isEmpty()) sb.append("  –  ").append(n.trim());
        if (m != null && !m.trim().isEmpty()) sb.append("   (").append(m.trim()).append(")");
        return sb.toString();
    }

    // ------------------------------------------------------------ IL / STL kódgenerálás

    String stlCim(Operandus op) {
        switch (op.tipus) {
            case I: return addrI(op.index);
            case Q: return addrQ(op.index);
            case M: return addrM(op.index);
            case TDN: return "T" + op.index;
            case CDN: return "C" + op.index;
        }
        return "?";
    }

    String ilCim(Operandus op) {
        switch (op.tipus) {
            case I: return "%IX" + addrI(op.index).substring(1);
            case Q: return "%QX" + addrQ(op.index).substring(1);
            case M: return "%MX" + addrM(op.index).substring(1);
            case TDN: return "T" + op.index + ".Q";
            case CDN: return "C" + op.index + ".Q";
        }
        return "?";
    }

    String cimMegjegyzes(Operandus op) {
        String n = cimkeNeve(op.tipus, op.index);
        return n != null ? ("   // " + n) : "";
    }

    static String ismetles(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(s);
        return b.toString();
    }

    static String iecIdo(long ms) {
        return (ms % 1000 == 0) ? ("T#" + (ms / 1000) + "s") : ("T#" + ms + "ms");
    }

    static String s5Ido(long ms) {
        return (ms % 1000 == 0) ? ("S5T#" + (ms / 1000) + "S") : ("S5T#" + ms + "MS");
    }

    String halozatFejlec(Sor sor, int sorszam) {
        return "// Hálózat " + sorszam + (sor.megjegyzes.isEmpty() ? "" : "  –  " + sor.megjegyzes);
    }

    String renderIL(List<KodSor> lista) {
        StringBuilder sb = new StringBuilder();
        int melyseg = 0;
        for (KodSor k : lista) {
            if (k.muvelet == KodMuvelet.ZAR) melyseg = Math.max(0, melyseg - 1);
            sb.append(ismetles("  ", melyseg));
            switch (k.muvelet) {
                case KONTAKT: {
                    String mnem = k.elso ? (k.neg ? "LDN " : "LD  ") : (k.vagy ? (k.neg ? "ORN " : "OR  ") : (k.neg ? "ANDN" : "AND "));
                    sb.append(mnem).append(' ').append(ilCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                }
                case NYIT: {
                    sb.append(k.elso ? "LD(" : (k.vagy ? "OR(" : "AND("));
                    melyseg++;
                    break;
                }
                case ZAR:
                    sb.append(")");
                    break;
                case IGAZ:
                    sb.append(k.elso ? "LD   TRUE" : "AND  TRUE");
                    break;
                case NEGAL:
                    sb.append("NOT");
                    break;
                case ST:
                    sb.append("ST   ").append(ilCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case S:
                    sb.append("S    ").append(ilCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case R:
                    sb.append("R    ").append(ilCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case TON: {
                    String tmp = "T" + k.idx + "_EN";
                    sb.append("ST   ").append(tmp).append('\n').append(ismetles("  ", melyseg));
                    sb.append("CAL  T").append(k.idx).append("(IN:=").append(tmp).append(", PT:=").append(iecIdo(k.presetMs)).append(")");
                    String nevT = cimkeNeve(OpTipus.TDN, k.idx);
                    if (nevT != null) sb.append("   // ").append(nevT);
                    break;
                }
                case CTU: {
                    String tmp = "C" + k.idx + "_EN";
                    sb.append("ST   ").append(tmp).append('\n').append(ismetles("  ", melyseg));
                    sb.append("CAL  C").append(k.idx).append("(CU:=").append(tmp).append(", PV:=").append(k.presetSzamlalo).append(")");
                    String nevC = cimkeNeve(OpTipus.CDN, k.idx);
                    if (nevC != null) sb.append("   // ").append(nevC);
                    break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    String renderSTL(List<KodSor> lista) {
        StringBuilder sb = new StringBuilder();
        int melyseg = 0;
        for (KodSor k : lista) {
            if (k.muvelet == KodMuvelet.ZAR) melyseg = Math.max(0, melyseg - 1);
            sb.append(ismetles("  ", melyseg));
            switch (k.muvelet) {
                case KONTAKT: {
                    String mnem = (!k.elso && k.vagy) ? (k.neg ? "ON  " : "O   ") : (k.neg ? "UN  " : "U   ");
                    sb.append(mnem).append(stlCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                }
                case NYIT: {
                    sb.append((!k.elso && k.vagy) ? "O(" : "U(");
                    melyseg++;
                    break;
                }
                case ZAR:
                    sb.append(")");
                    break;
                case IGAZ:
                    sb.append("SET");
                    break;
                case NEGAL:
                    sb.append("NOT");
                    break;
                case ST:
                    sb.append("=    ").append(stlCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case S:
                    sb.append("S    ").append(stlCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case R:
                    sb.append("R    ").append(stlCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case TON: {
                    sb.append("L    ").append(s5Ido(k.presetMs)).append('\n').append(ismetles("  ", melyseg));
                    sb.append("SE   T").append(k.idx);
                    String nevT = cimkeNeve(OpTipus.TDN, k.idx);
                    if (nevT != null) sb.append("   // ").append(nevT);
                    break;
                }
                case CTU: {
                    sb.append("L    C#").append(k.presetSzamlalo).append('\n').append(ismetles("  ", melyseg));
                    sb.append("ZV   C").append(k.idx);
                    String nevC = cimkeNeve(OpTipus.CDN, k.idx);
                    if (nevC != null) sb.append("   // ").append(nevC);
                    sb.append('\n').append(ismetles("  ", melyseg));
                    sb.append("// megjegyzés: valós AWL-ben az előválasztó betöltése/beállítása (S) jellemzően külön hálózatban történik");
                    break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    String teljesKodIL() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Instruction List (IEC 61131-3) – automatikusan generált, egyszerűsített kód.\n");
        sb.append("// Az időzítő/számláló funkcióblokk-hívás (CAL) a feltétel egy belső segédváltozóba (*_EN) mentésével készül.\n\n");
        int i = 1;
        for (Sor sor : program) {
            sb.append(halozatFejlec(sor, i)).append('\n');
            sb.append(renderIL(forditSor(sor)));
            sb.append('\n');
            i++;
        }
        if (!fbdBlokkok.isEmpty()) {
            sb.append("// ===== Funkcióblokk-diagram (FBD) – kiértékelési sorrend szerint =====\n");
            sb.append("// A blokkok közötti huzal-értékek \"FB<szám>\" néven szerepelnek szimbolikus segédváltozóként.\n\n");
            for (FbdBlock b : fbdBlokkok) {
                sb.append(fbdKodIL(b));
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    String teljesKodSTL() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Siemens STL (AWL) – automatikusan generált, egyszerűsített kód.\n");
        sb.append("// Az időzítő SE (bekapcsolás-késleltetés), a számláló ZV (előre számlálás) utasítással készül.\n\n");
        int i = 1;
        for (Sor sor : program) {
            sb.append(halozatFejlec(sor, i)).append('\n');
            sb.append(renderSTL(forditSor(sor)));
            sb.append('\n');
            i++;
        }
        if (!fbdBlokkok.isEmpty()) {
            sb.append("// ===== Funkcióblokk-diagram (FBD) – kiértékelési sorrend szerint =====\n");
            sb.append("// A blokkok közötti huzal-értékek \"FB<szám>\" néven szerepelnek szimbolikus segédváltozóként.\n\n");
            for (FbdBlock b : fbdBlokkok) {
                sb.append(fbdKodSTL(b));
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Mitsubishi FX/Q utasításlista

    String mitsubishiCim(Operandus op) {
        switch (op.tipus) {
            case I: return "X" + op.index;
            case Q: return "Y" + op.index;
            case M: return "M" + op.index;
            case TDN: return "T" + op.index;
            case CDN: return "C" + op.index;
        }
        return "?";
    }

    String renderMitsubishi(List<KodSor> lista) {
        StringBuilder sb = new StringBuilder();
        Deque<Boolean> vagyVerem = new ArrayDeque<>();
        for (KodSor k : lista) {
            switch (k.muvelet) {
                case KONTAKT: {
                    String mnem = k.elso ? (k.neg ? "LDI " : "LD  ") : (k.vagy ? (k.neg ? "ORI " : "OR  ") : (k.neg ? "ANI " : "AND "));
                    sb.append(mnem).append(mitsubishiCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                }
                case NYIT:
                    vagyVerem.push(k.vagy);
                    continue; // a Mitsubishi LD/LDI-vel kezdi az új blokkot, önálló nyitó utasítás nélkül
                case ZAR: {
                    boolean vagy = !vagyVerem.isEmpty() && vagyVerem.pop();
                    sb.append(vagy ? "ORB" : "ANB");
                    break;
                }
                case IGAZ:
                    sb.append(k.elso ? "LD   M8000" : (k.vagy ? "OR   M8000" : "AND  M8000")).append("   // mindig ON (RUN monitor relé)");
                    break;
                case NEGAL:
                    sb.append("INV");
                    break;
                case ST:
                    sb.append("OUT  ").append(mitsubishiCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case S:
                    sb.append("SET  ").append(mitsubishiCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case R:
                    sb.append("RST  ").append(mitsubishiCim(k.op)).append(cimMegjegyzes(k.op));
                    break;
                case TON: {
                    long preset100ms = Math.max(1, Math.round(k.presetMs / 100.0));
                    sb.append("OUT  T").append(k.idx).append(" K").append(preset100ms);
                    String nevT = cimkeNeve(OpTipus.TDN, k.idx);
                    if (nevT != null) sb.append("   // ").append(nevT);
                    break;
                }
                case CTU: {
                    sb.append("OUT  C").append(k.idx).append(" K").append(k.presetSzamlalo);
                    String nevC = cimkeNeve(OpTipus.CDN, k.idx);
                    if (nevC != null) sb.append("   // ").append(nevC);
                    break;
                }
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    String teljesKodMitsubishi() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Mitsubishi Electric FX/Q/L/iQ-F/iQ-R – utasításlista (IL), egyszerűsített decimális címzéssel.\n");
        sb.append("// Az időzítő natív \"OUT T.. K..\" (100 ms-os alapegység), a számláló \"OUT C.. K..\" formában készül.\n");
        sb.append("// Az FBD blokkok ebben a nézetben nem szerepelnek.\n\n");
        int i = 1;
        for (Sor sor : program) {
            sb.append(halozatFejlec(sor, i)).append('\n');
            sb.append(renderMitsubishi(forditSor(sor)));
            sb.append('\n');
            i++;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ Allen-Bradley / Rockwell (SLC-500) utasításkészlet

    String abCim(Operandus op) {
        switch (op.tipus) {
            case I: return "I:0/" + op.index;
            case Q: return "O:0/" + op.index;
            case M: return "B3:0/" + op.index;
            case TDN: return "T4:" + op.index + "/DN";
            case CDN: return "C5:" + op.index + "/DN";
        }
        return "?";
    }

    String abKontaktSor(Erintkezo e) {
        String mnem = e.no ? "XIC " : "XIO ";
        return mnem + " " + abCim(e.op) + cimMegjegyzes(e.op);
    }

    String renderABHalozat(Sor sor) {
        StringBuilder sb = new StringBuilder();
        for (Erintkezo e : sor.elotag) sb.append(abKontaktSor(e)).append('\n');
        if (!sor.agak.isEmpty()) {
            sb.append("BST\n");
            for (int bi = 0; bi < sor.agak.size(); bi++) {
                List<Erintkezo> ag = sor.agak.get(bi);
                if (bi > 0) sb.append("NXB\n");
                if (ag.isEmpty()) {
                    sb.append("// (üres ág – feltétel nélkül vezet)\n");
                } else {
                    for (Erintkezo e : ag) sb.append(abKontaktSor(e)).append('\n');
                }
            }
            sb.append("BND\n");
        }
        for (Erintkezo e : sor.utotag) sb.append(abKontaktSor(e)).append('\n');

        Kimenet k = sor.kimenet;
        switch (k.tipus) {
            case TEKERCS:
                if (k.negalt) sb.append("// AB: nincs natív negált tekercs – invertálja a feltételt kézzel\n");
                sb.append("OTE  ").append(abCim(k.cel)).append(cimMegjegyzes(k.cel)).append('\n');
                break;
            case SET:
                sb.append("OTL  ").append(abCim(k.cel)).append(cimMegjegyzes(k.cel)).append('\n');
                break;
            case RESET:
                sb.append("OTU  ").append(abCim(k.cel)).append(cimMegjegyzes(k.cel)).append('\n');
                break;
            case TON: {
                String nevT = cimkeNeve(OpTipus.TDN, k.idx);
                sb.append("TON  T4:").append(k.idx).append("   Preset:").append(k.presetMs).append("ms");
                if (nevT != null) sb.append("   // ").append(nevT);
                sb.append('\n');
                break;
            }
            case CTU: {
                String nevC = cimkeNeve(OpTipus.CDN, k.idx);
                sb.append("CTU  C5:").append(k.idx).append("   Preset:").append(k.presetSzamlalo);
                if (nevC != null) sb.append("   // ").append(nevC);
                sb.append('\n');
                break;
            }
        }
        return sb.toString();
    }

    String teljesKodAllenBradley() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Allen-Bradley / Rockwell (SLC-500, MicroLogix) utasításkészlet – egyszerűsített, pedagógiai célú reprezentáció.\n");
        sb.append("// XIC/XIO = vizsgálat zárt/nyitott, OTE/OTL/OTU = tekercs/set/reset, BST/NXB/BND = párhuzamos ág kezdete/köv./vége.\n");
        sb.append("// Az FBD blokkok ebben a nézetben nem szerepelnek.\n\n");
        int i = 1;
        for (Sor sor : program) {
            sb.append(halozatFejlec(sor, i)).append('\n');
            sb.append(renderABHalozat(sor));
            sb.append('\n');
            i++;
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ FBD -> IL / STL fordítás

    String fbdFejlec(FbdBlock b) {
        return "// FB" + b.id + "  [" + b.tipus + "]" + (b.felirat == null || b.felirat.isEmpty() ? "" : "  – " + b.felirat);
    }

    String fbdKimenetSzoveg(FbdBlock b, boolean il) {
        switch (b.tipus) {
            case BEMENET: return il ? ilCim(b.op) : stlCim(b.op);
            case TON: return il ? ("T" + b.idx + ".Q") : ("T" + b.idx);
            case CTU: return il ? ("C" + b.idx + ".Q") : ("C" + b.idx);
            case R_TRIG: case F_TRIG: return il ? ("FB" + b.id + ".Q") : ("FB" + b.id);
            default: return "FB" + b.id; // AND / OR / NOT / XOR / SR
        }
    }

    String fbdBemenetSzoveg(FbdBlock b, int i, boolean il) {
        int fid = (b.forrasId != null && i < b.forrasId.length) ? b.forrasId[i] : -1;
        FbdBlock forras = fid >= 0 ? fbdBlockById(fid) : null;
        return forras != null ? fbdKimenetSzoveg(forras, il) : null; // null = nincs kötve
    }

    String fbdKodIL(FbdBlock b) {
        StringBuilder sb = new StringBuilder();
        sb.append(fbdFejlec(b)).append('\n');
        String[] be = new String[b.bemenetSzam];
        for (int i = 0; i < b.bemenetSzam; i++) be[i] = fbdBemenetSzoveg(b, i, true);

        switch (b.tipus) {
            case BEMENET:
                sb.append("// (nincs külön utasítás – ").append(ilCim(b.op)).append(" a forrásoknál közvetlenül felhasználva)\n");
                break;
            case KIMENET:
                sb.append(be.length > 0 && be[0] != null ? ("LD   " + be[0]) : "LD   FALSE").append('\n');
                sb.append("ST   ").append(ilCim(b.op)).append(cimMegjegyzes(b.op)).append('\n');
                break;
            case AND: {
                boolean mindVan = true;
                for (String s : be) if (s == null) mindVan = false;
                if (!mindVan) {
                    sb.append("LD   FALSE   // nyitott bemenet -> ÉS eredménye mindig hamis\n");
                } else {
                    boolean elso = true;
                    for (String s : be) { sb.append(elso ? "LD   " : "AND  ").append(s).append('\n'); elso = false; }
                }
                sb.append("ST   FB").append(b.id).append('\n');
                break;
            }
            case OR: {
                boolean irtMar = false;
                for (String s : be) {
                    if (s == null) continue;
                    sb.append(irtMar ? "OR   " : "LD   ").append(s).append('\n');
                    irtMar = true;
                }
                if (!irtMar) sb.append("LD   FALSE   // nincs kötött bemenet\n");
                sb.append("ST   FB").append(b.id).append('\n');
                break;
            }
            case NOT:
                sb.append(be.length > 0 && be[0] != null ? ("LDN  " + be[0]) : "LD   TRUE").append('\n');
                sb.append("ST   FB").append(b.id).append('\n');
                break;
            case XOR: {
                String a = be.length > 0 ? be[0] : null, bb = be.length > 1 ? be[1] : null;
                if (a != null && bb != null) { sb.append("LD   ").append(a).append('\n'); sb.append("XOR  ").append(bb).append('\n'); }
                else if (a != null) sb.append("LD   ").append(a).append('\n');
                else if (bb != null) sb.append("LD   ").append(bb).append('\n');
                else sb.append("LD   FALSE\n");
                sb.append("ST   FB").append(b.id).append('\n');
                break;
            }
            case SR:
                sb.append(be.length > 0 && be[0] != null ? ("LD   " + be[0]) : "LD   FALSE").append('\n');
                sb.append("S    FB").append(b.id).append('\n');
                sb.append(be.length > 1 && be[1] != null ? ("LD   " + be[1]) : "LD   FALSE").append('\n');
                sb.append("R    FB").append(b.id).append("    // reset-domináns: az R felülírja az S-t\n");
                break;
            case TON: {
                String tmp = "FB" + b.id + "_EN";
                sb.append(be.length > 0 && be[0] != null ? ("LD   " + be[0]) : "LD   FALSE").append('\n');
                sb.append("ST   ").append(tmp).append('\n');
                sb.append("CAL  T").append(b.idx).append("(IN:=").append(tmp).append(", PT:=").append(iecIdo(b.presetMs)).append(")\n");
                break;
            }
            case CTU: {
                String tmp = "FB" + b.id + "_CU";
                sb.append(be.length > 0 && be[0] != null ? ("LD   " + be[0]) : "LD   FALSE").append('\n');
                sb.append("ST   ").append(tmp).append('\n');
                sb.append("CAL  C").append(b.idx).append("(CU:=").append(tmp).append(", PV:=").append(b.presetSzamlalo).append(")\n");
                break;
            }
            case R_TRIG: case F_TRIG: {
                String tmp = "FB" + b.id + "_CLK";
                sb.append(be.length > 0 && be[0] != null ? ("LD   " + be[0]) : "LD   FALSE").append('\n');
                sb.append("ST   ").append(tmp).append('\n');
                sb.append("CAL  FB").append(b.id).append("(CLK:=").append(tmp).append(")   // ").append(b.tipus == FbdTipus.R_TRIG ? "R_TRIG" : "F_TRIG").append('\n');
                break;
            }
        }
        return sb.toString();
    }

    String fbdKodSTL(FbdBlock b) {
        StringBuilder sb = new StringBuilder();
        sb.append(fbdFejlec(b)).append('\n');
        String[] be = new String[b.bemenetSzam];
        for (int i = 0; i < b.bemenetSzam; i++) be[i] = fbdBemenetSzoveg(b, i, false);

        switch (b.tipus) {
            case BEMENET:
                sb.append("// (nincs külön utasítás – ").append(stlCim(b.op)).append(" a forrásoknál közvetlenül felhasználva)\n");
                break;
            case KIMENET:
                sb.append(be.length > 0 && be[0] != null ? ("U    " + be[0]) : "CLR").append('\n');
                sb.append("=    ").append(stlCim(b.op)).append(cimMegjegyzes(b.op)).append('\n');
                break;
            case AND: {
                boolean mindVan = true;
                for (String s : be) if (s == null) mindVan = false;
                if (!mindVan) {
                    sb.append("CLR          // nyitott bemenet -> ÉS eredménye mindig hamis\n");
                } else {
                    for (String s : be) sb.append("U    ").append(s).append('\n');
                }
                sb.append("=    FB").append(b.id).append('\n');
                break;
            }
            case OR: {
                boolean irtMar = false;
                for (String s : be) {
                    if (s == null) continue;
                    sb.append("O    ").append(s).append('\n');
                    irtMar = true;
                }
                if (!irtMar) sb.append("CLR          // nincs kötött bemenet\n");
                sb.append("=    FB").append(b.id).append('\n');
                break;
            }
            case NOT:
                sb.append(be.length > 0 && be[0] != null ? ("UN   " + be[0]) : "SET").append('\n');
                sb.append("=    FB").append(b.id).append('\n');
                break;
            case XOR: {
                String a = be.length > 0 ? be[0] : null, bb = be.length > 1 ? be[1] : null;
                if (a != null && bb != null) { sb.append("U    ").append(a).append('\n'); sb.append("X    ").append(bb).append('\n'); }
                else if (a != null) sb.append("U    ").append(a).append('\n');
                else if (bb != null) sb.append("U    ").append(bb).append('\n');
                else sb.append("CLR\n");
                sb.append("=    FB").append(b.id).append('\n');
                break;
            }
            case SR:
                sb.append(be.length > 0 && be[0] != null ? ("U    " + be[0]) : "CLR").append('\n');
                sb.append("S    FB").append(b.id).append('\n');
                sb.append(be.length > 1 && be[1] != null ? ("U    " + be[1]) : "CLR").append('\n');
                sb.append("R    FB").append(b.id).append("    // reset-domináns: az R felülírja az S-t\n");
                break;
            case TON: {
                sb.append(be.length > 0 && be[0] != null ? ("U    " + be[0]) : "CLR").append('\n');
                sb.append("L    ").append(s5Ido(b.presetMs)).append('\n');
                sb.append("SE   T").append(b.idx).append('\n');
                break;
            }
            case CTU: {
                sb.append(be.length > 0 && be[0] != null ? ("U    " + be[0]) : "CLR").append('\n');
                sb.append("L    C#").append(b.presetSzamlalo).append('\n');
                sb.append("ZV   C").append(b.idx).append('\n');
                sb.append("// megjegyzés: valós AWL-ben az előválasztó betöltése/beállítása (S) jellemzően külön hálózatban történik\n");
                break;
            }
            case R_TRIG: case F_TRIG: {
                String edgeMem = "M_el_FB" + b.id;
                sb.append(be.length > 0 && be[0] != null ? ("U    " + be[0]) : "CLR").append('\n');
                sb.append(b.tipus == FbdTipus.R_TRIG ? "FP   " : "FN   ").append(edgeMem).append("   // fenntartott él-tároló segédbit\n");
                sb.append("=    FB").append(b.id).append('\n');
                break;
            }
        }
        return sb.toString();
    }

    // ================================================================ hivatkozás-ellenőrzés

    static class Problema {
        String szint;  // "Hiba" / "Figyelmeztetés"
        String szoveg;
        Problema(String szint, String szoveg) { this.szint = szint; this.szoveg = szoveg; }
    }

    List<Problema> ellenorzesFut() {
        List<Problema> hibak = new ArrayList<>();

        // 1. több hely írja közvetlenül ugyanazt a Q/M kimenetet (létra TEKERCS + FBD KIMENET)
        Map<String, List<String>> irasHelyek = new LinkedHashMap<>();
        int hi = 1;
        for (Sor s : program) {
            if (s.kimenet.tipus == KimTipus.TEKERCS) {
                irasHelyek.computeIfAbsent(s.kimenet.cel.cim(), x -> new ArrayList<>()).add(tr("Hálózat") + " " + hi);
            }
            hi++;
        }
        for (FbdBlock b : fbdBlokkok) {
            if (b.tipus == FbdTipus.KIMENET) {
                irasHelyek.computeIfAbsent(b.op.cim(), x -> new ArrayList<>()).add("FB" + b.id);
            }
        }
        for (OraImpulzus o : oraImpulzusok) {
            irasHelyek.computeIfAbsent(addrM(o.mIndex), x -> new ArrayList<>()).add(tr("Órajel") + " (" + o.frekvenciaHz + " Hz)");
        }
        for (Map.Entry<String, List<String>> e : irasHelyek.entrySet()) {
            if (e.getValue().size() > 1) {
                hibak.add(new Problema(tr("Hiba"), tr("Több hely írja közvetlenül a") + " " + e.getKey() + " " + tr("kimenetet:") + " "
                        + String.join(", ", e.getValue()) + " – " + tr("scan-ciklusonként az utoljára kiértékelt felülírja a többit.")));
            }
        }

        // 2. több hely vezérli ugyanazt az időzítőt/számlálót (létra TON/CTU + FBD TON/CTU)
        Map<String, List<String>> idzSzHelyek = new LinkedHashMap<>();
        hi = 1;
        for (Sor s : program) {
            if (s.kimenet.tipus == KimTipus.TON) idzSzHelyek.computeIfAbsent("T" + s.kimenet.idx, x -> new ArrayList<>()).add(tr("Hálózat") + " " + hi);
            if (s.kimenet.tipus == KimTipus.CTU) idzSzHelyek.computeIfAbsent("C" + s.kimenet.idx, x -> new ArrayList<>()).add(tr("Hálózat") + " " + hi);
            hi++;
        }
        for (FbdBlock b : fbdBlokkok) {
            if (b.tipus == FbdTipus.TON) idzSzHelyek.computeIfAbsent("T" + b.idx, x -> new ArrayList<>()).add("FB" + b.id);
            if (b.tipus == FbdTipus.CTU) idzSzHelyek.computeIfAbsent("C" + b.idx, x -> new ArrayList<>()).add("FB" + b.id);
        }
        for (Map.Entry<String, List<String>> e : idzSzHelyek.entrySet()) {
            if (e.getValue().size() > 1) {
                hibak.add(new Problema(tr("Hiba"), tr("Több hely vezérli ugyanazt az időzítőt/számlálót") + " (" + e.getKey() + "): "
                        + String.join(", ", e.getValue()) + "."));
            }
        }

        // 3. FBD: nyitott (nem kötött) bemenetek és önhivatkozás
        for (FbdBlock b : fbdBlokkok) {
            List<Integer> nyitott = new ArrayList<>();
            for (int i = 0; i < b.bemenetSzam; i++) {
                int fid = (b.forrasId != null && i < b.forrasId.length) ? b.forrasId[i] : -1;
                if (fid < 0) nyitott.add(i + 1);
                else if (fid == b.id) hibak.add(new Problema(tr("Hiba"), "FB" + b.id + " (" + b.tipus + ") " + tr("önmagára hivatkozik a") + " " + (i + 1) + ". " + tr("bemeneten.")));
            }
            if (!nyitott.isEmpty()) {
                hibak.add(new Problema(tr("Figyelmeztetés"), "FB" + b.id + " (" + b.tipus + ") " + tr("nyitott (nem kötött) bemenete(i):")
                        + " " + nyitott + " – " + tr("futás közben hamis (0) értéket vesz fel.")));
            }
        }

        return hibak;
    }

    void ellenorzesAblak() {
        List<Problema> hibak = ellenorzesFut();
        StringBuilder sb = new StringBuilder();
        if (hibak.isEmpty()) {
            sb.append(tr("Nem találtam hivatkozási problémát.")).append('\n');
        } else {
            for (Problema p : hibak) sb.append("[").append(p.szint).append("]  ").append(p.szoveg).append("\n\n");
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(640, 360));
        JOptionPane.showMessageDialog(this, sp, tr("Program ellenőrzése"),
                hibak.isEmpty() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
    }

    // ================================================================ kereszthivatkozás-keresés

    boolean illik(Operandus op, OpTipus t, int idx) { return op != null && op.tipus == t && op.index == idx; }

    String kereszthivatkozasSzoveg(OpTipus t, int idx, String cimSzoveg) {
        StringBuilder sb = new StringBuilder();
        sb.append(tr("Cím:")).append(" ").append(cimSzoveg).append("\n\n");
        boolean talalt = false;

        int hi = 1;
        for (Sor s : program) {
            List<String> helyek = new ArrayList<>();
            for (Erintkezo e : s.elotag) if (illik(e.op, t, idx)) helyek.add(tr("előtag"));
            for (List<Erintkezo> ag : s.agak) for (Erintkezo e : ag) if (illik(e.op, t, idx)) helyek.add(tr("ág"));
            for (Erintkezo e : s.utotag) if (illik(e.op, t, idx)) helyek.add(tr("utótag"));
            boolean kimenetTalalat = false;
            String kimenetLeiras = null;
            switch (s.kimenet.tipus) {
                case TEKERCS: case SET: case RESET:
                    if (illik(s.kimenet.cel, t, idx)) { kimenetTalalat = true; kimenetLeiras = tr("kimenet") + " (" + s.kimenet.tipus + ")"; }
                    break;
                case TON:
                    if (t == OpTipus.TDN && s.kimenet.idx == idx) { kimenetTalalat = true; kimenetLeiras = tr("TON időzítő"); }
                    break;
                case CTU:
                    if (t == OpTipus.CDN && s.kimenet.idx == idx) { kimenetTalalat = true; kimenetLeiras = tr("CTU számláló"); }
                    break;
            }
            if (!helyek.isEmpty() || kimenetTalalat) {
                talalt = true;
                sb.append(tr("Létra")).append(" – ").append(tr("Hálózat")).append(" ").append(hi);
                if (!s.megjegyzes.isEmpty()) sb.append(" (").append(s.megjegyzes).append(")");
                sb.append(":");
                if (!helyek.isEmpty()) sb.append("  ").append(tr("kontaktus")).append(" [").append(String.join(", ", helyek)).append("]");
                if (kimenetTalalat) sb.append("  ").append(kimenetLeiras);
                sb.append('\n');
            }
            hi++;
        }

        for (FbdBlock b : fbdBlokkok) {
            boolean ez = false;
            String leiras = null;
            if ((b.tipus == FbdTipus.BEMENET || b.tipus == FbdTipus.KIMENET) && illik(b.op, t, idx)) {
                ez = true; leiras = (b.tipus == FbdTipus.BEMENET ? tr("Bemenet blokk") : tr("Kimenet blokk"));
            }
            if (b.tipus == FbdTipus.TON && t == OpTipus.TDN && b.idx == idx) { ez = true; leiras = tr("TON blokk"); }
            if (b.tipus == FbdTipus.CTU && t == OpTipus.CDN && b.idx == idx) { ez = true; leiras = tr("CTU blokk"); }
            if (ez) {
                talalt = true;
                sb.append("FBD – FB").append(b.id).append(" (").append(leiras).append(")")
                        .append(b.felirat == null || b.felirat.isEmpty() ? "" : "  – " + b.felirat).append('\n');
            }
        }

        for (HmiKepernyo kep : hmiKepernyok) {
            for (HmiWidget w : kep.widgetek) {
                if (w.tipus == HmiTipus.FELIRAT || w.tipus == HmiTipus.TEGLALAP) continue;
                if (illik(w.op, t, idx)) {
                    talalt = true;
                    sb.append("HMI – \"").append(kep.nev).append("\" ").append(tr("képernyő:")).append(" ").append(w.tipus)
                            .append(w.felirat == null || w.felirat.isEmpty() ? "" : "  (" + w.felirat + ")").append('\n');
                }
            }
        }

        if (!talalt) sb.append(tr("Nincs hivatkozás.")).append('\n');
        return sb.toString();
    }

    void kereszthivatkozasAblak() {
        List<OpTipus> tipusLista = new ArrayList<>();
        List<Integer> idxLista = new ArrayList<>();
        List<String> cimLista = new ArrayList<>();
        for (int k = 0; k < 16; k++) { tipusLista.add(OpTipus.I); idxLista.add(k); cimLista.add(addrI(k)); }
        for (int k = 0; k < 16; k++) { tipusLista.add(OpTipus.Q); idxLista.add(k); cimLista.add(addrQ(k)); }
        for (int k = 0; k < 16; k++) { tipusLista.add(OpTipus.M); idxLista.add(k); cimLista.add(addrM(k)); }
        for (int k = 0; k < 8; k++) { tipusLista.add(OpTipus.TDN); idxLista.add(k); cimLista.add("T" + k); }
        for (int k = 0; k < 8; k++) { tipusLista.add(OpTipus.CDN); idxLista.add(k); cimLista.add("C" + k); }

        JComboBox<String> combo = new JComboBox<>();
        for (int i = 0; i < cimLista.size(); i++) {
            String nev = cimkeNeve(tipusLista.get(i), idxLista.get(i));
            combo.addItem(cimLista.get(i) + (nev != null ? "  –  " + nev : ""));
        }

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(620, 380));

        Runnable frissit = () -> {
            int idx = combo.getSelectedIndex();
            if (idx < 0) return;
            ta.setText(kereszthivatkozasSzoveg(tipusLista.get(idx), idxLista.get(idx), cimLista.get(idx)));
            ta.setCaretPosition(0);
        };
        combo.addActionListener(e -> frissit.run());

        JPanel felso = new JPanel(new BorderLayout(6, 6));
        felso.add(new JLabel(tr("Cím:") + " "), BorderLayout.WEST);
        felso.add(combo, BorderLayout.CENTER);

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        fo.add(felso, BorderLayout.NORTH);
        fo.add(sp, BorderLayout.CENTER);

        combo.setSelectedIndex(0);
        frissit.run();

        JOptionPane.showMessageDialog(this, fo, tr("Hivatkozások keresése"), JOptionPane.PLAIN_MESSAGE);
    }

    void kodExportAblak() {
        JTextArea ta = new JTextArea();
        ta.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        ta.setEditable(false);
        ta.setTabSize(4);

        JRadioButton ilVal = ketsorosRadio(tr("Instruction List (IEC 61131-3)"),
                "Omron, Hitachi, Schneider Electric, Beckhoff, Keyence, Panasonic, Delta", true);
        JRadioButton stlVal = ketsorosRadio(tr("Siemens STL (AWL)"), "Siemens S7-200 / 300 / 400 / 1200 / 1500", false);
        JRadioButton mitsuVal = ketsorosRadio(tr("Mitsubishi FX/Q utasításlista"), "Mitsubishi Electric FX, Q, L, iQ-F, iQ-R", false);
        JRadioButton abVal = ketsorosRadio(tr("Allen-Bradley (SLC-500) utasításkészlet"),
                "Rockwell MicroLogix, SLC-500, CompactLogix/ControlLogix", false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(ilVal); bg.add(stlVal); bg.add(mitsuVal); bg.add(abVal);

        Runnable frissit = () -> {
            String kod;
            if (ilVal.isSelected()) kod = teljesKodIL();
            else if (stlVal.isSelected()) kod = teljesKodSTL();
            else if (mitsuVal.isSelected()) kod = teljesKodMitsubishi();
            else kod = teljesKodAllenBradley();
            ta.setText(kod);
            ta.setCaretPosition(0);
        };
        ilVal.addActionListener(e -> frissit.run());
        stlVal.addActionListener(e -> frissit.run());
        mitsuVal.addActionListener(e -> frissit.run());
        abVal.addActionListener(e -> frissit.run());
        frissit.run();

        JPanel felso = new JPanel();
        felso.setLayout(new BoxLayout(felso, BoxLayout.Y_AXIS));
        JPanel radioSor = new JPanel(new GridLayout(1, 4, 10, 0));
        radioSor.add(ilVal); radioSor.add(stlVal); radioSor.add(mitsuVal); radioSor.add(abVal);
        felso.add(radioSor);

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(760, 560));

        JButton masolGomb = new JButton(tr("Vágólapra másolás"));
        masolGomb.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(ta.getText()), null);
            statusz.setText(" " + tr("Gépi kód a vágólapra másolva."));
        });
        JButton mentGomb = new JButton(tr("Mentés .txt fájlba..."));
        mentGomb.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(tr("Szöveges fájl (*.txt)"), "txt"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File cel = fc.getSelectedFile();
            if (!cel.getName().toLowerCase(Locale.ROOT).endsWith(".txt")) cel = new File(cel.getPath() + ".txt");
            try (Writer w = new OutputStreamWriter(new FileOutputStream(cel), "UTF-8")) {
                w.write(ta.getText());
                statusz.setText(" " + tr("Gépi kód elmentve:") + " " + cel.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, tr("Nem sikerült menteni:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton gyartoGomb = new JButton(tr("Gyártói kompatibilitás..."));
        gyartoGomb.addActionListener(e -> gyartoiTablaAblak());

        JPanel alsoGombok = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        alsoGombok.add(gyartoGomb); alsoGombok.add(masolGomb); alsoGombok.add(mentGomb);

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        fo.add(felso, BorderLayout.NORTH);
        fo.add(sp, BorderLayout.CENTER);
        fo.add(alsoGombok, BorderLayout.SOUTH);

        JDialog dlg = new JDialog(this, tr("Gépi kód megtekintése"), false);
        dlg.setContentPane(fo);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    JRadioButton ketsorosRadio(String cim, String gyartok, boolean kivalasztva) {
        String html = "<html><b>" + cim + "</b><br><span style='color:#777777;font-size:90%'>" + gyartok + "</span></html>";
        return new JRadioButton(html, kivalasztva);
    }

    void gyartoiTablaAblak() {
        String[][] sorok = {
                {"Siemens", "S7-200/300/400/1200/1500", "STL / AWL", tr("Teljes körűen támogatott")},
                {"Mitsubishi Electric", "FX, Q, L, iQ-F, iQ-R", "IL / Instruction List", tr("Teljes körűen támogatott")},
                {"Omron", "CP, CJ, CS, NX", "IL / Instruction List", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Hitachi", "EH, H-series", "Instruction List", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Allen-Bradley / Rockwell", "MicroLogix, CompactLogix, ControlLogix", "Instruction List / SLC Instruction Set", tr("Teljes körűen támogatott")},
                {"Schneider Electric", "Modicon M340, M580, Premium", "IL", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Beckhoff", "TwinCAT / IPC", "IL, ST", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Keyence", "KV series", "Instruction / mnemonic", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Panasonic", "FP series", "FP Instruction List", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Delta", "DVP, AS series", "Instruction List", tr("Instruction List (IEC 61131-3) nézettel kompatibilis")},
                {"Yaskawa", "MPiec, VIPA stb.", tr("gyártóspecifikus"), tr("Nem támogatott – egyedi formátum")},
                {"FANUC", "CNC/robot", tr("saját vezérlési nyelvek"), tr("Nem támogatott – robotnyelv, nem PLC-logika")},
                {"Kawasaki", tr("robotvezérlők"), "AS " + tr("nyelv"), tr("Nem támogatott – robotnyelv, nem PLC-logika")},
                {"ABB", tr("robotvezérlők"), "RAPID", tr("Nem támogatott – robotnyelv, nem PLC-logika")},
        };
        String[] oszlopok = {tr("Gyártó"), tr("Jellemző PLC-k"), tr("Alacsony szintű / utasítás jellegű nyelv"), tr("Állapot ebben a szimulátorban")};
        DefaultTableModel model = new DefaultTableModel(sorok, oszlopok) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tabla = new JTable(model);
        tabla.setRowHeight(26);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(200);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(230);

        JScrollPane sp = new JScrollPane(tabla);
        Dimension kepernyo = Toolkit.getDefaultToolkit().getScreenSize();
        sp.setPreferredSize(new Dimension(Math.min(800, kepernyo.width - 100), Math.min(460, kepernyo.height - 220)));

        JOptionPane.showMessageDialog(this, sp, tr("Gyártói kompatibilitás"), JOptionPane.PLAIN_MESSAGE);
    }

    void frissitIOPanel() {
        for (int k = 0; k < 16; k++) {
            beGombok[k].setSelected(allapot.i[k]);
            String nevI = cimkeNeve(OpTipus.I, k);
            beGombok[k].setText(nevI != null ? ("<html><center>" + addrI(k) + "<br><b>" + nevI + "</b></center></html>") : addrI(k));
            beGombok[k].setToolTipText(eszkozleiras(OpTipus.I, k));
            kiLedek[k].setBackground(allapot.q[k] ? DOBOZ_HATTER_BE : DOBOZ_HATTER_KI);
            String nevQ = cimkeNeve(OpTipus.Q, k);
            kiLedek[k].setText(nevQ != null ? ("<html><center>" + addrQ(k) + "<br><b>" + nevQ + "</b></center></html>") : addrQ(k));
            kiLedek[k].setToolTipText(eszkozleiras(OpTipus.Q, k));
            mLedek[k].setBackground(allapot.m[k] ? DOBOZ_HATTER_BE : DOBOZ_HATTER_KI);
            String nevM = cimkeNeve(OpTipus.M, k);
            mLedek[k].setText(nevM != null ? ("<html><center>" + addrM(k) + "<br><b>" + nevM + "</b></center></html>") : addrM(k));
            mLedek[k].setToolTipText(eszkozleiras(OpTipus.M, k));
        }
        for (int k = 0; k < 8; k++) {
            Idozito t = allapot.t[k];
            String nevT = cimkeNeve(OpTipus.TDN, k);
            idozitoLabel[k].setText("T" + k + (nevT != null ? " (" + nevT + ")" : "") + ": " + t.akkMs + " / " + t.presetMs + " ms" + (t.kesz ? "  ✔" : ""));
            idozitoLabel[k].setToolTipText(eszkozleiras(OpTipus.TDN, k));
            idozitoLabel[k].setForeground(t.kesz ? ENERGIZALT.darker() : Color.BLACK);
            Szamlalo c = allapot.c[k];
            String nevC = cimkeNeve(OpTipus.CDN, k);
            szamlaloLabel[k].setText("C" + k + (nevC != null ? " (" + nevC + ")" : "") + ": " + c.ertek + " / " + c.preset + (c.kesz ? "  ✔" : ""));
            szamlaloLabel[k].setToolTipText(eszkozleiras(OpTipus.CDN, k));
            szamlaloLabel[k].setForeground(c.kesz ? ENERGIZALT.darker() : Color.BLACK);
        }
    }

    /**
     * A folytonos (a scan-ciklustól független, simább) HMI-animációkat lépteti: az ajtó
     * nyitottság-átmenetét, a futó szállítószalagok csíkmintáját és munkadarabját, az aktív
     * érzékelők pulzáló jelzőfényét, valamint a Fanuc/Kuka robotkarok folytonos pick&place mozgását.
     */
    void hmiAnimLepes() {
        boolean kellFrissit = false;
        for (HmiKepernyo kep : hmiKepernyok) {
            for (HmiWidget w : kep.widgetek) {
                switch (w.tipus) {
                    case AJTO: {
                        kellFrissit = true;
                        double cel = w.op.olvas(allapot) ? 1.0 : 0.0;
                        double lepes = 0.06;
                        if (w.ajtoNyitottsag < cel) w.ajtoNyitottsag = Math.min(cel, w.ajtoNyitottsag + lepes);
                        else if (w.ajtoNyitottsag > cel) w.ajtoNyitottsag = Math.max(cel, w.ajtoNyitottsag - lepes);
                        break;
                    }
                    case SZALLITOSZALAG: {
                        if (w.op.olvas(allapot)) {
                            kellFrissit = true;
                            w.szalagFazis += 0.045;
                            if (w.szalagFazis > 1000) w.szalagFazis -= 1000;
                        }
                        break;
                    }
                    case SZENZOR: {
                        if (w.op.olvas(allapot)) {
                            kellFrissit = true;
                            w.szenzorPulzus += 0.12;
                            if (w.szenzorPulzus > Math.PI * 2) w.szenzorPulzus -= Math.PI * 2;
                        }
                        break;
                    }
                    case ROBOT_FANUC: case ROBOT_KUKA: {
                        if (w.op.olvas(allapot)) {
                            kellFrissit = true;
                            w.robotFazis += 0.035;
                            if (w.robotFazis > Math.PI * 2) w.robotFazis -= Math.PI * 2;
                        }
                        break;
                    }
                    default: break;
                }
            }
        }
        if (kellFrissit) hmi.repaint();
    }

    /** A konfigurált óraimpulzus-generátorok (clock bitek) léptetése: minden egyes M-bitet a beállított frekvencián kapcsol. */
    void kiertekelOraImpulzusok() {
        for (OraImpulzus o : oraImpulzusok) {
            if (o.frekvenciaHz <= 0 || o.mIndex < 0 || o.mIndex >= 16) continue;
            long felPeriodusMs = Math.max(1, Math.round(500.0 / o.frekvenciaHz));
            o.akkMs += ciklusIdoMs;
            while (o.akkMs >= felPeriodusMs) {
                o.akkMs -= felPeriodusMs;
                allapot.m[o.mIndex] = !allapot.m[o.mIndex];
            }
        }
    }

    // ================================================================ szimuláció vezérlés

    void indit() {
        fut = true;
        indBtn.setEnabled(false);
        leaBtn.setEnabled(true);
        lepesBtn.setEnabled(false);
        scanTimer.start();
        statusz.setText(" " + tr("Fut – ciklusidő:") + " " + ciklusIdoMs + " ms.");
        letra.repaint();
        hmi.repaint();
        fbd.repaint();
        grafcetPanel.repaint();
    }

    void leallit() {
        fut = false;
        scanTimer.stop();
        indBtn.setEnabled(true);
        leaBtn.setEnabled(false);
        lepesBtn.setEnabled(true);
        statusz.setText(" " + tr("Leállítva. Scan-ciklusok:") + " " + scanSzamlalo);
        letra.repaint();
        hmi.repaint();
        fbd.repaint();
        grafcetPanel.repaint();
    }

    void lepes() {
        pordulEgyCiklus();
        statusz.setText(" " + tr("Egy lépés végrehajtva. Scan-ciklusok:") + " " + scanSzamlalo);
    }

    void nullaz() {
        leallit();
        allapot.nullaz();
        for (FbdBlock b : fbdBlokkok) { b.kimenetErtek = false; b.elozoClk = false; }
        for (OraImpulzus o : oraImpulzusok) o.akkMs = 0;
        grafcetNullaz();
        for (HmiKepernyo kep : hmiKepernyok) for (HmiWidget w : kep.widgetek) if (w.trendTortenet != null) w.trendTortenet.clear();
        scanSzamlalo = 0;
        frissitIOPanel();
        letra.repaint();
        hmi.repaint();
        fbd.repaint();
        grafcetPanel.repaint();
        statusz.setText(" " + tr("Nullázva."));
    }

    void pordulEgyCiklus() {
        kiertekelOraImpulzusok();
        for (Sor sor : program) {
            boolean teljesit = sor.kiertekel(allapot);
            Kimenet k = sor.kimenet;
            switch (k.tipus) {
                case TEKERCS:
                    irBit(k.cel, k.negalt ? !teljesit : teljesit);
                    break;
                case SET:
                    if (teljesit) irBit(k.cel, true);
                    break;
                case RESET:
                    if (teljesit) irBit(k.cel, false);
                    break;
                case TON: {
                    Idozito t = allapot.t[k.idx];
                    t.presetMs = k.presetMs;
                    if (teljesit) {
                        t.akkMs = Math.min(t.presetMs, t.akkMs + ciklusIdoMs);
                        t.kesz = t.akkMs >= t.presetMs;
                    } else {
                        t.akkMs = 0;
                        t.kesz = false;
                    }
                    break;
                }
                case CTU: {
                    Szamlalo c = allapot.c[k.idx];
                    c.preset = k.presetSzamlalo;
                    boolean felfuto = teljesit && !c.elozo;
                    if (felfuto) c.ertek = Math.min(c.preset, c.ertek + 1);
                    c.elozo = teljesit;
                    c.kesz = c.ertek >= c.preset;
                    break;
                }
            }
        }
        kiertekelFbd();
        kiertekelGrafcet();
        hmiTrendMintavetel();
        scanSzamlalo++;
        frissitIOPanel();
        letra.repaint();
        hmi.repaint();
        fbd.repaint();
        grafcetPanel.repaint();
        if (fut) statusz.setText(" " + tr("Fut – ciklusidő:") + " " + ciklusIdoMs + " ms.  " + tr("Scan-ciklusok:") + " " + scanSzamlalo);
    }

    void irBit(Operandus op, boolean ertek) {
        if (op.tipus == OpTipus.Q) allapot.q[op.index] = ertek;
        else if (op.tipus == OpTipus.M) allapot.m[op.index] = ertek;
    }

    // ================================================================ FBD kiértékelés

    FbdBlock fbdBlockById(int id) {
        for (FbdBlock b : fbdBlokkok) if (b.id == id) return b;
        return null;
    }

    boolean fbdKimenetOlvas(int blockId) {
        FbdBlock b = fbdBlockById(blockId);
        return b != null && b.kimenetErtek;
    }

    void kiertekelFbd() {
        for (FbdBlock b : fbdBlokkok) {
            boolean[] be = new boolean[b.bemenetSzam];
            for (int i = 0; i < b.bemenetSzam; i++) {
                int fid = (b.forrasId != null && i < b.forrasId.length) ? b.forrasId[i] : -1;
                be[i] = fid >= 0 && fbdKimenetOlvas(fid);
            }
            switch (b.tipus) {
                case BEMENET:
                    b.kimenetErtek = b.op.olvas(allapot);
                    break;
                case KIMENET:
                    b.kimenetErtek = be.length > 0 && be[0];
                    irBit(b.op, b.kimenetErtek);
                    break;
                case AND: {
                    boolean r = b.bemenetSzam > 0;
                    for (boolean x : be) r &= x;
                    b.kimenetErtek = r;
                    break;
                }
                case OR: {
                    boolean r = false;
                    for (boolean x : be) r |= x;
                    b.kimenetErtek = r;
                    break;
                }
                case NOT:
                    b.kimenetErtek = !(be.length > 0 && be[0]);
                    break;
                case XOR:
                    b.kimenetErtek = (be.length > 0 && be[0]) ^ (be.length > 1 && be[1]);
                    break;
                case SR: {
                    boolean s = be.length > 0 && be[0], r = be.length > 1 && be[1];
                    if (r) b.kimenetErtek = false; else if (s) b.kimenetErtek = true;
                    break;
                }
                case TON: {
                    boolean in = be.length > 0 && be[0];
                    Idozito t = allapot.t[b.idx];
                    t.presetMs = b.presetMs;
                    if (in) { t.akkMs = Math.min(t.presetMs, t.akkMs + ciklusIdoMs); t.kesz = t.akkMs >= t.presetMs; }
                    else { t.akkMs = 0; t.kesz = false; }
                    b.kimenetErtek = t.kesz;
                    break;
                }
                case CTU: {
                    boolean cu = be.length > 0 && be[0];
                    boolean rst = be.length > 1 && be[1];
                    Szamlalo c = allapot.c[b.idx];
                    c.preset = b.presetSzamlalo;
                    if (rst) { c.ertek = 0; c.kesz = false; c.elozo = false; }
                    else {
                        boolean felfuto = cu && !c.elozo;
                        if (felfuto) c.ertek = Math.min(c.preset, c.ertek + 1);
                        c.elozo = cu;
                        c.kesz = c.ertek >= c.preset;
                    }
                    b.kimenetErtek = c.kesz;
                    break;
                }
                case R_TRIG: {
                    boolean clk = be.length > 0 && be[0];
                    b.kimenetErtek = clk && !b.elozoClk;
                    b.elozoClk = clk;
                    break;
                }
                case F_TRIG: {
                    boolean clk = be.length > 0 && be[0];
                    b.kimenetErtek = !clk && b.elozoClk;
                    b.elozoClk = clk;
                    break;
                }
            }
        }
    }

    // ================================================================ GRAFCET kiértékelés

    GrafcetLepes grafcetLepesById(int id) {
        for (GrafcetLepes l : grafcet.lepesek) if (l.id == id) return l;
        return null;
    }

    GrafcetAtmenet grafcetAtmenetById(int id) {
        for (GrafcetAtmenet t : grafcet.atmenetek) if (t.id == id) return t;
        return null;
    }

    boolean grafcetAtmenetFeltetelIgaz(GrafcetAtmenet t) {
        for (Erintkezo e : t.feltetel) if (!e.zart(allapot)) return false;
        return true;
    }

    boolean grafcetAtmenetSulhet(GrafcetAtmenet t) {
        if (t.forrasLepesek.isEmpty()) return false;
        for (int sid : t.forrasLepesek) {
            GrafcetLepes s = grafcetLepesById(sid);
            if (s == null || !s.aktiv) return false;
        }
        return grafcetAtmenetFeltetelIgaz(t);
    }

    void kiertekelGrafcet() {
        List<GrafcetAtmenet> sulnek = new ArrayList<>();
        for (GrafcetAtmenet t : grafcet.atmenetek) if (grafcetAtmenetSulhet(t)) sulnek.add(t);
        for (GrafcetAtmenet t : sulnek) {
            for (int sid : t.forrasLepesek) {
                GrafcetLepes s = grafcetLepesById(sid);
                if (s != null) s.aktiv = false;
            }
        }
        for (GrafcetAtmenet t : sulnek) {
            for (int tid : t.celLepesek) {
                GrafcetLepes c = grafcetLepesById(tid);
                if (c != null) c.aktiv = true;
            }
        }
        // Ha már generáltunk létrát ebből a GRAFCET-ből, a tényleges I/O-t innentől a létra vezérli;
        // a GRAFCET fül ezután csak vizuális előnézetként animálja a lépéseket (nem ír ki/vezérel bitet),
        // hogy a két reprezentáció ne versenyezzen ugyanazért a kimenetért.
        if (grafcetKezdetiMIndexek.isEmpty()) {
            for (GrafcetLepes l : grafcet.lepesek) {
                for (GrafcetAkcio a : l.akciok) {
                    switch (a.tipus) {
                        case FOLYAMATOS: irBit(a.cel, l.aktiv); break;
                        case SET: if (l.aktiv) irBit(a.cel, true); break;
                        case RESET: if (l.aktiv) irBit(a.cel, false); break;
                    }
                }
            }
        }
    }

    void grafcetNullaz() {
        for (GrafcetLepes l : grafcet.lepesek) l.aktiv = l.kezdeti;
        for (int idx : grafcetKezdetiMIndexek) if (idx >= 0 && idx < 16) allapot.m[idx] = true;
    }

    // ================================================================ visszavonás / ismét (undo/redo)

    static class SzerkesztesiAllapot implements Serializable {
        List<Sor> program;
        Map<String, String> cimkeNev;
        Map<String, String> cimkeMegj;
        List<HmiKepernyo> hmiKepernyok;
        int hmiAktivKepernyo;
        List<FbdBlock> fbdBlokkok;
        int fbdKovetkezoId;
        GrafcetDiagram grafcet;
        List<Integer> grafcetKezdetiMIndexek;
        List<OraImpulzus> oraImpulzusok;
    }

    Deque<byte[]> undoVerem = new ArrayDeque<>();
    Deque<byte[]> redoVerem = new ArrayDeque<>();
    byte[] folyamatbanLevoElotte = null;
    static final int UNDO_MAX = 60;

    Action visszavonAction = new AbstractAction("↶ Visszavonás") {
        @Override public void actionPerformed(ActionEvent e) { visszavon(); }
    };
    Action ismetAction = new AbstractAction("↷ Ismét") {
        @Override public void actionPerformed(ActionEvent e) { ismet(); }
    };

    @SuppressWarnings("unchecked")
    <T> T deepCopy(T obj) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (ObjectOutputStream oo = new ObjectOutputStream(bo)) { oo.writeObject(obj); }
            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bo.toByteArray()));
            return (T) oi.readObject();
        } catch (Exception ex) {
            return null;
        }
    }

    byte[] pillanatkep() {
        try {
            SzerkesztesiAllapot s = new SzerkesztesiAllapot();
            s.program = program;
            s.cimkeNev = cimkeNev;
            s.cimkeMegj = cimkeMegj;
            s.hmiKepernyok = hmiKepernyok;
            s.hmiAktivKepernyo = hmiAktivKepernyo;
            s.fbdBlokkok = fbdBlokkok;
            s.fbdKovetkezoId = fbdKovetkezoId;
            s.grafcet = grafcet;
            s.grafcetKezdetiMIndexek = grafcetKezdetiMIndexek;
            s.oraImpulzusok = oraImpulzusok;
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (ObjectOutputStream oo = new ObjectOutputStream(bo)) { oo.writeObject(s); }
            return bo.toByteArray();
        } catch (IOException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    void visszaallitPillanatkepbol(byte[] adat) {
        if (adat == null) return;
        try {
            ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(adat));
            SzerkesztesiAllapot s = (SzerkesztesiAllapot) oi.readObject();
            program = s.program;
            cimkeNev = s.cimkeNev;
            cimkeMegj = s.cimkeMegj;
            hmiKepernyok = (s.hmiKepernyok != null && !s.hmiKepernyok.isEmpty()) ? s.hmiKepernyok : ujHmiKepernyoLista();
            hmiAktivKepernyo = Math.max(0, Math.min(s.hmiAktivKepernyo, hmiKepernyok.size() - 1));
            hmiWidgetek = hmiKepernyok.get(hmiAktivKepernyo).widgetek;
            fbdBlokkok = s.fbdBlokkok;
            fbdKovetkezoId = s.fbdKovetkezoId;
            grafcet = (s.grafcet != null) ? s.grafcet : new GrafcetDiagram();
            grafcetKezdetiMIndexek = (s.grafcetKezdetiMIndexek != null) ? s.grafcetKezdetiMIndexek : new ArrayList<>();
            oraImpulzusok = (s.oraImpulzusok != null) ? s.oraImpulzusok : new ArrayList<>();
            kivalasztottSor = null;
            hmi.kivalasztott = null;
            fbd.kivalasztott = null;
            grafcetPanel.kivalasztottLepes = null;
            grafcetPanel.kivalasztottAtmenet = null;
            frissitIOPanel();
            letra.ujraMeret(); letra.repaint();
            if (hmiKepernyoCombo != null) hmiKepernyoComboFrissit();
            hmi.ujraMeret(); hmi.repaint();
            fbd.ujraMeret(); fbd.repaint();
            grafcetPanel.ujraMeret(); grafcetPanel.repaint();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, tr("Visszaállítási hiba:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
        }
    }

    /** A gesztus/szerkesztés MEGKEZDÉSE előtt hívandó: elmenti az "előtte" állapotot. */
    void undoPontBeallit() { folyamatbanLevoElotte = pillanatkep(); }

    /** Akkor hívandó, ha a gesztus valós változást eredményezett: rögzíti az "előtte" állapotot a visszavonási veremben. */
    void undoPontRogzit() {
        if (folyamatbanLevoElotte == null) return;
        demoProgram = false;
        aktivMintaNev = null;
        undoVerem.push(folyamatbanLevoElotte);
        while (undoVerem.size() > UNDO_MAX) undoVerem.removeLast();
        redoVerem.clear();
        folyamatbanLevoElotte = null;
        frissitUndoAllapot();
    }

    /** Akkor hívandó, ha a gesztus megszakadt / nem történt valós változás: elveti az elmentett "előtte" állapotot. */
    void undoPontEldob() { folyamatbanLevoElotte = null; }

    void visszavon() {
        if (undoVerem.isEmpty()) return;
        byte[] elotte = undoVerem.pop();
        redoVerem.push(pillanatkep());
        visszaallitPillanatkepbol(elotte);
        frissitUndoAllapot();
        statusz.setText(" " + tr("Visszavonva."));
    }

    void ismet() {
        if (redoVerem.isEmpty()) return;
        byte[] utana = redoVerem.pop();
        undoVerem.push(pillanatkep());
        visszaallitPillanatkepbol(utana);
        frissitUndoAllapot();
        statusz.setText(" " + tr("Ismét végrehajtva."));
    }

    void undoTorol() {
        undoVerem.clear();
        redoVerem.clear();
        folyamatbanLevoElotte = null;
        frissitUndoAllapot();
    }

    void frissitUndoAllapot() {
        visszavonAction.setEnabled(!undoVerem.isEmpty());
        ismetAction.setEnabled(!redoVerem.isEmpty());
    }

    void bindUndoRedoGyorsgombok(JRootPane rp) {
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "visszavon");
        rp.getActionMap().put("visszavon", visszavonAction);
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "ismet");
        rp.getActionMap().put("ismet", ismetAction);
    }

    // ================================================================ hálózat szerkesztés

    void ujHalozat() {
        undoPontBeallit();
        Sor s = new Sor();
        s.kimenet.cel = new Operandus(OpTipus.Q, kovetkezoSzabadQ());
        String megj = JOptionPane.showInputDialog(this, tr("Hálózat megjegyzése (opcionális):"), tr("Új hálózat"), JOptionPane.QUESTION_MESSAGE);
        s.megjegyzes = (megj != null) ? megj.trim() : "";
        program.add(s);
        kivalasztottSor = s;
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar vsb = letraScroll.getVerticalScrollBar();
            vsb.setValue(vsb.getMaximum());
        });
    }

    int kovetkezoSzabadQ() {
        boolean[] hasznalt = new boolean[16];
        for (Sor s : program) {
            if ((s.kimenet.tipus == KimTipus.TEKERCS || s.kimenet.tipus == KimTipus.SET || s.kimenet.tipus == KimTipus.RESET)
                    && s.kimenet.cel.tipus == OpTipus.Q) {
                hasznalt[s.kimenet.cel.index] = true;
            }
        }
        for (int k = 0; k < 16; k++) if (!hasznalt[k]) return k;
        return 0;
    }

    void halozatMegjegyzesSzerkeszto(Sor sor) {
        undoPontBeallit();
        String uj = JOptionPane.showInputDialog(this, tr("Hálózat megjegyzése:"), sor.megjegyzes);
        if (uj != null) {
            sor.megjegyzes = uj.trim();
            undoPontRogzit();
            letra.repaint();
        } else {
            undoPontEldob();
        }
    }

    void torolHalozat() {
        if (kivalasztottSor == null) {
            JOptionPane.showMessageDialog(this, tr("Előbb jelöljön ki egy hálózatot (kattintson a bal oldali sávjára)."));
            return;
        }
        undoPontBeallit();
        program.remove(kivalasztottSor);
        kivalasztottSor = null;
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }

    void halozatFel() {
        if (kivalasztottSor == null) return;
        int idx = program.indexOf(kivalasztottSor);
        if (idx <= 0) return;
        undoPontBeallit();
        Collections.swap(program, idx, idx - 1);
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }

    void halozatLe() {
        if (kivalasztottSor == null) return;
        int idx = program.indexOf(kivalasztottSor);
        if (idx < 0 || idx >= program.size() - 1) return;
        undoPontBeallit();
        Collections.swap(program, idx, idx + 1);
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }

    void halozatMasolas() {
        if (kivalasztottSor == null) {
            JOptionPane.showMessageDialog(this, tr("Előbb jelöljön ki egy hálózatot (kattintson a bal oldali sávjára)."));
            return;
        }
        vagolapHalozat = deepCopy(kivalasztottSor);
        statusz.setText(" " + tr("Hálózat vágólapra másolva."));
    }

    void halozatBeillesztes() {
        if (vagolapHalozat == null) {
            JOptionPane.showMessageDialog(this, tr("Nincs vágólapra másolt hálózat."));
            return;
        }
        undoPontBeallit();
        Sor masolat = deepCopy(vagolapHalozat);
        int idx = kivalasztottSor != null ? program.indexOf(kivalasztottSor) : -1;
        if (idx >= 0) program.add(idx + 1, masolat); else program.add(masolat);
        kivalasztottSor = masolat;
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
        statusz.setText(" " + tr("Hálózat beillesztve a vágólapról."));
    }

    void addBranch(Sor sor) {
        undoPontBeallit();
        if (sor.agak.isEmpty()) {
            sor.agak.add(new ArrayList<>());
            sor.agak.add(new ArrayList<>());
        } else {
            sor.agak.add(new ArrayList<>());
        }
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }

    void removeBranch(Sor sor, List<Erintkezo> ag) {
        undoPontBeallit();
        sor.agak.remove(ag);
        if (sor.agak.size() == 1) {
            sor.elotag.addAll(sor.agak.get(0));
            sor.agak.clear();
        }
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }

    // ================================================================ fájl műveletek

    void ujProgram() {
        if (JOptionPane.showConfirmDialog(this, tr("Biztosan töröl minden hálózatot?"), tr("Új program"),
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        leallit();
        program = new ArrayList<>();
        demoProgram = false;
        aktivMintaNev = null;
        allapot = new PlcAllapot();
        cimkeNev = new LinkedHashMap<>();
        cimkeMegj = new LinkedHashMap<>();
        hmiKepernyok = ujHmiKepernyoLista();
        hmiAktivKepernyo = 0;
        hmiWidgetek = hmiKepernyok.get(0).widgetek;
        fbdBlokkok = new ArrayList<>();
        fbdKovetkezoId = 1;
        grafcet = new GrafcetDiagram();
        grafcetKezdetiMIndexek = new ArrayList<>();
        oraImpulzusok = new ArrayList<>();
        aktualisFajl = null;
        kivalasztottSor = null;
        scanSzamlalo = 0;
        frissitIOPanel();
        letra.ujraMeret();
        letra.repaint();
        hmiKepernyoComboFrissit();
        hmi.ujraMeret();
        hmi.repaint();
        fbd.ujraMeret();
        fbd.repaint();
        grafcetPanel.ujraMeret();
        grafcetPanel.repaint();
        undoTorol();
        setTitle(PROGRAM);
    }

    @SuppressWarnings("unchecked")
    void megnyit() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(tr("PLC program (*.plc)"), "plc"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
            leallit();
            demoProgram = false;
            aktivMintaNev = null;
            Object obj = in.readObject();
            if (obj instanceof ProjektAdat) {
                ProjektAdat p = (ProjektAdat) obj;
                program = p.program != null ? p.program : new ArrayList<>();
                cimkeNev = p.cimkeNev != null ? new LinkedHashMap<>(p.cimkeNev) : new LinkedHashMap<>();
                cimkeMegj = p.cimkeMegj != null ? new LinkedHashMap<>(p.cimkeMegj) : new LinkedHashMap<>();
                if (p.hmiKepernyok != null && !p.hmiKepernyok.isEmpty()) {
                    hmiKepernyok = new ArrayList<>(p.hmiKepernyok);
                } else if (p.hmiWidgetek != null) {
                    HmiKepernyo k = new HmiKepernyo();
                    k.widgetek = new ArrayList<>(p.hmiWidgetek);
                    hmiKepernyok = new ArrayList<>(Collections.singletonList(k));
                } else {
                    hmiKepernyok = ujHmiKepernyoLista();
                }
                hmiAktivKepernyo = 0;
                hmiWidgetek = hmiKepernyok.get(0).widgetek;
                fbdBlokkok = p.fbdBlokkok != null ? new ArrayList<>(p.fbdBlokkok) : new ArrayList<>();
                fbdKovetkezoId = Math.max(1, p.fbdKovetkezoId);
                grafcet = p.grafcet != null ? p.grafcet : new GrafcetDiagram();
                grafcetKezdetiMIndexek = p.grafcetKezdetiMIndexek != null ? new ArrayList<>(p.grafcetKezdetiMIndexek) : new ArrayList<>();
                oraImpulzusok = p.oraImpulzusok != null ? new ArrayList<>(p.oraImpulzusok) : new ArrayList<>();
            } else if (obj instanceof List) {
                program = (List<Sor>) obj;
                cimkeNev = new LinkedHashMap<>();
                cimkeMegj = new LinkedHashMap<>();
                hmiKepernyok = ujHmiKepernyoLista();
                hmiAktivKepernyo = 0;
                hmiWidgetek = hmiKepernyok.get(0).widgetek;
                fbdBlokkok = new ArrayList<>();
                fbdKovetkezoId = 1;
                grafcet = new GrafcetDiagram();
                grafcetKezdetiMIndexek = new ArrayList<>();
                oraImpulzusok = new ArrayList<>();
            } else {
                throw new IOException(tr("Ismeretlen fájlformátum."));
            }
            allapot = new PlcAllapot();
            aktualisFajl = fc.getSelectedFile();
            kivalasztottSor = null;
            scanSzamlalo = 0;
            frissitIOPanel();
            letra.ujraMeret();
            letra.repaint();
            hmiKepernyoComboFrissit();
            hmi.ujraMeret();
            hmi.repaint();
            fbd.ujraMeret();
            fbd.repaint();
            grafcetPanel.ujraMeret();
            grafcetPanel.repaint();
            undoTorol();
            setTitle(PROGRAM + " – " + aktualisFajl.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, tr("Nem sikerült megnyitni:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
        }
    }

    void mentes(boolean masKent) {
        File cel = aktualisFajl;
        if (masKent || cel == null) {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(tr("PLC program (*.plc)"), "plc"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            cel = fc.getSelectedFile();
            if (!cel.getName().toLowerCase(Locale.ROOT).endsWith(".plc")) cel = new File(cel.getPath() + ".plc");
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cel))) {
            ProjektAdat p = new ProjektAdat();
            p.program = new ArrayList<>(program);
            p.cimkeNev = new LinkedHashMap<>(cimkeNev);
            p.cimkeMegj = new LinkedHashMap<>(cimkeMegj);
            p.hmiKepernyok = new ArrayList<>(hmiKepernyok);
            p.fbdBlokkok = new ArrayList<>(fbdBlokkok);
            p.fbdKovetkezoId = fbdKovetkezoId;
            p.grafcet = grafcet;
            p.grafcetKezdetiMIndexek = new ArrayList<>(grafcetKezdetiMIndexek);
            p.oraImpulzusok = new ArrayList<>(oraImpulzusok);
            out.writeObject(p);
            aktualisFajl = cel;
            setTitle(PROGRAM + " – " + aktualisFajl.getName());
            statusz.setText(" " + tr("Mentve:") + " " + aktualisFajl.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, tr("Nem sikerült menteni:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
        }
    }

    // ================================================================ nyomtatás / PDF export

    interface LapRajzolo { void rajzol(Graphics2D g2, PageFormat pf); }

    static class TobbLapNyomtato implements Printable {
        final List<LapRajzolo> lapok;
        TobbLapNyomtato(List<LapRajzolo> lapok) { this.lapok = lapok; }
        public int print(Graphics graphics, PageFormat pf, int pageIndex) {
            if (pageIndex < 0 || pageIndex >= lapok.size()) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            lapok.get(pageIndex).rajzol(g2, pf);
            return Printable.PAGE_EXISTS;
        }
    }

    static class PanelNyomtato implements Printable {
        final Component komponens;
        final String cim;
        PanelNyomtato(Component komponens, String cim) { this.komponens = komponens; this.cim = cim; }
        public int print(Graphics graphics, PageFormat pf, int pageIndex) {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.translate(pf.getImageableX(), pf.getImageableY());
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g2.drawString(cim, 0, 14);
            double fejMagassag = 24;
            double szelesseg = pf.getImageableWidth();
            double magassag = pf.getImageableHeight() - fejMagassag;
            int compW = Math.max(1, komponens.getWidth());
            int compH = Math.max(1, komponens.getHeight());
            double meretezes = Math.min(szelesseg / compW, magassag / compH);
            g2.translate(0, fejMagassag);
            g2.scale(meretezes, meretezes);
            komponens.printAll(g2);
            return Printable.PAGE_EXISTS;
        }
    }

    void nyomtatPanel(Component komponens, String cim) {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.LANDSCAPE);
        pf = job.validatePage(pf);
        job.setPrintable(new PanelNyomtato(komponens, cim), pf);
        job.setJobName(PROGRAM + " - " + cim);
        if (job.printDialog()) {
            try {
                job.print();
                statusz.setText(" " + tr("Nyomtatás elindítva:") + " " + cim);
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, tr("Nyomtatási hiba:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    void nyomtatasAblak() {
        JDialog dlg = new JDialog(this, tr("Nyomtatás / PDF export"), true);
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel info = new JLabel("<html><body style='width:320px'>" + tr("Válassza ki, melyik ábrát nyomtatja. A megjelenő nyomtatási párbeszédablakban a \"Microsoft Print to PDF\" nyomtatót választva PDF fájlként is elmentheti.") + "</body></html>");
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(info);
        p.add(Box.createVerticalStrut(10));

        JButton b1 = new JButton(tr("Létradiagram nyomtatása..."));
        b1.setAlignmentX(Component.LEFT_ALIGNMENT);
        b1.addActionListener(e -> { dlg.dispose(); nyomtatPanel(letra, tr("Létradiagram")); });
        JButton b2 = new JButton(tr("HMI nyomtatása..."));
        b2.setAlignmentX(Component.LEFT_ALIGNMENT);
        b2.addActionListener(e -> { dlg.dispose(); nyomtatPanel(hmi, "HMI"); });
        JButton b3 = new JButton(tr("Funkcióblokk (FBD) nyomtatása..."));
        b3.setAlignmentX(Component.LEFT_ALIGNMENT);
        b3.addActionListener(e -> { dlg.dispose(); nyomtatPanel(fbd, tr("Funkcióblokk (FBD)")); });
        JButton b4 = new JButton(tr("GRAFCET nyomtatása..."));
        b4.setAlignmentX(Component.LEFT_ALIGNMENT);
        b4.addActionListener(e -> { dlg.dispose(); nyomtatPanel(grafcetPanel, "GRAFCET"); });
        p.add(b1); p.add(Box.createVerticalStrut(4));
        p.add(b2); p.add(Box.createVerticalStrut(4));
        p.add(b3); p.add(Box.createVerticalStrut(4));
        p.add(b4);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    void rajzoljKozepre(Graphics2D g2, String s, double szelesseg, double y) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(s);
        g2.drawString(s, (float) ((szelesseg - w) / 2), (float) y);
    }

    LapRajzolo dokCimlapRajzolo() {
        return (g2, pf) -> {
            double szelesseg = pf.getImageableWidth();
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
            rajzoljKozepre(g2, PROGRAM, szelesseg, 140);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
            rajzoljKozepre(g2, tr("Projekt dokumentáció"), szelesseg, 170);
            g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            String fajlNev = (aktualisFajl != null) ? aktualisFajl.getName() : tr("(nincs elmentve)");
            rajzoljKozepre(g2, tr("Fájl:") + " " + fajlNev, szelesseg, 200);
            rajzoljKozepre(g2, tr("Létrehozva:") + " " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), szelesseg, 218);
            g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 10));
            rajzoljKozepre(g2, "PLC-szimulátor – Sisak László (helmetsoft.com)", szelesseg, 260);
        };
    }

    void addCimkeSor(List<String[]> sorok, OpTipus t, int idx) {
        String nev = cimkeNeve(t, idx);
        String megj = cimkeMegj.get(cimkeKulcs(t, idx));
        if ((nev == null || nev.isEmpty()) && (megj == null || megj.trim().isEmpty())) return;
        sorok.add(new String[]{cimkeKulcs(t, idx), nev != null ? nev : "", megj != null ? megj.trim() : ""});
    }

    List<LapRajzolo> dokCimkeTablaLapok(PageFormat pf) {
        List<String[]> sorok = new ArrayList<>();
        for (int k = 0; k < 16; k++) addCimkeSor(sorok, OpTipus.I, k);
        for (int k = 0; k < 16; k++) addCimkeSor(sorok, OpTipus.Q, k);
        for (int k = 0; k < 16; k++) addCimkeSor(sorok, OpTipus.M, k);
        for (int k = 0; k < 8; k++) addCimkeSor(sorok, OpTipus.TDN, k);
        for (int k = 0; k < 8; k++) addCimkeSor(sorok, OpTipus.CDN, k);

        List<LapRajzolo> lapok = new ArrayList<>();
        double sorMagassag = 16;
        double fejMagassag = 50;
        int sorPerLap = Math.max(1, (int) ((pf.getImageableHeight() - fejMagassag) / sorMagassag));

        if (sorok.isEmpty()) {
            lapok.add((g2, p) -> {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g2.drawString(tr("Címke lista (I/Q/M/T/C)"), 0, 16);
                g2.setFont(new Font(Font.SANS_SERIF, Font.ITALIC, 11));
                g2.drawString(tr("Nincs elnevezett címke."), 0, 44);
            });
            return lapok;
        }
        for (int start = 0; start < sorok.size(); start += sorPerLap) {
            List<String[]> reszlet = sorok.subList(start, Math.min(sorok.size(), start + sorPerLap));
            lapok.add((g2, p) -> {
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g2.drawString(tr("Címke lista (I/Q/M/T/C)"), 0, 16);
                g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                double y = 40;
                g2.drawString(tr("Cím"), 0, (float) y);
                g2.drawString(tr("Név"), 60, (float) y);
                g2.drawString(tr("Megjegyzés"), 220, (float) y);
                g2.drawLine(0, (int) y + 4, (int) p.getImageableWidth(), (int) y + 4);
                g2.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
                y += sorMagassag;
                for (String[] sor : reszlet) {
                    g2.drawString(sor[0], 0, (float) y);
                    g2.drawString(sor[1], 60, (float) y);
                    g2.drawString(sor[2], 220, (float) y);
                    y += sorMagassag;
                }
            });
        }
        return lapok;
    }

    LapRajzolo dokDiagramLapRajzolo(Component komponens, String cim) {
        return (g2, pf) -> {
            g2.setColor(Color.BLACK);
            g2.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            g2.drawString(cim, 0, 16);
            double fejMagassag = 28;
            double szelesseg = pf.getImageableWidth();
            double magassag = pf.getImageableHeight() - fejMagassag;
            int compW = Math.max(1, komponens.getWidth());
            int compH = Math.max(1, komponens.getHeight());
            double meretezes = Math.min(szelesseg / compW, magassag / compH);
            Graphics2D g2b = (Graphics2D) g2.create();
            g2b.translate(0, fejMagassag);
            g2b.scale(meretezes, meretezes);
            komponens.printAll(g2b);
            g2b.dispose();
        };
    }

    void dokumentacioNyomtatas() {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pf = job.defaultPage();
        pf.setOrientation(PageFormat.PORTRAIT);
        pf = job.validatePage(pf);
        final PageFormat vegsoPf = pf;

        List<LapRajzolo> lapok = new ArrayList<>();
        lapok.add(dokCimlapRajzolo());
        lapok.addAll(dokCimkeTablaLapok(vegsoPf));
        lapok.add(dokDiagramLapRajzolo(letra, tr("Létradiagram")));
        lapok.add(dokDiagramLapRajzolo(hmi, "HMI"));
        if (!fbdBlokkok.isEmpty()) lapok.add(dokDiagramLapRajzolo(fbd, tr("Funkcióblokk (FBD)")));
        if (!grafcet.lepesek.isEmpty()) lapok.add(dokDiagramLapRajzolo(grafcetPanel, "GRAFCET"));

        job.setPrintable(new TobbLapNyomtato(lapok), vegsoPf);
        job.setJobName(PROGRAM + " - " + tr("Projekt dokumentáció"));
        if (job.printDialog()) {
            try {
                job.print();
                statusz.setText(" " + tr("A projekt dokumentáció nyomtatása elindítva."));
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, tr("Nyomtatási hiba:") + " " + ex.getMessage(), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ================================================================ demó program induláskor

    static List<Sor> uj_demoProgram() {
        List<Sor> l = new ArrayList<>();

        Sor s1 = new Sor();
        s1.megjegyzes = tr("Motor indítás/leállítás (önfenntartó kapcsolás)");
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.I, 0))));
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.Q, 0))));
        s1.utotag.add(e(false, OpTipus.I, 1));
        s1.kimenet.tipus = KimTipus.TEKERCS;
        s1.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("3 másodperces késleltetés a motor indítása után");
        s2.elotag.add(e(true, OpTipus.Q, 0));
        s2.kimenet.tipus = KimTipus.TON;
        s2.kimenet.idx = 0;
        s2.kimenet.presetMs = 3000;
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("Jelzőlámpa a késleltetés lejártakor");
        s3.elotag.add(e(true, OpTipus.TDN, 0));
        s3.kimenet.tipus = KimTipus.TEKERCS;
        s3.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Indítások számlálása");
        s4.elotag.add(e(true, OpTipus.I, 0));
        s4.kimenet.tipus = KimTipus.CTU;
        s4.kimenet.idx = 0;
        s4.kimenet.presetSzamlalo = 5;
        l.add(s4);

        Sor s5 = new Sor();
        s5.megjegyzes = tr("Jelzés 5 indítás után");
        s5.elotag.add(e(true, OpTipus.CDN, 0));
        s5.kimenet.tipus = KimTipus.TEKERCS;
        s5.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s5);

        return l;
    }

    static Erintkezo e(boolean no, OpTipus t, int idx) {
        Erintkezo e = new Erintkezo();
        e.no = no;
        e.op = new Operandus(t, idx);
        return e;
    }

    // ================================================================ minta programok (programkönyvtár)

    static List<Sor> mintaProgramEpit(String id) {
        if (id == null) return uj_demoProgram();
        switch (id) {
            case "CSILLAGDELTA": return uj_demoCsillagDelta();
            case "SZALLITOSZALAG": return uj_demoSzallitoSzalag();
            case "ROBOTCELLA": return uj_demoRobotcella();
            case "KOZLEKEDESILAMPA": return uj_demoKozlekedesiLampa();
            case "LIFT": return uj_demoLift();
            case "UTKERESZTEZODES": return uj_demoUtkeresztezodes();
            case "VILLOGOLAMPA": return uj_demoVillogoLampa();
            case "FUTOFENY": return uj_demoFutofeny();
            default: return uj_demoProgram();
        }
    }

    static List<Sor> uj_demoCsillagDelta() {
        List<Sor> l = new ArrayList<>();

        Sor s1 = new Sor();
        s1.megjegyzes = tr("Fő mágneskapcsoló indítás/leállítás (önfenntartó kapcsolás)");
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.I, 0))));
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.Q, 0))));
        s1.utotag.add(e(false, OpTipus.I, 1));
        s1.kimenet.tipus = KimTipus.TEKERCS;
        s1.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("Csillag–delta átkapcsolási idő (3 s)");
        s2.elotag.add(e(true, OpTipus.Q, 0));
        s2.kimenet.tipus = KimTipus.TON;
        s2.kimenet.idx = 0;
        s2.kimenet.presetMs = 3000;
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("Csillag kontaktor (az átkapcsolási idő lejártáig)");
        s3.elotag.add(e(true, OpTipus.Q, 0));
        s3.elotag.add(e(false, OpTipus.TDN, 0));
        s3.kimenet.tipus = KimTipus.TEKERCS;
        s3.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Delta kontaktor (az átkapcsolási idő után)");
        s4.elotag.add(e(true, OpTipus.Q, 0));
        s4.elotag.add(e(true, OpTipus.TDN, 0));
        s4.kimenet.tipus = KimTipus.TEKERCS;
        s4.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s4);

        return l;
    }

    static List<Sor> uj_demoSzallitoSzalag() {
        List<Sor> l = new ArrayList<>();

        Sor s1 = new Sor();
        s1.megjegyzes = tr("1. szállítószalag indítása/leállítása (önfenntartó kapcsolás)");
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.I, 0))));
        s1.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.Q, 0))));
        s1.utotag.add(e(false, OpTipus.I, 1));
        s1.kimenet.tipus = KimTipus.TEKERCS;
        s1.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("Késleltetés a 2. szalag indítása előtt (2 s)");
        s2.elotag.add(e(true, OpTipus.Q, 0));
        s2.kimenet.tipus = KimTipus.TON;
        s2.kimenet.idx = 0;
        s2.kimenet.presetMs = 2000;
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("2. szállítószalag (2 s késleltetéssel)");
        s3.elotag.add(e(true, OpTipus.Q, 0));
        s3.elotag.add(e(true, OpTipus.TDN, 0));
        s3.utotag.add(e(false, OpTipus.I, 1));
        s3.kimenet.tipus = KimTipus.TEKERCS;
        s3.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Késleltetés a 3. szalag indítása előtt (további 2 s)");
        s4.elotag.add(e(true, OpTipus.Q, 1));
        s4.kimenet.tipus = KimTipus.TON;
        s4.kimenet.idx = 1;
        s4.kimenet.presetMs = 2000;
        l.add(s4);

        Sor s5 = new Sor();
        s5.megjegyzes = tr("3. szállítószalag (4 s késleltetéssel)");
        s5.elotag.add(e(true, OpTipus.Q, 1));
        s5.elotag.add(e(true, OpTipus.TDN, 1));
        s5.utotag.add(e(false, OpTipus.I, 1));
        s5.kimenet.tipus = KimTipus.TEKERCS;
        s5.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s5);

        return l;
    }

    static List<Sor> uj_demoKozlekedesiLampa() {
        List<Sor> l = new ArrayList<>();

        Sor s0 = new Sor();
        s0.megjegyzes = tr("Kezdeti állapot: piros (ha még nincs aktív állapot)");
        s0.elotag.add(e(false, OpTipus.M, 0));
        s0.elotag.add(e(false, OpTipus.M, 1));
        s0.elotag.add(e(false, OpTipus.M, 2));
        s0.kimenet.tipus = KimTipus.SET;
        s0.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s0);

        Sor s1 = new Sor();
        s1.megjegyzes = tr("Piros fázis időzítése (4 s)");
        s1.elotag.add(e(true, OpTipus.M, 0));
        s1.kimenet.tipus = KimTipus.TON;
        s1.kimenet.idx = 0;
        s1.kimenet.presetMs = 4000;
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("Piros → sárga átkapcsolás");
        s2.elotag.add(e(true, OpTipus.M, 0));
        s2.elotag.add(e(true, OpTipus.TDN, 0));
        s2.kimenet.tipus = KimTipus.SET;
        s2.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("Piros → sárga átkapcsolás (piros állapot megszüntetése)");
        s3.elotag.add(e(true, OpTipus.M, 0));
        s3.elotag.add(e(true, OpTipus.TDN, 0));
        s3.kimenet.tipus = KimTipus.RESET;
        s3.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Sárga fázis időzítése (1 s)");
        s4.elotag.add(e(true, OpTipus.M, 1));
        s4.kimenet.tipus = KimTipus.TON;
        s4.kimenet.idx = 1;
        s4.kimenet.presetMs = 1000;
        l.add(s4);

        Sor s5 = new Sor();
        s5.megjegyzes = tr("Sárga → zöld átkapcsolás");
        s5.elotag.add(e(true, OpTipus.M, 1));
        s5.elotag.add(e(true, OpTipus.TDN, 1));
        s5.kimenet.tipus = KimTipus.SET;
        s5.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s5);

        Sor s6 = new Sor();
        s6.megjegyzes = tr("Sárga → zöld átkapcsolás (sárga állapot megszüntetése)");
        s6.elotag.add(e(true, OpTipus.M, 1));
        s6.elotag.add(e(true, OpTipus.TDN, 1));
        s6.kimenet.tipus = KimTipus.RESET;
        s6.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s6);

        Sor s7 = new Sor();
        s7.megjegyzes = tr("Zöld fázis időzítése (3 s)");
        s7.elotag.add(e(true, OpTipus.M, 2));
        s7.kimenet.tipus = KimTipus.TON;
        s7.kimenet.idx = 2;
        s7.kimenet.presetMs = 3000;
        l.add(s7);

        Sor s8 = new Sor();
        s8.megjegyzes = tr("Zöld → piros átkapcsolás (ciklus újrakezdése)");
        s8.elotag.add(e(true, OpTipus.M, 2));
        s8.elotag.add(e(true, OpTipus.TDN, 2));
        s8.kimenet.tipus = KimTipus.SET;
        s8.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s8);

        Sor s9 = new Sor();
        s9.megjegyzes = tr("Zöld → piros átkapcsolás (zöld állapot megszüntetése)");
        s9.elotag.add(e(true, OpTipus.M, 2));
        s9.elotag.add(e(true, OpTipus.TDN, 2));
        s9.kimenet.tipus = KimTipus.RESET;
        s9.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s9);

        Sor s10 = new Sor();
        s10.megjegyzes = tr("Piros lámpa");
        s10.elotag.add(e(true, OpTipus.M, 0));
        s10.kimenet.tipus = KimTipus.TEKERCS;
        s10.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s10);

        Sor s11 = new Sor();
        s11.megjegyzes = tr("Sárga lámpa");
        s11.elotag.add(e(true, OpTipus.M, 1));
        s11.kimenet.tipus = KimTipus.TEKERCS;
        s11.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s11);

        Sor s12 = new Sor();
        s12.megjegyzes = tr("Zöld lámpa");
        s12.elotag.add(e(true, OpTipus.M, 2));
        s12.kimenet.tipus = KimTipus.TEKERCS;
        s12.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s12);

        return l;
    }

    // ================================================================ minta programok: HMI / FBD / GRAFCET

    static HmiWidget hmiElem(HmiTipus t, OpTipus ot, int idx, String felirat, int x, int y, int w, int h) {
        HmiWidget wd = new HmiWidget();
        wd.tipus = t;
        wd.op = new Operandus(ot, idx);
        wd.felirat = felirat;
        wd.x = x; wd.y = y; wd.w = w; wd.h = h;
        return wd;
    }

    static HmiWidget hmiKijelzo(OpTipus ot, int idx, String felirat, int x, int y, int w, int h) {
        HmiWidget wd = hmiElem(HmiTipus.KIJELZO, ot, idx, felirat, x, y, w, h);
        wd.szamkent = true;
        return wd;
    }

    static HmiWidget hmiFelirat(String szoveg, int x, int y, int w, int h) {
        HmiWidget wd = new HmiWidget();
        wd.tipus = HmiTipus.FELIRAT;
        wd.felirat = szoveg;
        wd.x = x; wd.y = y; wd.w = w; wd.h = h;
        return wd;
    }

    static FbdBlock fbdBemenet(int id, OpTipus ot, int idx, String felirat, int x, int y) {
        FbdBlock b = new FbdBlock();
        b.id = id; b.tipus = FbdTipus.BEMENET; b.bemenetSzam = 0;
        b.op = new Operandus(ot, idx); b.felirat = felirat;
        b.x = x; b.y = y;
        return b;
    }

    static FbdBlock fbdKimenet(int id, OpTipus ot, int idx, String felirat, int forras, int x, int y) {
        FbdBlock b = new FbdBlock();
        b.id = id; b.tipus = FbdTipus.KIMENET; b.bemenetSzam = 1; b.forrasId = new int[]{forras};
        b.op = new Operandus(ot, idx); b.felirat = felirat;
        b.x = x; b.y = y;
        return b;
    }

    static FbdBlock fbdLogika(int id, FbdTipus tipus, String felirat, int x, int y, int... forrasok) {
        FbdBlock b = new FbdBlock();
        b.id = id; b.tipus = tipus; b.bemenetSzam = forrasok.length; b.forrasId = forrasok.clone();
        b.felirat = felirat;
        b.x = x; b.y = y;
        return b;
    }

    static int fbdKovetkezoIdSzamol(List<FbdBlock> l) {
        int max = 0;
        for (FbdBlock b : l) max = Math.max(max, b.id);
        return max + 1;
    }

    static GrafcetAkcio grafcetAkcio(GrafcetAkcioTipus tipus, OpTipus ot, int idx) {
        GrafcetAkcio a = new GrafcetAkcio();
        a.tipus = tipus;
        a.cel = new Operandus(ot, idx);
        return a;
    }

    static GrafcetLepes grafcetLepes(int id, boolean kezdeti, String felirat, int x, int y, GrafcetAkcio... akciok) {
        GrafcetLepes l = new GrafcetLepes();
        l.id = id; l.kezdeti = kezdeti; l.felirat = felirat; l.x = x; l.y = y;
        for (GrafcetAkcio a : akciok) l.akciok.add(a);
        return l;
    }

    static GrafcetAtmenet grafcetAtmenet(int id, int forras, int cel, int x, int y, Erintkezo... feltetelek) {
        GrafcetAtmenet t = new GrafcetAtmenet();
        t.id = id; t.forrasLepesek.add(forras); t.celLepesek.add(cel); t.x = x; t.y = y;
        for (Erintkezo e : feltetelek) t.feltetel.add(e);
        return t;
    }

    static GrafcetDiagram grafcetDiagram(List<GrafcetLepes> lepesek, List<GrafcetAtmenet> atmenetek) {
        GrafcetDiagram g = new GrafcetDiagram();
        g.lepesek.addAll(lepesek);
        g.atmenetek.addAll(atmenetek);
        int maxL = 0; for (GrafcetLepes l : lepesek) maxL = Math.max(maxL, l.id);
        int maxT = 0; for (GrafcetAtmenet t : atmenetek) maxT = Math.max(maxT, t.id);
        g.kovetkezoLepesId = maxL + 1;
        g.kovetkezoAtmenetId = maxT + 1;
        return g;
    }

    static List<HmiKepernyo> mintaHmiEpit(String id) {
        List<HmiWidget> w;
        if (id == null) w = uj_demoHmiAlap();
        else switch (id) {
            case "CSILLAGDELTA": w = uj_demoHmiCsillagDelta(); break;
            case "SZALLITOSZALAG": w = uj_demoHmiSzallitoSzalag(); break;
            case "ROBOTCELLA": w = uj_demoHmiRobotcella(); break;
            case "KOZLEKEDESILAMPA": w = uj_demoHmiKozlekedesiLampa(); break;
            case "LIFT": w = uj_demoHmiLift(); break;
            case "UTKERESZTEZODES": w = uj_demoHmiUtkeresztezodes(); break;
            case "VILLOGOLAMPA": w = uj_demoHmiVillogoLampa(); break;
            case "FUTOFENY": w = uj_demoHmiFutofeny(); break;
            default: w = uj_demoHmiAlap();
        }
        HmiKepernyo kep = new HmiKepernyo();
        kep.widgetek = w;
        List<HmiKepernyo> l = new ArrayList<>();
        l.add(kep);
        return l;
    }

    static List<FbdBlock> mintaFbdEpit(String id) {
        if (id == null) return uj_demoFbdAlap();
        switch (id) {
            case "CSILLAGDELTA": return uj_demoFbdCsillagDelta();
            case "SZALLITOSZALAG": return uj_demoFbdSzallitoSzalag();
            case "ROBOTCELLA": return uj_demoFbdRobotcella();
            case "KOZLEKEDESILAMPA": return uj_demoFbdKozlekedesiLampa();
            case "LIFT": return uj_demoFbdLift();
            case "UTKERESZTEZODES": return uj_demoFbdUtkeresztezodes();
            case "VILLOGOLAMPA": return new ArrayList<>();   // nincs FBD rész ebben a mintában
            case "FUTOFENY": return uj_demoFbdFutofeny();
            default: return uj_demoFbdAlap();
        }
    }

    static GrafcetDiagram mintaGrafcetEpit(String id) {
        if (id == null) return uj_demoGrafcetAlap();
        switch (id) {
            case "CSILLAGDELTA": return uj_demoGrafcetCsillagDelta();
            case "SZALLITOSZALAG": return uj_demoGrafcetSzallitoSzalag();
            case "ROBOTCELLA": return uj_demoGrafcetRobotcella();
            case "KOZLEKEDESILAMPA": return uj_demoGrafcetKozlekedesiLampa();
            case "LIFT": return uj_demoGrafcetLift();
            case "UTKERESZTEZODES": return uj_demoGrafcetUtkeresztezodes();
            case "VILLOGOLAMPA": return new GrafcetDiagram();   // nincs GRAFCET rész ebben a mintában
            case "FUTOFENY": return new GrafcetDiagram();   // nincs GRAFCET rész, hogy ne írja felül a futófény Q kimeneteit
            default: return uj_demoGrafcetAlap();
        }
    }

    // ---- Motorindítás (alapértelmezett demó) ----

    static List<HmiWidget> uj_demoHmiAlap() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Motor indítás/leállítás"), 20, 10, 260, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Indítás"), 20, 60, 90, 50));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 1, tr("Leállítás"), 120, 60, 90, 50));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Motor"), 240, 50, 70, 70));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 1, tr("Jelzőlámpa"), 330, 50, 70, 70));
        l.add(hmiKijelzo(OpTipus.TDN, 0, tr("Késleltetés"), 20, 130, 130, 40));
        l.add(hmiKijelzo(OpTipus.CDN, 0, tr("Indítások"), 160, 130, 130, 40));
        return l;
    }

    static List<FbdBlock> uj_demoFbdAlap() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.I, 0, tr("Indítás"), 40, 40));
        l.add(fbdBemenet(2, OpTipus.I, 1, tr("Leállítás"), 40, 140));
        l.add(fbdLogika(3, FbdTipus.SR, tr("Önfenntartó (SR)"), 220, 80, 1, 2));
        l.add(fbdKimenet(4, OpTipus.Q, 3, tr("Motor (FBD demo)"), 3, 400, 80));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetAlap() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Megállva"), 80, 40));
        lepesek.add(grafcetLepes(2, false, tr("Motor fut"), 80, 220, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.Q, 4)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 120, e(true, OpTipus.I, 0)));
        atmenetek.add(grafcetAtmenet(2, 2, 1, 240, 120, e(true, OpTipus.I, 1)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Csillag-delta motorindítás ----

    static List<HmiWidget> uj_demoHmiCsillagDelta() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Csillag-delta motorindítás"), 20, 10, 280, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Indítás"), 20, 60, 90, 50));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 1, tr("Leállítás"), 120, 60, 90, 50));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Fő"), 240, 50, 70, 70));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 1, tr("Csillag"), 330, 50, 70, 70));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 2, tr("Delta"), 420, 50, 70, 70));
        l.add(hmiKijelzo(OpTipus.TDN, 0, tr("Átkapcsolási idő"), 20, 130, 160, 40));
        return l;
    }

    static List<FbdBlock> uj_demoFbdCsillagDelta() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.Q, 0, tr("Fő (figyelő)"), 40, 40));
        l.add(fbdBemenet(2, OpTipus.TDN, 0, tr("Átkapcsolási idő (figyelő)"), 40, 140));
        l.add(fbdLogika(3, FbdTipus.NOT, tr("Még nem telt le"), 220, 140, 2));
        l.add(fbdLogika(4, FbdTipus.AND, tr("Csillag ellenőrzés"), 400, 90, 1, 3));
        l.add(fbdKimenet(5, OpTipus.Q, 3, tr("Csillag jelzés (FBD demo)"), 4, 580, 90));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetCsillagDelta() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Állva"), 80, 40));
        lepesek.add(grafcetLepes(2, false, tr("Csillag fázis"), 80, 160, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.Q, 4)));
        lepesek.add(grafcetLepes(3, false, tr("Delta fázis"), 80, 280, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.Q, 5)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 110, e(true, OpTipus.I, 0)));
        atmenetek.add(grafcetAtmenet(2, 2, 3, 80, 230, e(true, OpTipus.TDN, 0)));
        atmenetek.add(grafcetAtmenet(3, 2, 1, 240, 110, e(true, OpTipus.I, 1)));
        atmenetek.add(grafcetAtmenet(4, 3, 1, 240, 230, e(true, OpTipus.I, 1)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Szállítószalag sorrendindítás ----

    static List<HmiWidget> uj_demoHmiSzallitoSzalag() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Szállítószalag sorrendindítás"), 20, 10, 320, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Indítás"), 20, 60, 90, 50));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 1, tr("Leállítás"), 120, 60, 90, 50));

        // gyártósor: 1. szalag -> Fanuc robot átadás -> 2. szalag -> Kuka robot átadás -> 3. szalag,
        // mindegyik elem a hozzá tartozó szalag kimenetéhez (Q0/Q1/Q2) kötve, ezért a szekvenciális
        // indítási késleltetésekkel pontosan együtt mozognak / animálódnak.
        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 0, tr("1. szalag"), 20, 190, 170, 40));
        l.add(hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 0, "", 200, 197, 26, 26));
        l.add(hmiElem(HmiTipus.ROBOT_FANUC, OpTipus.Q, 1, "Fanuc", 240, 50, 110, 140));
        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 1, tr("2. szalag"), 370, 190, 170, 40));
        l.add(hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 1, "", 550, 197, 26, 26));
        l.add(hmiElem(HmiTipus.ROBOT_KUKA, OpTipus.Q, 2, "Kuka", 590, 50, 110, 140));
        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 2, tr("3. szalag"), 720, 190, 170, 40));
        l.add(hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 2, "", 900, 197, 26, 26));

        l.add(hmiKijelzo(OpTipus.TDN, 0, tr("1. késleltetés"), 20, 260, 160, 40));
        l.add(hmiKijelzo(OpTipus.TDN, 1, tr("2. késleltetés"), 190, 260, 160, 40));
        return l;
    }

    static List<FbdBlock> uj_demoFbdSzallitoSzalag() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.Q, 0, tr("1. szalag (figyelő)"), 40, 40));
        l.add(fbdBemenet(2, OpTipus.Q, 1, tr("2. szalag (figyelő)"), 40, 140));
        l.add(fbdBemenet(3, OpTipus.Q, 2, tr("3. szalag (figyelő)"), 40, 240));
        l.add(fbdLogika(4, FbdTipus.AND, tr("Mind a 3 szalag fut"), 220, 140, 1, 2, 3));
        l.add(fbdKimenet(5, OpTipus.Q, 3, tr("Teljes sor üzemel (FBD demo)"), 4, 400, 140));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetSzallitoSzalag() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Állva"), 80, 40));
        lepesek.add(grafcetLepes(2, false, tr("Sor fut"), 80, 220, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.Q, 4)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 120, e(true, OpTipus.I, 0)));
        atmenetek.add(grafcetAtmenet(2, 2, 1, 240, 120, e(true, OpTipus.I, 1)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Robotcella (motorblokk be- és kirakás) ----
    // Ugyanazt a szekvenciális szalagindítási logikát használja, mint a "Szállítószalag sorrendindítás"
    // minta (1-2-3. szalag önfenntartó indítása, egyenként 2 s késleltetéssel) – csak a HMI-kép más:
    // itt a két robotkar és a négy, eltérő forgatású érzékelő mutatja be az új HMI-elemeket.

    static List<Sor> uj_demoRobotcella() { return uj_demoSzallitoSzalag(); }

    static List<HmiWidget> uj_demoHmiRobotcella() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Robotcella (motorblokk be- és kirakás)"), 20, 10, 380, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Indítás"), 20, 60, 90, 50));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 1, tr("Leállítás"), 120, 60, 90, 50));

        // 270°-ra forgatott érzékelő a beérkező szalag elején (alkatrész jelenléte)
        HmiWidget szenzorD = hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 0, "270°", 20, 140, 26, 26);
        szenzorD.forgatas = 270;
        l.add(szenzorD);

        // gyártósor: 1. szalag -> Fanuc robot átadás -> 2. szalag -> Kuka robot átadás (tükrözve) -> 3. szalag
        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 0, tr("1. szalag"), 20, 190, 170, 40));

        HmiWidget szenzorA = hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 0, "0°", 200, 197, 26, 26);
        l.add(szenzorA); // alapállás (0°) – nincs elforgatva

        l.add(hmiElem(HmiTipus.ROBOT_FANUC, OpTipus.Q, 1, "Fanuc", 240, 50, 110, 140));
        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 1, tr("2. szalag"), 370, 190, 170, 40));

        HmiWidget szenzorB = hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 1, "90°", 550, 197, 26, 26);
        szenzorB.forgatas = 90;
        l.add(szenzorB);

        HmiWidget kuka = hmiElem(HmiTipus.ROBOT_KUKA, OpTipus.Q, 2, "Kuka", 590, 50, 110, 140);
        kuka.tukrozve = true;   // tükrözve: a sor másik oldala felé nyúl ki, mint a Fanuc
        l.add(kuka);

        l.add(hmiElem(HmiTipus.SZALLITOSZALAG, OpTipus.Q, 2, tr("3. szalag"), 720, 190, 170, 40));

        HmiWidget szenzorC = hmiElem(HmiTipus.SZENZOR, OpTipus.Q, 2, "180°", 900, 197, 26, 26);
        szenzorC.forgatas = 180;
        l.add(szenzorC);

        l.add(hmiKijelzo(OpTipus.TDN, 0, tr("1. késleltetés"), 20, 260, 160, 40));
        l.add(hmiKijelzo(OpTipus.TDN, 1, tr("2. késleltetés"), 190, 260, 160, 40));
        l.add(hmiFelirat(tr("Az érzékelők 0° / 90° / 180° / 270° forgatásban, a Kuka kar tükrözve látható."), 20, 315, 520, 20));
        return l;
    }

    static List<FbdBlock> uj_demoFbdRobotcella() { return uj_demoFbdSzallitoSzalag(); }
    static GrafcetDiagram uj_demoGrafcetRobotcella() { return uj_demoGrafcetSzallitoSzalag(); }

    // ---- Közlekedési lámpa (állapotgép) ----

    static List<HmiWidget> uj_demoHmiKozlekedesiLampa() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Közlekedési lámpa"), 20, 10, 220, 28));
        HmiWidget piros = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Piros"), 20, 60, 70, 70);
        piros.szinBe = new Color(220, 30, 30);
        l.add(piros);
        HmiWidget sarga = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 1, tr("Sárga"), 110, 60, 70, 70);
        sarga.szinBe = new Color(230, 200, 0);
        l.add(sarga);
        HmiWidget zold = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 2, tr("Zöld"), 200, 60, 70, 70);
        zold.szinBe = new Color(30, 180, 60);
        l.add(zold);
        return l;
    }

    static List<FbdBlock> uj_demoFbdKozlekedesiLampa() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.Q, 1, tr("Sárga (figyelő)"), 40, 40));
        l.add(fbdLogika(2, FbdTipus.R_TRIG, tr("Felfutás-érzékelés"), 220, 40, 1));
        l.add(fbdKimenet(3, OpTipus.Q, 3, tr("Sárga felfutás impulzus (FBD demo)"), 2, 400, 40));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetKozlekedesiLampa() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Piros (GRAFCET demo)"), 80, 40, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 3)));
        lepesek.add(grafcetLepes(2, false, tr("Sárga (GRAFCET demo)"), 80, 160, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 4)));
        lepesek.add(grafcetLepes(3, false, tr("Zöld (GRAFCET demo)"), 80, 280, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 5)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 110, e(true, OpTipus.TDN, 0)));
        atmenetek.add(grafcetAtmenet(2, 2, 3, 80, 230, e(true, OpTipus.TDN, 1)));
        atmenetek.add(grafcetAtmenet(3, 3, 1, 240, 160, e(true, OpTipus.TDN, 2)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Lift (2 szintes) ----

    static List<Sor> uj_demoLift() {
        List<Sor> l = new ArrayList<>();

        Sor s0 = new Sor();
        s0.megjegyzes = tr("Kezdeti állapot: földszinten, ajtó zárva (ha még nincs aktív állapot)");
        s0.elotag.add(e(false, OpTipus.M, 0));
        s0.elotag.add(e(false, OpTipus.M, 1));
        s0.elotag.add(e(false, OpTipus.M, 2));
        s0.elotag.add(e(false, OpTipus.M, 3));
        s0.elotag.add(e(false, OpTipus.M, 4));
        s0.elotag.add(e(false, OpTipus.M, 5));
        s0.kimenet.tipus = KimTipus.SET;
        s0.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s0);

        Sor s1 = new Sor();
        s1.megjegyzes = tr("Földszinten állva: hívás emeletre -> indulás felfelé");
        s1.elotag.add(e(true, OpTipus.M, 0));
        s1.elotag.add(e(true, OpTipus.I, 1));
        s1.kimenet.tipus = KimTipus.SET;
        s1.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("Földszinten állva: indulás felfelé (állapotváltás)");
        s2.elotag.add(e(true, OpTipus.M, 0));
        s2.elotag.add(e(true, OpTipus.I, 1));
        s2.kimenet.tipus = KimTipus.RESET;
        s2.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("Földszinten állva: hívás földszintre -> ajtó nyitása");
        s3.elotag.add(e(true, OpTipus.M, 0));
        s3.elotag.add(e(true, OpTipus.I, 0));
        s3.kimenet.tipus = KimTipus.SET;
        s3.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Földszinten állva: ajtó nyitása (állapotváltás)");
        s4.elotag.add(e(true, OpTipus.M, 0));
        s4.elotag.add(e(true, OpTipus.I, 0));
        s4.kimenet.tipus = KimTipus.RESET;
        s4.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s4);

        Sor s5 = new Sor();
        s5.megjegyzes = tr("Ajtónyitás ideje földszinten (3 s)");
        s5.elotag.add(e(true, OpTipus.M, 1));
        s5.kimenet.tipus = KimTipus.TON;
        s5.kimenet.idx = 0;
        s5.kimenet.presetMs = 3000;
        l.add(s5);

        Sor s6 = new Sor();
        s6.megjegyzes = tr("Ajtó becsukódik földszinten, vissza alapállapotba");
        s6.elotag.add(e(true, OpTipus.M, 1));
        s6.elotag.add(e(true, OpTipus.TDN, 0));
        s6.kimenet.tipus = KimTipus.SET;
        s6.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s6);

        Sor s7 = new Sor();
        s7.megjegyzes = tr("Ajtó becsukódik földszinten (állapotváltás)");
        s7.elotag.add(e(true, OpTipus.M, 1));
        s7.elotag.add(e(true, OpTipus.TDN, 0));
        s7.kimenet.tipus = KimTipus.RESET;
        s7.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s7);

        Sor s8 = new Sor();
        s8.megjegyzes = tr("Felfelé haladás ideje (2,5 s)");
        s8.elotag.add(e(true, OpTipus.M, 2));
        s8.kimenet.tipus = KimTipus.TON;
        s8.kimenet.idx = 1;
        s8.kimenet.presetMs = 2500;
        l.add(s8);

        Sor s9 = new Sor();
        s9.megjegyzes = tr("Megérkezés az emeletre: ajtó automatikus nyitása");
        s9.elotag.add(e(true, OpTipus.M, 2));
        s9.elotag.add(e(true, OpTipus.TDN, 1));
        s9.kimenet.tipus = KimTipus.SET;
        s9.kimenet.cel = new Operandus(OpTipus.M, 4);
        l.add(s9);

        Sor s10 = new Sor();
        s10.megjegyzes = tr("Megérkezés az emeletre (állapotváltás)");
        s10.elotag.add(e(true, OpTipus.M, 2));
        s10.elotag.add(e(true, OpTipus.TDN, 1));
        s10.kimenet.tipus = KimTipus.RESET;
        s10.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s10);

        Sor s11 = new Sor();
        s11.megjegyzes = tr("Emeleten állva: hívás földszintre -> indulás lefelé");
        s11.elotag.add(e(true, OpTipus.M, 3));
        s11.elotag.add(e(true, OpTipus.I, 0));
        s11.kimenet.tipus = KimTipus.SET;
        s11.kimenet.cel = new Operandus(OpTipus.M, 5);
        l.add(s11);

        Sor s12 = new Sor();
        s12.megjegyzes = tr("Emeleten állva: indulás lefelé (állapotváltás)");
        s12.elotag.add(e(true, OpTipus.M, 3));
        s12.elotag.add(e(true, OpTipus.I, 0));
        s12.kimenet.tipus = KimTipus.RESET;
        s12.kimenet.cel = new Operandus(OpTipus.M, 3);
        l.add(s12);

        Sor s13 = new Sor();
        s13.megjegyzes = tr("Emeleten állva: hívás emeletre -> ajtó nyitása");
        s13.elotag.add(e(true, OpTipus.M, 3));
        s13.elotag.add(e(true, OpTipus.I, 1));
        s13.kimenet.tipus = KimTipus.SET;
        s13.kimenet.cel = new Operandus(OpTipus.M, 4);
        l.add(s13);

        Sor s14 = new Sor();
        s14.megjegyzes = tr("Emeleten állva: ajtó nyitása (állapotváltás)");
        s14.elotag.add(e(true, OpTipus.M, 3));
        s14.elotag.add(e(true, OpTipus.I, 1));
        s14.kimenet.tipus = KimTipus.RESET;
        s14.kimenet.cel = new Operandus(OpTipus.M, 3);
        l.add(s14);

        Sor s15 = new Sor();
        s15.megjegyzes = tr("Ajtónyitás ideje emeleten (3 s)");
        s15.elotag.add(e(true, OpTipus.M, 4));
        s15.kimenet.tipus = KimTipus.TON;
        s15.kimenet.idx = 2;
        s15.kimenet.presetMs = 3000;
        l.add(s15);

        Sor s16 = new Sor();
        s16.megjegyzes = tr("Ajtó becsukódik emeleten, vissza alapállapotba");
        s16.elotag.add(e(true, OpTipus.M, 4));
        s16.elotag.add(e(true, OpTipus.TDN, 2));
        s16.kimenet.tipus = KimTipus.SET;
        s16.kimenet.cel = new Operandus(OpTipus.M, 3);
        l.add(s16);

        Sor s17 = new Sor();
        s17.megjegyzes = tr("Ajtó becsukódik emeleten (állapotváltás)");
        s17.elotag.add(e(true, OpTipus.M, 4));
        s17.elotag.add(e(true, OpTipus.TDN, 2));
        s17.kimenet.tipus = KimTipus.RESET;
        s17.kimenet.cel = new Operandus(OpTipus.M, 4);
        l.add(s17);

        Sor s18 = new Sor();
        s18.megjegyzes = tr("Lefelé haladás ideje (2,5 s)");
        s18.elotag.add(e(true, OpTipus.M, 5));
        s18.kimenet.tipus = KimTipus.TON;
        s18.kimenet.idx = 3;
        s18.kimenet.presetMs = 2500;
        l.add(s18);

        Sor s19 = new Sor();
        s19.megjegyzes = tr("Megérkezés a földszintre: ajtó automatikus nyitása");
        s19.elotag.add(e(true, OpTipus.M, 5));
        s19.elotag.add(e(true, OpTipus.TDN, 3));
        s19.kimenet.tipus = KimTipus.SET;
        s19.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s19);

        Sor s20 = new Sor();
        s20.megjegyzes = tr("Megérkezés a földszintre (állapotváltás)");
        s20.elotag.add(e(true, OpTipus.M, 5));
        s20.elotag.add(e(true, OpTipus.TDN, 3));
        s20.kimenet.tipus = KimTipus.RESET;
        s20.kimenet.cel = new Operandus(OpTipus.M, 5);
        l.add(s20);

        Sor s21 = new Sor();
        s21.megjegyzes = tr("Jelzés: a kabin a földszinten van");
        s21.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 0))));
        s21.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 1))));
        s21.kimenet.tipus = KimTipus.TEKERCS;
        s21.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s21);

        Sor s22 = new Sor();
        s22.megjegyzes = tr("Jelzés: a kabin az emeleten van");
        s22.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 3))));
        s22.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 4))));
        s22.kimenet.tipus = KimTipus.TEKERCS;
        s22.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s22);

        Sor s23 = new Sor();
        s23.megjegyzes = tr("Ajtó nyitva jelzés (a HMI ajtó-animációnak)");
        s23.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 1))));
        s23.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 4))));
        s23.kimenet.tipus = KimTipus.TEKERCS;
        s23.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s23);

        Sor s24 = new Sor();
        s24.megjegyzes = tr("Jelzés: a kabin mozgásban van");
        s24.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 2))));
        s24.agak.add(new ArrayList<>(Collections.singletonList(e(true, OpTipus.M, 5))));
        s24.kimenet.tipus = KimTipus.TEKERCS;
        s24.kimenet.cel = new Operandus(OpTipus.Q, 3);
        l.add(s24);

        return l;
    }

    static List<HmiWidget> uj_demoHmiLift() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Lift (2 szintes)"), 20, 10, 260, 28));
        HmiWidget hivasFold = hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Hívás: földszint"), 20, 60, 150, 50);
        hivasFold.gombMod = GombMod.PILLANAT;
        l.add(hivasFold);
        HmiWidget hivasEmelet = hmiElem(HmiTipus.GOMB, OpTipus.I, 1, tr("Hívás: emelet"), 20, 120, 150, 50);
        hivasEmelet.gombMod = GombMod.PILLANAT;
        l.add(hivasEmelet);
        l.add(hmiElem(HmiTipus.TEGLALAP, OpTipus.I, 0, "", 250, 50, 110, 220));
        l.add(hmiFelirat(tr("Emelet"), 260, 55, 80, 20));
        l.add(hmiFelirat(tr("Földszint"), 255, 250, 90, 20));
        l.add(hmiElem(HmiTipus.AJTO, OpTipus.Q, 2, tr("Ajtó"), 275, 120, 60, 90));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 1, tr("Emeleten"), 390, 60, 50, 50));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Földszinten"), 390, 230, 50, 50));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 3, tr("Mozgásban"), 460, 140, 50, 50));
        return l;
    }

    static List<FbdBlock> uj_demoFbdLift() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.Q, 2, tr("Ajtó nyitva (figyelő)"), 40, 40));
        l.add(fbdBemenet(2, OpTipus.Q, 3, tr("Mozgásban (figyelő)"), 40, 140));
        l.add(fbdLogika(3, FbdTipus.AND, tr("Egyszerre? (hibajelzés)"), 220, 90, 1, 2));
        l.add(fbdKimenet(4, OpTipus.Q, 4, tr("Biztonsági hiba (FBD demo)"), 3, 400, 90));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetLift() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Földszinten (GRAFCET demo)"), 80, 40, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 6)));
        lepesek.add(grafcetLepes(2, false, tr("Emeleten (GRAFCET demo)"), 80, 180, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 7)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 110, e(true, OpTipus.Q, 1)));
        atmenetek.add(grafcetAtmenet(2, 2, 1, 240, 110, e(true, OpTipus.Q, 0)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Útkereszteződés gyalogátkelővel ----

    static List<Sor> uj_demoUtkeresztezodes() {
        List<Sor> l = new ArrayList<>();

        Sor s0 = new Sor();
        s0.megjegyzes = tr("Kezdeti állapot: piros (ha még nincs aktív állapot)");
        s0.elotag.add(e(false, OpTipus.M, 0));
        s0.elotag.add(e(false, OpTipus.M, 1));
        s0.elotag.add(e(false, OpTipus.M, 2));
        s0.kimenet.tipus = KimTipus.SET;
        s0.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s0);

        Sor s1 = new Sor();
        s1.megjegyzes = tr("Piros fázis időzítése (5 s)");
        s1.elotag.add(e(true, OpTipus.M, 0));
        s1.kimenet.tipus = KimTipus.TON;
        s1.kimenet.idx = 0;
        s1.kimenet.presetMs = 5000;
        l.add(s1);

        Sor s2 = new Sor();
        s2.megjegyzes = tr("Piros → sárga átkapcsolás");
        s2.elotag.add(e(true, OpTipus.M, 0));
        s2.elotag.add(e(true, OpTipus.TDN, 0));
        s2.kimenet.tipus = KimTipus.SET;
        s2.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s2);

        Sor s3 = new Sor();
        s3.megjegyzes = tr("Piros → sárga átkapcsolás (piros állapot megszüntetése)");
        s3.elotag.add(e(true, OpTipus.M, 0));
        s3.elotag.add(e(true, OpTipus.TDN, 0));
        s3.kimenet.tipus = KimTipus.RESET;
        s3.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s3);

        Sor s4 = new Sor();
        s4.megjegyzes = tr("Sárga fázis időzítése (1 s)");
        s4.elotag.add(e(true, OpTipus.M, 1));
        s4.kimenet.tipus = KimTipus.TON;
        s4.kimenet.idx = 1;
        s4.kimenet.presetMs = 1000;
        l.add(s4);

        Sor s5 = new Sor();
        s5.megjegyzes = tr("Sárga → zöld átkapcsolás");
        s5.elotag.add(e(true, OpTipus.M, 1));
        s5.elotag.add(e(true, OpTipus.TDN, 1));
        s5.kimenet.tipus = KimTipus.SET;
        s5.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s5);

        Sor s6 = new Sor();
        s6.megjegyzes = tr("Sárga → zöld átkapcsolás (sárga állapot megszüntetése)");
        s6.elotag.add(e(true, OpTipus.M, 1));
        s6.elotag.add(e(true, OpTipus.TDN, 1));
        s6.kimenet.tipus = KimTipus.RESET;
        s6.kimenet.cel = new Operandus(OpTipus.M, 1);
        l.add(s6);

        Sor s7 = new Sor();
        s7.megjegyzes = tr("Zöld fázis időzítése (4 s)");
        s7.elotag.add(e(true, OpTipus.M, 2));
        s7.kimenet.tipus = KimTipus.TON;
        s7.kimenet.idx = 2;
        s7.kimenet.presetMs = 4000;
        l.add(s7);

        Sor s8 = new Sor();
        s8.megjegyzes = tr("Zöld → piros átkapcsolás (ciklus újrakezdése)");
        s8.elotag.add(e(true, OpTipus.M, 2));
        s8.elotag.add(e(true, OpTipus.TDN, 2));
        s8.kimenet.tipus = KimTipus.SET;
        s8.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(s8);

        Sor s9 = new Sor();
        s9.megjegyzes = tr("Zöld → piros átkapcsolás (zöld állapot megszüntetése)");
        s9.elotag.add(e(true, OpTipus.M, 2));
        s9.elotag.add(e(true, OpTipus.TDN, 2));
        s9.kimenet.tipus = KimTipus.RESET;
        s9.kimenet.cel = new Operandus(OpTipus.M, 2);
        l.add(s9);

        Sor s10 = new Sor();
        s10.megjegyzes = tr("Piros lámpa (autós)");
        s10.elotag.add(e(true, OpTipus.M, 0));
        s10.kimenet.tipus = KimTipus.TEKERCS;
        s10.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s10);

        Sor s11 = new Sor();
        s11.megjegyzes = tr("Sárga lámpa (autós)");
        s11.elotag.add(e(true, OpTipus.M, 1));
        s11.kimenet.tipus = KimTipus.TEKERCS;
        s11.kimenet.cel = new Operandus(OpTipus.Q, 1);
        l.add(s11);

        Sor s12 = new Sor();
        s12.megjegyzes = tr("Zöld lámpa (autós)");
        s12.elotag.add(e(true, OpTipus.M, 2));
        s12.kimenet.tipus = KimTipus.TEKERCS;
        s12.kimenet.cel = new Operandus(OpTipus.Q, 2);
        l.add(s12);

        Sor s13 = new Sor();
        s13.megjegyzes = tr("Gyalogos hívásgomb regisztrálása (bármikor nyomható)");
        s13.elotag.add(e(true, OpTipus.I, 0));
        s13.kimenet.tipus = KimTipus.SET;
        s13.kimenet.cel = new Operandus(OpTipus.M, 6);
        l.add(s13);

        Sor s14 = new Sor();
        s14.megjegyzes = tr("Gyalogos zöld: piros autós fázisban, ha volt hívás");
        s14.elotag.add(e(true, OpTipus.M, 0));
        s14.elotag.add(e(true, OpTipus.M, 6));
        s14.kimenet.tipus = KimTipus.TEKERCS;
        s14.kimenet.cel = new Operandus(OpTipus.Q, 4);
        l.add(s14);

        Sor s15 = new Sor();
        s15.megjegyzes = tr("Gyalogos piros: amikor nincs gyalogos zöld (negált kimenet)");
        s15.elotag.add(e(true, OpTipus.Q, 4));
        s15.kimenet.tipus = KimTipus.TEKERCS;
        s15.kimenet.cel = new Operandus(OpTipus.Q, 3);
        s15.kimenet.negalt = true;
        l.add(s15);

        Sor s16 = new Sor();
        s16.megjegyzes = tr("Kiszolgált gyalogos hívás nyugtázása");
        s16.elotag.add(e(true, OpTipus.Q, 4));
        s16.kimenet.tipus = KimTipus.RESET;
        s16.kimenet.cel = new Operandus(OpTipus.M, 6);
        l.add(s16);

        return l;
    }

    static List<HmiWidget> uj_demoHmiUtkeresztezodes() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Útkereszteződés gyalogátkelővel"), 20, 10, 300, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Gyalogos hívás"), 20, 170, 140, 50));
        l.add(hmiElem(HmiTipus.TEGLALAP, OpTipus.I, 0, "", 20, 70, 300, 80));
        l.add(hmiElem(HmiTipus.ZEBRA, OpTipus.I, 0, "", 140, 70, 60, 80));
        HmiWidget piros = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Piros"), 340, 60, 45, 45);
        piros.szinBe = new Color(220, 30, 30);
        l.add(piros);
        HmiWidget sarga = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 1, tr("Sárga"), 340, 112, 45, 45);
        sarga.szinBe = new Color(230, 200, 0);
        l.add(sarga);
        HmiWidget zold = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 2, tr("Zöld"), 340, 164, 45, 45);
        zold.szinBe = new Color(30, 180, 60);
        l.add(zold);
        HmiWidget gyalogPiros = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 3, tr("Gyalogos piros"), 420, 80, 40, 40);
        gyalogPiros.szinBe = new Color(220, 30, 30);
        l.add(gyalogPiros);
        HmiWidget gyalogZold = hmiElem(HmiTipus.LAMPA, OpTipus.Q, 4, tr("Gyalogos zöld"), 420, 140, 40, 40);
        gyalogZold.szinBe = new Color(30, 180, 60);
        l.add(gyalogZold);
        return l;
    }

    static List<FbdBlock> uj_demoFbdUtkeresztezodes() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.M, 6, tr("Gyalogos hívás várakozik (figyelő)"), 40, 40));
        l.add(fbdLogika(2, FbdTipus.F_TRIG, tr("Lefutás-érzékelés (hívás kiszolgálva)"), 220, 40, 1));
        l.add(fbdKimenet(3, OpTipus.Q, 5, tr("Hívás kiszolgálva impulzus (FBD demo)"), 2, 400, 40));
        return l;
    }

    static GrafcetDiagram uj_demoGrafcetUtkeresztezodes() {
        List<GrafcetLepes> lepesek = new ArrayList<>();
        List<GrafcetAtmenet> atmenetek = new ArrayList<>();
        lepesek.add(grafcetLepes(1, true, tr("Gyalogos tilos (GRAFCET demo)"), 80, 40, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 7)));
        lepesek.add(grafcetLepes(2, false, tr("Gyalogos szabad (GRAFCET demo)"), 80, 180, grafcetAkcio(GrafcetAkcioTipus.FOLYAMATOS, OpTipus.M, 8)));
        atmenetek.add(grafcetAtmenet(1, 1, 2, 80, 110, e(true, OpTipus.Q, 4)));
        atmenetek.add(grafcetAtmenet(2, 2, 1, 240, 110, e(true, OpTipus.Q, 3)));
        return grafcetDiagram(lepesek, atmenetek);
    }

    // ---- Villogó lámpa (óraimpulzus / clock bit legegyszerűbb bemutatója) ----
    //
    // M15 = órajel (az Óraimpulzusok szerkesztőben beállított clock bit célja, 1 Hz).
    // Egyetlen rönk: a Q0 kimenet (és a rá kötött HMI lámpa) közvetlenül az M15 kontaktust
    // követi - semmi más nem írja M15-öt vagy Q0-t, így tisztán, zavaró mellékhatás nélkül
    // látszik az óraimpulzus hatása.

    static List<Sor> uj_demoVillogoLampa() {
        List<Sor> l = new ArrayList<>();
        Sor s = new Sor();
        s.megjegyzes = tr("A Q0 lámpa közvetlenül az órajel (M15) állapotát követi");
        s.elotag.add(e(true, OpTipus.M, 15));
        s.kimenet.tipus = KimTipus.TEKERCS;
        s.kimenet.cel = new Operandus(OpTipus.Q, 0);
        l.add(s);
        return l;
    }

    static List<HmiWidget> uj_demoHmiVillogoLampa() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Villogó lámpa (óraimpulzussal)"), 20, 10, 320, 28));
        l.add(hmiElem(HmiTipus.LAMPA, OpTipus.Q, 0, tr("Lámpa"), 40, 70, 100, 100));
        return l;
    }

    // ---- Futófény (óraimpulzus / clock bit bemutató program) ----
    //
    // M15 = órajel (az Óraimpulzusok szerkesztőben beállított clock bit célja, 1 Hz).
    // M14 = lépésimpulzus: a M15 felfutásakor FBD R_TRIG blokkal előállított, 1 scan-ciklus
    //       hosszú impulzus - ez mozdítja egyet a futófényt minden órajel-periódusban.
    // M13 = lépés-zár: megakadályozza, hogy egy impulzus alatt a lánc egyszerre több pozíciót
    //       is továbbléptessen (az aktuális pozíció léptetésekor azonnal zárol, a zár csak a
    //       következő impulzusig oldódik fel).
    // M0..M7 = a futófény aktuális pozíciója, Q0..Q7 = a hozzá tartozó kimeneti lámpák.
    // I0 = a futás engedélyezése (HMI-n "Futás" gomb / I/O panelen I0.0).

    static List<Sor> uj_demoFutofeny() {
        List<Sor> l = new ArrayList<>();

        Sor zar = new Sor();
        zar.megjegyzes = tr("Lépés-zár feloldása két impulzus között (felkészülés a következő lépésre)");
        zar.elotag.add(e(false, OpTipus.M, 14));
        zar.kimenet.tipus = KimTipus.RESET;
        zar.kimenet.cel = new Operandus(OpTipus.M, 13);
        l.add(zar);

        Sor init = new Sor();
        init.megjegyzes = tr("Kezdeti pozíció beállítása (ha még semelyik sem aktív)");
        for (int k = 0; k < 8; k++) init.elotag.add(e(false, OpTipus.M, k));
        init.kimenet.tipus = KimTipus.SET;
        init.kimenet.cel = new Operandus(OpTipus.M, 0);
        l.add(init);

        for (int i = 0; i < 8; i++) {
            int kov = (i + 1) % 8;

            // "lep" és "zarBe" ugyanazt a (még nem módosított) M(i)/M13 állapotot nézi,
            // ezért ez a kettő pontosan egyszer sül el impulzusonként - a lezárás (M13)
            // ezután már blokkolja a lánc többi (később kiértékelt) tagját ugyanebben a
            // scan-ciklusban, így nem futhat át egy impulzus alatt több pozíción is.
            Sor lep = new Sor();
            lep.megjegyzes = tr("Pozíció") + " " + i + " -> " + kov + ": " + tr("lépés az órajel impulzusára");
            lep.elotag.add(e(true, OpTipus.M, i));
            lep.elotag.add(e(true, OpTipus.I, 0));
            lep.elotag.add(e(true, OpTipus.M, 14));
            lep.elotag.add(e(false, OpTipus.M, 13));
            lep.kimenet.tipus = KimTipus.SET;
            lep.kimenet.cel = new Operandus(OpTipus.M, kov);
            l.add(lep);

            Sor zarBe = new Sor();
            zarBe.megjegyzes = tr("Lépés-zár beállítása (impulzusonként csak egy lépés)");
            zarBe.elotag.add(e(true, OpTipus.M, i));
            zarBe.elotag.add(e(true, OpTipus.I, 0));
            zarBe.elotag.add(e(true, OpTipus.M, 14));
            zarBe.elotag.add(e(false, OpTipus.M, 13));
            zarBe.kimenet.tipus = KimTipus.SET;
            zarBe.kimenet.cel = new Operandus(OpTipus.M, 13);
            l.add(zarBe);

            // A régi pozíció törlése nem az impulzusra/zárra figyel (azokra már a fentiek
            // figyelnek), hanem közvetlenül arra, hogy M(i) és a frissen beállított M(kov)
            // egyszerre aktív - ez csak az átmenet pillanatában fordulhat elő, egyértelműen
            // csak a valódi "régi" pozíciót jelölve ki (a következő pozíció M(kov+1) bitje
            // ekkor még hamis, így nem keverhető össze vele).
            Sor torol = new Sor();
            torol.megjegyzes = tr("Pozíció") + " " + i + " " + tr("törlése a lépés után");
            torol.elotag.add(e(true, OpTipus.M, i));
            torol.elotag.add(e(true, OpTipus.M, kov));
            torol.kimenet.tipus = KimTipus.RESET;
            torol.kimenet.cel = new Operandus(OpTipus.M, i);
            l.add(torol);
        }

        for (int i = 0; i < 8; i++) {
            Sor ki = new Sor();
            ki.megjegyzes = tr("Kimeneti lámpa") + " Q" + i;
            ki.elotag.add(e(true, OpTipus.M, i));
            ki.kimenet.tipus = KimTipus.TEKERCS;
            ki.kimenet.cel = new Operandus(OpTipus.Q, i);
            l.add(ki);
        }

        return l;
    }

    static List<HmiWidget> uj_demoHmiFutofeny() {
        List<HmiWidget> l = new ArrayList<>();
        l.add(hmiFelirat(tr("Futófény (óraimpulzussal vezérelve)"), 20, 10, 360, 28));
        l.add(hmiElem(HmiTipus.GOMB, OpTipus.I, 0, tr("Futás"), 20, 60, 100, 50));
        for (int i = 0; i < 8; i++) {
            HmiWidget lampa = hmiElem(HmiTipus.LAMPA, OpTipus.Q, i, "Q" + i, 150 + i * 70, 60, 60, 60);
            lampa.szinBe = new Color(255, 160, 0);
            l.add(lampa);
        }
        return l;
    }

    static List<FbdBlock> uj_demoFbdFutofeny() {
        List<FbdBlock> l = new ArrayList<>();
        l.add(fbdBemenet(1, OpTipus.M, 15, tr("Órajel (figyelő)"), 40, 40));
        l.add(fbdLogika(2, FbdTipus.R_TRIG, tr("Felfutás-érzékelés"), 220, 40, 1));
        l.add(fbdKimenet(3, OpTipus.M, 14, tr("Lépésimpulzus (FBD demo)"), 2, 400, 40));
        return l;
    }

    /** A minta programokhoz tartozó alapértelmezett óraimpulzus (clock bit) beállítások; a többi mintánál nincs ilyen. */
    static List<OraImpulzus> mintaOraImpulzusEpit(String id) {
        List<OraImpulzus> l = new ArrayList<>();
        if ("FUTOFENY".equals(id) || "VILLOGOLAMPA".equals(id)) {
            OraImpulzus o = new OraImpulzus();
            o.mIndex = 15;
            o.frekvenciaHz = 1.0;
            l.add(o);
        }
        return l;
    }

    void betoltMintaProgram(String id, String nev) {
        if (JOptionPane.showConfirmDialog(this, tr("A jelenlegi program elvetésre kerül. Biztosan betölti a mintát?"), nev,
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        leallit();
        program = mintaProgramEpit(id);
        demoProgram = true;
        aktivMintaNev = id;
        allapot = new PlcAllapot();
        cimkeNev = new LinkedHashMap<>();
        cimkeMegj = new LinkedHashMap<>();
        hmiKepernyok = mintaHmiEpit(id);
        hmiAktivKepernyo = 0;
        hmiWidgetek = hmiKepernyok.get(0).widgetek;
        fbdBlokkok = mintaFbdEpit(id);
        fbdKovetkezoId = fbdKovetkezoIdSzamol(fbdBlokkok);
        grafcet = mintaGrafcetEpit(id);
        grafcetKezdetiMIndexek = new ArrayList<>();
        oraImpulzusok = mintaOraImpulzusEpit(id);
        grafcetNullaz();
        aktualisFajl = null;
        kivalasztottSor = null;
        scanSzamlalo = 0;
        frissitIOPanel();
        letra.ujraMeret();
        letra.repaint();
        hmiKepernyoComboFrissit();
        hmi.ujraMeret();
        hmi.repaint();
        fbd.ujraMeret();
        fbd.repaint();
        grafcetPanel.ujraMeret();
        grafcetPanel.repaint();
        undoTorol();
        setTitle(PROGRAM + " – " + nev);
        statusz.setText(" " + tr("Minta program betöltve:") + " " + nev);
    }

    // ================================================================ cél leíró (kattintás kezeléshez)

    static class Celpont {
        Sor sor;
        List<Erintkezo> lista;     // elotag / ag / utotag
        int index;                 // index a listában, == lista.size() esetén "+ hely"
        boolean kimenetCel;
        boolean uj_ag_cel;
        boolean uj_halozat_cel;
        List<Erintkezo> ag_torles_cel;

        static Celpont elem(Sor sor, List<Erintkezo> lista, int index) {
            Celpont c = new Celpont(); c.sor = sor; c.lista = lista; c.index = index; return c;
        }

        static Celpont kimenet(Sor sor) { Celpont c = new Celpont(); c.sor = sor; c.kimenetCel = true; return c; }
        static Celpont ujAg(Sor sor) { Celpont c = new Celpont(); c.sor = sor; c.uj_ag_cel = true; return c; }
        static Celpont agTorles(Sor sor, List<Erintkezo> ag) { Celpont c = new Celpont(); c.sor = sor; c.ag_torles_cel = ag; return c; }
        static Celpont ujHalozat() { Celpont c = new Celpont(); c.uj_halozat_cel = true; return c; }
    }

    // ================================================================ létradiagram panel

    class LetraPanel extends JPanel {

        List<Rectangle> celTeruletek = new ArrayList<>();
        List<Celpont> celpontok = new ArrayList<>();
        List<Rectangle> sorTeruletek = new ArrayList<>();
        List<Sor> sorHivatkozasok = new ArrayList<>();

        LetraPanel() {
            setBackground(HATTER);
            ujraMeret();
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { kattintas(e); }
            });
        }

        int logikSzelesseg, logikMagassag;

        void ujraMeret() {
            int szelesseg = RAIL_MARGIN * 2 + 400;
            int magassag = 20;
            for (Sor s : program) {
                int sorSzelesseg = sorSzelesseg(s);
                szelesseg = Math.max(szelesseg, RAIL_MARGIN * 2 + sorSzelesseg * COL_W + OUT_COL_W + 40);
                magassag += NETWORK_LABEL_H + sorMagassagSorokban(s) * ROW_H + RUNG_GAP;
            }
            magassag += ROW_H + RUNG_GAP; // "+ Új hálózat" sor a lista végén
            logikSzelesseg = szelesseg;
            logikMagassag = Math.max(magassag, 600);
            setPreferredSize(new Dimension((int) (logikSzelesseg * nagyitas), (int) (logikMagassag * nagyitas)));
            revalidate();
        }

        int sorSzelesseg(Sor s) {
            int w = s.elotag.size() + 1; // +1 a "+ hely" miatt
            if (!s.agak.isEmpty()) {
                int maxAg = 0;
                for (List<Erintkezo> ag : s.agak) maxAg = Math.max(maxAg, ag.size() + 1);
                w += maxAg;
                w += s.utotag.size() + 1;
            }
            return w;
        }

        int sorMagassagSorokban(Sor s) {
            int alap = Math.max(1, s.agak.size());
            return alap + 1; // +1 a "+ ág" sor miatt
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(nagyitas, nagyitas);

            celTeruletek.clear();
            celpontok.clear();
            sorTeruletek.clear();
            sorHivatkozasok.clear();

            int railLeft = RAIL_MARGIN;
            int railRight = logikSzelesseg - 16;

            int y = 16;
            for (Sor sor : program) {
                int rendMagassagSorok = sorMagassagSorokban(sor);
                int sorTeljesMagassag = NETWORK_LABEL_H + rendMagassagSorok * ROW_H;

                boolean kivalasztva = (sor == kivalasztottSor);
                Rectangle sorTerulet = new Rectangle(0, y, logikSzelesseg, sorTeljesMagassag);
                if (kivalasztva) {
                    g.setColor(KIVALASZTVA);
                    g.fillRect(0, y, logikSzelesseg, sorTeljesMagassag);
                }
                sorTeruletek.add(sorTerulet);
                sorHivatkozasok.add(sor);

                int cimY = y + 14;
                g.setColor(Color.DARK_GRAY);
                g.setFont(HALO_FONT);
                String cim = tr("Hálózat") + " " + (program.indexOf(sor) + 1) + (sor.megjegyzes.isEmpty() ? "" : "  –  " + sor.megjegyzes);
                g.drawString(cim, railLeft, cimY);

                int rowTop = y + NETWORK_LABEL_H;

                // sínek ezen a hálózaton
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(2.5f));
                g.drawLine(railLeft, rowTop, railLeft, rowTop + rendMagassagSorok * ROW_H);
                g.drawLine(railRight, rowTop, railRight, rowTop + rendMagassagSorok * ROW_H);

                rajzolHalozat(g, sor, railLeft, railRight, rowTop);

                y += sorTeljesMagassag + RUNG_GAP;
            }

            // "+ Új hálózat" sor a lista végén
            Rectangle ujHaloTer = new Rectangle(railLeft, y, railRight - railLeft, ROW_H);
            celTeruletek.add(ujHaloTer);
            celpontok.add(Celpont.ujHalozat());
            g.setColor(new Color(90, 90, 190));
            float[] dash = {5f, 4f};
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g.drawRoundRect(railLeft, y + 6, railRight - railLeft, ROW_H - 12, 10, 10);
            g.setFont(HALO_FONT);
            FontMetrics fm0 = g.getFontMetrics();
            String ujHaloFelirat = tr("+ Új hálózat");
            g.drawString(ujHaloFelirat, railLeft + (railRight - railLeft - fm0.stringWidth(ujHaloFelirat)) / 2, y + ROW_H / 2 + 4);
        }

        void rajzolHalozat(Graphics2D g, Sor sor, int railLeft, int railRight, int rowTop) {
            int row0Cy = rowTop + ROW_H / 2;
            int x = railLeft;
            boolean enterP = fut && true; // bal sín mindig "élő", de csak futás közben színezünk

            // bal sín -> előtag kezdete
            g.setColor(szin(enterP));
            g.setStroke(new BasicStroke(2f));
            // előtag
            boolean folytPower = enterP;
            for (int k = 0; k < sor.elotag.size(); k++) {
                Erintkezo el = sor.elotag.get(k);
                boolean be = folytPower;
                boolean ez = be && el.zart(allapot);
                rajzolErintkezo(g, x, row0Cy, el, be, ez);
                celpontok.add(Celpont.elem(sor, sor.elotag, k));
                celTeruletek.add(new Rectangle(x, row0Cy - ROW_H / 2, COL_W, ROW_H));
                folytPower = ez;
                x += COL_W;
            }
            // "+ hely" az előtag végén
            rajzolHely(g, x, row0Cy, folytPower);
            celpontok.add(Celpont.elem(sor, sor.elotag, sor.elotag.size()));
            celTeruletek.add(new Rectangle(x, row0Cy - ROW_H / 2, COL_W, ROW_H));
            x += COL_W;

            if (!sor.agak.isEmpty()) {
                int maxAg = 0;
                for (List<Erintkezo> ag : sor.agak) maxAg = Math.max(maxAg, ag.size() + 1);
                int blokkX0 = x;
                int blokkX1 = x + maxAg * COL_W;

                boolean barmelyikVezet = false;
                int agY = rowTop;
                for (int ai = 0; ai < sor.agak.size(); ai++) {
                    List<Erintkezo> ag = sor.agak.get(ai);
                    int agCy = agY + ROW_H / 2;
                    int ax = blokkX0;
                    boolean agPower = folytPower;
                    for (int k = 0; k < ag.size(); k++) {
                        Erintkezo el = ag.get(k);
                        boolean be = agPower;
                        boolean ez = be && el.zart(allapot);
                        rajzolErintkezo(g, ax, agCy, el, be, ez);
                        celpontok.add(Celpont.elem(sor, ag, k));
                        celTeruletek.add(new Rectangle(ax, agCy - ROW_H / 2, COL_W, ROW_H));
                        agPower = ez;
                        ax += COL_W;
                    }
                    // "+ hely" az ág végén (kitöltő vezeték a blokk széléig)
                    boolean agKesz = agPower;
                    while (ax < blokkX1) {
                        boolean utolsoHely = (ax + COL_W >= blokkX1);
                        if (utolsoHely) {
                            rajzolHely(g, ax, agCy, agKesz);
                            celpontok.add(Celpont.elem(sor, ag, ag.size()));
                            celTeruletek.add(new Rectangle(ax, agCy - ROW_H / 2, COL_W, ROW_H));
                        } else {
                            g.setColor(szin(agKesz));
                            g.drawLine(ax, agCy, ax + COL_W, agCy);
                        }
                        ax += COL_W;
                    }
                    if (ag.isEmpty()) agKesz = folytPower;

                    if (agKesz) barmelyikVezet = true;

                    // "✕" törlés hely az ág elején
                    Rectangle torlesTer = new Rectangle(blokkX0 - 20, agCy - 9, 18, 18);
                    celTeruletek.add(torlesTer);
                    celpontok.add(Celpont.agTorles(sor, ag));
                    g.setColor(new Color(180, 60, 60));
                    g.setFont(CIM_FONT);
                    g.drawString("✕", blokkX0 - 18, agCy + 4);

                    agY += ROW_H;
                }

                // bekötő és kimenő összekötő sínek (a párhuzamos blokk zárójelei)
                g.setColor(szin(folytPower));
                g.setStroke(new BasicStroke(2f));
                int also = rowTop + sor.agak.size() * ROW_H - ROW_H / 2;
                g.drawLine(blokkX0, row0Cy, blokkX0, also);
                g.setColor(szin(barmelyikVezet));
                g.drawLine(blokkX1, row0Cy, blokkX1, also);

                // "+ ág" sor a blokk alatt
                int ujAgCy = rowTop + sor.agak.size() * ROW_H + ROW_H / 2;
                Rectangle ujAgTer = new Rectangle(blokkX0, ujAgCy - ROW_H / 2, maxAg * COL_W, ROW_H);
                celTeruletek.add(ujAgTer);
                celpontok.add(Celpont.ujAg(sor));
                g.setColor(new Color(130, 130, 200));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRoundRect(blokkX0 + 4, ujAgCy - 10, maxAg * COL_W - 8, 20, 8, 8);
                g.setFont(CIM_FONT);
                g.drawString(tr("+ ág"), blokkX0 + 10, ujAgCy + 4);

                x = blokkX1;
                folytPower = barmelyikVezet;

                // utótag
                for (int k = 0; k < sor.utotag.size(); k++) {
                    Erintkezo el = sor.utotag.get(k);
                    boolean be = folytPower;
                    boolean ez = be && el.zart(allapot);
                    rajzolErintkezo(g, x, row0Cy, el, be, ez);
                    celpontok.add(Celpont.elem(sor, sor.utotag, k));
                    celTeruletek.add(new Rectangle(x, row0Cy - ROW_H / 2, COL_W, ROW_H));
                    folytPower = ez;
                    x += COL_W;
                }
                rajzolHely(g, x, row0Cy, folytPower);
                celpontok.add(Celpont.elem(sor, sor.utotag, sor.utotag.size()));
                celTeruletek.add(new Rectangle(x, row0Cy - ROW_H / 2, COL_W, ROW_H));
                x += COL_W;
            } else {
                // nincs párhuzamos ág -> "+ ág" marker rögtön az előtag után
                int ujAgCy = rowTop + ROW_H + ROW_H / 2;
                Rectangle ujAgTer = new Rectangle(x - COL_W, ujAgCy - ROW_H / 2, COL_W, ROW_H);
                celTeruletek.add(ujAgTer);
                celpontok.add(Celpont.ujAg(sor));
                g.setColor(new Color(130, 130, 200));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRoundRect(x - COL_W + 4, ujAgCy - 10, COL_W - 8, 20, 8, 8);
                g.setFont(CIM_FONT);
                g.drawString(tr("+ ág"), x - COL_W + 10, ujAgCy + 4);
            }

            // kimenet
            boolean kimenetBe = folytPower;
            g.setColor(szin(kimenetBe));
            g.drawLine(x, row0Cy, x + 14, row0Cy);
            rajzolKimenet(g, x + 14, row0Cy, sor.kimenet, kimenetBe);
            celpontok.add(Celpont.kimenet(sor));
            celTeruletek.add(new Rectangle(x + 14, row0Cy - ROW_H / 2, OUT_COL_W, ROW_H));
            int kimenetVege = x + 14 + OUT_COL_W;
            g.setColor(szin(kimenetBe));
            g.drawLine(kimenetVege, row0Cy, railRight, row0Cy);
        }

        Color szin(boolean energizalt) { return (fut && energizalt) ? ENERGIZALT : SEMLEGES; }

        String levag(Graphics2D g, String s, int maxW) {
            FontMetrics fm = g.getFontMetrics();
            if (fm.stringWidth(s) <= maxW) return s;
            String v = s;
            while (v.length() > 1 && fm.stringWidth(v + "…") > maxW) v = v.substring(0, v.length() - 1);
            return v + "…";
        }

        @Override
        public String getToolTipText(MouseEvent e) {
            Point p = logikaiPont(e);
            for (int k = celTeruletek.size() - 1; k >= 0; k--) {
                if (celTeruletek.get(k).contains(p)) {
                    Celpont c = celpontok.get(k);
                    if (c.kimenetCel) {
                        Kimenet ki = c.sor.kimenet;
                        switch (ki.tipus) {
                            case TEKERCS: case SET: case RESET: return eszkozleiras(ki.cel.tipus, ki.cel.index);
                            case TON: return eszkozleiras(OpTipus.TDN, ki.idx);
                            case CTU: return eszkozleiras(OpTipus.CDN, ki.idx);
                        }
                    } else if (c.lista != null && c.index < c.lista.size()) {
                        Erintkezo el = c.lista.get(c.index);
                        return eszkozleiras(el.op.tipus, el.op.index);
                    }
                    return null;
                }
            }
            return null;
        }

        void rajzolHely(Graphics2D g, int x, int cy, boolean bejovoPower) {
            g.setColor(szin(bejovoPower));
            g.setStroke(new BasicStroke(1.8f));
            g.drawLine(x, cy, x + 14, cy);
            g.setColor(new Color(170, 170, 170));
            float[] dash = {3f, 3f};
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g.drawRect(x + 16, cy - 11, 22, 22);
            g.setFont(HALO_FONT);
            g.setColor(new Color(130, 130, 130));
            g.drawString("+", x + 23, cy + 5);
            g.setColor(szin(bejovoPower));
            g.setStroke(new BasicStroke(1.8f));
            g.drawLine(x + 38, cy, x + COL_W, cy);
        }

        void rajzolErintkezo(Graphics2D g, int x, int cy, Erintkezo el, boolean bejovo, boolean vezet) {
            int cx = x + COL_W / 2;
            g.setColor(szin(bejovo));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(x, cy, cx - 16, cy);
            g.setColor(szin(vezet));
            g.drawLine(cx - 16, cy - 13, cx - 16, cy + 13);
            g.drawLine(cx + 16, cy - 13, cx + 16, cy + 13);
            if (!el.no) {
                g.drawLine(cx - 16, cy + 13, cx + 16, cy - 13);
            }
            g.setColor(szin(vezet));
            g.drawLine(cx + 16, cy, x + COL_W, cy);

            g.setFont(CIM_FONT);
            g.setColor(Color.BLACK);
            String lab = rovidFelirat(el.op);
            FontMetrics fm = g.getFontMetrics();
            lab = levag(g, lab, COL_W - 4);
            g.drawString(lab, cx - fm.stringWidth(lab) / 2, cy - 18);
        }

        void rajzolKimenet(Graphics2D g, int x, int cy, Kimenet k, boolean bejovo) {
            boolean doboz = (k.tipus == KimTipus.TON || k.tipus == KimTipus.CTU);
            boolean aktivKi = bejovo && (k.tipus != KimTipus.TON && k.tipus != KimTipus.CTU)
                    || (k.tipus == KimTipus.TON && allapot.t[k.idx].kesz)
                    || (k.tipus == KimTipus.CTU && allapot.c[k.idx].kesz);

            boolean bitErtek;
            switch (k.tipus) {
                case TEKERCS: case SET: case RESET: bitErtek = k.cel.olvas(allapot); break;
                case TON: bitErtek = allapot.t[k.idx].kesz; break;
                case CTU: bitErtek = allapot.c[k.idx].kesz; break;
                default: bitErtek = false;
            }

            Color tolto = (fut && bitErtek) ? DOBOZ_HATTER_BE : DOBOZ_HATTER_KI;

            if (!doboz) {
                int w = 34, h = 30;
                int cx = x + OUT_COL_W / 2;
                g.setColor(tolto);
                g.fillOval(cx - w / 2, cy - h / 2, w, h);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1.8f));
                g.drawOval(cx - w / 2, cy - h / 2, w, h);
                if (k.tipus == KimTipus.TEKERCS && k.negalt) {
                    g.drawLine(cx - w / 2 + 4, cy + h / 2 - 4, cx + w / 2 - 4, cy - h / 2 + 4);
                }
                String jel = k.tipus == KimTipus.SET ? "S" : (k.tipus == KimTipus.RESET ? "R" : "");
                if (!jel.isEmpty()) {
                    g.setFont(HALO_FONT);
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(jel, cx - fm.stringWidth(jel) / 2, cy + 4);
                }
                g.setFont(CIM_FONT);
                String lab = levag(g, rovidFelirat(k.cel), OUT_COL_W - 8);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(lab, cx - fm.stringWidth(lab) / 2, cy - h / 2 - 6);
            } else {
                int w = OUT_COL_W - 16, h = 36;
                g.setColor(tolto);
                g.fillRect(x + 8, cy - h / 2, w, h);
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke(1.8f));
                g.drawRect(x + 8, cy - h / 2, w, h);
                g.setFont(HALO_FONT);
                String cim = k.tipus == KimTipus.TON ? "TON" : "CTU";
                g.drawString(cim, x + 14, cy - 10);
                g.setFont(CIM_FONT);
                if (k.tipus == KimTipus.TON) {
                    Idozito t = allapot.t[k.idx];
                    String nevT = cimkeNeve(OpTipus.TDN, k.idx);
                    String sor2 = "T" + k.idx + (nevT != null ? " (" + nevT + ")" : "") + "  " + t.akkMs + "/" + t.presetMs + " ms";
                    g.drawString(levag(g, sor2, w - 8), x + 14, cy + 6);
                } else {
                    Szamlalo c = allapot.c[k.idx];
                    String nevC = cimkeNeve(OpTipus.CDN, k.idx);
                    String sor2 = "C" + k.idx + (nevC != null ? " (" + nevC + ")" : "") + "  " + c.ertek + "/" + c.preset;
                    g.drawString(levag(g, sor2, w - 8), x + 14, cy + 6);
                }
            }
        }

        // ---------------------------------------------------------- kattintás kezelése

        void kattintas(MouseEvent e) {
            Point p = logikaiPont(e);
            for (int k = celTeruletek.size() - 1; k >= 0; k--) {
                if (celTeruletek.get(k).contains(p)) {
                    kezeljCelpont(celpontok.get(k), e.isControlDown());
                    return;
                }
            }
            for (int k = 0; k < sorTeruletek.size(); k++) {
                if (sorTeruletek.get(k).contains(p)) {
                    kivalasztottSor = sorHivatkozasok.get(k);
                    repaint();
                    if (e.getClickCount() >= 2) halozatMegjegyzesSzerkeszto(sorHivatkozasok.get(k));
                    return;
                }
            }
        }

        void kezeljCelpont(Celpont c, boolean ctrlDown) {
            if (c.uj_halozat_cel) {
                ujHalozat();
                return;
            }
            kivalasztottSor = c.sor;
            if (c.kimenetCel) {
                kimenetSzerkeszto(c.sor.kimenet);
            } else if (c.uj_ag_cel) {
                addBranch(c.sor);
            } else if (c.ag_torles_cel != null) {
                removeBranch(c.sor, c.ag_torles_cel);
            } else {
                boolean letezo = c.index < c.lista.size();
                if (ctrlDown) {
                    if (letezo) {
                        vagolapErintkezo = masolt(c.lista.get(c.index));
                        statusz.setText(" " + tr("Kontaktus vágólapra másolva."));
                    } else if (vagolapErintkezo != null) {
                        undoPontBeallit();
                        c.lista.add(masolt(vagolapErintkezo));
                        undoPontRogzit();
                        statusz.setText(" " + tr("Kontaktus beillesztve a vágólapról."));
                    }
                } else {
                    Erintkezo szerk = letezo ? c.lista.get(c.index) : masolt(uj());
                    undoPontBeallit();
                    Erintkezo eredmeny = erintkezoSzerkeszto(szerk, letezo);
                    if (eredmeny == TOROL_JEL) {
                        c.lista.remove(c.index);
                        undoPontRogzit();
                    } else if (eredmeny != null) {
                        if (letezo) c.lista.set(c.index, eredmeny);
                        else c.lista.add(eredmeny);
                        undoPontRogzit();
                    } else {
                        undoPontEldob();
                    }
                }
            }
            ujraMeret();
            repaint();
        }

        Erintkezo uj() { return new Erintkezo(); }
        Erintkezo masolt(Erintkezo e) { Erintkezo m = new Erintkezo(); m.no = e.no; m.op = new Operandus(e.op.tipus, e.op.index); return m; }
    }

    static final Erintkezo TOROL_JEL = new Erintkezo();

    // ================================================================ HMI panel

    void irBitHmi(Operandus op, boolean ertek) {
        if (op.tipus == OpTipus.I) allapot.i[op.index] = ertek;
        else if (op.tipus == OpTipus.M) allapot.m[op.index] = ertek;
        frissitIOPanel();
    }

    void hmiTrendMintavetel() {
        for (HmiKepernyo kep : hmiKepernyok) {
            for (HmiWidget w : kep.widgetek) {
                if (w.tipus != HmiTipus.TREND) continue;
                if (w.trendTortenet == null) w.trendTortenet = new ArrayDeque<>();
                double ertek;
                if (w.szamkent && w.op.tipus == OpTipus.TDN) ertek = allapot.t[w.op.index].akkMs;
                else if (w.szamkent && w.op.tipus == OpTipus.CDN) ertek = allapot.c[w.op.index].ertek;
                else ertek = w.op.olvas(allapot) ? 1.0 : 0.0;
                w.trendTortenet.addLast(ertek);
                while (w.trendTortenet.size() > HMI_TREND_HOSSZ) w.trendTortenet.removeFirst();
            }
        }
    }

    String levag(Graphics2D g, String s, int maxW) {
        if (s == null || s.isEmpty()) return s;
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(s) <= maxW) return s;
        String v = s;
        while (v.length() > 1 && fm.stringWidth(v + "…") > maxW) v = v.substring(0, v.length() - 1);
        return v + "…";
    }

    class HmiPanel extends JPanel {
        boolean szerkesztesMod = false;
        HmiTipus aktivEszkoz = null;
        HmiWidget kivalasztott = null;
        HmiWidget aktivGomb = null;
        Point dragStart = null;
        int dragX0, dragY0, dragW0, dragH0;
        boolean atmeretez = false;
        static final int FOGANTYU = 9;

        HmiPanel() {
            setBackground(new Color(233, 236, 240));
            ujraMeret();
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { lenyomva(e); }
                @Override public void mouseReleased(MouseEvent e) { felengedve(e); }
                @Override public void mouseClicked(MouseEvent e) { kattintva(e); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) { huzva(e); }
            });
        }

        void ujraMeret() {
            int maxX = 1400, maxY = 900;
            for (HmiWidget w : hmiWidgetek) {
                maxX = Math.max(maxX, w.x + w.w + 40);
                maxY = Math.max(maxY, w.y + w.h + 40);
            }
            setPreferredSize(new Dimension((int) (maxX * nagyitas), (int) (maxY * nagyitas)));
            revalidate();
        }

        Rectangle terulet(HmiWidget w) { return new Rectangle(w.x, w.y, w.w, w.h); }

        HmiWidget talal(Point p) {
            for (int i = hmiWidgetek.size() - 1; i >= 0; i--) {
                HmiWidget w = hmiWidgetek.get(i);
                if (terulet(w).contains(p)) return w;
            }
            return null;
        }

        void lenyomva(MouseEvent e) {
            if (szerkesztesMod) {
                if (aktivEszkoz != null) {
                    HmiWidget w = new HmiWidget();
                    w.tipus = aktivEszkoz;
                    switch (aktivEszkoz) {
                        case LAMPA: w.w = 60; w.h = 60; w.felirat = tr("Lámpa"); break;
                        case GOMB: w.w = 90; w.h = 50; w.felirat = tr("Gomb"); w.op = new Operandus(OpTipus.I, 0); break;
                        case KIJELZO: w.w = 130; w.h = 40; w.felirat = ""; break;
                        case FELIRAT: w.w = 140; w.h = 24; w.felirat = tr("Felirat"); break;
                        case TEGLALAP: w.w = 160; w.h = 100; w.felirat = ""; break;
                        case TREND: w.w = 240; w.h = 100; w.felirat = "Trend"; break;
                        case ZEBRA: w.w = 120; w.h = 60; w.felirat = ""; w.szinKi = new Color(60, 60, 60); w.szinBe = Color.WHITE; break;
                        case AJTO: w.w = 80; w.h = 110; w.felirat = tr("Ajtó"); w.op = new Operandus(OpTipus.Q, 0); w.szinBe = new Color(180, 185, 190); w.szinKi = Color.DARK_GRAY; break;
                        case SZALLITOSZALAG: w.w = 220; w.h = 50; w.felirat = tr("Szállítószalag"); w.op = new Operandus(OpTipus.Q, 0); break;
                        case SZENZOR: w.w = 40; w.h = 30; w.felirat = tr("Érzékelő"); w.op = new Operandus(OpTipus.I, 0); break;
                        case ROBOT_FANUC: w.w = 110; w.h = 140; w.felirat = tr("Fanuc robot"); w.op = new Operandus(OpTipus.Q, 0); break;
                        case ROBOT_KUKA: w.w = 110; w.h = 140; w.felirat = tr("Kuka robot"); w.op = new Operandus(OpTipus.Q, 0); break;
                    }
                    Point logP = logikaiPont(e);
                    w.x = Math.max(0, logP.x - w.w / 2);
                    w.y = Math.max(0, logP.y - w.h / 2);
                    undoPontBeallit();
                    hmiWidgetek.add(w);
                    kivalasztott = w;
                    aktivEszkoz = null;
                    if (hmiEszkozCombo != null) hmiEszkozCombo.setSelectedIndex(0);
                    ujraMeret();
                    repaint();
                    SwingUtilities.invokeLater(() -> widgetSzerkeszto(w, true));
                    return;
                }
                Point p = logikaiPont(e);
                HmiWidget hit = talal(p);
                kivalasztott = hit;
                if (hit != null) {
                    Rectangle fog = new Rectangle(hit.x + hit.w - FOGANTYU, hit.y + hit.h - FOGANTYU, FOGANTYU * 2, FOGANTYU * 2);
                    atmeretez = fog.contains(p);
                    dragStart = p;
                    dragX0 = hit.x; dragY0 = hit.y; dragW0 = hit.w; dragH0 = hit.h;
                    undoPontBeallit();
                }
                repaint();
            } else {
                HmiWidget hit = talal(logikaiPont(e));
                if (hit != null && hit.tipus == HmiTipus.GOMB && hit.gombMod == GombMod.PILLANAT) {
                    irBitHmi(hit.op, true);
                    aktivGomb = hit;
                    repaint();
                }
            }
        }

        void felengedve(MouseEvent e) {
            if (szerkesztesMod) {
                boolean voltHuzas = (dragStart != null);
                boolean valtozott = voltHuzas && kivalasztott != null &&
                        (kivalasztott.x != dragX0 || kivalasztott.y != dragY0 || kivalasztott.w != dragW0 || kivalasztott.h != dragH0);
                dragStart = null;
                atmeretez = false;
                ujraMeret();
                if (voltHuzas) { if (valtozott) undoPontRogzit(); else undoPontEldob(); }
            } else if (aktivGomb != null) {
                irBitHmi(aktivGomb.op, false);
                aktivGomb = null;
                repaint();
            }
        }

        void kattintva(MouseEvent e) {
            if (szerkesztesMod) {
                if (e.getClickCount() == 2) {
                    HmiWidget hit = talal(logikaiPont(e));
                    if (hit != null) widgetSzerkeszto(hit, false);
                }
            } else {
                HmiWidget hit = talal(logikaiPont(e));
                if (hit != null && hit.tipus == HmiTipus.GOMB && hit.gombMod == GombMod.VALTO) {
                    boolean jelen = hit.op.olvas(allapot);
                    irBitHmi(hit.op, !jelen);
                    repaint();
                }
            }
        }

        void huzva(MouseEvent e) {
            if (szerkesztesMod && kivalasztott != null && dragStart != null) {
                Point lp = logikaiPont(e);
                int dx = lp.x - dragStart.x, dy = lp.y - dragStart.y;
                if (atmeretez) {
                    kivalasztott.w = Math.max(20, dragW0 + dx);
                    kivalasztott.h = Math.max(20, dragH0 + dy);
                } else {
                    kivalasztott.x = Math.max(0, dragX0 + dx);
                    kivalasztott.y = Math.max(0, dragY0 + dy);
                }
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(nagyitas, nagyitas);
            for (HmiWidget w : hmiWidgetek) rajzolWidget(g, w);
            if (szerkesztesMod && kivalasztott != null) {
                g.setColor(new Color(30, 120, 220));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRect(kivalasztott.x, kivalasztott.y, kivalasztott.w, kivalasztott.h);
                g.setColor(new Color(30, 120, 220));
                g.fillRect(kivalasztott.x + kivalasztott.w - FOGANTYU, kivalasztott.y + kivalasztott.h - FOGANTYU, FOGANTYU, FOGANTYU);
            }
            if (szerkesztesMod && aktivEszkoz != null) {
                g.setColor(new Color(90, 90, 190));
                g.setFont(HALO_FONT);
                g.drawString(tr("Kattintson a vászonra az új elem elhelyezéséhez..."), 12, 20);
            }
        }

        void rajzolWidget(Graphics2D g, HmiWidget w) {
            switch (w.tipus) {
                case LAMPA: {
                    boolean be = w.op.olvas(allapot);
                    int d = Math.min(w.w, w.h);
                    int cx = w.x + w.w / 2, cy = w.y + w.h / 2;
                    g.setColor(be ? w.szinBe : w.szinKi);
                    g.fillOval(cx - d / 2, cy - d / 2, d, d);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.8f));
                    g.drawOval(cx - d / 2, cy - d / 2, d, d);
                    g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, w.fontMeret));
                    FontMetrics fm = g.getFontMetrics();
                    if (w.felirat != null && !w.felirat.isEmpty()) {
                        g.setColor(Color.BLACK);
                        String t1 = levag(g, w.felirat, Math.max(w.w, 70));
                        g.drawString(t1, cx - fm.stringWidth(t1) / 2, w.y - 4);
                    }
                    g.setFont(CIM_FONT);
                    fm = g.getFontMetrics();
                    String t2 = rovidFelirat(w.op);
                    g.setColor(Color.GRAY);
                    g.drawString(t2, cx - fm.stringWidth(t2) / 2, w.y + w.h + fm.getAscent() + 2);
                    break;
                }
                case GOMB: {
                    boolean aktiv = w.op.olvas(allapot);
                    g.setColor(aktiv ? w.szinBe : new Color(222, 222, 222));
                    g.fillRoundRect(w.x, w.y, w.w, w.h, 12, 12);
                    g.setColor(Color.DARK_GRAY);
                    g.setStroke(new BasicStroke(1.8f));
                    g.drawRoundRect(w.x, w.y, w.w, w.h, 12, 12);
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, w.fontMeret));
                    g.setColor(aktiv ? Color.WHITE : Color.BLACK);
                    FontMetrics fm = g.getFontMetrics();
                    String t = levag(g, w.felirat, w.w - 8);
                    g.drawString(t, w.x + (w.w - fm.stringWidth(t)) / 2, w.y + w.h / 2 + 4);
                    g.setFont(CIM_FONT);
                    fm = g.getFontMetrics();
                    String t2 = rovidFelirat(w.op);
                    g.setColor(Color.GRAY);
                    g.drawString(t2, w.x + (w.w - fm.stringWidth(t2)) / 2, w.y + w.h + 12);
                    break;
                }
                case KIJELZO: {
                    g.setColor(Color.WHITE);
                    g.fillRect(w.x, w.y, w.w, w.h);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRect(w.x, w.y, w.w, w.h);
                    String ertek;
                    boolean kesz;
                    if (w.szamkent && w.op.tipus == OpTipus.TDN) {
                        Idozito t = allapot.t[w.op.index];
                        ertek = t.akkMs + " / " + t.presetMs + " ms";
                        kesz = t.kesz;
                    } else if (w.szamkent && w.op.tipus == OpTipus.CDN) {
                        Szamlalo c = allapot.c[w.op.index];
                        ertek = c.ertek + " / " + c.preset;
                        kesz = c.kesz;
                    } else {
                        boolean b = w.op.olvas(allapot);
                        ertek = b ? "1" : "0";
                        kesz = b;
                    }
                    g.setFont(new Font(Font.MONOSPACED, Font.BOLD, w.fontMeret));
                    g.setColor(kesz ? ENERGIZALT.darker() : Color.BLACK);
                    FontMetrics fm = g.getFontMetrics();
                    String ev = levag(g, ertek, w.w - 6);
                    g.drawString(ev, w.x + (w.w - fm.stringWidth(ev)) / 2, w.y + w.h / 2 + fm.getAscent() / 2 - 2);
                    g.setFont(CIM_FONT);
                    g.setColor(Color.GRAY);
                    String lab = (w.felirat == null || w.felirat.isEmpty()) ? rovidFelirat(w.op) : w.felirat + " (" + rovidFelirat(w.op) + ")";
                    g.drawString(levag(g, lab, w.w), w.x, w.y - 4);
                    break;
                }
                case FELIRAT: {
                    g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, w.fontMeret));
                    g.setColor(w.szinKi);
                    g.drawString(w.felirat, w.x, w.y + w.fontMeret);
                    break;
                }
                case TEGLALAP: {
                    g.setColor(w.szinBe);
                    g.fillRect(w.x, w.y, w.w, w.h);
                    g.setColor(w.szinKi);
                    g.setStroke(new BasicStroke(1.8f));
                    g.drawRect(w.x, w.y, w.w, w.h);
                    if (w.felirat != null && !w.felirat.isEmpty()) {
                        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, w.fontMeret));
                        g.setColor(Color.BLACK);
                        g.drawString(w.felirat, w.x + 6, w.y + w.fontMeret + 4);
                    }
                    break;
                }
                case TREND: {
                    g.setColor(Color.WHITE);
                    g.fillRect(w.x, w.y, w.w, w.h);
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRect(w.x, w.y, w.w, w.h);
                    Deque<Double> hist = w.trendTortenet;
                    if (hist != null && hist.size() > 1) {
                        double maxErtek;
                        if (w.szamkent && w.op.tipus == OpTipus.TDN) maxErtek = Math.max(1, allapot.t[w.op.index].presetMs);
                        else if (w.szamkent && w.op.tipus == OpTipus.CDN) maxErtek = Math.max(1, allapot.c[w.op.index].preset);
                        else maxErtek = 1.0;
                        Double[] arr = hist.toArray(new Double[0]);
                        int n = arr.length;
                        int belsoH = w.h - 10;
                        g.setColor(ENERGIZALT);
                        g.setStroke(new BasicStroke(1.6f));
                        for (int i = 1; i < n; i++) {
                            int x1 = w.x + 4 + (int) ((i - 1) * (w.w - 8) / (double) (HMI_TREND_HOSSZ - 1));
                            int x2 = w.x + 4 + (int) (i * (w.w - 8) / (double) (HMI_TREND_HOSSZ - 1));
                            int y1 = w.y + w.h - 5 - (int) (Math.min(1.0, arr[i - 1] / maxErtek) * belsoH);
                            int y2 = w.y + w.h - 5 - (int) (Math.min(1.0, arr[i] / maxErtek) * belsoH);
                            g.drawLine(x1, y1, x2, y2);
                        }
                    }
                    g.setFont(CIM_FONT);
                    g.setColor(Color.GRAY);
                    String lab = (w.felirat == null || w.felirat.isEmpty()) ? rovidFelirat(w.op) : w.felirat + " (" + rovidFelirat(w.op) + ")";
                    g.drawString(levag(g, lab, w.w), w.x, w.y - 4);
                    break;
                }
                case ZEBRA: {
                    g.setColor(w.szinKi);
                    g.fillRect(w.x, w.y, w.w, w.h);
                    g.setColor(w.szinBe);
                    boolean fekvo = w.w >= w.h;
                    int savSzam = Math.max(3, (fekvo ? w.w : w.h) / 18);
                    if (fekvo) {
                        double sav = w.w / (double) (savSzam * 2);
                        for (int i = 0; i < savSzam; i++) {
                            int x1 = w.x + (int) Math.round((2 * i + 0.5) * sav);
                            int szel = (int) Math.round(sav);
                            g.fillRect(x1, w.y, szel, w.h);
                        }
                    } else {
                        double sav = w.h / (double) (savSzam * 2);
                        for (int i = 0; i < savSzam; i++) {
                            int y1 = w.y + (int) Math.round((2 * i + 0.5) * sav);
                            int mag = (int) Math.round(sav);
                            g.fillRect(w.x, y1, w.w, mag);
                        }
                    }
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1.2f));
                    g.drawRect(w.x, w.y, w.w, w.h);
                    if (w.felirat != null && !w.felirat.isEmpty()) {
                        g.setFont(CIM_FONT);
                        g.setColor(Color.GRAY);
                        g.drawString(levag(g, w.felirat, w.w), w.x, w.y - 4);
                    }
                    break;
                }
                case AJTO: {
                    g.setColor(new Color(235, 235, 235));
                    g.fillRect(w.x, w.y, w.w, w.h);
                    Shape regiClip = g.getClip();
                    g.clipRect(w.x, w.y, w.w, w.h);
                    int panelW = w.w / 2;
                    int csuszas = (int) Math.round(panelW * w.ajtoNyitottsag);
                    g.setColor(w.szinBe);
                    g.fillRect(w.x - csuszas, w.y, panelW, w.h);
                    g.fillRect(w.x + panelW + csuszas, w.y, panelW, w.h);
                    g.setColor(w.szinKi);
                    g.setStroke(new BasicStroke(1.5f));
                    g.drawRect(w.x - csuszas, w.y, panelW, w.h);
                    g.drawRect(w.x + panelW + csuszas, w.y, panelW, w.h);
                    g.setClip(regiClip);
                    g.setColor(w.szinKi);
                    g.setStroke(new BasicStroke(2.2f));
                    g.drawRect(w.x, w.y, w.w, w.h);
                    if (w.felirat != null && !w.felirat.isEmpty()) {
                        g.setFont(CIM_FONT);
                        g.setColor(Color.GRAY);
                        g.drawString(levag(g, w.felirat, w.w), w.x, w.y - 4);
                    }
                    break;
                }
                case SZALLITOSZALAG: rajzolSzalag(g, w); break;
                case SZENZOR: rajzolSzenzor(g, w); break;
                case ROBOT_FANUC: rajzolRobot(g, w, new Color(255, 206, 0), new Color(55, 55, 58), false); break;
                case ROBOT_KUKA: rajzolRobot(g, w, new Color(255, 102, 0), new Color(20, 20, 20), true); break;
            }
        }

        /** Mozgó szállítószalag: fém keret, két görgő, a szalag felületén futó csíkmintával és – működés közben – a szalagon végtelenített sorban haladó motorblokkokkal. */
        void rajzolSzalag(Graphics2D g, HmiWidget w) {
            boolean fekvo = w.w >= w.h;
            boolean futE = w.op.olvas(allapot);
            Graphics2D g2 = (Graphics2D) g.create();
            int cx = w.x + w.w / 2, cy = w.y + w.h / 2;
            int hossz, vastag;
            if (fekvo) {
                hossz = w.w; vastag = w.h;
            } else {
                g2.rotate(Math.PI / 2, cx, cy);
                hossz = w.h; vastag = w.w;
            }
            int x0 = cx - hossz / 2, y0 = cy - vastag / 2;
            int rollerR = Math.max(6, (int) (vastag * 0.42));

            // keretoszlopok (állvány)
            g2.setColor(new Color(130, 132, 136));
            int labW = Math.max(6, vastag / 6);
            g2.fillRect(x0 + rollerR / 2, y0 + vastag, labW, Math.max(6, vastag / 3));
            g2.fillRect(x0 + hossz - rollerR / 2 - labW, y0 + vastag, labW, Math.max(6, vastag / 3));

            // szalag belső területe (sötét futófelület), görgők között
            int beltY0 = y0 + (vastag - rollerR) / 2;
            Rectangle belt = new Rectangle(x0 + rollerR, beltY0, Math.max(1, hossz - 2 * rollerR), rollerR);
            g2.setColor(new Color(32, 34, 36));
            g2.fillRect(belt.x, belt.y, belt.width, belt.height);

            // futó diagonális csíkminta (klip a szalag belső felületére)
            Shape regiClip = g2.getClip();
            g2.clipRect(belt.x, belt.y, belt.width, belt.height);
            g2.setColor(new Color(70, 73, 77));
            int csikTav = Math.max(10, rollerR);
            int offset = (int) Math.round((w.szalagFazis * csikTav * 4)) % csikTav;
            g2.setStroke(new BasicStroke(Math.max(2f, rollerR * 0.3f)));
            for (int sx = belt.x - csikTav - offset; sx < belt.x + belt.width + csikTav; sx += csikTav) {
                g2.drawLine(sx, belt.y + belt.height, sx + rollerR, belt.y);
            }
            g2.setClip(regiClip);

            // görgők (fém, kiemelt középpel)
            for (int gx : new int[]{x0 + rollerR, x0 + hossz - rollerR}) {
                int gy = y0 + vastag / 2;
                g2.setColor(new Color(90, 92, 96));
                g2.fillOval(gx - rollerR, gy - rollerR, rollerR * 2, rollerR * 2);
                g2.setColor(new Color(165, 168, 172));
                g2.fillOval(gx - rollerR / 2, gy - rollerR / 2, rollerR, rollerR);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawOval(gx - rollerR, gy - rollerR, rollerR * 2, rollerR * 2);
            }

            // futó motorblokkok a szalagon (csak működés közben), végtelenített, egyenletes sorban haladó folyamként
            if (futE && belt.width > rollerR * 2) {
                int blokkW = Math.max(14, (int) (rollerR * 1.3));
                int blokkH = Math.max(10, (int) (rollerR * 1.0));
                int tavolsag = blokkW * 3;
                double eltolasPx = (w.szalagFazis * 0.22 * tavolsag * 3) % tavolsag;
                Shape munkaKlip = g2.getClip();
                g2.clipRect(belt.x, belt.y - blokkH - 2, belt.width, belt.height + blokkH + 2);
                for (double bx = belt.x - tavolsag + eltolasPx; bx < belt.x + belt.width; bx += tavolsag) {
                    rajzolMotorblokk(g2, (int) Math.round(bx), belt.y - blokkH + 1, blokkW, blokkH);
                }
                g2.setClip(munkaKlip);
            }

            // keret körvonal
            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(x0, y0, hossz, vastag, Math.max(4, rollerR / 2), Math.max(4, rollerR / 2));
            g2.dispose();

            if (w.felirat != null && !w.felirat.isEmpty()) {
                g.setFont(CIM_FONT);
                g.setColor(Color.GRAY);
                g.drawString(levag(g, w.felirat, w.w), w.x, w.y - 4);
            }
        }

        /** Egyszerűsített motorblokk-ikon: főtest, hengerfejfedél és rögzítő csavarfejek – a szállítószalagokon haladó munkadarab. */
        void rajzolMotorblokk(Graphics2D g, int x, int y, int w, int h) {
            g.setColor(new Color(118, 120, 124));
            g.fillRoundRect(x, y, w, h, 3, 3);
            g.setColor(new Color(150, 153, 158));
            int fedelW = (int) (w * 0.62), fedelH = Math.max(3, (int) (h * 0.4));
            g.fillRoundRect(x + (w - fedelW) / 2, y - fedelH + 2, fedelW, fedelH, 2, 2);
            g.setColor(new Color(70, 72, 76));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(x, y, w, h, 3, 3);
            g.drawRoundRect(x + (w - fedelW) / 2, y - fedelH + 2, fedelW, fedelH, 2, 2);
            g.setColor(new Color(60, 62, 66));
            int boltR = Math.max(1, h / 7);
            g.fillOval(x + boltR, y + h - boltR * 2, boltR, boltR);
            g.fillOval(x + w - boltR * 2, y + h - boltR * 2, boltR, boltR);
        }

        /** Optikai közelítés-érzékelő: fém ház, lencse és – aktív (detektáló) állapotban – pulzáló jelző-LED és fénykúp. A ház a `forgatas` mező (0/90/180/270°) szerint elforgatva jelenik meg. */
        void rajzolSzenzor(Graphics2D g, HmiWidget w) {
            boolean aktiv = w.op.olvas(allapot);
            double puls = 0.5 + 0.5 * Math.sin(w.szenzorPulzus);
            int cx = w.x + w.w / 2, cy = w.y + w.h / 2;

            Graphics2D g2 = (Graphics2D) g.create();
            if (w.forgatas != 0) g2.rotate(Math.toRadians(w.forgatas), cx, cy);

            g2.setColor(new Color(58, 60, 64));
            g2.fillRoundRect(w.x, w.y, w.w, w.h, 6, 6);
            g2.setColor(new Color(130, 132, 136));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(w.x, w.y, w.w, w.h, 6, 6);

            int lensR = Math.max(4, Math.min(w.w, w.h) / 4);
            int lensCx = w.x + w.w - lensR - Math.max(3, w.w / 10);
            int lensCy = w.y + w.h / 2;

            if (aktiv) {
                Composite regi = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) (0.12 + 0.22 * puls)));
                g2.setColor(new Color(255, 120, 40));
                int[] kxs = {lensCx, lensCx + lensR * 5, lensCx + lensR * 5};
                int[] kys = {lensCy, lensCy - lensR * 2, lensCy + lensR * 2};
                g2.fillPolygon(kxs, kys, 3);
                g2.setComposite(regi);
            }

            g2.setColor(aktiv ? new Color(255, 90, 30) : new Color(35, 60, 70));
            g2.fillOval(lensCx - lensR, lensCy - lensR, lensR * 2, lensR * 2);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1.1f));
            g2.drawOval(lensCx - lensR, lensCy - lensR, lensR * 2, lensR * 2);

            int ledR = Math.max(3, lensR / 2);
            int ledCx = w.x + ledR + 4, ledCy = w.y + ledR + 4;
            Color ledSzin = aktiv ? blendSzin(new Color(20, 90, 20), new Color(60, 255, 60), puls) : new Color(60, 60, 60);
            g2.setColor(ledSzin);
            g2.fillOval(ledCx - ledR, ledCy - ledR, ledR * 2, ledR * 2);
            g2.dispose();

            if (w.felirat != null && !w.felirat.isEmpty()) {
                g.setFont(CIM_FONT);
                g.setColor(Color.GRAY);
                g.drawString(levag(g, w.felirat, w.w), w.x, w.y - 4);
            }
            g.setFont(CIM_FONT);
            g.setColor(Color.GRAY);
            String t2 = rovidFelirat(w.op);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(t2, w.x + (w.w - fm.stringWidth(t2)) / 2, w.y + w.h + fm.getAscent() + 2);
        }

        Color blendSzin(Color a, Color b, double t) {
            t = Math.max(0, Math.min(1, t));
            return new Color(
                    (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        /**
         * Ipari robotkar (egyszerűsített, 2 karú csuklós szerkezet) folytonos pick&place mozgással,
         * amíg a vezérlő bitje aktív. A gyártó-specifikus szín (Fanuc sárga / Kuka narancs) a karelemeken,
         * a csuklók és a talp sötét fémszürke/fekete színnel jelennek meg.
         */
        void rajzolRobot(Graphics2D g, HmiWidget w, Color karSzin, Color izuletSzin, boolean kuka) {
            boolean aktiv = w.op.olvas(allapot);
            double t = 0.5 - 0.5 * Math.cos(w.robotFazis); // 0..1..0 folytonos, sima pick&place lengés

            Graphics2D g2 = (Graphics2D) g.create();
            if (w.tukrozve) {
                int cx = w.x + w.w / 2;
                g2.translate(cx, 0);
                g2.scale(-1, 1);
                g2.translate(-cx, 0);
            }

            int floorY = w.y + w.h;
            int talpW = (int) (w.w * 0.5);
            int talpX = w.x + (w.w - talpW) / 2;
            int talpH = Math.max(6, (int) (w.h * 0.14));

            // padló-árnyék
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillOval(w.x + (int) (w.w * 0.12), floorY - 5, (int) (w.w * 0.76), Math.max(4, w.h / 18));

            // talp
            g2.setColor(izuletSzin);
            g2.fillRoundRect(talpX, floorY - talpH, talpW, talpH, 5, 5);

            // oszlop (forgó talpazat)
            int colW = Math.max(8, (int) (w.w * 0.18));
            int colH = (int) (w.h * 0.20);
            int colX = w.x + w.w / 2 - colW / 2;
            int colY = floorY - talpH - colH;
            g2.setColor(karSzin.darker());
            g2.fillRoundRect(colX, colY, colW, colH, 4, 4);

            int shoulderCx = w.x + w.w / 2, shoulderCy = colY;
            double l1 = w.h * 0.40, l2 = w.h * 0.38;

            double theta1 = Math.toRadians(lerp(-50, 20, t));
            int elbowCx = shoulderCx + (int) Math.round(l1 * Math.sin(theta1));
            int elbowCy = shoulderCy - (int) Math.round(l1 * Math.cos(theta1));

            double theta2 = theta1 + Math.toRadians(lerp(25, 110, t));
            int wristCx = elbowCx + (int) Math.round(l2 * Math.sin(theta2));
            int wristCy = elbowCy - (int) Math.round(l2 * Math.cos(theta2));

            float karVastag = Math.max(5f, (float) (w.w * 0.14));
            g2.setStroke(new BasicStroke(karVastag, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            // felkar
            g2.setColor(karSzin);
            g2.drawLine(shoulderCx, shoulderCy, elbowCx, elbowCy);
            // alkar (kicsit sötétebb, hogy elkülönüljön)
            g2.setColor(kuka ? karSzin : karSzin.darker());
            g2.drawLine(elbowCx, elbowCy, wristCx, wristCy);

            // csuklók (izületek)
            int izR = Math.max(4, (int) (karVastag * 0.6));
            g2.setColor(izuletSzin);
            g2.fillOval(shoulderCx - izR, shoulderCy - izR, izR * 2, izR * 2);
            g2.fillOval(elbowCx - izR, elbowCy - izR, izR * 2, izR * 2);

            // fogó (egyszerű kétágú fogó a csukló végén, a kar irányában)
            double fogoSzog = theta2;
            int fogoHossz = Math.max(6, izR * 2);
            int fx = wristCx + (int) Math.round(fogoHossz * Math.sin(fogoSzog));
            int fy = wristCy - (int) Math.round(fogoHossz * Math.cos(fogoSzog));
            g2.setStroke(new BasicStroke(Math.max(2.5f, karVastag * 0.35f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(izuletSzin);
            double szarnySzog = Math.toRadians(18);
            int fx1 = wristCx + (int) Math.round(fogoHossz * Math.sin(fogoSzog - szarnySzog));
            int fy1 = wristCy - (int) Math.round(fogoHossz * Math.cos(fogoSzog - szarnySzog));
            int fx2 = wristCx + (int) Math.round(fogoHossz * Math.sin(fogoSzog + szarnySzog));
            int fy2 = wristCy - (int) Math.round(fogoHossz * Math.cos(fogoSzog + szarnySzog));
            g2.drawLine(wristCx, wristCy, fx1, fy1);
            g2.drawLine(wristCx, wristCy, fx2, fy2);
            g2.fillOval(wristCx - izR / 2, wristCy - izR / 2, izR, izR);

            // állapotjelző LED a talpon
            int ledR = Math.max(3, izR / 2);
            g2.setColor(aktiv ? new Color(60, 230, 60) : new Color(90, 90, 90));
            g2.fillOval(talpX + talpW - ledR * 2 - 3, floorY - talpH / 2 - ledR, ledR * 2, ledR * 2);
            g2.dispose();

            if (w.felirat != null && !w.felirat.isEmpty()) {
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, w.fontMeret));
                g.setColor(Color.DARK_GRAY);
                FontMetrics fm = g.getFontMetrics();
                String t1 = levag(g, w.felirat, Math.max(w.w, 70));
                g.drawString(t1, w.x + (w.w - fm.stringWidth(t1)) / 2, w.y - 4);
            }
        }

        double lerp(double a, double b, double t) { return a + (b - a) * t; }
    }

    // ================================================================ FBD panel

    class FbdPanel extends JPanel {
        boolean szerkesztesMod = false;
        FbdTipus aktivEszkoz = null;
        FbdBlock kivalasztott = null;
        Integer huzalozasForras = null;   // a jelenleg húzott huzal forrás blokkjának id-ja
        Point egerPoz = null;
        Point dragStart = null;
        int dragX0, dragY0;
        static final int PIN_R = 6;

        FbdPanel() {
            setBackground(new Color(245, 247, 250));
            ujraMeret();
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { lenyomva(e); }
                @Override public void mouseReleased(MouseEvent e) { felengedve(e); }
                @Override public void mouseClicked(MouseEvent e) { kattintva(e); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) { huzva(e); }
                @Override public void mouseMoved(MouseEvent e) { egerPoz = logikaiPont(e); if (huzalozasForras != null) repaint(); }
            });
        }

        void ujraMeret() {
            int maxX = 1400, maxY = 900;
            for (FbdBlock b : fbdBlokkok) { maxX = Math.max(maxX, b.x + b.w + 60); maxY = Math.max(maxY, b.y + b.h + 40); }
            setPreferredSize(new Dimension((int) (maxX * nagyitas), (int) (maxY * nagyitas)));
            revalidate();
        }

        Point kimenetPinPoz(FbdBlock b) { return new Point(b.x + b.w, b.y + b.h / 2); }
        Point bemenetPinPoz(FbdBlock b, int i) { return new Point(b.x, b.y + (b.h * (i + 1)) / (b.bemenetSzam + 1)); }

        FbdBlock talalTest(Point p) {
            for (int i = fbdBlokkok.size() - 1; i >= 0; i--) {
                FbdBlock b = fbdBlokkok.get(i);
                if (new Rectangle(b.x, b.y, b.w, b.h).contains(p)) return b;
            }
            return null;
        }

        FbdBlock talalKimenetPin(Point p) {
            for (int i = fbdBlokkok.size() - 1; i >= 0; i--) {
                FbdBlock b = fbdBlokkok.get(i);
                if (!fbdVanKimenetPin(b.tipus)) continue;
                if (p.distance(kimenetPinPoz(b)) <= PIN_R + 3) return b;
            }
            return null;
        }

        // {blokk, bemenet-index} - tömb hossza 2, [0]=blokk indexe a listában, [1]=bemenet index
        int[] talalBemenetPin(Point p) {
            for (int i = fbdBlokkok.size() - 1; i >= 0; i--) {
                FbdBlock b = fbdBlokkok.get(i);
                for (int k = 0; k < b.bemenetSzam; k++) {
                    if (p.distance(bemenetPinPoz(b, k)) <= PIN_R + 3) return new int[]{i, k};
                }
            }
            return null;
        }

        void lenyomva(MouseEvent e) {
            if (!szerkesztesMod) return;
            Point p = logikaiPont(e);
            if (aktivEszkoz != null) {
                FbdBlock b = new FbdBlock();
                b.tipus = aktivEszkoz;
                b.id = fbdKovetkezoId++;
                b.bemenetSzam = fbdAlapBemenetSzam(aktivEszkoz);
                b.forrasId = ujForrasTomb(b.bemenetSzam);
                b.w = (aktivEszkoz == FbdTipus.BEMENET || aktivEszkoz == FbdTipus.KIMENET) ? 110 : 90;
                b.h = Math.max(50, b.bemenetSzam * 22 + 18);
                b.x = Math.max(0, p.x - b.w / 2);
                b.y = Math.max(0, p.y - b.h / 2);
                undoPontBeallit();
                fbdBlokkok.add(b);
                kivalasztott = b;
                aktivEszkoz = null;
                if (fbdEszkozCombo != null) fbdEszkozCombo.setSelectedIndex(0);
                ujraMeret();
                repaint();
                SwingUtilities.invokeLater(() -> fbdBlokkSzerkeszto(b, true));
                return;
            }
            FbdBlock kimenetForras = talalKimenetPin(p);
            if (kimenetForras != null) {
                huzalozasForras = kimenetForras.id;
                repaint();
                return;
            }
            int[] cel = talalBemenetPin(p);
            if (cel != null) {
                FbdBlock b = fbdBlokkok.get(cel[0]);
                int ujErtek = (huzalozasForras != null) ? huzalozasForras : -1;
                if (ujErtek != b.forrasId[cel[1]]) {
                    undoPontBeallit();
                    b.forrasId[cel[1]] = ujErtek;
                    undoPontRogzit();
                }
                huzalozasForras = null;
                repaint();
                return;
            }
            FbdBlock test = talalTest(p);
            kivalasztott = test;
            huzalozasForras = null;
            if (test != null) {
                dragStart = p; dragX0 = test.x; dragY0 = test.y;
                undoPontBeallit();
            }
            repaint();
        }

        void felengedve(MouseEvent e) {
            boolean voltHuzas = (dragStart != null);
            boolean valtozott = voltHuzas && kivalasztott != null && (kivalasztott.x != dragX0 || kivalasztott.y != dragY0);
            dragStart = null;
            if (kivalasztott != null) ujraMeret();
            if (voltHuzas) { if (valtozott) undoPontRogzit(); else undoPontEldob(); }
        }

        void kattintva(MouseEvent e) {
            if (szerkesztesMod && e.getClickCount() == 2) {
                FbdBlock b = talalTest(logikaiPont(e));
                if (b != null) fbdBlokkSzerkeszto(b, false);
            }
        }

        void huzva(MouseEvent e) {
            if (szerkesztesMod && kivalasztott != null && dragStart != null) {
                Point lp = logikaiPont(e);
                int dx = lp.x - dragStart.x, dy = lp.y - dragStart.y;
                kivalasztott.x = Math.max(0, dragX0 + dx);
                kivalasztott.y = Math.max(0, dragY0 + dy);
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(nagyitas, nagyitas);

            // huzalok
            g.setStroke(new BasicStroke(1.8f));
            for (FbdBlock b : fbdBlokkok) {
                for (int i = 0; i < b.bemenetSzam; i++) {
                    int fid = (b.forrasId != null && i < b.forrasId.length) ? b.forrasId[i] : -1;
                    if (fid < 0) continue;
                    FbdBlock forras = fbdBlockById(fid);
                    if (forras == null) continue;
                    boolean aktiv = fut && forras.kimenetErtek;
                    g.setColor(aktiv ? ENERGIZALT : SEMLEGES);
                    Point p1 = kimenetPinPoz(forras), p2 = bemenetPinPoz(b, i);
                    g.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
            if (huzalozasForras != null && egerPoz != null) {
                FbdBlock forras = fbdBlockById(huzalozasForras);
                if (forras != null) {
                    g.setColor(new Color(90, 90, 190));
                    float[] dash = {5f, 4f};
                    g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                    Point p1 = kimenetPinPoz(forras);
                    g.drawLine(p1.x, p1.y, egerPoz.x, egerPoz.y);
                }
            }

            for (FbdBlock b : fbdBlokkok) rajzolBlokk(g, b);

            if (szerkesztesMod && kivalasztott != null) {
                g.setColor(new Color(30, 120, 220));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRect(kivalasztott.x - 3, kivalasztott.y - 3, kivalasztott.w + 6, kivalasztott.h + 6);
            }
            if (szerkesztesMod && aktivEszkoz != null) {
                g.setColor(new Color(90, 90, 190));
                g.setFont(HALO_FONT);
                g.drawString(tr("Kattintson a vászonra az új blokk elhelyezéséhez..."), 12, 20);
            } else if (szerkesztesMod && huzalozasForras != null) {
                g.setColor(new Color(90, 90, 190));
                g.setFont(HALO_FONT);
                g.drawString(tr("Kattintson egy bemenet pontra a huzal befejezéséhez (üres helyre kattintva megszakítja)..."), 12, 20);
            }
        }

        String blokkSzoveg(FbdBlock b) {
            switch (b.tipus) {
                case BEMENET: case KIMENET: return rovidFelirat(b.op);
                case AND: return "&";
                case OR: return ">=1";
                case NOT: return "1";
                case XOR: return "=1";
                case SR: return "SR";
                case TON: return "TON";
                case CTU: return "CTU";
                case R_TRIG: return "P";
                case F_TRIG: return "N";
            }
            return "?";
        }

        void rajzolBlokk(Graphics2D g, FbdBlock b) {
            boolean kiemelt = fut && b.kimenetErtek;
            boolean terminal = (b.tipus == FbdTipus.BEMENET || b.tipus == FbdTipus.KIMENET);

            g.setColor(terminal ? (kiemelt ? DOBOZ_HATTER_BE : DOBOZ_HATTER_KI) : (kiemelt ? new Color(225, 245, 255) : Color.WHITE));
            if (terminal) g.fillRoundRect(b.x, b.y, b.w, b.h, 16, 16); else g.fillRect(b.x, b.y, b.w, b.h);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.8f));
            if (terminal) g.drawRoundRect(b.x, b.y, b.w, b.h, 16, 16); else g.drawRect(b.x, b.y, b.w, b.h);

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, terminal ? 11 : 15));
            FontMetrics fm = g.getFontMetrics();
            String txt = terminal ? levag(g, blokkSzoveg(b), b.w - 10) : blokkSzoveg(b);
            g.drawString(txt, b.x + (b.w - fm.stringWidth(txt)) / 2, b.y + b.h / 2 + (terminal ? 4 : 5));

            if (!terminal && !b.felirat.isEmpty()) {
                g.setFont(CIM_FONT);
                g.setColor(Color.GRAY);
                g.drawString(levag(g, b.felirat, b.w), b.x, b.y - 4);
            }
            if (b.tipus == FbdTipus.TON || b.tipus == FbdTipus.CTU) {
                g.setFont(CIM_FONT);
                g.setColor(Color.DARK_GRAY);
                String info = b.tipus == FbdTipus.TON
                        ? ("T" + b.idx + "  " + allapot.t[b.idx].akkMs + "/" + b.presetMs + "ms")
                        : ("C" + b.idx + "  " + allapot.c[b.idx].ertek + "/" + b.presetSzamlalo);
                g.drawString(levag(g, info, b.w), b.x, b.y + b.h + 12);
            }

            String[] beFeliratok = fbdBemenetFeliratok(b.tipus);
            g.setFont(CIM_FONT);
            for (int i = 0; i < b.bemenetSzam; i++) {
                Point p = bemenetPinPoz(b, i);
                int fid = (b.forrasId != null && i < b.forrasId.length) ? b.forrasId[i] : -1;
                boolean kotve = fid >= 0 && fbdBlockById(fid) != null;
                boolean aktiv = fut && kotve && fbdKimenetOlvas(fid);
                g.setColor(kotve ? (aktiv ? ENERGIZALT : SEMLEGES) : new Color(190, 190, 190));
                g.fillOval(p.x - PIN_R, p.y - PIN_R, PIN_R * 2, PIN_R * 2);
                g.setColor(Color.BLACK);
                g.drawOval(p.x - PIN_R, p.y - PIN_R, PIN_R * 2, PIN_R * 2);
                if (beFeliratok != null && i < beFeliratok.length) {
                    g.setColor(Color.DARK_GRAY);
                    g.drawString(beFeliratok[i], p.x + PIN_R + 2, p.y + 4);
                }
            }
            if (fbdVanKimenetPin(b.tipus)) {
                Point p = kimenetPinPoz(b);
                g.setColor(kiemelt ? ENERGIZALT : SEMLEGES);
                g.fillOval(p.x - PIN_R, p.y - PIN_R, PIN_R * 2, PIN_R * 2);
                g.setColor(Color.BLACK);
                g.drawOval(p.x - PIN_R, p.y - PIN_R, PIN_R * 2, PIN_R * 2);
                if (b.tipus == FbdTipus.NOT) {
                    // NOT kapu kimeneti "buborék" (IEC jelölés: doboz "1" + invertáló kör)
                    g.drawOval(p.x + PIN_R + 1, p.y - 4, 8, 8);
                }
                String kiLab = (b.tipus == FbdTipus.SR || b.tipus == FbdTipus.TON || b.tipus == FbdTipus.CTU) ? "Q" : null;
                if (kiLab != null) {
                    g.setColor(Color.DARK_GRAY);
                    FontMetrics fm2 = g.getFontMetrics();
                    g.drawString(kiLab, p.x - PIN_R - 4 - fm2.stringWidth(kiLab), p.y + 4);
                }
            }
        }
    }

    static int[] ujForrasTomb(int n) {
        int[] t = new int[n];
        Arrays.fill(t, -1);
        return t;
    }

    // ================================================================ GRAFCET panel

    double pontTavSzakasztol(Point p, Point a, Point b) {
        double dx = b.x - a.x, dy = b.y - a.y;
        double len2 = dx * dx + dy * dy;
        if (len2 < 0.0001) return p.distance(a);
        double t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double px = a.x + t * dx, py = a.y + t * dy;
        return Math.hypot(p.x - px, p.y - py);
    }

    String grafcetFeltetelSzoveg(GrafcetAtmenet t) {
        if (t.feltetel.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.feltetel.size(); i++) {
            Erintkezo e = t.feltetel.get(i);
            if (i > 0) sb.append(" • ");
            if (!e.no) sb.append("/");
            sb.append(rovidFelirat(e.op));
        }
        return sb.toString();
    }

    class GrafcetPanel extends JPanel {
        boolean szerkesztesMod = false;
        String aktivEszkoz = null;   // "LEPES" vagy "ATMENET", vagy null = kiválasztás
        GrafcetLepes kivalasztottLepes = null;
        GrafcetAtmenet kivalasztottAtmenet = null;
        GrafcetLepes huzalLepes = null;
        GrafcetAtmenet huzalAtmenet = null;
        Point egerPoz = null;
        Point dragStart = null;
        boolean dragLepes = true;
        int dragX0, dragY0;
        static final int PIN_R = 6;

        GrafcetPanel() {
            setBackground(new Color(248, 248, 242));
            ujraMeret();
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { lenyomva(e); }
                @Override public void mouseReleased(MouseEvent e) { felengedve(e); }
                @Override public void mouseClicked(MouseEvent e) { kattintva(e); }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent e) { huzva(e); }
                @Override public void mouseMoved(MouseEvent e) { egerPoz = logikaiPont(e); if (huzalLepes != null || huzalAtmenet != null) repaint(); }
            });
        }

        void ujraMeret() {
            int maxX = 1200, maxY = 900;
            for (GrafcetLepes l : grafcet.lepesek) { maxX = Math.max(maxX, l.x + l.w + 160); maxY = Math.max(maxY, l.y + l.h + 40); }
            for (GrafcetAtmenet t : grafcet.atmenetek) { maxX = Math.max(maxX, t.x + t.w + 160); maxY = Math.max(maxY, t.y + t.h + 40); }
            setPreferredSize(new Dimension((int) (maxX * nagyitas), (int) (maxY * nagyitas)));
            revalidate();
        }

        Point lepesBemenetPoz(GrafcetLepes l) { return new Point(l.x + l.w / 2, l.y); }
        Point lepesKimenetPoz(GrafcetLepes l) { return new Point(l.x + l.w / 2, l.y + l.h); }
        Point atmenetBemenetPoz(GrafcetAtmenet t) { return new Point(t.x + t.w / 2, t.y); }
        Point atmenetKimenetPoz(GrafcetAtmenet t) { return new Point(t.x + t.w / 2, t.y + t.h); }

        GrafcetLepes talalLepesTest(Point p) {
            for (int i = grafcet.lepesek.size() - 1; i >= 0; i--) {
                GrafcetLepes l = grafcet.lepesek.get(i);
                if (new Rectangle(l.x, l.y, l.w, l.h).contains(p)) return l;
            }
            return null;
        }

        GrafcetAtmenet talalAtmenetTest(Point p) {
            for (int i = grafcet.atmenetek.size() - 1; i >= 0; i--) {
                GrafcetAtmenet t = grafcet.atmenetek.get(i);
                if (new Rectangle(t.x - 4, t.y - 10, t.w + 8, t.h + 20).contains(p)) return t;
            }
            return null;
        }

        GrafcetLepes talalLepesKimenetPin(Point p) {
            for (int i = grafcet.lepesek.size() - 1; i >= 0; i--) {
                GrafcetLepes l = grafcet.lepesek.get(i);
                if (p.distance(lepesKimenetPoz(l)) <= PIN_R + 3) return l;
            }
            return null;
        }

        GrafcetLepes talalLepesBemenetPin(Point p) {
            for (int i = grafcet.lepesek.size() - 1; i >= 0; i--) {
                GrafcetLepes l = grafcet.lepesek.get(i);
                if (p.distance(lepesBemenetPoz(l)) <= PIN_R + 3) return l;
            }
            return null;
        }

        GrafcetAtmenet talalAtmenetKimenetPin(Point p) {
            for (int i = grafcet.atmenetek.size() - 1; i >= 0; i--) {
                GrafcetAtmenet t = grafcet.atmenetek.get(i);
                if (p.distance(atmenetKimenetPoz(t)) <= PIN_R + 3) return t;
            }
            return null;
        }

        GrafcetAtmenet talalAtmenetBemenetPin(Point p) {
            for (int i = grafcet.atmenetek.size() - 1; i >= 0; i--) {
                GrafcetAtmenet t = grafcet.atmenetek.get(i);
                if (p.distance(atmenetBemenetPoz(t)) <= PIN_R + 3) return t;
            }
            return null;
        }

        void lenyomva(MouseEvent e) {
            if (!szerkesztesMod) return;
            Point p = logikaiPont(e);

            if ("LEPES".equals(aktivEszkoz)) {
                GrafcetLepes l = new GrafcetLepes();
                l.id = grafcet.kovetkezoLepesId++;
                l.x = Math.max(0, p.x - l.w / 2);
                l.y = Math.max(0, p.y - l.h / 2);
                undoPontBeallit();
                grafcet.lepesek.add(l);
                kivalasztottLepes = l; kivalasztottAtmenet = null;
                aktivEszkoz = null;
                if (grafcetEszkozCombo != null) grafcetEszkozCombo.setSelectedIndex(0);
                ujraMeret(); repaint();
                SwingUtilities.invokeLater(() -> grafcetLepesSzerkeszto(l, true));
                return;
            }
            if ("ATMENET".equals(aktivEszkoz)) {
                GrafcetAtmenet t = new GrafcetAtmenet();
                t.id = grafcet.kovetkezoAtmenetId++;
                t.x = Math.max(0, p.x - t.w / 2);
                t.y = Math.max(0, p.y - t.h / 2);
                undoPontBeallit();
                grafcet.atmenetek.add(t);
                kivalasztottAtmenet = t; kivalasztottLepes = null;
                aktivEszkoz = null;
                if (grafcetEszkozCombo != null) grafcetEszkozCombo.setSelectedIndex(0);
                ujraMeret(); repaint();
                SwingUtilities.invokeLater(() -> grafcetAtmenetSzerkeszto(t, true));
                return;
            }

            GrafcetLepes lKi = talalLepesKimenetPin(p);
            if (lKi != null) { huzalLepes = lKi; huzalAtmenet = null; repaint(); return; }
            GrafcetAtmenet tKi = talalAtmenetKimenetPin(p);
            if (tKi != null) { huzalAtmenet = tKi; huzalLepes = null; repaint(); return; }

            GrafcetLepes lBe = talalLepesBemenetPin(p);
            if (lBe != null) {
                if (huzalAtmenet != null) {
                    undoPontBeallit();
                    if (!huzalAtmenet.celLepesek.contains(lBe.id)) huzalAtmenet.celLepesek.add(lBe.id);
                    undoPontRogzit();
                    huzalAtmenet = null;
                }
                repaint();
                return;
            }
            GrafcetAtmenet tBe = talalAtmenetBemenetPin(p);
            if (tBe != null) {
                if (huzalLepes != null) {
                    undoPontBeallit();
                    if (!tBe.forrasLepesek.contains(huzalLepes.id)) tBe.forrasLepesek.add(huzalLepes.id);
                    undoPontRogzit();
                    huzalLepes = null;
                }
                repaint();
                return;
            }

            GrafcetLepes lTest = talalLepesTest(p);
            if (lTest != null) {
                kivalasztottLepes = lTest; kivalasztottAtmenet = null;
                huzalLepes = null; huzalAtmenet = null;
                dragStart = p; dragLepes = true; dragX0 = lTest.x; dragY0 = lTest.y;
                undoPontBeallit();
                repaint();
                return;
            }
            GrafcetAtmenet tTest = talalAtmenetTest(p);
            if (tTest != null) {
                kivalasztottAtmenet = tTest; kivalasztottLepes = null;
                huzalLepes = null; huzalAtmenet = null;
                dragStart = p; dragLepes = false; dragX0 = tTest.x; dragY0 = tTest.y;
                undoPontBeallit();
                repaint();
                return;
            }

            // huzal törlése, ha a kattintás egy vezetékre esik
            for (GrafcetAtmenet t : grafcet.atmenetek) {
                for (int i = 0; i < t.forrasLepesek.size(); i++) {
                    GrafcetLepes s = grafcetLepesById(t.forrasLepesek.get(i));
                    if (s == null) continue;
                    if (pontTavSzakasztol(p, lepesKimenetPoz(s), atmenetBemenetPoz(t)) <= 5) {
                        undoPontBeallit();
                        t.forrasLepesek.remove(i);
                        undoPontRogzit();
                        repaint();
                        return;
                    }
                }
                for (int i = 0; i < t.celLepesek.size(); i++) {
                    GrafcetLepes c = grafcetLepesById(t.celLepesek.get(i));
                    if (c == null) continue;
                    if (pontTavSzakasztol(p, atmenetKimenetPoz(t), lepesBemenetPoz(c)) <= 5) {
                        undoPontBeallit();
                        t.celLepesek.remove(i);
                        undoPontRogzit();
                        repaint();
                        return;
                    }
                }
            }

            kivalasztottLepes = null; kivalasztottAtmenet = null;
            huzalLepes = null; huzalAtmenet = null;
            dragStart = null;
            repaint();
        }

        void felengedve(MouseEvent e) {
            boolean voltHuzas = (dragStart != null);
            boolean valtozott = false;
            if (voltHuzas) {
                if (dragLepes && kivalasztottLepes != null) valtozott = (kivalasztottLepes.x != dragX0 || kivalasztottLepes.y != dragY0);
                else if (!dragLepes && kivalasztottAtmenet != null) valtozott = (kivalasztottAtmenet.x != dragX0 || kivalasztottAtmenet.y != dragY0);
            }
            dragStart = null;
            ujraMeret();
            if (voltHuzas) { if (valtozott) undoPontRogzit(); else undoPontEldob(); }
        }

        void kattintva(MouseEvent e) {
            if (szerkesztesMod && e.getClickCount() == 2) {
                Point p = logikaiPont(e);
                GrafcetLepes l = talalLepesTest(p);
                if (l != null) { grafcetLepesSzerkeszto(l, false); return; }
                GrafcetAtmenet t = talalAtmenetTest(p);
                if (t != null) { grafcetAtmenetSzerkeszto(t, false); return; }
            }
        }

        void huzva(MouseEvent e) {
            if (szerkesztesMod && dragStart != null) {
                Point lp = logikaiPont(e);
                int dx = lp.x - dragStart.x, dy = lp.y - dragStart.y;
                if (dragLepes && kivalasztottLepes != null) {
                    kivalasztottLepes.x = Math.max(0, dragX0 + dx);
                    kivalasztottLepes.y = Math.max(0, dragY0 + dy);
                } else if (!dragLepes && kivalasztottAtmenet != null) {
                    kivalasztottAtmenet.x = Math.max(0, dragX0 + dx);
                    kivalasztottAtmenet.y = Math.max(0, dragY0 + dy);
                }
                repaint();
            }
        }

        void rajzolNyil(Graphics2D g, Point a, Point b) {
            g.drawLine(a.x, a.y, b.x, b.y);
            double ang = Math.atan2(b.y - a.y, b.x - a.x);
            int hossz = 8;
            double a1 = ang + Math.toRadians(150), a2 = ang - Math.toRadians(150);
            g.drawLine(b.x, b.y, (int) (b.x + hossz * Math.cos(a1)), (int) (b.y + hossz * Math.sin(a1)));
            g.drawLine(b.x, b.y, (int) (b.x + hossz * Math.cos(a2)), (int) (b.y + hossz * Math.sin(a2)));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.scale(nagyitas, nagyitas);

            g.setStroke(new BasicStroke(1.6f));
            for (GrafcetAtmenet t : grafcet.atmenetek) {
                for (int sid : t.forrasLepesek) {
                    GrafcetLepes s = grafcetLepesById(sid);
                    if (s == null) continue;
                    g.setColor(fut && s.aktiv ? ENERGIZALT : SEMLEGES);
                    rajzolNyil(g, lepesKimenetPoz(s), atmenetBemenetPoz(t));
                }
                boolean sulhet = fut && grafcetAtmenetSulhet(t);
                for (int tid : t.celLepesek) {
                    GrafcetLepes c = grafcetLepesById(tid);
                    if (c == null) continue;
                    g.setColor(sulhet ? ENERGIZALT : SEMLEGES);
                    rajzolNyil(g, atmenetKimenetPoz(t), lepesBemenetPoz(c));
                }
            }
            if ((huzalLepes != null || huzalAtmenet != null) && egerPoz != null) {
                g.setColor(new Color(90, 90, 190));
                float[] dash = {5f, 4f};
                g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                Point forras = huzalLepes != null ? lepesKimenetPoz(huzalLepes) : atmenetKimenetPoz(huzalAtmenet);
                g.drawLine(forras.x, forras.y, egerPoz.x, egerPoz.y);
            }

            for (GrafcetAtmenet t : grafcet.atmenetek) rajzolAtmenet(g, t);
            for (GrafcetLepes l : grafcet.lepesek) rajzolLepes(g, l);

            if (szerkesztesMod && kivalasztottLepes != null) {
                g.setColor(new Color(30, 120, 220));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRect(kivalasztottLepes.x - 3, kivalasztottLepes.y - 3, kivalasztottLepes.w + 6, kivalasztottLepes.h + 6);
            }
            if (szerkesztesMod && kivalasztottAtmenet != null) {
                g.setColor(new Color(30, 120, 220));
                float[] dash = {4f, 3f};
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
                g.drawRect(kivalasztottAtmenet.x - 3, kivalasztottAtmenet.y - 8, kivalasztottAtmenet.w + 6, kivalasztottAtmenet.h + 16);
            }
            if (szerkesztesMod && aktivEszkoz != null) {
                g.setColor(new Color(90, 90, 190));
                g.setFont(HALO_FONT);
                g.drawString(tr("Kattintson a vászonra az új elem elhelyezéséhez..."), 12, 20);
            }
        }

        void rajzolLepes(Graphics2D g, GrafcetLepes l) {
            boolean aktiv = fut && l.aktiv;
            g.setColor(aktiv ? DOBOZ_HATTER_BE : Color.WHITE);
            g.fillRect(l.x, l.y, l.w, l.h);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.8f));
            g.drawRect(l.x, l.y, l.w, l.h);
            if (l.kezdeti) g.drawRect(l.x + 4, l.y + 4, l.w - 8, l.h - 8);

            g.setFont(HALO_FONT);
            String szam = String.valueOf(l.id);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(szam, l.x + (l.w - fm.stringWidth(szam)) / 2, l.y + l.h / 2 + 5);

            Point be = lepesBemenetPoz(l), ki = lepesKimenetPoz(l);
            g.setColor(SEMLEGES);
            g.fillOval(be.x - 4, be.y - 4, 8, 8);
            g.fillOval(ki.x - 4, ki.y - 4, 8, 8);
            g.setColor(Color.BLACK);
            g.drawOval(be.x - 4, be.y - 4, 8, 8);
            g.drawOval(ki.x - 4, ki.y - 4, 8, 8);

            g.setFont(CIM_FONT);
            if (l.felirat != null && !l.felirat.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString(levag(g, l.felirat, l.w + 100), l.x, l.y - 4);
            }
            int ay = l.y + 12;
            g.setColor(Color.DARK_GRAY);
            for (GrafcetAkcio a : l.akciok) {
                String jel = a.tipus == GrafcetAkcioTipus.SET ? "S" : (a.tipus == GrafcetAkcioTipus.RESET ? "R" : "N");
                String sor = jel + "  " + rovidFelirat(a.cel);
                g.drawString(levag(g, sor, 130), l.x + l.w + 8, ay);
                ay += 13;
            }
        }

        void rajzolAtmenet(Graphics2D g, GrafcetAtmenet t) {
            boolean suelhet = fut && grafcetAtmenetSulhet(t);
            g.setColor(suelhet ? ENERGIZALT : Color.BLACK);
            g.setStroke(new BasicStroke(3f));
            g.drawLine(t.x, t.y + t.h / 2, t.x + t.w, t.y + t.h / 2);

            Point be = atmenetBemenetPoz(t), ki = atmenetKimenetPoz(t);
            g.setColor(SEMLEGES);
            g.fillOval(be.x - 4, be.y - 4, 8, 8);
            g.fillOval(ki.x - 4, ki.y - 4, 8, 8);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1f));
            g.drawOval(be.x - 4, be.y - 4, 8, 8);
            g.drawOval(ki.x - 4, ki.y - 4, 8, 8);

            g.setFont(CIM_FONT);
            g.setColor(Color.DARK_GRAY);
            String felt = grafcetFeltetelSzoveg(t);
            g.drawString(levag(g, felt, 150), t.x + t.w + 8, t.y + t.h / 2 + 4);
        }
    }

    // ================================================================ szerkesztő dialógusok

    Erintkezo erintkezoSzerkeszto(Erintkezo kiindulo, boolean torolhetoE) {
        JComboBox<String> tipusVal = new JComboBox<>(new String[]{tr("Bemenet (I)"), tr("Kimenet (Q)"), tr("Belső relé (M)"), tr("Időzítő kész (T.DN)"), tr("Számláló kész (C.DN)")});
        JComboBox<String> fajtaVal = new JComboBox<>(new String[]{tr("Záró (NO)"), tr("Nyitó (NC)")});
        SpinnerNumberModel idxModel = new SpinnerNumberModel(0, 0, 15, 1);
        JSpinner idxSpin = new JSpinner(idxModel);

        OpTipus[] map = {OpTipus.I, OpTipus.Q, OpTipus.M, OpTipus.TDN, OpTipus.CDN};
        int startSel = 0;
        for (int k = 0; k < map.length; k++) if (map[k] == kiindulo.op.tipus) startSel = k;
        tipusVal.setSelectedIndex(startSel);
        fajtaVal.setSelectedIndex(kiindulo.no ? 0 : 1);
        idxModel.setMaximum(Operandus.maxIndex(kiindulo.op.tipus));
        idxModel.setValue(kiindulo.op.index);

        tipusVal.addActionListener(e -> {
            OpTipus t = map[tipusVal.getSelectedIndex()];
            int max = Operandus.maxIndex(t);
            int cur = (Integer) idxSpin.getValue();
            idxModel.setMaximum(max);
            if (cur > max) idxSpin.setValue(0);
        });

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel(tr("Érintkező típusa:"))); panel.add(fajtaVal);
        panel.add(new JLabel(tr("Operandus típusa:"))); panel.add(tipusVal);
        panel.add(new JLabel(tr("Index:"))); panel.add(idxSpin);

        Object[] opciok = torolhetoE ? new Object[]{tr("OK"), tr("Törlés"), tr("Mégse")} : new Object[]{tr("OK"), tr("Mégse")};
        int v = JOptionPane.showOptionDialog(this, panel, tr("Érintkező szerkesztése"), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opciok, opciok[0]);

        if (torolhetoE && v == 1) return TOROL_JEL;
        if (v != 0) return null;

        Erintkezo e = new Erintkezo();
        e.no = fajtaVal.getSelectedIndex() == 0;
        e.op = new Operandus(map[tipusVal.getSelectedIndex()], (Integer) idxSpin.getValue());
        return e;
    }

    // ================================================================ címkék (tag-ek) szerkesztője

    void cimkeSzerkeszto() {
        undoPontBeallit();
        List<String[]> sorok = new ArrayList<>(); // {kulcs, leiras}
        for (int k = 0; k < 16; k++) sorok.add(new String[]{cimkeKulcs(OpTipus.I, k), addrI(k)});
        for (int k = 0; k < 16; k++) sorok.add(new String[]{cimkeKulcs(OpTipus.Q, k), addrQ(k)});
        for (int k = 0; k < 16; k++) sorok.add(new String[]{cimkeKulcs(OpTipus.M, k), addrM(k)});
        for (int k = 0; k < 8; k++) sorok.add(new String[]{cimkeKulcs(OpTipus.TDN, k), "T" + k});
        for (int k = 0; k < 8; k++) sorok.add(new String[]{cimkeKulcs(OpTipus.CDN, k), "C" + k});

        String[] oszlopok = {tr("Cím"), tr("Név"), tr("Megjegyzés")};
        Object[][] adat = new Object[sorok.size()][3];
        for (int r = 0; r < sorok.size(); r++) {
            String kulcs = sorok.get(r)[0];
            adat[r][0] = sorok.get(r)[1];
            adat[r][1] = cimkeNev.getOrDefault(kulcs, "");
            adat[r][2] = cimkeMegj.getOrDefault(kulcs, "");
        }
        DefaultTableModel model = new DefaultTableModel(adat, oszlopok) {
            @Override public boolean isCellEditable(int row, int col) { return col != 0; }
        };
        JTable tabla = new JTable(model);
        tabla.setRowHeight(22);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(260);

        JScrollPane sp = new JScrollPane(tabla);
        sp.setPreferredSize(new Dimension(560, 420));

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        fo.add(new JLabel(tr("Adjon nevet és/vagy megjegyzést a bemeneteknek, kimeneteknek, belső reléknek, időzítőknek és számlálóknak:")), BorderLayout.NORTH);
        fo.add(sp, BorderLayout.CENTER);

        int v = JOptionPane.showConfirmDialog(this, fo, tr("Címkék (tag-ek) szerkesztése"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (v != JOptionPane.OK_OPTION) { undoPontEldob(); return; }

        if (tabla.isEditing()) tabla.getCellEditor().stopCellEditing();
        for (int r = 0; r < sorok.size(); r++) {
            String kulcs = sorok.get(r)[0];
            String nev = String.valueOf(model.getValueAt(r, 1)).trim();
            String megj = String.valueOf(model.getValueAt(r, 2)).trim();
            if (nev.isEmpty()) cimkeNev.remove(kulcs); else cimkeNev.put(kulcs, nev);
            if (megj.isEmpty()) cimkeMegj.remove(kulcs); else cimkeMegj.put(kulcs, megj);
        }
        undoPontRogzit();
        frissitIOPanel();
        letra.ujraMeret();
        letra.repaint();
        if (hmi != null) hmi.repaint();
        if (fbd != null) fbd.repaint();
        if (grafcetPanel != null) grafcetPanel.repaint();
    }

    // ================================================================ óraimpulzus (clock bit) szerkesztő

    String oraImpulzusSzoveg(OraImpulzus o) {
        return addrM(o.mIndex) + "  –  " + o.frekvenciaHz + " Hz";
    }

    OraImpulzus oraImpulzusSorSzerkeszto(OraImpulzus kiindulo) {
        SpinnerNumberModel idxModel = new SpinnerNumberModel(kiindulo.mIndex, 0, 15, 1);
        JSpinner idxSpin = new JSpinner(idxModel);
        SpinnerNumberModel frekvModel = new SpinnerNumberModel(kiindulo.frekvenciaHz, 0.1, 50.0, 0.1);
        JSpinner frekvSpin = new JSpinner(frekvModel);
        ((JSpinner.NumberEditor) frekvSpin.getEditor()).getFormat().setMinimumFractionDigits(1);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel(tr("Cél (M index):"))); panel.add(idxSpin);
        panel.add(new JLabel(tr("Frekvencia (Hz):"))); panel.add(frekvSpin);

        int v = JOptionPane.showConfirmDialog(this, panel, tr("Óraimpulzus szerkesztése"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (v != JOptionPane.OK_OPTION) return null;

        OraImpulzus o = new OraImpulzus();
        o.mIndex = (Integer) idxSpin.getValue();
        o.frekvenciaHz = (Double) frekvModel.getValue();
        return o;
    }

    void oraImpulzusSzerkeszto() {
        undoPontBeallit();
        List<OraImpulzus> munka = new ArrayList<>();
        for (OraImpulzus o : oraImpulzusok) { OraImpulzus m = new OraImpulzus(); m.mIndex = o.mIndex; m.frekvenciaHz = o.frekvenciaHz; munka.add(m); }
        DefaultListModel<String> model = new DefaultListModel<>();
        for (OraImpulzus o : munka) model.addElement(oraImpulzusSzoveg(o));
        JList<String> lista = new JList<>(model);
        JScrollPane scroll = new JScrollPane(lista);
        scroll.setPreferredSize(new Dimension(280, 160));

        JButton hozzaadGomb = new JButton(tr("+ Hozzáad"));
        hozzaadGomb.addActionListener(e -> {
            OraImpulzus eredmeny = oraImpulzusSorSzerkeszto(new OraImpulzus());
            if (eredmeny != null) { munka.add(eredmeny); model.addElement(oraImpulzusSzoveg(eredmeny)); }
        });
        JButton szerkesztGomb = new JButton(tr("Szerkesztés..."));
        szerkesztGomb.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx < 0) return;
            OraImpulzus eredmeny = oraImpulzusSorSzerkeszto(munka.get(idx));
            if (eredmeny != null) { munka.set(idx, eredmeny); model.set(idx, oraImpulzusSzoveg(eredmeny)); }
        });
        JButton torolGomb = new JButton(tr("Törlés"));
        torolGomb.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx >= 0) { munka.remove(idx); model.remove(idx); }
        });
        JPanel gombok = new JPanel(new FlowLayout(FlowLayout.LEFT));
        gombok.add(hozzaadGomb); gombok.add(szerkesztGomb); gombok.add(torolGomb);

        JLabel info = new JLabel("<html><body style='width:300px'>" + tr("Minden sor egy belső relét (M) adott frekvencián automatikusan kapcsol (50%-os kitöltéssel) – pl. vibráló jelzőlámpához. A bit bármely létra-, FBD- vagy HMI-elemben felhasználható kontaktusként.") + "</body></html>");
        info.setBorder(new EmptyBorder(0, 0, 8, 0));

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        fo.add(info, BorderLayout.NORTH);
        fo.add(scroll, BorderLayout.CENTER);
        fo.add(gombok, BorderLayout.SOUTH);

        int v = JOptionPane.showConfirmDialog(this, fo, tr("Óraimpulzusok (clock bitek) szerkesztése"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (v != JOptionPane.OK_OPTION) { undoPontEldob(); return; }

        Set<Integer> latott = new HashSet<>();
        for (OraImpulzus o : munka) {
            if (!latott.add(o.mIndex)) {
                JOptionPane.showMessageDialog(this, tr("Két óraimpulzus nem célozhatja ugyanazt az M bitet:") + " " + addrM(o.mIndex), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
                undoPontEldob();
                return;
            }
        }

        oraImpulzusok = munka;
        undoPontRogzit();
        frissitIOPanel();
    }

    // ================================================================ HMI elem szerkesztője

    void widgetSzerkeszto(HmiWidget w, boolean uj) {
        if (!uj) undoPontBeallit();
        JComboBox<String> tipusVal = new JComboBox<>(new String[]{tr("Lámpa"), tr("Gomb"), tr("Kijelző"), tr("Felirat"), tr("Téglalap"), tr("Trend"), tr("Zebra"), tr("Ajtó"), tr("Szállítószalag"), tr("Érzékelő"), tr("Fanuc robot"), tr("Kuka robot")});
        HmiTipus[] map = {HmiTipus.LAMPA, HmiTipus.GOMB, HmiTipus.KIJELZO, HmiTipus.FELIRAT, HmiTipus.TEGLALAP, HmiTipus.TREND, HmiTipus.ZEBRA, HmiTipus.AJTO, HmiTipus.SZALLITOSZALAG, HmiTipus.SZENZOR, HmiTipus.ROBOT_FANUC, HmiTipus.ROBOT_KUKA};
        for (int i = 0; i < map.length; i++) if (map[i] == w.tipus) tipusVal.setSelectedIndex(i);

        JTextField feliratVal = new JTextField(w.felirat, 18);

        JComboBox<String> opTipusVal = new JComboBox<>(new String[]{tr("Bemenet (I)"), tr("Kimenet (Q)"), tr("Belső relé (M)"), tr("Időzítő (T)"), tr("Számláló (C)")});
        OpTipus[] opMap = {OpTipus.I, OpTipus.Q, OpTipus.M, OpTipus.TDN, OpTipus.CDN};
        int startSel = 0;
        for (int i = 0; i < opMap.length; i++) if (opMap[i] == w.op.tipus) startSel = i;
        opTipusVal.setSelectedIndex(startSel);
        SpinnerNumberModel idxModel = new SpinnerNumberModel(w.op.index, 0, Operandus.maxIndex(w.op.tipus), 1);
        JSpinner idxSpin = new JSpinner(idxModel);
        opTipusVal.addActionListener(e -> {
            OpTipus t = opMap[opTipusVal.getSelectedIndex()];
            int max = Operandus.maxIndex(t);
            idxModel.setMaximum(max);
            if ((Integer) idxSpin.getValue() > max) idxSpin.setValue(0);
        });

        JCheckBox szamkentVal = new JCheckBox(tr("Számérték (T/C aktuális értéke) a kész-bit helyett"), w.szamkent);
        JComboBox<String> gombModVal = new JComboBox<>(new String[]{tr("Váltó – kattintásra kapcsol"), tr("Pillanat – csak lenyomva aktív")});
        gombModVal.setSelectedIndex(w.gombMod == GombMod.VALTO ? 0 : 1);

        JButton szinBeGomb = new JButton(tr("Szín: BE / kitöltés"));
        szinBeGomb.setBackground(w.szinBe);
        szinBeGomb.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, tr("Szín kiválasztása"), szinBeGomb.getBackground());
            if (c != null) szinBeGomb.setBackground(c);
        });
        JButton szinKiGomb = new JButton(tr("Szín: KI / keret"));
        szinKiGomb.setBackground(w.szinKi);
        szinKiGomb.addActionListener(e -> {
            Color c = JColorChooser.showDialog(this, tr("Szín kiválasztása"), szinKiGomb.getBackground());
            if (c != null) szinKiGomb.setBackground(c);
        });

        SpinnerNumberModel fontModel = new SpinnerNumberModel(w.fontMeret, 8, 48, 1);
        JSpinner fontSpin = new JSpinner(fontModel);

        JComboBox<String> forgatasVal = new JComboBox<>(new String[]{"0°", "90°", "180°", "270°"});
        int[] forgatasMap = {0, 90, 180, 270};
        for (int i = 0; i < forgatasMap.length; i++) if (forgatasMap[i] == w.forgatas) forgatasVal.setSelectedIndex(i);

        JCheckBox tukrozveVal = new JCheckBox(tr("Tükrözve (ellentétes irányba néz)"), w.tukrozve);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel(tr("Widget típusa:"))); panel.add(tipusVal);
        panel.add(new JLabel(tr("Felirat / szöveg:"))); panel.add(feliratVal);
        panel.add(new JLabel(tr("Operandus típusa:"))); panel.add(opTipusVal);
        panel.add(new JLabel(tr("Index:"))); panel.add(idxSpin);
        panel.add(new JLabel(tr("Kijelzőnél:"))); panel.add(szamkentVal);
        panel.add(new JLabel(tr("Gombnál:"))); panel.add(gombModVal);
        panel.add(new JLabel(tr("Szín 1 (lámpa BE / téglalap kitöltés):"))); panel.add(szinBeGomb);
        panel.add(new JLabel(tr("Szín 2 (lámpa KI / téglalap-felirat szín):"))); panel.add(szinKiGomb);
        panel.add(new JLabel(tr("Betűméret:"))); panel.add(fontSpin);
        panel.add(new JLabel(tr("Forgatás (érzékelőnél):"))); panel.add(forgatasVal);
        panel.add(new JLabel(tr("Robotkarnál:"))); panel.add(tukrozveVal);

        Object[] opciok = {tr("OK"), tr("Törlés"), tr("Mégse")};
        int v = JOptionPane.showOptionDialog(this, panel, tr("HMI elem szerkesztése"), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opciok, opciok[0]);

        if (v == 1) {
            hmiWidgetek.remove(w);
            hmi.kivalasztott = null;
            if (uj) undoPontEldob(); else undoPontRogzit();
            hmi.ujraMeret();
            hmi.repaint();
            return;
        }
        if (v != 0) {
            if (uj) { hmiWidgetek.remove(w); hmi.kivalasztott = null; }
            undoPontEldob();
            hmi.ujraMeret();
            hmi.repaint();
            return;
        }

        w.tipus = map[tipusVal.getSelectedIndex()];
        w.felirat = feliratVal.getText();
        w.op = new Operandus(opMap[opTipusVal.getSelectedIndex()], (Integer) idxSpin.getValue());
        w.szamkent = szamkentVal.isSelected();
        w.gombMod = gombModVal.getSelectedIndex() == 0 ? GombMod.VALTO : GombMod.PILLANAT;
        w.szinBe = szinBeGomb.getBackground();
        w.szinKi = szinKiGomb.getBackground();
        w.fontMeret = (Integer) fontSpin.getValue();
        w.forgatas = forgatasMap[forgatasVal.getSelectedIndex()];
        w.tukrozve = tukrozveVal.isSelected();
        undoPontRogzit();
        hmi.ujraMeret();
        hmi.repaint();
    }

    void hmiKepernyoValtas(int idx) {
        if (idx < 0 || idx >= hmiKepernyok.size()) return;
        hmiAktivKepernyo = idx;
        hmiWidgetek = hmiKepernyok.get(idx).widgetek;
        hmi.kivalasztott = null;
        hmi.ujraMeret();
        hmi.repaint();
    }

    void hmiKepernyoComboFrissit() {
        hmiKepernyoCombo.removeAllItems();
        for (HmiKepernyo k : hmiKepernyok) hmiKepernyoCombo.addItem(k.nev);
        hmiKepernyoCombo.setSelectedIndex(hmiAktivKepernyo);
    }

    JPanel epitHmiLap() {
        JPanel fo = new JPanel(new BorderLayout());

        JToolBar kepTb = new JToolBar();
        kepTb.setFloatable(false);
        kepTb.add(new JLabel(tr("Képernyő: ")));
        hmiKepernyoCombo = new JComboBox<>();
        for (HmiKepernyo k : hmiKepernyok) hmiKepernyoCombo.addItem(k.nev);
        hmiKepernyoCombo.setSelectedIndex(hmiAktivKepernyo);
        hmiKepernyoCombo.addActionListener(e -> hmiKepernyoValtas(hmiKepernyoCombo.getSelectedIndex()));
        kepTb.add(hmiKepernyoCombo);

        JButton ujKepGomb = new JButton(tr("+ Új képernyő"));
        ujKepGomb.addActionListener(e -> {
            String nev = JOptionPane.showInputDialog(this, tr("Új képernyő neve:"), tr("Képernyő") + " " + (hmiKepernyok.size() + 1));
            if (nev == null) return;
            undoPontBeallit();
            HmiKepernyo uj = new HmiKepernyo();
            uj.nev = nev.trim().isEmpty() ? (tr("Képernyő") + " " + (hmiKepernyok.size() + 1)) : nev.trim();
            hmiKepernyok.add(uj);
            hmiAktivKepernyo = hmiKepernyok.size() - 1;
            hmiWidgetek = uj.widgetek;
            undoPontRogzit();
            hmiKepernyoComboFrissit();
            hmi.ujraMeret();
            hmi.repaint();
        });
        kepTb.add(ujKepGomb);

        JButton atnevGomb = new JButton(tr("Átnevezés..."));
        atnevGomb.addActionListener(e -> {
            HmiKepernyo akt = hmiKepernyok.get(hmiAktivKepernyo);
            String nev = JOptionPane.showInputDialog(this, tr("Képernyő neve:"), akt.nev);
            if (nev != null && !nev.trim().isEmpty()) {
                undoPontBeallit();
                akt.nev = nev.trim();
                undoPontRogzit();
                hmiKepernyoComboFrissit();
            }
        });
        kepTb.add(atnevGomb);

        JButton torolKepGomb = new JButton(tr("Képernyő törlése"));
        torolKepGomb.addActionListener(e -> {
            if (hmiKepernyok.size() <= 1) {
                JOptionPane.showMessageDialog(this, tr("Legalább egy képernyőnek maradnia kell."));
                return;
            }
            if (JOptionPane.showConfirmDialog(this, tr("Biztosan törli a") + " \"" + hmiKepernyok.get(hmiAktivKepernyo).nev
                    + "\" " + tr("képernyőt és minden elemét?"), tr("Képernyő törlése"), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            undoPontBeallit();
            hmiKepernyok.remove(hmiAktivKepernyo);
            hmiAktivKepernyo = Math.max(0, hmiAktivKepernyo - 1);
            hmiWidgetek = hmiKepernyok.get(hmiAktivKepernyo).widgetek;
            undoPontRogzit();
            hmiKepernyoComboFrissit();
            hmi.ujraMeret();
            hmi.repaint();
        });
        kepTb.add(torolKepGomb);

        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        hmiSzerkesztesGomb = new JToggleButton(tr("✎ HMI szerkesztés"));
        hmiSzerkesztesGomb.setToolTipText(tr("Szerkesztés mód: elemek elhelyezése, mozgatása, átméretezése és tulajdonságainak módosítása"));
        hmiSzerkesztesGomb.addActionListener(e -> {
            hmi.szerkesztesMod = hmiSzerkesztesGomb.isSelected();
            hmi.kivalasztott = null;
            hmi.aktivEszkoz = null;
            hmiEszkozCombo.setEnabled(hmi.szerkesztesMod);
            hmiEszkozCombo.setSelectedIndex(0);
            hmi.repaint();
        });
        tb.add(hmiSzerkesztesGomb);
        tb.addSeparator();
        tb.add(new JLabel(tr("Új elem: ")));
        hmiEszkozCombo = new JComboBox<>(new String[]{tr("Kiválasztás / mozgatás"), tr("Lámpa"), tr("Gomb"), tr("Kijelző"), tr("Felirat"), tr("Téglalap"), tr("Trend"), tr("Zebra"), tr("Ajtó"), tr("Szállítószalag"), tr("Érzékelő"), tr("Fanuc robot"), tr("Kuka robot")});
        hmiEszkozCombo.setEnabled(false);
        hmiEszkozCombo.addActionListener(e -> {
            int sel = hmiEszkozCombo.getSelectedIndex();
            HmiTipus[] m = {null, HmiTipus.LAMPA, HmiTipus.GOMB, HmiTipus.KIJELZO, HmiTipus.FELIRAT, HmiTipus.TEGLALAP, HmiTipus.TREND, HmiTipus.ZEBRA, HmiTipus.AJTO, HmiTipus.SZALLITOSZALAG, HmiTipus.SZENZOR, HmiTipus.ROBOT_FANUC, HmiTipus.ROBOT_KUKA};
            hmi.aktivEszkoz = m[sel];
        });
        tb.add(hmiEszkozCombo);
        tb.addSeparator();
        JButton torlesGomb = new JButton(tr("Kijelölt elem törlése"));
        torlesGomb.addActionListener(e -> {
            if (hmi.kivalasztott != null) {
                undoPontBeallit();
                hmiWidgetek.remove(hmi.kivalasztott);
                hmi.kivalasztott = null;
                undoPontRogzit();
                hmi.ujraMeret();
                hmi.repaint();
            }
        });
        tb.add(torlesGomb);
        tb.addSeparator();
        tb.add(new JLabel("  " + tr("Futás közben (szerkesztés nélkül) a gombok és lámpák élőben reagálnak.") + "  "));
        tb.add(Box.createHorizontalGlue());
        hmiLevalasztGomb = new JToggleButton(tr("⧉ Külön ablakban"));
        hmiLevalasztGomb.setToolTipText(tr("A HMI megjelenítése külön, mozgatható ablakban – így a létradiagrammal egymás mellett is látható"));
        hmiLevalasztGomb.addActionListener(e -> hmiLevalasztasValtas());
        tb.add(hmiLevalasztGomb);

        JPanel eszkoztarok = new JPanel();
        eszkoztarok.setLayout(new BoxLayout(eszkoztarok, BoxLayout.Y_AXIS));
        eszkoztarok.add(kepTb);
        eszkoztarok.add(tb);
        fo.add(eszkoztarok, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(hmi);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        fo.add(sp, BorderLayout.CENTER);
        return fo;
    }

    void hmiLevalasztasValtas() {
        if (!hmiLevalasztva) {
            hmiFulIndex = fulek.indexOfComponent(hmiLap);
            if (hmiFulIndex >= 0) fulek.remove(hmiFulIndex);

            if (hmiAblak == null) {
                hmiAblak = new JFrame("HMI – " + PROGRAM);
                hmiAblak.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                hmiAblak.addWindowListener(new WindowAdapter() {
                    @Override public void windowClosing(WindowEvent e) { hmiLevalasztasValtas(); }
                });
                hmiAblak.setSize(900, 700);
                hmiAblak.setLocationRelativeTo(this);
                bindUndoRedoGyorsgombok(hmiAblak.getRootPane());
            }
            hmiAblak.setContentPane(hmiLap);
            hmiAblak.revalidate();
            hmiAblak.setVisible(true);
            hmiLevalasztva = true;
        } else {
            hmiAblak.setVisible(false);
            if (hmiFulIndex >= 0 && hmiFulIndex <= fulek.getTabCount()) fulek.insertTab("HMI", null, hmiLap, null, hmiFulIndex);
            else fulek.addTab("HMI", hmiLap);
            fulek.setSelectedComponent(hmiLap);
            hmiLevalasztva = false;
        }
        hmiLevalasztGomb.setSelected(hmiLevalasztva);
    }

    // ================================================================ FBD blokk szerkesztője

    void fbdBlokkSzerkeszto(FbdBlock b, boolean uj) {
        if (!uj) undoPontBeallit();
        JComboBox<String> tipusVal = new JComboBox<>(new String[]{
                tr("Bemenet (I/Q/M/T.DN/C.DN olvasása)"), tr("Kimenet (Q/M írása)"), tr("AND (ÉS)"), tr("OR (VAGY)"), tr("NOT (NEM)"),
                tr("XOR (KIZÁRÓ VAGY)"), tr("SR tároló (Reset-domináns)"), tr("TON (bekapcsolás-késleltetés)"),
                tr("CTU (felfelé számláló)"), tr("R_TRIG (felfutó él, P)"), tr("F_TRIG (lefutó él, N)")});
        FbdTipus[] map = {FbdTipus.BEMENET, FbdTipus.KIMENET, FbdTipus.AND, FbdTipus.OR, FbdTipus.NOT,
                FbdTipus.XOR, FbdTipus.SR, FbdTipus.TON, FbdTipus.CTU, FbdTipus.R_TRIG, FbdTipus.F_TRIG};
        for (int i = 0; i < map.length; i++) if (map[i] == b.tipus) tipusVal.setSelectedIndex(i);

        JTextField feliratVal = new JTextField(b.felirat, 16);

        JComboBox<String> opTipusVal = new JComboBox<>(new String[]{tr("Bemenet (I)"), tr("Kimenet (Q)"), tr("Belső relé (M)"), tr("Időzítő kész (T.DN)"), tr("Számláló kész (C.DN)")});
        OpTipus[] opMap = {OpTipus.I, OpTipus.Q, OpTipus.M, OpTipus.TDN, OpTipus.CDN};
        int startSel = 0;
        for (int i = 0; i < opMap.length; i++) if (opMap[i] == b.op.tipus) startSel = i;
        opTipusVal.setSelectedIndex(startSel);
        SpinnerNumberModel idxModel = new SpinnerNumberModel(b.op.index, 0, Operandus.maxIndex(b.op.tipus), 1);
        JSpinner idxSpin = new JSpinner(idxModel);
        opTipusVal.addActionListener(e -> {
            OpTipus t = opMap[opTipusVal.getSelectedIndex()];
            int max = Operandus.maxIndex(t);
            idxModel.setMaximum(max);
            if ((Integer) idxSpin.getValue() > max) idxSpin.setValue(0);
        });

        SpinnerNumberModel beSzamModel = new SpinnerNumberModel(Math.max(2, b.bemenetSzam), 2, 8, 1);
        JSpinner beSzamSpin = new JSpinner(beSzamModel);

        SpinnerNumberModel tIdxModel = new SpinnerNumberModel(b.idx, 0, 7, 1);
        JSpinner tIdxSpin = new JSpinner(tIdxModel);
        SpinnerNumberModel presetMsModel = new SpinnerNumberModel(b.presetMs, 0, 3600000, 100);
        JSpinner presetMsSpin = new JSpinner(presetMsModel);

        SpinnerNumberModel cIdxModel = new SpinnerNumberModel(b.idx, 0, 7, 1);
        JSpinner cIdxSpin = new JSpinner(cIdxModel);
        SpinnerNumberModel presetSzModel = new SpinnerNumberModel(b.presetSzamlalo, 1, 100000, 1);
        JSpinner presetSzSpin = new JSpinner(presetSzModel);

        JPanel opPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        opPanel.add(new JLabel(tr("Operandus típusa:"))); opPanel.add(opTipusVal);
        opPanel.add(new JLabel(tr("Index:"))); opPanel.add(idxSpin);

        JPanel beSzamPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        beSzamPanel.add(new JLabel(tr("Bemenetek száma:"))); beSzamPanel.add(beSzamSpin);

        JPanel tonPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        tonPanel.add(new JLabel(tr("Időzítő index (T):"))); tonPanel.add(tIdxSpin);
        tonPanel.add(new JLabel(tr("Beállított idő (ms):"))); tonPanel.add(presetMsSpin);

        JPanel ctuPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        ctuPanel.add(new JLabel(tr("Számláló index (C):"))); ctuPanel.add(cIdxSpin);
        ctuPanel.add(new JLabel(tr("Beállított érték:"))); ctuPanel.add(presetSzSpin);

        JPanel ures = new JPanel();

        JPanel kartyak = new JPanel(new CardLayout());
        kartyak.add(opPanel, "op");
        kartyak.add(beSzamPanel, "beszam");
        kartyak.add(tonPanel, "ton");
        kartyak.add(ctuPanel, "ctu");
        kartyak.add(ures, "ures");

        Runnable frissit = () -> {
            CardLayout cl = (CardLayout) kartyak.getLayout();
            FbdTipus t = map[tipusVal.getSelectedIndex()];
            if (t == FbdTipus.BEMENET || t == FbdTipus.KIMENET) cl.show(kartyak, "op");
            else if (t == FbdTipus.AND || t == FbdTipus.OR) cl.show(kartyak, "beszam");
            else if (t == FbdTipus.TON) cl.show(kartyak, "ton");
            else if (t == FbdTipus.CTU) cl.show(kartyak, "ctu");
            else cl.show(kartyak, "ures");
        };
        tipusVal.addActionListener(e -> frissit.run());
        frissit.run();

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        JPanel felso = new JPanel(new GridLayout(0, 2, 6, 6));
        felso.add(new JLabel(tr("Blokk típusa:"))); felso.add(tipusVal);
        felso.add(new JLabel(tr("Felirat / megjegyzés:"))); felso.add(feliratVal);
        fo.add(felso, BorderLayout.NORTH);
        fo.add(kartyak, BorderLayout.CENTER);

        Object[] opciok = {tr("OK"), tr("Törlés"), tr("Mégse")};
        int v = JOptionPane.showOptionDialog(this, fo, tr("FBD blokk szerkesztése"), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opciok, opciok[0]);

        if (v == 1) {
            fbdBlokkok.remove(b);
            fbd.kivalasztott = null;
            if (uj) undoPontEldob(); else undoPontRogzit();
            fbd.ujraMeret();
            fbd.repaint();
            return;
        }
        if (v != 0) {
            if (uj) { fbdBlokkok.remove(b); fbd.kivalasztott = null; }
            undoPontEldob();
            fbd.ujraMeret();
            fbd.repaint();
            return;
        }

        FbdTipus ujTipus = map[tipusVal.getSelectedIndex()];
        int ujBeSzam = (ujTipus == FbdTipus.AND || ujTipus == FbdTipus.OR)
                ? (Integer) beSzamSpin.getValue() : fbdAlapBemenetSzam(ujTipus);
        if (ujBeSzam != b.bemenetSzam || b.forrasId == null) {
            int[] uj2 = ujForrasTomb(ujBeSzam);
            if (b.forrasId != null) for (int i = 0; i < Math.min(uj2.length, b.forrasId.length); i++) uj2[i] = b.forrasId[i];
            b.forrasId = uj2;
        }
        b.tipus = ujTipus;
        b.bemenetSzam = ujBeSzam;
        b.felirat = feliratVal.getText();
        b.op = new Operandus(opMap[opTipusVal.getSelectedIndex()], (Integer) idxSpin.getValue());
        b.idx = (b.tipus == FbdTipus.TON) ? (Integer) tIdxSpin.getValue() : (b.tipus == FbdTipus.CTU ? (Integer) cIdxSpin.getValue() : b.idx);
        b.presetMs = ((Number) presetMsSpin.getValue()).longValue();
        b.presetSzamlalo = (Integer) presetSzSpin.getValue();
        b.h = Math.max(50, b.bemenetSzam * 22 + 18);
        undoPontRogzit();
        fbd.ujraMeret();
        fbd.repaint();
    }

    JPanel epitFbdLap() {
        JPanel fo = new JPanel(new BorderLayout());
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        fbdSzerkesztesGomb = new JToggleButton(tr("✎ FBD szerkesztés"));
        fbdSzerkesztesGomb.setToolTipText(tr("Szerkesztés mód: blokkok elhelyezése, mozgatása, huzalozása és tulajdonságainak módosítása"));
        fbdSzerkesztesGomb.addActionListener(e -> {
            fbd.szerkesztesMod = fbdSzerkesztesGomb.isSelected();
            fbd.kivalasztott = null;
            fbd.aktivEszkoz = null;
            fbd.huzalozasForras = null;
            fbdEszkozCombo.setEnabled(fbd.szerkesztesMod);
            fbdEszkozCombo.setSelectedIndex(0);
            fbd.repaint();
        });
        tb.add(fbdSzerkesztesGomb);
        tb.addSeparator();
        tb.add(new JLabel(tr("Új blokk: ")));
        fbdEszkozCombo = new JComboBox<>(new String[]{tr("Kiválasztás / mozgatás"), tr("Bemenet"), tr("Kimenet"), "AND", "OR", "NOT",
                "XOR", tr("SR tároló"), "TON", "CTU", "R_TRIG (P)", "F_TRIG (N)"});
        fbdEszkozCombo.setEnabled(false);
        fbdEszkozCombo.addActionListener(e -> {
            int sel = fbdEszkozCombo.getSelectedIndex();
            FbdTipus[] m = {null, FbdTipus.BEMENET, FbdTipus.KIMENET, FbdTipus.AND, FbdTipus.OR, FbdTipus.NOT,
                    FbdTipus.XOR, FbdTipus.SR, FbdTipus.TON, FbdTipus.CTU, FbdTipus.R_TRIG, FbdTipus.F_TRIG};
            fbd.aktivEszkoz = m[sel];
        });
        tb.add(fbdEszkozCombo);
        tb.addSeparator();
        JButton torlesGomb = new JButton(tr("Kijelölt blokk törlése"));
        torlesGomb.addActionListener(e -> {
            if (fbd.kivalasztott != null) {
                undoPontBeallit();
                fbdBlokkok.remove(fbd.kivalasztott);
                fbd.kivalasztott = null;
                undoPontRogzit();
                fbd.ujraMeret();
                fbd.repaint();
            }
        });
        tb.add(torlesGomb);
        tb.addSeparator();
        tb.add(new JLabel("  " + tr("Huzalozás: kattintson egy kimeneti pontra, majd a célbemenetre.") + "  "));

        fo.add(tb, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(fbd);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        fo.add(sp, BorderLayout.CENTER);
        return fo;
    }

    // ================================================================ GRAFCET szerkesztő és létra-generátor

    JPanel epitGrafcetLap() {
        JPanel fo = new JPanel(new BorderLayout());
        JToolBar tb = new JToolBar();
        tb.setFloatable(false);

        grafcetSzerkesztesGomb = new JToggleButton(tr("✎ GRAFCET szerkesztés"));
        grafcetSzerkesztesGomb.setToolTipText(tr("Szerkesztés mód: lépések és átmenetek elhelyezése, mozgatása, huzalozása és tulajdonságainak módosítása"));
        grafcetSzerkesztesGomb.addActionListener(e -> {
            grafcetPanel.szerkesztesMod = grafcetSzerkesztesGomb.isSelected();
            grafcetPanel.kivalasztottLepes = null;
            grafcetPanel.kivalasztottAtmenet = null;
            grafcetPanel.aktivEszkoz = null;
            grafcetEszkozCombo.setEnabled(grafcetPanel.szerkesztesMod);
            grafcetEszkozCombo.setSelectedIndex(0);
            grafcetPanel.repaint();
        });
        tb.add(grafcetSzerkesztesGomb);
        tb.addSeparator();
        tb.add(new JLabel(tr("Új elem: ")));
        grafcetEszkozCombo = new JComboBox<>(new String[]{tr("Kiválasztás / mozgatás"), tr("Lépés"), tr("Átmenet")});
        grafcetEszkozCombo.setEnabled(false);
        grafcetEszkozCombo.addActionListener(e -> {
            int sel = grafcetEszkozCombo.getSelectedIndex();
            grafcetPanel.aktivEszkoz = sel == 1 ? "LEPES" : (sel == 2 ? "ATMENET" : null);
        });
        tb.add(grafcetEszkozCombo);
        tb.addSeparator();
        JButton torlesGomb = new JButton(tr("Kijelölt elem törlése"));
        torlesGomb.addActionListener(e -> {
            if (grafcetPanel.kivalasztottLepes != null) {
                undoPontBeallit();
                GrafcetLepes l = grafcetPanel.kivalasztottLepes;
                grafcet.lepesek.remove(l);
                for (GrafcetAtmenet t : grafcet.atmenetek) { t.forrasLepesek.remove((Integer) l.id); t.celLepesek.remove((Integer) l.id); }
                grafcetPanel.kivalasztottLepes = null;
                undoPontRogzit();
                grafcetPanel.ujraMeret();
                grafcetPanel.repaint();
            } else if (grafcetPanel.kivalasztottAtmenet != null) {
                undoPontBeallit();
                grafcet.atmenetek.remove(grafcetPanel.kivalasztottAtmenet);
                grafcetPanel.kivalasztottAtmenet = null;
                undoPontRogzit();
                grafcetPanel.ujraMeret();
                grafcetPanel.repaint();
            }
        });
        tb.add(torlesGomb);
        tb.addSeparator();
        JButton letraGenGomb = new JButton(tr("Létra generálása..."));
        letraGenGomb.addActionListener(e -> grafcetLetraGeneralas());
        tb.add(letraGenGomb);
        tb.addSeparator();
        tb.add(new JLabel("  " + tr("Huzalozás: kattintson egy lépés/átmenet kimeneti pontjára, majd a célbemenetre.") + "  "));

        fo.add(tb, BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(grafcetPanel);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        fo.add(sp, BorderLayout.CENTER);
        return fo;
    }

    String grafcetAkcioSzoveg(GrafcetAkcio a) {
        String jel = a.tipus == GrafcetAkcioTipus.SET ? "S" : (a.tipus == GrafcetAkcioTipus.RESET ? "R" : "N");
        return jel + "   " + rovidFelirat(a.cel);
    }

    GrafcetAkcio grafcetAkcioSzerkeszto(GrafcetAkcio kiindulo) {
        JComboBox<String> tipusVal = new JComboBox<>(new String[]{tr("Folyamatos (N)"), tr("Set (S)"), tr("Reset (R)")});
        GrafcetAkcioTipus[] map = {GrafcetAkcioTipus.FOLYAMATOS, GrafcetAkcioTipus.SET, GrafcetAkcioTipus.RESET};
        for (int i = 0; i < map.length; i++) if (map[i] == kiindulo.tipus) tipusVal.setSelectedIndex(i);

        JComboBox<String> celTipus = new JComboBox<>(new String[]{tr("Kimenet (Q)"), tr("Belső relé (M)")});
        celTipus.setSelectedIndex(kiindulo.cel.tipus == OpTipus.M ? 1 : 0);
        SpinnerNumberModel idxModel = new SpinnerNumberModel(kiindulo.cel.index, 0, 15, 1);
        JSpinner idxSpin = new JSpinner(idxModel);

        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel(tr("Akció típusa:"))); panel.add(tipusVal);
        panel.add(new JLabel(tr("Cél:"))); panel.add(celTipus);
        panel.add(new JLabel(tr("Index:"))); panel.add(idxSpin);

        int v = JOptionPane.showConfirmDialog(this, panel, tr("Akció szerkesztése"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (v != JOptionPane.OK_OPTION) return null;

        GrafcetAkcio a = new GrafcetAkcio();
        a.tipus = map[tipusVal.getSelectedIndex()];
        a.cel = new Operandus(celTipus.getSelectedIndex() == 0 ? OpTipus.Q : OpTipus.M, (Integer) idxSpin.getValue());
        return a;
    }

    void grafcetLepesSzerkeszto(GrafcetLepes l, boolean uj) {
        if (!uj) undoPontBeallit();

        JCheckBox kezdetiVal = new JCheckBox(tr("Kezdeti lépés"), l.kezdeti);
        JTextField feliratVal = new JTextField(l.felirat, 16);

        List<GrafcetAkcio> munka = new ArrayList<>();
        for (GrafcetAkcio a : l.akciok) { GrafcetAkcio m = new GrafcetAkcio(); m.tipus = a.tipus; m.cel = new Operandus(a.cel.tipus, a.cel.index); munka.add(m); }
        DefaultListModel<String> akcioModel = new DefaultListModel<>();
        for (GrafcetAkcio a : munka) akcioModel.addElement(grafcetAkcioSzoveg(a));
        JList<String> akcioLista = new JList<>(akcioModel);
        JScrollPane akcioScroll = new JScrollPane(akcioLista);
        akcioScroll.setPreferredSize(new Dimension(260, 90));

        JButton hozzaadGomb = new JButton(tr("+ Hozzáad"));
        hozzaadGomb.addActionListener(e -> {
            GrafcetAkcio eredmeny = grafcetAkcioSzerkeszto(new GrafcetAkcio());
            if (eredmeny != null) { munka.add(eredmeny); akcioModel.addElement(grafcetAkcioSzoveg(eredmeny)); }
        });
        JButton torolAkcioGomb = new JButton(tr("Törlés"));
        torolAkcioGomb.addActionListener(e -> {
            int idx = akcioLista.getSelectedIndex();
            if (idx >= 0) { munka.remove(idx); akcioModel.remove(idx); }
        });
        JPanel akcioGombok = new JPanel(new FlowLayout(FlowLayout.LEFT));
        akcioGombok.add(hozzaadGomb); akcioGombok.add(torolAkcioGomb);

        JPanel akciokPanel = new JPanel(new BorderLayout(4, 4));
        akciokPanel.add(akcioScroll, BorderLayout.CENTER);
        akciokPanel.add(akcioGombok, BorderLayout.SOUTH);
        akciokPanel.setBorder(BorderFactory.createTitledBorder(tr("Akciók (amíg a lépés aktív)")));

        JPanel felso = new JPanel(new GridLayout(0, 2, 6, 6));
        felso.add(new JLabel(tr("Felirat:"))); felso.add(feliratVal);
        felso.add(new JLabel("")); felso.add(kezdetiVal);

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        fo.add(felso, BorderLayout.NORTH);
        fo.add(akciokPanel, BorderLayout.CENTER);

        Object[] opciok = {tr("OK"), tr("Törlés"), tr("Mégse")};
        int v = JOptionPane.showOptionDialog(this, fo, tr("Lépés szerkesztése"), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opciok, opciok[0]);

        if (v == 1) {
            grafcet.lepesek.remove(l);
            for (GrafcetAtmenet t : grafcet.atmenetek) { t.forrasLepesek.remove((Integer) l.id); t.celLepesek.remove((Integer) l.id); }
            if (grafcetPanel.kivalasztottLepes == l) grafcetPanel.kivalasztottLepes = null;
            if (uj) undoPontEldob(); else undoPontRogzit();
            grafcetPanel.ujraMeret(); grafcetPanel.repaint();
            return;
        }
        if (v != 0) {
            if (uj) {
                grafcet.lepesek.remove(l);
                for (GrafcetAtmenet t : grafcet.atmenetek) { t.forrasLepesek.remove((Integer) l.id); t.celLepesek.remove((Integer) l.id); }
                if (grafcetPanel.kivalasztottLepes == l) grafcetPanel.kivalasztottLepes = null;
            }
            undoPontEldob();
            grafcetPanel.ujraMeret(); grafcetPanel.repaint();
            return;
        }

        l.kezdeti = kezdetiVal.isSelected();
        l.felirat = feliratVal.getText();
        l.akciok = munka;
        undoPontRogzit();
        grafcetPanel.ujraMeret();
        grafcetPanel.repaint();
    }

    void grafcetAtmenetSzerkeszto(GrafcetAtmenet t, boolean uj) {
        if (!uj) undoPontBeallit();

        List<Erintkezo> munka = new ArrayList<>();
        for (Erintkezo e : t.feltetel) { Erintkezo m = new Erintkezo(); m.no = e.no; m.op = new Operandus(e.op.tipus, e.op.index); munka.add(m); }
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Erintkezo e : munka) model.addElement((e.no ? "" : "/ ") + felirat(e.op));
        JList<String> lista = new JList<>(model);
        JScrollPane listaScroll = new JScrollPane(lista);
        listaScroll.setPreferredSize(new Dimension(260, 90));

        JButton hozzaadGomb = new JButton(tr("+ Hozzáad"));
        hozzaadGomb.addActionListener(e -> {
            Erintkezo eredmeny = erintkezoSzerkeszto(new Erintkezo(), false);
            if (eredmeny != null && eredmeny != TOROL_JEL) {
                munka.add(eredmeny);
                model.addElement((eredmeny.no ? "" : "/ ") + felirat(eredmeny.op));
            }
        });
        JButton torolGomb = new JButton(tr("Törlés"));
        torolGomb.addActionListener(e -> {
            int idx = lista.getSelectedIndex();
            if (idx >= 0) { munka.remove(idx); model.remove(idx); }
        });
        JPanel feltGombok = new JPanel(new FlowLayout(FlowLayout.LEFT));
        feltGombok.add(hozzaadGomb); feltGombok.add(torolGomb);

        JPanel feltPanel = new JPanel(new BorderLayout(4, 4));
        feltPanel.add(listaScroll, BorderLayout.CENTER);
        feltPanel.add(feltGombok, BorderLayout.SOUTH);
        feltPanel.setBorder(BorderFactory.createTitledBorder(tr("Feltétel (receptivitás) – ÉS kapcsolatban lévő kontaktusok")));

        Object[] opciok = {tr("OK"), tr("Törlés"), tr("Mégse")};
        int v = JOptionPane.showOptionDialog(this, feltPanel, tr("Átmenet szerkesztése"), JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, opciok, opciok[0]);

        if (v == 1) {
            grafcet.atmenetek.remove(t);
            if (grafcetPanel.kivalasztottAtmenet == t) grafcetPanel.kivalasztottAtmenet = null;
            if (uj) undoPontEldob(); else undoPontRogzit();
            grafcetPanel.ujraMeret(); grafcetPanel.repaint();
            return;
        }
        if (v != 0) {
            if (uj) {
                grafcet.atmenetek.remove(t);
                if (grafcetPanel.kivalasztottAtmenet == t) grafcetPanel.kivalasztottAtmenet = null;
            }
            undoPontEldob();
            grafcetPanel.ujraMeret(); grafcetPanel.repaint();
            return;
        }

        t.feltetel = munka;
        undoPontRogzit();
        grafcetPanel.ujraMeret();
        grafcetPanel.repaint();
    }

    Sor grafcetSorEpit(List<GrafcetAtmenet> atmenetek, int sajatMIdx, Map<Integer, Integer> lepesToM, KimTipus kimTipus, String megj) {
        Sor s = new Sor();
        s.megjegyzes = megj;
        s.kimenet.tipus = kimTipus;
        s.kimenet.cel = new Operandus(OpTipus.M, sajatMIdx);
        if (atmenetek.size() == 1) {
            GrafcetAtmenet t = atmenetek.get(0);
            for (int sid : t.forrasLepesek) {
                Integer m = lepesToM.get(sid);
                if (m == null) continue;
                s.elotag.add(ujMErintkezo(m));
            }
            for (Erintkezo fe : t.feltetel) s.elotag.add(masoltErintkezo(fe));
        } else {
            for (GrafcetAtmenet t : atmenetek) {
                List<Erintkezo> ag = new ArrayList<>();
                for (int sid : t.forrasLepesek) {
                    Integer m = lepesToM.get(sid);
                    if (m == null) continue;
                    ag.add(ujMErintkezo(m));
                }
                for (Erintkezo fe : t.feltetel) ag.add(masoltErintkezo(fe));
                s.agak.add(ag);
            }
        }
        return s;
    }

    static Erintkezo ujMErintkezo(int mIdx) {
        Erintkezo e = new Erintkezo(); e.no = true; e.op = new Operandus(OpTipus.M, mIdx); return e;
    }

    static Erintkezo masoltErintkezo(Erintkezo fe) {
        Erintkezo e = new Erintkezo(); e.no = fe.no; e.op = new Operandus(fe.op.tipus, fe.op.index); return e;
    }

    void grafcetLetraGeneralas() {
        if (grafcet.lepesek.isEmpty()) {
            JOptionPane.showMessageDialog(this, tr("A GRAFCET diagram üres – adjon hozzá legalább egy lépést."), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean vanKezdeti = false;
        for (GrafcetLepes l : grafcet.lepesek) if (l.kezdeti) vanKezdeti = true;
        if (!vanKezdeti) {
            JOptionPane.showMessageDialog(this, tr("Nincs kijelölve kezdeti lépés – szerkessze valamelyik lépést, és jelölje be a \"Kezdeti lépés\" jelölőnégyzetet."), tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Set<Integer> hasznaltM = new HashSet<>();
        for (Sor s : program) {
            for (Erintkezo e : s.elotag) if (e.op.tipus == OpTipus.M) hasznaltM.add(e.op.index);
            for (List<Erintkezo> ag : s.agak) for (Erintkezo e : ag) if (e.op.tipus == OpTipus.M) hasznaltM.add(e.op.index);
            for (Erintkezo e : s.utotag) if (e.op.tipus == OpTipus.M) hasznaltM.add(e.op.index);
            if ((s.kimenet.tipus == KimTipus.TEKERCS || s.kimenet.tipus == KimTipus.SET || s.kimenet.tipus == KimTipus.RESET) && s.kimenet.cel.tipus == OpTipus.M)
                hasznaltM.add(s.kimenet.cel.index);
        }
        for (FbdBlock b : fbdBlokkok) {
            if ((b.tipus == FbdTipus.BEMENET || b.tipus == FbdTipus.KIMENET) && b.op.tipus == OpTipus.M) hasznaltM.add(b.op.index);
        }
        for (HmiKepernyo kep : hmiKepernyok) for (HmiWidget w : kep.widgetek) if (w.op != null && w.op.tipus == OpTipus.M) hasznaltM.add(w.op.index);

        List<Integer> szabadM = new ArrayList<>();
        for (int i = 0; i < 16; i++) if (!hasznaltM.contains(i)) szabadM.add(i);

        if (szabadM.size() < grafcet.lepesek.size()) {
            JOptionPane.showMessageDialog(this,
                    tr("Nincs elég szabad belső relé (M) a létra generálásához:") + " " + grafcet.lepesek.size() + " "
                            + tr("lépéshez csak") + " " + szabadM.size() + " "
                            + tr("szabad M bit áll rendelkezésre (összesen 16 van). Csökkentse a lépések számát, vagy szabadítson fel M biteket."),
                    tr("Hiba"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<GrafcetLepes> rendezett = new ArrayList<>(grafcet.lepesek);
        rendezett.sort((a, b) -> Integer.compare(a.id, b.id));
        Map<Integer, Integer> lepesToM = new LinkedHashMap<>();
        for (int i = 0; i < rendezett.size(); i++) lepesToM.put(rendezett.get(i).id, szabadM.get(i));

        StringBuilder osszegzes = new StringBuilder();
        osszegzes.append(tr("A következő belső relék (M) kerülnek felhasználásra:")).append("\n");
        for (GrafcetLepes l : rendezett) {
            osszegzes.append("  M").append(lepesToM.get(l.id)).append("  <-  ").append(tr("Lépés")).append(" ").append(l.id);
            if (!l.felirat.isEmpty()) osszegzes.append(" (").append(l.felirat).append(")");
            osszegzes.append("\n");
        }
        osszegzes.append("\n").append(tr("Ez új hálózatokat hoz létre (aktiválás + deaktiválás lépésenként, plusz az akció-hálózatok)."));
        osszegzes.append("\n").append(tr("Ettől kezdve a tényleges kimeneteket a generált létra vezérli; a GRAFCET fül csak vizuális előnézetként animálja a lépéseket tovább."));
        osszegzes.append("\n").append(tr("Folytatja?"));

        if (JOptionPane.showConfirmDialog(this, osszegzes.toString(), tr("Létra generálása GRAFCET-ből"),
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        undoPontBeallit();

        List<Sor> ujHalozatok = new ArrayList<>();
        for (GrafcetLepes l : rendezett) {
            List<GrafcetAtmenet> bejovo = new ArrayList<>();
            for (GrafcetAtmenet t : grafcet.atmenetek) if (t.celLepesek.contains(l.id)) bejovo.add(t);
            if (bejovo.isEmpty()) continue;
            ujHalozatok.add(grafcetSorEpit(bejovo, lepesToM.get(l.id), lepesToM, KimTipus.SET,
                    "GRAFCET: " + l.id + ". " + tr("lépés aktiválása") + (l.felirat.isEmpty() ? "" : " (" + l.felirat + ")")));
        }
        for (GrafcetLepes l : rendezett) {
            List<GrafcetAtmenet> kimeno = new ArrayList<>();
            for (GrafcetAtmenet t : grafcet.atmenetek) if (t.forrasLepesek.contains(l.id)) kimeno.add(t);
            if (kimeno.isEmpty()) continue;
            ujHalozatok.add(grafcetSorEpit(kimeno, lepesToM.get(l.id), lepesToM, KimTipus.RESET,
                    "GRAFCET: " + l.id + ". " + tr("lépés deaktiválása") + (l.felirat.isEmpty() ? "" : " (" + l.felirat + ")")));
        }
        for (GrafcetLepes l : rendezett) {
            int mIdx = lepesToM.get(l.id);
            for (GrafcetAkcio a : l.akciok) {
                Sor s = new Sor();
                s.megjegyzes = "GRAFCET: " + l.id + ". " + tr("lépés akciója");
                s.elotag.add(ujMErintkezo(mIdx));
                s.kimenet.tipus = a.tipus == GrafcetAkcioTipus.SET ? KimTipus.SET : (a.tipus == GrafcetAkcioTipus.RESET ? KimTipus.RESET : KimTipus.TEKERCS);
                s.kimenet.cel = new Operandus(a.cel.tipus, a.cel.index);
                ujHalozatok.add(s);
            }
        }

        program.addAll(ujHalozatok);

        grafcetKezdetiMIndexek.clear();
        for (GrafcetLepes l : rendezett) {
            if (l.kezdeti) {
                int idx = lepesToM.get(l.id);
                grafcetKezdetiMIndexek.add(idx);
                allapot.m[idx] = true;
            }
        }

        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
        frissitIOPanel();
        if (fulek != null) fulek.setSelectedIndex(0);
        statusz.setText(" " + ujHalozatok.size() + " " + tr("hálózat hozzáadva a létraprogramhoz a GRAFCET alapján."));
    }

    void kimenetSzerkeszto(Kimenet k) {
        undoPontBeallit();
        JComboBox<String> tipusVal = new JComboBox<>(new String[]{tr("Tekercs (OTE)"), tr("Set (bekapcsolás)"), tr("Reset (kikapcsolás)"), tr("Időzítő (TON)"), tr("Számláló (CTU)")});
        KimTipus[] map = {KimTipus.TEKERCS, KimTipus.SET, KimTipus.RESET, KimTipus.TON, KimTipus.CTU};
        for (int i = 0; i < map.length; i++) if (map[i] == k.tipus) tipusVal.setSelectedIndex(i);

        JComboBox<String> celTipus = new JComboBox<>(new String[]{tr("Kimenet (Q)"), tr("Belső relé (M)")});
        celTipus.setSelectedIndex(k.cel.tipus == OpTipus.M ? 1 : 0);
        SpinnerNumberModel celIdxModel = new SpinnerNumberModel(k.cel.index, 0, 15, 1);
        JSpinner celIdxSpin = new JSpinner(celIdxModel);

        SpinnerNumberModel tIdxModel = new SpinnerNumberModel(k.idx, 0, 7, 1);
        JSpinner tIdxSpin = new JSpinner(tIdxModel);
        SpinnerNumberModel presetMsModel = new SpinnerNumberModel(k.presetMs, 0, 3600000, 100);
        JSpinner presetMsSpin = new JSpinner(presetMsModel);

        SpinnerNumberModel cIdxModel = new SpinnerNumberModel(k.idx, 0, 7, 1);
        JSpinner cIdxSpin = new JSpinner(cIdxModel);
        SpinnerNumberModel presetSzModel = new SpinnerNumberModel(k.presetSzamlalo, 1, 100000, 1);
        JSpinner presetSzSpin = new JSpinner(presetSzModel);

        JCheckBox negaltVal = new JCheckBox(tr("Negált (NOT) tekercs – csak Tekercs típusnál érvényes"), k.negalt);

        JPanel coilPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        coilPanel.add(new JLabel(tr("Cél:"))); coilPanel.add(celTipus);
        coilPanel.add(new JLabel(tr("Index:"))); coilPanel.add(celIdxSpin);
        coilPanel.add(new JLabel(tr("Negálás:"))); coilPanel.add(negaltVal);

        JPanel tonPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        tonPanel.add(new JLabel(tr("Időzítő index (T):"))); tonPanel.add(tIdxSpin);
        tonPanel.add(new JLabel(tr("Beállított idő (ms):"))); tonPanel.add(presetMsSpin);

        JPanel ctuPanel = new JPanel(new GridLayout(0, 2, 6, 6));
        ctuPanel.add(new JLabel(tr("Számláló index (C):"))); ctuPanel.add(cIdxSpin);
        ctuPanel.add(new JLabel(tr("Beállított érték:"))); ctuPanel.add(presetSzSpin);

        JPanel kartyak = new JPanel(new CardLayout());
        kartyak.add(coilPanel, "coil");
        kartyak.add(tonPanel, "ton");
        kartyak.add(ctuPanel, "ctu");

        Runnable frissit = () -> {
            CardLayout cl = (CardLayout) kartyak.getLayout();
            int sel = tipusVal.getSelectedIndex();
            if (sel == 3) cl.show(kartyak, "ton");
            else if (sel == 4) cl.show(kartyak, "ctu");
            else cl.show(kartyak, "coil");
        };
        tipusVal.addActionListener(e -> frissit.run());
        frissit.run();

        JPanel fo = new JPanel(new BorderLayout(6, 6));
        JPanel felso = new JPanel(new GridLayout(0, 2, 6, 6));
        felso.add(new JLabel(tr("Kimenet típusa:"))); felso.add(tipusVal);
        fo.add(felso, BorderLayout.NORTH);
        fo.add(kartyak, BorderLayout.CENTER);

        int v = JOptionPane.showConfirmDialog(this, fo, tr("Kimenet szerkesztése"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (v != JOptionPane.OK_OPTION) { undoPontEldob(); return; }

        k.tipus = map[tipusVal.getSelectedIndex()];
        switch (k.tipus) {
            case TEKERCS: case SET: case RESET:
                k.cel = new Operandus(celTipus.getSelectedIndex() == 0 ? OpTipus.Q : OpTipus.M, (Integer) celIdxSpin.getValue());
                k.negalt = (k.tipus == KimTipus.TEKERCS) && negaltVal.isSelected();
                break;
            case TON:
                k.idx = (Integer) tIdxSpin.getValue();
                k.presetMs = ((Number) presetMsSpin.getValue()).longValue();
                break;
            case CTU:
                k.idx = (Integer) cIdxSpin.getValue();
                k.presetSzamlalo = (Integer) presetSzSpin.getValue();
                break;
        }
        undoPontRogzit();
        letra.ujraMeret();
        letra.repaint();
    }
}
