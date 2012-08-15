Reguläre Ausdrücke
==================
Diese Seite bietet eine kurze und knappe Einführung in reguläre Ausdrücke. Das Thema kann hier in keinster Weise erschöpfend abgedeckt werden, da es sich bei regulären Ausdrücken um eine komplette Sprache für den Vergleich von Zeichenfolgen (Pattern Matching) handelt. Wenn Sie mehr über reguläre Ausdrücke erfahren wollen, sollten Sie im Internet nach 'reguläre ausdrücke einführung' oder dergleichen suchen; damit werden Sie eine Unmenge an Material zu dem Thema finden.

Identifikation von Microsoft-Excel-Dateien: `.*\.xlsx?`
-------------------------------------------------------
In regulären Ausdrücken (häufig abgekürzt als "Regex") haben bestimmte Zeichen eine besondere Bedeutung. Bspw. steht der ***Punkt*** (`'.'`) für genau ein unbekanntes Zeichen. Somit können Sie z.&nbsp;B. den Regex `h.llo` verwenden, um Zeichenfolgen wie `hallo` oder `hello` zu identifizieren, aber auch Zeichenfolgen wie `hzllo` oder `h8llo`.

Ein weiteres Sonderzeichen ist das ***Sternchen*** (`'*'`), das im Wesentlichen ausdrückt: "das vorhergehende Zeichen, keinmal, einmal oder mehrmals wiederholt". Der Regex `hello*` würde also bspw. zu folgenden Zeichenfolgen passen: `hell`, `hello`, `helloo`, `hellooo`, und so weiter.

Eine Implikation der vorangegangenen Regeln ist, dass eine Kombination des Punktes und des Sternchens eine beliebig lange Zeichenfolge repräsentiert. Bspw. würde der Regex `gen.*ion` zu folgenden Zeichenfolgen passen: `genion`, `generalization`, `generation`, `gentrification`, und so weiter.

Ein Sonderzeichen, das ähnlich wie das Sternchen funktioniert, ist das ***Fragezeichen*** (`'?'`), welches aussagt: "das vorhergehende Zeichen, keinmal oder genau einmal". Man könnte dies auch so formulieren: "das vorhergehende Zeichen kann, muss aber nicht vorhanden sein". Genau wie das Sternchen kann das Fragezeichen ebenfalls mit dem Punkt kombiniert werden. Deshalb passt der Regex `hell.?` zu folgenden Zeichenfolgen: `hell`, `hello`, `hells`, `hell4`, etc.

Da Zeichen wie der Punkt und das Sternchen eine Sonderbedeutung haben, muss jeweils ein *Escape-Zeichen* vorangestellt werden, falls diese Zeichen ohne Sonderbedeutung, d.&nbsp;h. "buchstäblich", verwendet werden sollen. Bei dem besagten Escape-Zeichen handelt es sich um ein weiteres Sonderzeichen, den umgekehrten Schrägstrich (`'\'`), der im Englischen als ***Backslash*** bezeichnet wird. Ein typisches Beispiel, wo der Backslash nötig ist, ist, wenn man einen Punkt in einem Dateinamen repräsentieren will. Um bspw. alle Dateien mit dem Dateinamen `license.txt` zu identifizieren, müssen Sie den Regex `license\.txt` anstelle von `license.txt` verwenden &mdash; letzterer würde nämlich z.&nbsp;B. auch auf `license-txt` passen.

Wenn wir also nun das oben Erklärte kombinieren, sind wir in der Lage, einen Regex zu schreiben, der alle Microsoft-Excel-Dateien identifiziert: `.*\.xlsx?`. Dieser Regex sagt im Wesentlichen aus: Eine beliebig lange Zeichenfolge, gefolgt von einem buchstäblichen Punkt, gefolgt von "xls", und mit einem optionalen "x" am Ende.

Identifikation einer Abfolgen von Nummern: `journal\d+\.doc`
------------------------------------------------------------
Angenommen, Sie wollten alle Microsoft-Word-Dateien identifizieren, die mit "journal" beginnen und mit einer Datumsangabe enden, z.&nbsp;B. so: "journal2007.doc". Außerdem jedoch soll der Regex *nicht* auf Dateien wie bspw. "journalism.doc" passen.

Ein Regex wie bspw. `journal.*\.doc` hilft hier nicht weiter, weil er auch auf "journalism.doc" passen würde. Der erste Schritt in Richtung Lösung besteht darin, den Punkt in dem Regex entweder durch `[0-9]` oder durch `\d` zu ersetzen, da beide ***genau eine Nummer*** repräsentieren. Der Ausdruck `[0-9]` ist dabei genaugenommen eine allgemeinere Notation als `\d`, weil man z.&nbsp;B. auch `[4-6]` schreiben kann, um eine der Nummern `4`, `5` und `6` zu repräsentieren. Dieses Klammer-Konstrukt funktioniert sogar mit Buchstaben: `[m-p]` repräsentiert genau einen Kleinbuchstaben aus dem Bereich von `m` bis `p`.

Wenn wir nun `\d` mit einem Sternchen versehen, können wir den Regex `journal\d*\.doc` schreiben, welches zu "journal2007.doc" passt, aber nicht zu "journalism.doc". Aber Moment mal, da passt etwas nicht: Wie Sie sich vielleicht noch erinnern können, ist mit dem Sternchen "das vorherige Zeichen, keinmal, einmal oder mehrmals wiederholt" gemeint. In diesem Fall wollen wir aber nicht mindestens *null* Nummern nach "journal" haben, sondern *mindestens eine* &mdash; denn sonst würde der Regex auch auf eine Datei wie "journal.doc" passen.

Deshalb brauchen wir ein weiteres Sonderzeichen: Das ***Pluszeichen*** (`'+'`) steht für "das vorherige Zeichen, einmal oder mehrmals wiederholt". Die endgültige Version unseres Regex sieht daher wie folgt aus: `journal\d+\.doc`
