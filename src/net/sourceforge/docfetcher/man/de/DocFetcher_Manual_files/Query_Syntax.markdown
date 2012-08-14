Suchanfrage-Syntax
==================
Diese Seite gibt einen Überblick über die verfügbaren Konstrukte der Suchanfrage-Syntax, wobei die Konstrukte von "einfach" nach "fortgeschritten" sortiert sind. Die Suchanfrage-Syntax wird von DocFetcher's interner Suchmaschine Apache Lucene bereitgestellt und ist auf etwas technischere Weise auf der [Suchanfrage-Syntax-Seite von Lucene](http://lucene.apache.org/java/3_4_0/queryparsersyntax.html) erklärt.


Boolesche Operatoren
--------------------
DocFetcher unterstützt die Booleschen Operatoren `OR`, `AND` und `NOT`. Wenn Suchbegriffe *ohne* Boolesche Operatoren hintereinander geschrieben werden, verknüpft DocFetcher sie standardmäßig mittels `OR`. Wenn Sie stattdessen eine `AND`-Verknüpfung als Standard wünschen, können Sie dies in den [Programm-Einstellungen](Preferences.html) festlegen.

Anstelle von `OR`, `AND` und `NOT` können Sie auch `||`, `&&` und `'-'` (Minuszeichen), verwenden. Sie können *Klammern* benutzen, um bestimmte Ausdrücke zu gruppieren. Hier einige Beispiele:

Suchanfrage              | Gefundene Dateien enthalten...
-------------------------|---------------------------------------------
`dog OR cat`             | entweder `dog`, oder `cat`, oder beides
`dog AND cat`            | sowohl `dog` als auch `cat`
`dog cat`                | (standardmäßig äquivalent zur Suchanfrage `dog OR cat`)
`dog NOT cat`            | `dog`, aber nicht `cat`
`-dog cat`               | `cat`, aber nicht `dog`
`(dog OR cat) AND mouse` | `mouse`, und entweder `dog` oder `cat`, oder beides


Keine Unterscheidung von Groß- und Kleinschreibung
--------------------------------------------------
DocFetcher unterscheidet nicht zwischen Groß- und Kleinschreibung, sodass es keine Rolle spielt, ob die Suchbegriffe komplett in Großbuchstaben, oder komplett in Kleinbuchstaben, oder in einer Mischung von Groß- und Kleinbuchstaben eingegeben werden. Die einzige Ausnahme sind die Schlüsselwörter `OR`, `AND`, `NOT` und `TO`, die stets in Großschreibung eingegeben werden müssen. (Das Schlüsselwort `TO` ist weiter unten im Abschnitt 'Bereichs-Suche' erklärt.)


Phrasen-Suche und mandatorische Suchbegriffe
--------------------------------------------
Um nach einer Phrase zu suchen (d.&nbsp;h. einer festen Abfolge von Wörtern), können Sie die Phrase in Anführungszeichen setzen. Um anzugeben, dass die aufzufindenden Dateien definitiv einen bestimmten Begriff enthalten müssen, können Sie ein `'+'` vor den Begriff setzen. Natürlich können Sie diese beiden Konstrukte mit Booleschen Operatoren und Klammern kombinieren. Hier wieder ein paar Beispiele:

Suchanfrage           | Gefundene Dateien enthalten...
----------------------|-------------------------------------
`"dog cat mouse"`     | die Wörter `dog`, `cat` und `mouse`, in dieser Reihenfolge
`+dog cat`            | definitiv `dog`, und evtl. auch `cat`
`"dog cat" AND mouse` | die Phrase `dog cat`, und das Wort `mouse`
`+dog +cat`           | (äquivalent zur Suchanfrage `dog AND cat`)


Wildcards (Platzhalter)
-----------------------
Fragezeichen (`'?'`) und Sternchen (`'*'`) können als Wildcards (engl. für Platzhalter) verwendet werden, d.&nbsp;h. sie geben an, dass bestimmte Zeichen unbekannt sind. Das Fragezeichen steht für genau ein unbekanntes Zeichen, und das Sternchen für ein oder mehrere unbekannte Zeichen. Beispiele:

Suchanfrage  | Gefundene Dateien enthalten...
-------------|-------------------------------------
`luc?`       | `lucy`, `luca`, ...
`luc*`       | `luc`, `lucy`, `luck`, `lucene`, ...
`*ene*`      | `lucene`, `energy`, `generator`, ...

