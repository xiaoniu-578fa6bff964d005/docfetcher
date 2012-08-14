Grundprinzip: Eine Warteschlange von Indizierungs-Aufträgen
===========================================================
Das Indizierungs-Fenster besteht aus einem oder mehreren Tabs, von denen jedes jeweils einem zu erstellenden oder zu aktualisierenden Index entspricht. Die Gesamtheit aller Tabs bildet eine Warteschlange, dessen Einträge einer nach dem anderen abgearbeitet werden. Sie können neue Einträge mit dem `'+'`-Button oben rechts zur Warteschlange hinzufügen.

Auf jedem Tab gibt es einen `Start`-Button unten rechts. Mit einem Klick darauf bestätigen Sie, dass der Indizierungs-Auftrag richtig konfiguriert und bereit für die Ausführung ist. Der Indizierungs-Prozess beginnt, sobald es einen Indizierungs-Auftrag in der Warteschlange gibt, der sich im "Bereit"-Zustand befindet.

Rechts neben dem `'+'`-Button befindet sich ein weiterer Button. Ein Klick darauf bewirkt, dass das gesamte Indizierungs-Fenster in DocFetcher's Statusleiste minimiert wird. Dies ermöglicht es, in existierenden Indizes zu suchen, während gleichzeitig neue Indizes im Hintergrund erstellt werden.

Sie können einen Indizierungs-Auftrag mit dem Schließen-Button (`'x'`) auf dessen Tab abbrechen. Wenn ein Indizierungs-Auftrag abgebrochen wird, werden Sie vor die Wahl gestellt, den unvollständigen Index entweder zu behalten oder zu löschen. Der Zweck des Behaltens ist, dass Sie damit einen Indizierungs-Auftrag jederzeit anhalten und später fortsetzen können. Das Fortsetzen geschieht einfach dadurch, dass Sie eine Index-Aktualisierung auf dem unvollständigen Index ausführen, vermittels `Aktualisieren` im Kontext-Menü des `Suchbereichs`.

Das Indizierungs-Fenster selbst verfügt ebenfalls über einen Schließen-Button (auf Windows ist das der `'x'`-Button oben rechts). Wenn Sie darauf klicken, werden alle Indizierungs-Aufträge abgebrochen und aus der Warteschlange entfernt.

Indizierungs-Einstellungen
==========================
Anm.: Dieser Abschnitt beschreibt hauptsächlich die verfügbaren Indizierungs-Einstellungen für Ordner- und Archiv-Indizes. Die Einstellungen für Outlook-PST-Indizes sind in der Tabelle "Diverse Einstellungen" weiter unten aufgeführt.

Datei-Erweiterungen
-------------------
In dem Bereich "Datei-Erweiterungen" auf dem Indizierungs-Fenster können Sie einstellen, welche Dateien als Textdateien oder Zip-Archive behandelt werden sollen. Ein häufiger Anwendungsfall hierfür besteht in der Indizierung von Quell-Code. Neben den beiden Textfeldern im "Datei-Erweiterungen"-Bereich gibt es zudem noch zwei "..."-Buttons. Wenn Sie auf diese klicken, durchsucht DocFetcher den zu indizierenden Ordner und stellt eine Liste von möglichen Datei-Erweiterungen zusammen, aus denen Sie die gewünschten auswählen können.

Dateien überspringen / MIME-Typ ermitteln
-----------------------------------------
Durch Hinzufügen von Einträgen zur Tabelle im Bereich "Dateien überspringen / MIME-Typ ermitteln" des Indizierungs-Fensters können Sie (1) bestimmte Dateien von der Indizierung ausschließen, und (2) die MIME-Typ-Detektion für bestimmte Dateien aktivieren. Beides macht von regulären Ausdrücken Gebrauch, welche in der [Einführung in reguläre Ausdrücke](Regular_Expressions.html) näher erläutert werden.

Und so funktioniert die Tabelle: Jeder Eintrag in der Tabelle besteht aus einem regulären Ausdruck und einer damit verknüpften Aktion. Der reguläre Ausdruck kann entweder auf Dateinamen oder Dateipfade angewandt werden, und die Aktion ist entweder der Ausschluss der Datei von der Indizierung oder die Aktivierung der MIME-Typ-Detektion. Wenn also während der Indizierung eine bestimmte Datei auf einen der regulären Ausdrücke "passt", wird die damit verknüpfte Aktion auf der Datei ausgeführt.

Sie können mit den `'+'`- und `'-'`-Buttons rechts neben der Tabelle Einträge hinzufügen oder entfernen. Mit den Hoch- und Runter-Buttons kann die *Priorität* des selektierten Tabellen-Eintrags erhöht oder gesenkt werden. Die Priorität kommt zum Tragen, wenn die regulären Ausdrücke mehrerer Tabellen-Einträge auf eine bestimmte Datei passen. In dem Fall wird dann nur die Aktion des Tabellen-Eintrags mit der höchsten Priorität ausgeführt, während die Aktionen aller anderen ignoriert werden.

Direkt unterhalb der Tabelle befindet sich ein kleines Hilfsmittel zum Erstellen regulärer Ausdrücke: Mittels Klick auf den `'...'`-Button auf der rechten Seite in diesem Bereich können Sie eine Datei oder einen Ordner innerhalb des zu indizierenden Ordners auswählen. Sodann erscheint der Pfad dieser Datei oder dieses Ordners im Textfeld, und die Textzeile direkt darüber teilt Ihnen mit, ob der gegenwärtig ausgewählte reguläre Ausdruck in der Tabelle zu der selektierten Datei bzw. dem selektierten Ordner passt.

Diverse Einstellungen
---------------------
Einstellung | Anmerkungen
------------|------------
HTML-Paare jeweils als ein einziges Dokument indizieren  |  Stellt ein, ob HTML-Dateien und die damit assoziierten Ordner (z.&nbsp;B. eine Datei namens `Beispiel.html` und ein Ordner namens `Beispiel-Dateien`) als ein einziges Dokument behandelt werden sollen.
Ausführbare Zip- und 7z-Archive detektieren (langsamer)  |  Stellt ein, ob DocFetcher für *jede* Datei mit der Datei-Erweiterung `exe` überprüfen soll, ob es sich um ein ausführbares Zip- oder 7z-Archiv handelt.
Dateinamen indizieren, selbst wenn Datei-Inhalt nicht extrahierbar  |  Stellt ein, ob DocFetcher *alle* Dateien indizieren soll, egal ob deren Text-Inhalt extrahiert werden konnte oder nicht. Durch Aktivieren dieser Einstellungen wird die volle Suche in Dateinamen eingeschaltet. Bitte beachten Sie aber, dass DocFetcher in Abhängigkeit von der Anzahl der Dateien im indizierten Ordner dann möglicherweise wesentlich mehr Arbeitsspeicher benötigt. Falls Sie damit an das interne Speicherlimit stoßen, haben Sie die Möglichkeit, [das Speicherlimit zu erhöhen](Memory_Limit.html).
Relative Pfade speichern, wenn möglich (zwecks Portabilität)  |  Diese Einstellung ist wichtig, wenn Sie die portable Version von DocFetcher benutzen. Mehr dazu auf der Seite über [portable Dokument-Repositories](Portable_Repositories.html).
Automatische Detektion von Datei-Änderungen  |  Stellt ein, ob DocFetcher Datei-Änderungen im indizierten Ordner automatisch erkennen ("Ordner-Überwachung") und die Indizes entsprechend aktualisieren soll. Diese Einstellung hat keinen Einfluss auf das Verhalten des DocFetcher-Daemons.
