Portable Dokument-Repositories
==============================

Grundlegende Benutzung
----------------------
Die portable Version von DocFetcher ermöglicht es, ein voll indiziertes und durchsuchbares Dokument-Repository zu erstellen, welches Sie dann mit sich herumtragen oder an andere Menschen weitergeben können. Falls Sie die portable Version noch nicht haben, können Sie sie von der [Projekt-Website](http://docfetcher.sourceforge.net/download.html) beziehen.

Die portable Version erfordert keine Installation; stattdessen müssen Sie einfach nur den Inhalt des heruntergeladenen Archivs in einen Ordner Ihrer Wahl entpacken. Sie können dann DocFetcher mit dem passenden Startprogramm für Ihr Betriebssystem starten: `DocFetcher.exe` auf Windows, `DocFetcher.sh` auf Linux und das `DocFetcher` Application Bundle auf Mac OS&nbsp;X. Es muss aber in jedem Fall eine Java-Laufzeit-Umgebung, Version 1.6 oder neuer, installiert sein.

<u>Relative Pfade</u>: Eine wichtige Sache, auf die zu achten ist, besteht darin, dass alle Indizes mit der Einstellung *Relative Pfade speichern* erstellt werden müssen. Ohne diese Einstellung wird DocFetcher nämlich *absolute* Pfad-Referenzen zu Ihren Dateien erstellen, sodass Sie dann nur DocFetcher und seine Indizes frei umherschieben können, aber nicht Ihre Dateien &mdash; jedenfalls nicht, ohne Datei-Referenzen ungültig zu machen. Hier ein Beispiel, um dies zu verdeutlichen:

* Relativer Pfad: `..\..\meine-dateien\eine-datei.txt`
* Absoluter Pfad: `C:\meine-dateien\eine-datei.txt`

Der relative Pfad sagt DocFetcher im Wesentlichen, dass es `eine-datei.txt` dadurch auffinden kann, dass es vom gegenwärtigen Ort zwei Ebenen nach oben und dann in den Ordner `meine-dateien` hineingehen soll. Der absolute Pfad hingegen stellt eine feste Referenz dar und ist unabhängig von DocFetcher's gegenwärtigem Standort, sodass Sie `eine-datei.txt` nicht umherschieben können, ohne die Datei-Referenz ungültig zu machen (womit DocFetcher dann nicht mehr imstande wäre, die Datei aufzufinden).

Beachten Sie, dass DocFetcher nur *versuchen* kann, relative Pfade zu speichern: Offenbar ist dies nicht möglich, wenn sich DocFetcher und Ihre Dateien auf unterschiedlichen Laufwerken befinden, z.&nbsp;B. DocFetcher in `D:\DocFetcher` und Ihre Dateien in `E:\meine-dateien`.

Tipps & Tricks
--------------

* ***CD-Archivierung***: Eigentlich offensichtlich, aber dennoch erwähnenswert: Wenn Sie DocFetcher auf eine CD brennen, werden Sie danach nicht mehr in der Lage sein, Programm-Einstellungen oder Indizes zu ändern. Folglich sollten Sie DocFetcher entsprechend konfigurieren, bevor Sie es auf die CD brennen. Zudem kann es sinnvoll sein, eine Java-Laufzeit-Umgebung mit auf die CD zu packen.
* ***Unterschiedliche Programm-Titel***: Für die Weitergabe Ihres portablen Dokument-Repositories, oder um die Arbeit mit mehreren DocFetcher-Instanzen weniger verwirrend zu machen, können Sie mehreren DocFetcher-Instanzen jeweils einen anderen Programm-Titel zuweisen. Öffnen Sie dazu `Erweiterte Einstellungen` auf dem Programm-Einstellungs-Fenster und ändern Sie die Einstellung `AppName` entsprechend ab.

Bitte beachten
--------------

* ***Finger weg vom `indexes`-Ordner***: Sie können, müssen aber nicht Ihre Dateien im DocFetcher-Ordner ablegen. Falls Sie das tun sollten, legen Sie auf keinen Fall irgendetwas im `indexes`-Ordner ab, da alles, was sich darin befindet, gelöscht werden könnte.
* ***Dateinamens-Inkompatibilitäten***: Hüten Sie sich vor Dateinamens-Inkompatibilitäten zwischen verschiedenen Betriebssystemen. Bspw. können Dateinamen auf Linux Zeichen wie ":" oder "|" enthalten, aber nicht auf Windows. Infolgedessen können Sie Ihr Dokument-Repository nur von Linux nach Windows oder in die umgekehrte Richtung bewegen, wenn die Dateien in Ihrem Repository keine inkompatiblen Dateinamen haben. Ein ganz anderes Thema sind übrigens Sonderzeichen wie die Umlaute im Deutschen...