Anm.: Suchanfragen, in denen Wildcards als erstes Zeichen in einem Suchbegriff verwendet werden, erfordern im Durchschnitt mehr Suchzeit als andere Suchanfragen. Dies hängt mit der internen Struktur von Indizes zusammen: Wildcards als erstes Zeichen zu verwenden ist so, als würde man die Telefonnummer einer Person im Telefonbuch nachschlagen, aber nur den Vornamen der Person kennen. Ein konkretes Beispiel: Eine Suchanfrage wie `*ene*` wird wahrscheinlich mehr Zeit benötigen als andere Suchanfragen, da `*ene*` mit einem Wildcard beginnt.


Fuzzy-Suche
-----------
Die Fuzzy-Suche ist eine Suche nach Wörtern, die einem gegebenen Wort *ähneln*. Wenn Sie bspw. `roam~` eingeben, wird DocFetcher nach Dateien suchen, die z.&nbsp;B. `foam` oder `roams` enthalten.

Darüber hinaus können Sie auch einen Ähnlichkeits-Schwellwert zwischen 0 und 1 an die Fuzzy-Suche anhängen, z.&nbsp;B. so: `roam~0.8`. Je höher der Schwellwert, desto höher die Ähnlichkeit der Fundstellen. Wenn die Schwellwert-Angabe ausgelassen wird, wird implizit ein Wert von 0.5 verwendet.


Nachbarschafts-Suche
--------------------
Mittels Nachbarschafts-Suche können Sie nach Wörtern suchen, die innerhalb einer bestimmten Höchst-Distanz entfernt voneinander liegen. Eine Nachbarschafts-Suche können Sie ausführen, indem Sie eine Tilde (`'~'`) an eine Phrase anhängen, gefolgt von einer Distanz-Angabe. &mdash; Man beachte, dass dies in syntaktischer Hinsicht der Fuzzy-Suche ähnelt. Beispiel: Um nach Dateien zu suchen, die die Wörter `wikipedia` und `lucene` enthalten, und in denen die beiden Wörter höchstens 10 Wörter weit voneinander entfernt sind, können Sie folgendes eingeben: `"wikipedia lucene"~10`


Boosting
--------
Sie können durch Zuweisung von Gewichten zu Suchbegriffen die Sortierung der Suchergebnisse nach Trefferquote beeinflussen. Beispiel: Wenn Sie `dog^4 cat` anstelle von `dog cat` eingeben, erhalten Dateien, die `dog` enthalten, eine höhere Wertung und werden daher weiter oben in der Ergebnisliste erscheinen.

Derartige Gewichte müssen positiv sein, können aber kleiner als 1 sein (z.&nbsp;B. 0.2). Wenn kein Gewicht angegeben ist, wird der Standardwert 1 verwendet.


Feld-Suche
----------
Standardmäßig sucht DocFetcher in jeglichem Text aus einer Datei, den es zu extrahieren imstande war. Dazu zählen der "eigentliche" Datei-Inhalt, der Dateiname, sowie Meta-Daten. Stattdessen können Sie die Suche aber auch auf den Dateinamen und/oder bestimmte Meta-Daten-Felder einschränken. Um bspw. nach Dateien zu suchen, deren Titel `wikipedia` enthält, können Sie eingeben: `title:wikipedia`. Dies kann mit Phrasen-Suche kombiniert werden, z.&nbsp;B. `title:"dog cat"`, oder auch mit Klammern, z.&nbsp;B. `title:(dog cat)`. Wenn Sie übrigens die Anführungszeichen und Klammern in den zwei vorherigen Beispielen auslassen, wird nur der Begriff `dog` auf den Datei-Titel eingeschränkt, aber nicht `cat`.

Welche Felder verfügbar sind, hängt zwar im Allgemeinen vom jeweiligen Dateityp ab, aber Sie können von folgender Daumenregel ausgehen:

<!-- Do not translate the following field names (filename, title, etc.) -->
* *Dateien*: filename, title, author
* *E-Mails*: subject, sender, recipients


Bereichs-Suche
--------------
DocFetcher erlaubt die Suche nach Wörtern, die sich lexikographisch *zwischen* zwei anderen Wörtern befinden. Bspw. liegt das Wort `beta` zwischen den Wörtern `alpha` und `gamma`. Um z.&nbsp;B. alle Dateien zu finden, die Wörter enthalten, welche sich zwischen `alpha` und `gamma` befinden, können Sie folgendes eingeben: `[alpha TO gamma]`.

Wenn Sie rechteckige Klammern verwenden, arbeitet die Bereichs-Suche *inklusiv*, d.&nbsp;h. `alpha` und `gamma` gehen mit in die Suchergebnisse ein. Um stattdessen eine *exklusive* Bereichs-Suche auszuführen, müssen Sie geschweifte Klammern verwenden: `{alpha TO gamma}`

Sie können auf folgende Weise Bereichs-Suche und Feld-Suche kombinieren: `title:{alpha TO gamma}`. Damit wird die Bereichs-Suche auf das `title`-Feld eingeschränkt.
