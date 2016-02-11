Einführung
==========
DocFetcher ist ein Open Source Desktop-Suchprogramm: Es ermöglicht die Volltext-Suche in Dateien auf dem Computer &mdash; eine Art Google für den Heimrechner.

**Index-basierte Suche**: Da die direkte Suche in Dateien in der Praxis bei einer größeren Anzahl von Dateien zu langsam wäre, müssen Sie *Indizes* für die Ordner erstellen, in denen Sie suchen wollen. Diese Indizes erlauben es DocFetcher, schnell Dateien anhand von Suchbegriffen zu finden, ähnlich wie man den Index am Ende eines Buches benutzt. Das Erstellen eines Index mag in Abhängigkeit von der Anzahl und Größe der indizierten Dateien eine Weile dauern. Jedoch muss dies nur einmal pro Ordner geschehen, und danach können Sie in den indizierten Ordnern so oft suchen wie Sie wollen.

**Erstellen eines Index**: Um einen Index zu erstellen, rechts-klicken Sie auf den `Suchbereich` links und wählen Sie den Menüpunkt `Index erstellen aus > Ordner` aus. Wählen Sie nun einen Ordner zum Indizieren aus. Für den Anfang sollte dies ein kleiner Ordner mit nicht zu vielen Dateien sein, z.&nbsp;B. einen mit ca. 50 Dateien. Nach Auswahl des Ordners erscheint ein Einstellungs-Fenster. Die Standard-Einstellungen sollten genügen, daher klicken Sie am besten einfach auf den `Start`-Button und warten Sie, bis DocFetcher mit dem Indizieren fertig ist. (Eine weitere Möglichkeit, Indizes zu erstellen, besteht darin, einen Ordner aus der Zwischenablage in den `Suchbereich` einzufügen.)

**Suche**: Geben Sie nun Suchbegriffe in das Textfeld über der Ergebnis-Tabelle ein und drücken Sie `Enter`. Die Suchergebnisse werden daraufhin in der Ergebnis-Tabelle angezeigt, sortiert nach absteigender Trefferzahl.

*Sofern Sie dieses Benutzerhandbuch innerhalb von DocFetcher lesen, wird das Befolgen der nachfolgenden Anweisungen dazu führen, dass das Benutzerhandbuch verschwindet. Um es wiederherzustellen, klicken Sie auf den `'?'`-Button oben rechts. Alternativ können Sie das Benutzerhandbuch auch in Ihrem Webbrowser öffnen, indem Sie auf den `In externem Webbrowser öffnen`-Button direkt über diesem Fensterbereich klicken.*

**Ergebnis-Tabelle und Vorschau-Fenster**: Unterhalb der Ergebnis-Tabelle (oder rechts davon, je nach Layout der Benutzeroberfläche), findet sich das Vorschau-Fenster, welches eine Text-Vorschau für die Datei anzeigt, die in der Ergebnis-Tabelle selektiert wurde. Erwähnenswert sind folgende Features:

* ***Hervorhebung***: Standardmäßig werden die eingegebenen Suchbegriffe in der Vorschau farblich hervorgehoben, und Sie können von einer Fundstelle zur vorherigen oder nächsten mittels der Hoch- und Runter-Buttons springen.
* ***Eingebauter Webbrowser***: Im Falle von HTML-Dateien können Sie die Vorschau zwischen einer Text-Vorschau und einem einfachen eingebauten Webbrowser umschalten. (Anm.: Letzterer ist auf manchen Linux-Distributionen nicht verfügbar.)

Eine Tastenkombination, die von Nutzen sein könnte: Drücken Sie `Strg+F` oder `Alt+F`, um das Such-Textfeld zu fokussieren. Um eine Datei mit dem damit assoziierten externen Programm zu öffnen, doppelklicken Sie auf die Datei in der Ergebnis-Tabelle.

**Sortierung**: Sie können die Sortierung der Ergebnisse mittels Klick auf eine der Spaltenköpfe der Ergebnis-Tabelle ändern. Um die Ergebnisse bspw. nach Dateinamen zu sortieren, klicken Sie auf den Spaltenkopf `Dateiname`. Wenn Sie auf denselben Spaltenkopf erneut klicken, wird nach dem dazugehörigen Kriterium in umgekehrter Reihenfolge sortiert. Sie können auch die Reihenfolge der Spalten mittels Drag & Drop ändern: Um bspw. die Spalte `Dateiname` zur ersten Spalte zu machen, ziehen Sie einfach den `Dateiname`-Spaltenkopf nach links.

