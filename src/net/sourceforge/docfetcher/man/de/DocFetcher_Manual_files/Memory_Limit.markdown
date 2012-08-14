Erhöhung des Speicherlimits
===========================
Für DocFetcher besteht standardmäßig ein Speicherlimit von 256&nbsp;MB. Dies kann durch Modifikation der plattformspezifischen Startprogramme abgeändert werden.

Windows
-------
In der Windows-Version von DocFetcher sind vorgefertige alternative Startprogramme mitgeliefert, welche andere Speicherlimits setzen. Diese alternativen Startprogramme sind auf folgende Weise zu benutzen:

* Öffnen Sie den DocFetcher-Ordner. Falls Sie die portable Version von DocFetcher verwenden, ist dies einfach der Ordner, den Sie heruntergeladen und entpackt haben. Falls Sie die nicht-portable Version benutzen, sollte sich DocFetcher in `C:\Program Files`, `C:\Program Files (x86)` oder an einem ähnlichen Ort befinden.
* Die alternativen Startprogramme befinden sich im Ordner `DocFetcher\misc`. Sie weisen alle einen Dateinamen `DocFetcher-XXX.exe` auf, wobei das `XXX` für das Speicherlimit des jeweiligen Startprogramms steht. Das Startprogramm `DocFetcher-512.exe` bspw. setzt ein Speicherlimit von 512&nbsp;MB.
* Vor Gebrauch eines der Startprogramme **müssen Sie es zuerst in den DocFetcher-Ordner darüber verschieben oder kopieren**. Es ist nicht notwendig, das normale Startprogramm zu löschen oder das alternative Startprogramm umzubenennen.

Ein weiterer Weg, das Speicherlimit zu ändern, besteht darin, die Datei `misc\DocFetcher.bat` in den DocFetcher-Ordner zu kopieren und den Ausdruck `-Xmx256m` in der letzten Zeile der Datei umzuändern, z.&nbsp;B. zu `-Xmx512m`.

Linux
-----
Öffnen Sie das Startskript `DocFetcher/DocFetcher.sh` mit einem Text-Editor und ändern Sie in der letzten Zeile den Ausdruck `-Xmx256m` entsprechend ab, bspw. zu `-Xmx512m`.

Mac OS&nbsp;X
-------------
Sowohl in der nicht-portablen als auch portablen Mac OS&nbsp;X Version wird DocFetcher mittels Application Bundles gestartet. In der nicht-portablen Version ist das Application Bundle einfach das, was Sie im heruntergeladenen Disk Image vorgefunden haben. In der portablen Version befindet sich das Application Bundle im DocFetcher-Ordner.

In beiden Fällen handelt es sich beim Application Bundle in Wirklichkeit um einen Ordner mit der Erweiterung `.app`. Im Finder sollte es einen Kontextmenü-Eintrag geben, um diesen Ordner zu öffnen. Dieser Eintrag sollte in der englischen Version von Mac OS&nbsp;X `Show Package Contents` heißen, und in der deutschen Version `Paketinhalt anzeigen`.

Im besagten Ordner finden Sie folgendes Startskript: `Contents/MacOS/DocFetcher`. Öffnen Sie es mit einem Text-Editor und ändern Sie in der letzten Zeile den Ausdruck `-Xmx256m` entsprechend ab, z.&nbsp;B. zu `-Xmx512m`.
