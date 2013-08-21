Beschreibung
============
DocFetcher ist ein Open Source Desktop-Suchprogramm: Es ermöglicht die Volltext-Suche in Dateien auf dem Computer &mdash; eine Art Google für den Heimrechner. Das Programm läuft auf Windows, Linux und Mac OS&nbsp;X, und ist verfügbar unter der [Eclipse Public License](http://en.wikipedia.org/wiki/Eclipse_Public_License).

Grundlegende Benutzung
======================
Der folgende Screenshot zeigt das Programm-Hauptfenster. Suchanfragen werden in das Textfeld bei (1) eingegeben. Die Suchergebnisse werden dann in der Ergebnis-Tabelle bei (2) angezeigt. Das Vorschau-Fenster bei (3) zeigt eine Text-Vorschau derjenigen Datei, die gegenwärtig in der Ergebnis-Tabelle selektiert ist. Alle Fundstellen in der Datei sind gelb hervorgehoben.

Sie können die Ergebnisse filtern nach Mindest- und/oder Maximal-Dateigröße (4), nach Datei-Typ (5) und nach Ort (6). Mit den Buttons bei (7) werden das Benutzer-Handbuch geöffnet, das Einstellungs-Fenster geöffnet, sowie das Programm in den System Tray minimiert.

<div id="img" style="text-align: center;"><a href="../all/intro-001-results-edited.png"><img style="width: 500px; height: 375px;" src="../all/intro-001-results-edited.png"></a></div>

DocFetcher erfordert das Erstellen sogenannter *Indizes* für die Ordner, die durchsucht werden sollen. Was Indizierung ist und wie es funktioniert, ist weiter unten erklärt. Grob gesagt erlaubt ein Index, sehr schnell (im Millisekunden-Bereich) herauszufinden, welche Dateien bestimmte Wörtern enthalten, wodurch sich die Suchgeschwindigkeit drastisch erhöht. Der folgende Screenshot zeigt DocFetcher's Dialog zum Erstellen neuer Indizes:

<div id="img" style="text-align: center;"><a href="../all/intro-002-config.png"><img style="width: 500px; height: 375px;" src="../all/intro-002-config.png"></a></div>

Durch einen Klick auf den "Run"-Button unten rechts auf diesem Dialog wird die Indizierung gestartet. Die Indizierung kann in Abhängigkeit von der Anzahl und Größe der Dateien eine Weile dauern. Eine gute Daumenregel ist 200 Dateien pro Minute.

Das Erstellen eines Index mag zwar eine Weile dauern, aber dies muss nur einmal pro Ordner ausgeführt werden. Zudem läuft die *Aktualisierung* eines Index nach Veränderungen im dazugehörigen Ordner wesentlich schneller ab &mdash; üblicherweise dauert eine Aktualisierung lediglich einige Sekunden.

Besondere Features
==================
* **Portable Version**: DocFetcher ist in einer portablen Version erhältlich, die auf Windows, Linux *und* Mac OS&nbsp;X läuft. Inwiefern dies nützlich ist, ist weiter unten beschrieben.
* **64-Bit-Unterstützung**: Sowohl 32-Bit- als auch 64-Bit-Betriebssysteme werden unterstützt.
* **Unicode-Unterstützung**: DocFetcher bietet solide Unicode-Unterstützung für die gängigsten Datei-Formate, einschließlich Microsoft Office, OpenOffice.org, PDF, HTML, RTF und Textdateien. Die einzige Ausnahme sind CHM-Dateien, für die noch keine Unicode-Unterstützung vorhanden ist.
* **Archiv-Unterstützung**: DocFetcher kommt mit folgenden Archiv-Formaten klar: zip, 7z, rar, sowie die gesamte tar.*-Format-Familie. Die Datei-Erweiterungen für Zip-Dateien können angepasst werden, sodass Sie bei Bedarf weitere zip-basierte Datei-Formate hinzufügen können. Zudem kann DocFetcher mit einer beliebig tiefen Verschachtelung von Archiven umgehen (bspw. ein Zip-Archiv, das ein 7z-Archiv enthält, welches ein Rar-Archiv enthält, usw.).
* **Suche in Quell-Code**: Die Datei-Erweiterungen, anhand derer DocFetcher Textdateien identifiziert, können angepasst werden, sodass DocFetcher in jeglichen textbasierten Datei-Formaten suchen kann, insbesondere in Quell-Code. (Dies funktioniert gut in Kombination mit den anpassbaren Zip-Erweiterungen, z. B. zur Suche in Java-Quell-Code, der in Jar-Dateien verpackt ist.)
* **Outlook PST-Dateien**: DocFetcher erlaubt die Suche in Outlook-E-Mails, die Microsoft Outlook typischerweise in PST-Dateien ablegt.
* **Detektion von HTML-Paaren**: Standardmäßig detektiert DocFetcher Paare von HTML-Dateien (z. B. eine Datei namens "Beispiel.html" und einen Ordner namens "Beispiel-Dateien"), und behandelt beides als ein einziges Dokument. Dieses Feature mag auf den ersten Blick nutzlos erscheinen; jedoch hat sich in der Praxis gezeigt, dass dies signifikant die Suchergebnisse verbessert, da nämlich der ganze "Müll" in den HTML-Ordnern aus den Suchergebnissen verschwindet.
* **Datei-Ausschluss mittels regulärer Ausdrücke**: Sie können mittels regulärer Ausdrücke bestimmte Dateien von der Indizierung ausschließen. Bspw. können Sie mit folgendem regulären Ausdruck alle Microsoft-Excel-Dateien von der Indizierung ausschließen: `.*\.xls`
* **MIME-Typ-Detektion**: Sie können mittels regulärer Ausdrücke eine "MIME-Typ-Detektion" für bestimmte Dateien einschalten. Dies bewirkt, dass DocFetcher deren tatsächlichen Datei-Typ dann nicht mehr nur anhand der Datei-Erweiterung, sondern auch durch Inspektion des Datei-Inhalts zu bestimmen versucht. Dies ist nützlich bei Dateien, denen eine falsche Datei-Erweiterung gegeben wurde.
* **Mächtige Suchanfrage-Syntax**: Über simple Konstrukte wie `OR`, `AND` und `NOT` hinaus unterstützt DocFetcher unter Anderem: Wildcards, Phrasen-Suche, Fuzzy-Suche ("finde Wörter, die folgenden Wörtern ähneln: ..."), Nachbarschafts-Suche ("folgende Wörter sollen höchstens 10 Wörter voneinander entfernt sein"), Boosting ("gib Dateien höheres Gewicht, die folgende Wörter enthalten: ...")

Unterstützte Datei-Formate
==========================
* Microsoft Office (doc, xls, ppt)
* Microsoft Office 2007 und neuere Versionen (docx, xlsx, pptx, docm, xlsm, pptm)
* Microsoft Outlook (pst)
* OpenOffice.org (odt, ods, odg, odp, ott, ots, otg, otp)
* Portable Document Format (pdf)
* HTML (html, xhtml, ...)
* Textdateien (anpassbar)
* Rich Text Format (rtf)
* AbiWord (abw, abw.gz, zabw)
* Microsoft Compiled HTML Help (chm)
* MP3 Metadaten (mp3)
* FLAC Metadaten (flac)
* JPEG Exif Metadaten (jpg, jpeg)
* Microsoft Visio (vsd)
* Scalable Vector Graphics (svg)

Was manche Leute von diesem Programm halten...
==============================================
${awards_table}

DocFetcher hat zudem gute Benutzer-Wertungen auf [unserer SourceForge.net-Seite](http://sourceforge.net/projects/docfetcher/reviews) erhalten.

Vergleich mit anderen Desktop-Suchprogrammen
============================================
Im Vergleich zu anderen Desktop-Suchprogrammen zeichnet sich DocFetcher auf folgende Weisen aus:

**Frei von Müll**: Wir geben uns besondere Mühe, DocFetcher's Benutzeroberfläche frei von Müll zu halten. Es werden keine Werbung und keine Registrierungs-Fenster angezeigt. Es wird auch kein Müll in Ihrem Webbrowser oder sonstwo in Ihrem Betriebssystem installiert.

**Privatsphäre**: DocFetcher sammelt definitiv keine persönlichen Benutzerdaten. Zweifler mögen sich DocFetcher's [Quell-Code](http://docfetcher.sourceforge.net/wiki/doku.php?id=source_code) anschauen, um dies zu bestätigen.

**Frei für immer**: Da DocFetcher Open Source ist, brauchen Sie nicht zu befürchten, dass das Programm jemals obsolet werden sollte, da nämlich der Quell-Code stets verfügbar sein wird. Übrigens, haben Sie mitbekommen, dass die Entwicklung von Google Desktop, einer von DocFetcher's kommerziellen Haupt-Konkurrenten, im Jahr 2011 eingestellt wurde? Tja...

**Plattformübergreifend**: Im Gegensatz zu vielen seiner Konkurrenten läuft DocFetcher nicht nur auf Windows, sondern auch auf Linux und Mac OS&nbsp;X. Sollten Sie also jemals das Bedürfnis verspüren, Ihren Windows-Computer hinter sich zu lassen und in Linux oder Mac OS&nbsp;X hineinzuschnuppern, wird DocFetcher dort bereits auf Sie warten.

**Portabel**: Eine von DocFetcher's größten Stärken besteht in seiner Portabilität. Im Wesentlichen heißt das, dass Sie mit DocFetcher ein vollständig indiziertes und durchsuchbares Dokument-Repository anlegen können, welches Sie dann z.&nbsp;B. auf Ihrem USB-Stick herumtragen können. Mehr dazu im folgenden Abschnitt.

**Nur indizieren, was nötig ist**: Unter DocFetcher's kommerziellen Konkurrenten scheint es eine Tendenz zu geben, Benutzer dazu zu drängen, die gesamte Festplatte zu indizieren &mdash; möglicherweise um vermeintlich "dummen" Benutzern so viele Entscheidungen wie möglich abzunehmen, oder schlimmer noch, um an mehr persönliche Benutzerdaten zu gelangen. Tatsächlich jedoch kann davon ausgegangen werden, dass die meisten Benutzer gerade *nicht* wollen, dass ihre gesamte Festplatte indiziert wird, denn dies ist nicht nur eine Verschwendung von Indizierungs-Zeit und Festplatten-Speicher, sondern müllt auch die Suchergebnisse mit irrelevanten Dateien voll. Daher indiziert DocFetcher nur das, was Sie explizit zur Indizierung angeordnet haben, und bietet darüber hinaus noch diverse Filter-Optionen.

Portable Dokument-Repositories
==============================
Eines von DocFetcher's herausragenden Features besteht darin, dass es als portable Version erhältlich ist, die es Ihnen erlaubt, *portable Dokument-Repositories* zu erstellen, d.&nbsp;h. vollständig indizierte und durchsuchbare Repositories all Ihrer wichtigen Dokumente, die Sie frei umherbewegen können.

**Anwendungs-Beispiele**: Mit solchen Repositories lässt sich allerhand machen: Sie können sie auf einem USB-Stick mit sich führen, zwecks Archivierung auf eine CD-ROM brennen, in einen verschlüsselten Datei-Container stecken (empfohlen: [TrueCrypt](http://www.truecrypt.org/)), mittels eines sogenannten Cloud Storage Service wie bspw. [DropBox](http://www.dropbox.com/) über mehrere Computer hinweg synchronisieren, usw. usf. Besser noch: Da DocFetcher Open Source ist, können Sie Ihr Repository ins Internet hochladen und mit der übrigen Welt teilen.

**Java: Performance und Portabilität**: Ein Aspekt an DocFetcher, der manchen Leuten mißfallen mag, ist der Umstand, dass das Programm in Java geschrieben wurde, welches den Ruf hat, "langsam" zu sein. Dies mag vor zehn Jahren der Wahrheit entsprochen haben, aber in der Zwischenzeit hat sich die Performance von Java drastisch verbessert &mdash; das behauptet jedenfalls [Wikipedia](http://en.wikipedia.org/wiki/Java_%28software_platform%29#Performance). Wie dem auch sei, das Tolle an Java ist, dass *dasselbe* DocFetcher-Paket auf Windows, Linux und Mac OS&nbsp;X ausgeführt werden kann &mdash; viele andere Programme erfordern separate Pakete für die einzelnen Betriebssysteme. Infolgedessen können Sie bspw. Ihr portables Dokument-Repository auf einem USB-Stick ablegen und dann von *jedem* der oben genannten Betriebssysteme aus darauf zugreifen, sofern auf dem jeweiligen Betriebssystem Java installiert ist.

Wie Indizierung funktioniert
============================
Dieser Abschnitt gibt eine kleine Einführung darüber, was Indizierung ist und wie es funktioniert.

**Der naive Ansatz der Datei-Suche**: Der einfachste Ansatz, nach Dateien zu suchen, besteht darin, bei jeder Suche alle Dateien nacheinander durchzugehen. Dies funktioniert prima für Suche, die sich auf *Dateinamen* beschränkt, da das Auslesen von Dateinamen sehr schnell vonstatten geht. Dieser Ansatz funktioniert jedoch nicht mehr so gut, wenn es darum geht, *Datei-Inhalte* zu durchsuchen, da nämlich die dazu erforderliche Text-Extraktion wesentlich mehr System-Ressourcen und Rechenzeit erfordert.

**Index-basierte Suche**: Das ist der Grund, warum DocFetcher, ein Suchprogramm für Datei-Inhalte, den Ansatz der *Indizierung* gewählt hat. Die Grundidee besteht darin, dass der Großteil der Dateien, in denen gesucht werden soll (z.&nbsp;B. über 95%), sehr selten oder überhaupt nicht modifiziert wird. Deshalb kann man darauf verzichten, bei jeder Suchanfrage eine volle Text-Extraktion vorzunehmen, und stattdessen diese Text-Extraktion *nur einmal* vornehmen, um daraus einen sogenannten *Index* zu erstellen. Einen solchen Index kann man sich vorstellen als eine Art Nachschlagewerk, welches erlaubt, zu einem gegebenen Wort schnell herauszufinden, welche Dateien dieses Wort enthalten.

**Telefonbuch-Analogie**: Als anschauliche Analogie möge man sich vergegenwärtigen, dass es wesentlich effizienter ist, die Telefonnummer einer bestimmten Person in einem Telefonbuch (dem "Index") nachzuschlagen, anstatt *jede* mögliche Telefonnummer auszuprobieren, nur um jeweils herauszufinden, ob die Person am anderen Ende der Leitung diejenige ist, nach der man sucht. &mdash; Jemanden über das Telefon anzurufen und die Text-Extraktion aus einer Datei sind beide "teure" Operationen. Außerdem ist der Fakt, dass Leute ihre Telefonnummern selten ändern, analog zu dem Fakt, dass die meisten Dateien auf einem Computer sehr selten, wenn überhaupt, modifiziert werden.

**Index-Aktualisierung**: Offenbar reflektiert ein Index nur den Zustand der indizierten Dateien zu dem Zeitpunkt, zu dem der Index erstellt wurde, und nicht zwangsläufig den aktuellen Zustand der Dateien. Das bedeutet, dass wenn der Index nicht auf dem neuesten Stand ist, die Suchergebnisse veraltet sein könnten, in derselben Weise wie auch Einträge in einem Telefonbuch veraltet sein könnten. Jedoch sollte dies kein großes Problem darstellen, wenn sich die Dateien selten ändern. Außerdem ist DocFetcher in der Lage, *automatisch* seine Indizes zu aktualisieren: (1) Während das Programm läuft, kann es Datei-Modifikationen detektieren und die Indizes entsprechend aktualisieren. (2) Wenn das Programm nicht läuft, detektiert ein kleines Daemon-Programm im Hintergrund Datei-Modifikationen und führt eine Liste von Indizes, die beim nächsten Ausführen von DocFetcher aktualisiert werden müssen. Übrigens gibt es keinen guten Grund, sich über den Daemon zu sorgen: Er beansprucht kaum Rechenzeit und Arbeitsspeicher, da er nichts anderes tut, als eine Liste von zu aktualisierenden Indizes zu verwalten, und die teureren Index-Aktualisierungen DocFetcher überlässt.