**Filtern**: Auf der linken Seite der Benutzeroberfläche befinden sich diverse Schaltflächen zum Filtern der Suchergebnisse: (1) Ganz oben können Sie eine minimale und/oder maximale Dateigröße eingeben. (2) Der `Dokument-Typen`-Bereich erlaubt das Filtern der Suchergebnisse nach Dateityp. (3) Durch Selektieren und Deselektieren von Einträgen im `Suchbereich` können Sie die Suchergebnisse nach Ort filtern.

**Index-Aktualisierungen**: Wenn in den indizierten Ordnern Dateien hinzugefügt, entfernt oder modifiziert werden, müssen die dazugehörigen Indizes aktualisiert werden, da sonst die Suchergebnisse veraltet sein können. Glücklicherweise läuft die Aktualisierung eines Index praktisch immer wesentlich schneller ab als das Neuerstellen eines Index, da nämlich nur die Datei-Veränderungen verarbeitet werden müssen. Zudem kann DocFetcher die Indizes auf zweierlei Weisen automatisch aktualisieren:

1. ***DocFetcher selbst***: Falls DocFetcher läuft und die *Ordner-Überwachung* für den modifizierten Ordner aktiviert wurde, detektiert DocFetcher die Modifikationen und aktualisiert umgehend seine Indizes.
2. ***DocFetcher-Daemon***: Falls DocFetcher nicht läuft, werden die Modifikationen von einem kleinen Daemon-Programm aufgezeichnet, welches im Hintergrund läuft. Die betroffenen Indizes werden dann aktualisiert, sobald DocFetcher das nächste Mal ausgeführt wird. (Anm.: Der Daemon ist derzeit leider nicht auf Mac OS&nbsp;X verfügbar.)

Bitte beachten: Falls Sie die portable Version von DocFetcher verwenden und den Daemon benutzen wollen, müssen Sie diesen zuerst manuell einrichten, indem Sie den Daemon zur Liste der Autostart-Programme Ihres Betriebssystems hinzufügen. Beachten Sie auch, dass weder DocFetcher noch der Daemon Datei-Änderungen auf Netz-Laufwerken detektieren können.<!-- this line should end with two spaces -->  
In jenen Fällen, in denen die Indizes nicht automatisch aktualisiert werden können, müssen Sie dies manuell erledigen: Selektieren Sie dazu im `Suchbereich` einen oder mehrere Indizes und wählen Sie dann entweder `Aktualisieren` im Kontextmenü aus, oder drücken Sie die `F5`-Taste.

* * *

<a name="Advanced_Usage"></a> <!-- Do not translate this line, just copy it verbatim. -->

Fortgeschrittene Benutzung
==========================

**Suchanfrage-Syntax**: Mit DocFetcher ist wesentlich mehr möglich als nur eine einfache Suche nach Wörtern. Bspw. können Sie mittels sogenannter Wildcards nach Wörtern suchen, die mit einer bestimmten Zeichenfolge beginnen. Beispiel: `wiki*`. Um nach einer bestimmten Phrase (d.&nbsp;h. einer Abfolge von Wörtern in genau der angegebenen Reihenfolge) zu suchen, können Sie die Phrase in Anführungszeichen einschließen: `"cogito ergo sum"`. Aber das ist erst der Anfang: Für einen Überblick über alle verfügbaren Suchkonstrukte werfen Sie bitte einen Blick auf die [Suchanfrage-Syntax-Seite](DocFetcher_Manual_files/Query_Syntax.html).

**Portable Dokument-Repositories**: Die portable Version von DocFetcher erlaubt das Bündeln von DocFetcher, Ihren Dateien und den dazugehörigen Indizes. Dieses Bündel können Sie dann frei umherbewegen &mdash; sogar von einem Betriebssystem zum anderen, z.&nbsp;B. von Windows nach Linux und umgekehrt. Bei der Benutzung der portablen Version ist zu beachten, dass die Indizes mit *relativen Pfaden* erstellt werden müssen. Klicken Sie [hier](DocFetcher_Manual_files/Portable_Repositories.html) für weiterführende Informationen zu portablen Dokument-Repositories. Übrigens: Falls Sie bisher DocFetcher 1.0.3 oder eine ältere Version benutzt haben, müssen Sie nun Ihre Dateien nicht mehr unbedingt in den DocFetcher-Ordner ablegen.

**Indizierungs-Einstellungen**: Klicken Sie [hier](DocFetcher_Manual_files/Indexing_Options.html) für eine umfassende Erläuterung aller Einstellungen auf dem Indizierungs-Fenster. Sie können diese Handbuch-Seite auch direkt vom Indizierungs-Fenster aus erreichen, indem Sie auf den `Hilfe`-Button am unteren Ende des Fensters klicken. Wohl zu den wichtigsten Indizierungs-Einstellungen zählen:

