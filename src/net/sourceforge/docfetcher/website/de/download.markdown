Hinweis zu Java-Sicherheitslücken
====================================
Falls Sie von den Java-Sicherheitslücken gelesen haben, die vor einer Weile in den Medien berichtet wurden, und sich nun davor scheuen, den Java-basierten DocFetcher zu installieren, sei hiermit Entwarnung gegeben:

Die berichteten Java-Sicherheitslücken betreffen nur **das Java-Plugin in Ihrem Webbrowser**, nicht eigenständige Java-basierte Programme wie DocFetcher. Deaktivieren oder deinstallieren Sie einfach das Java-Plugin, und schon sind Sie auf der sicheren Seite. Die Java-Laufzeit-Umgebung, DocFetcher und andere Java-basierte Programme zu installieren ist genauso ungefährlich (oder gefährlich) wie jedwede andere Programme zu installieren.

Für weitere Info sei auf [diese Diskussion](http://sourceforge.net/p/docfetcher/discussion/702424/thread/b4c0d714/) in unserem Forum verwiesen.


Schritt 1: Java-Laufzeit-Umgebung installieren
====================================
Eine installierte Java-Laufzeit-Umgebung (JRE), Version 1.6.0 oder neuer, ist erforderlich. Um herauszufinden, welche JRE installiert ist, können Sie eine Eingabeaufforderung (Konsole) öffnen und eingeben: "java -version"

Falls Java 1.6.0 oder neuer nicht auf Ihrem System verfügbar ist, können Sie es von folgenden Orten beziehen:

* Windows: <http://java.com>
* Linux: Im offiziellen Software-Repository Ihrer Distribution.
* Mac OS&nbsp;X: <http://www.apple.com/support/>

Schritt 2: DocFetcher ${version} installieren
======================================

Für eine Liste von Änderungen im Vergleich zu vorherigen Versionen, siehe den [ChangeLog](http://docfetcher.sourceforge.net/wiki/doku.php?id=changelog).

Zwischen Version 1.0.3 und Version 1.1 beta 1 wurde DocFetcher von Grund auf neu geschrieben, was zu einer großen Anzahl neuer Features und Änderungen geführt hat. [Diese Seite](http://docfetcher.sourceforge.net/wiki/doku.php?id=changes_in_v1.1) gibt einen Überblick über die wichtigsten Neuerungen.

Alle unten aufgeführten Downloads unterstützen sowohl 32-Bit- als auch 64-Bit-Betriebssysteme.

<table>
<tr>
<th>Download & Installations-Hinweise</th>
<th>Unterstützte Betriebssysteme</th>
</tr>
<tr>
<td align="left"><a href="http://sourceforge.net/projects/docfetcher/files/docfetcher/${version}/docfetcher_${version}_win32_setup.exe/download">docfetcher_${version}_win32_setup.exe</a> <br/> Führen Sie das Installations-Programm aus und folgen Sie den Anweisungen.</td>
<td>Windows&nbsp;XP/Vista/7</td>
</tr>
<tr>
<td align="left"><a href="http://sourceforge.net/projects/docfetcher/files/docfetcher/${version}/docfetcher-${version}-portable.zip/download">docfetcher-${version}-portable.zip</a> <br/> Dies ist die portable Version, die auf allen unterstützten Betriebssystemen läuft. Installation: Entpacken Sie das Archiv in einen Ordner Ihrer Wahl, und starten Sie dann DocFetcher mittels Doppelklick auf die passende Datei für Ihr Betriebssystem. Stellen Sie sicher, dass Sie über Schreibrechte für den Ordner verfügen, in den DocFetcher entpackt wurde (d.&nbsp;h., dass DocFetcher nicht in einen Ordner wie bspw. "C:\Program&nbsp;Files" entpackt werden sollte).
</td>
<td>Windows&nbsp;XP/Vista/7; Linux; Mac OS&nbsp;X 10.5 oder neuer</td>
</tr>
<tr>
<td align="left"><a href="http://sourceforge.net/projects/docfetcher/files/docfetcher/${version}/DocFetcher-${version}.dmg/download">DocFetcher-${version}.dmg</a> <br/> Starten Sie DocFetcher mittels Doppelklick auf das Application Bundle.
</td>
<td>Mac OS&nbsp;X 10.5 oder neuer</td>
</tr>
</table>

Andere Downloads
================
Ältere Versionen sind auf der [SourceForge.net Download-Seite](http://sourceforge.net/projects/docfetcher/files/docfetcher/) erhältlich.

Wie man an Quell-Code gelangt, ist auf [dieser Wiki Seite](http://docfetcher.sourceforge.net/wiki/doku.php?id=source_code) erklärt.