* ***Anpassbare Datei-Erweiterungen***: Die Datei-Erweiterungen für Textdateien sowie für Zip-Archive sind vollständig anpassbar. Dies ist insbesondere nützlich zur Indizierung von Quell-Code.
* ***Ausschluss von Dateien***: Sie können mittels regulärer Ausdrücke bestimmte Dateien von der Indizierung ausschließen.
* ***MIME-Typ-Detektion***: Ohne MIME-Typ-Detektion wird DocFetcher lediglich auf die Datei-Erweiterung einer Datei (z.&nbsp;B. `'.doc'`) schauen, um dessen Datei-Typ zu bestimmen. Mit MIME-Typ-Detektion wird DocFetcher zusätzlich auch den Datei-Inhalt inspizieren, um nach weiteren Hinweisen auf den korrekten Datei-Typ zu suchen. Dies läuft ein wenig langsamer ab im Vergleich zur reinen Dateinamens-Analyse, kann aber nützlich sein bei Dateien, denen eine falsche Datei-Erweiterung gegeben wurde.
* ***HTML-Paarung***: Standardmäßig behandelt DocFetcher eine HTML-Datei und den dazugehörigen HTML-Ordner (z.&nbsp;B. eine Datei namens `Beispiel.html` und einen Ordner namens `Beispiel-Dateien`) als ein einziges Dokument. Der Hauptnutzen dieser Logik besteht darin, den "Datei-Müll" in HTML-Ordnern aus den Suchergebnissen zu entfernen.

**Reguläre Ausdrücke**: Sowohl der Ausschluss von Dateien als auch die MIME-Typ-Detektion arbeiten mit sogenannten *regulären Ausdrücken*. Bei diesen handelt es sich um benutzer-definierte "Zeichenabfolgen-Muster", die DocFetcher mit Dateinamen oder Dateipfaden vergleicht. Um bspw. alle Dateien von der Indizierung auszuschließen, die mit der Zeichenkette "journal" beginnen, können Sie folgenden regulären Ausdruck verwenden: `journal.*`. Beachten Sie, dass dieser Ausdruck ein wenig von der Suchanfrage-Syntax abweicht, bei der der Punkt ausgelassen werden müsste: `journal*`. Wenn Sie mehr über reguläre Ausdrücke erfahren wollen, lesen Sie bitte diese [kurze Einführung](DocFetcher_Manual_files/Regular_Expressions.html).

**Benachrichtigung über neue Programm-Versionen**: DocFetcher sucht nicht automatisch nach neuen Programm-Versionen, jedoch können Sie, wenn Sie wollen, über das Erscheinen neuer Programm-Versionen benachrichtigt werden. Folgen Sie dazu bitte [diesen Anweisungen](DocFetcher_Manual_files/Release_Notification.html).

* * *

<a name="Caveats"></a> <!-- Do not translate this line, just copy it verbatim. -->

Hinweise und häufige Problemfälle
=================================

**Erhöhung des Speicherlimits**: Wie alle anderen Java-Programme auch besteht für DocFetcher ein festes Limit bezüglich der Menge an Arbeitsspeicher, die es nutzen darf &mdash; die sogenannte *Java Heap Size*. Dieses Speicherlimit kann bei Programmstart eingestellt werden, und DocFetcher wählt derzeit einen Standardwert von 256&nbsp;MB. Falls Sie versuchen, eine sehr, sehr große Menge an Dateien, und/oder sehr große Dateien zu indizieren (letzteres kommt bei PDF-Dateien häufig vor), kann es passieren, dass DocFetcher an dieses Speicherlimit stößt. Wenn dies passiert, können Sie [das Speicherlimit erhöhen](DocFetcher_Manual_files/Memory_Limit.html).

**Indizieren Sie keine System-Ordner**: Im Gegensatz zu anderen Desktop-Suchmaschinen wurde DocFetcher nicht dafür konzipiert, System-Ordner wie bspw. `C:` oder `C:\Windows` zu indizieren. Davon, dies dennoch zu versuchen, wird aus folgenden Gründen abgeraten:

1. ***Leistungseinbruch***: Dateien in System-Ordnern tendieren dazu, sehr häufig modifiziert zu werden. Falls die Ordner-Überwachung für den fraglichen System-Ordner eingeschaltet ist, wird dies dazu führen, dass DocFetcher ständig versucht, seine Indizes zu aktualisieren, was einen signifikanten Leistungseinbruch auf Ihrem Computer zur Folge hat.
2. ***Speicher-Probleme***: DocFetcher muss für jede indizierte Datei eine winzigkleine Repräsentation im Arbeitsspeicher halten. Da aber System-Ordner üblicherweise eine sehr große Anzahl an Dateien enthalten, steigt durch Indizieren von System-Ordnern die Wahrscheinlichkeit, dass DocFetcher an sein Speicherlimit stößt.
3. ***Verschwendung von System-Ressourcen, schlechtere Suchergebnisse***: Über die vorgenannten technischen Gründe hinaus wird ein Indizieren von System-Ordnern wahrscheinlich auch eine Verschwendung von Indizierungs-Zeit und Festplatten-Speicher sein, sowie die Suchergebnisse mit irrelevanten Ergebnissen zumüllen. D.&nbsp;h. also, dass Sie für die besten Ergebnisse in der kürzestmöglichen Indizierungs-Zeit nur das indizieren sollten, was Sie wirklich benötigen.

**Unicode-Unterstützung**: DocFetcher bietet volle Unicode-Unterstützung für alle unterstützten Dateiformate. Im Falle von Textdateien muss DocFetcher auf [gewisse Heuristiken](http://www-archive.mozilla.org/projects/intl/UniversalCharsetDetection.html) zurückgreifen, um die korrekte Zeichenkodierung zu erraten, da Textdateien keine explizite Zeichenkodierungs-Angaben enthalten.

**Archiv-Unterstützung**: DocFetcher unterstützt derzeit folgende Archiv-Formate: Zip und davon abgeleitete Formate, 7z, Rar, sowie die gesamte tar.*-Format-Familie. Zudem werden auch ausführbare Zip- und 7z-Archive unterstützt, jedoch keine ausführbaren Rar-Archive. DocFetcher behandelt alle Archive so, als wären sie gewöhnliche Ordner, und es kann auch mit einer beliebigen Verschachtelung von Archiven umgehen (z.&nbsp;B. ein Zip-Archiv, das ein 7z-Archiv enthält, das ein Rar-Archiv enthält, usw.).<!-- this line should end with two spaces -->  
Beachten Sie, dass die Unterstützung für Zip- und 7z-Archive robuster und ausgereifter ist als die für andere Archiv-Formate. Die Indizierung von tar.gz, tar.bz2 und ähnlichen Formaten indes ist im Allgemeinen weniger effizient, da diese Formate keine interne Datei-Tabelle enthalten, weshalb DocFetcher gezwungen ist, bei jedem Zugriff auf solche Archive alle Dateien darin zu entpacken. Fazit: Falls Ihnen dies möglich ist, sollten Sie für maximale Kompatibilität mit DocFetcher bevorzugt Zip- und 7z-Archive verwenden.

**Der DocFetcher-Daemon ist unschuldig**: Falls Sie den DocFetcher-Daemon der Verlangsamung Ihres Computers verdächtigen, irren Sie sich wahrscheinlich. Tatsächlich ist der Daemon ein ziemlich simples Programm mit geringem Speicherverbrauch und geringer CPU-Belastung, und es macht nichts anderes, als Ordner zu überwachen. Falls Sie den Daemon dennoch loswerden wollen, können Sie entweder einfach die ausführbaren Daemon-Dateien umbenennen, sodass sie nicht mehr automatisch gestartet werden, oder auf die portable Version von DocFetcher umsteigen, bei welcher der Daemon standardmäßig deaktiviert ist.

* * *

<a name="Subpages"></a> <!-- Do not translate this line, just copy it verbatim. -->

Unterseiten des Benutzerhandbuchs
=================================
* [Suchanfrage-Syntax](DocFetcher_Manual_files/Query_Syntax.html)
* [Portable Dokument-Repositories](DocFetcher_Manual_files/Portable_Repositories.html)
* [Indizierungs-Einstellungen](DocFetcher_Manual_files/Indexing_Options.html)
* [Reguläre Ausdrücke](DocFetcher_Manual_files/Regular_Expressions.html)
* [Benachrichtigung über neue Programm-Versionen](DocFetcher_Manual_files/Release_Notification.html)
* [Erhöhung des Speicherlimits](DocFetcher_Manual_files/Memory_Limit.html)
* [Erhöhung des Ordner-Überwachungs-Limits (Linux)](DocFetcher_Manual_files/Watch_Limit.html)
* [Programm-Einstellungen](DocFetcher_Manual_files/Preferences.html)

Weitere Informationen
=====================
Für weitere Informationen sei auf unser [Wiki](http://docfetcher.sourceforge.net/wiki/doku.php) verwiesen. Für Fragen steht unser [Forum](http://sourceforge.net/projects/docfetcher/forums/forum/702424) zur Verfügung. Fehlerberichte können auf unserem [Bug Tracker](http://sourceforge.net/tracker/?group_id=197779&atid=962834) eingereicht werden.
