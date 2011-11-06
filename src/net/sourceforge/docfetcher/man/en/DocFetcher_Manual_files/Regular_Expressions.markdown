Regular Expressions
===================
This page is meant to be a very brief introduction to regular expressions. It is by no means exhaustive, since regular expressions basically represent an entire pattern matching language. If you want to dig deeper, you can find a ton of information about it on the internet; just search for 'regular expression tutorial' or 'regular expression introduction' or something like that.

Matching all Microsoft Excel files: `.*\.xlsx?`
-----------------------------------------------
In regular expressions (often abbreviated to "regex"), certain characters have a special meaning. For example, the ***dot*** (`'.'`) stands for exactly one unknown character. So you could, for instance, use the regex `h.llo` to match strings like `hallo` or `hello`, but also `hzllo` or `h8llo`.

Another special character is the ***asterisk*** (`'*'`), which stands for "the preceding character, repeated zero or more times". So if you enter the regex `hello*`, the following strings would match: `hell`, `hello`, `helloo`, `hellooo`, and so on.

As a consequence of these rules, if the dot and the asterisk are put together, they will match an arbitrary sequence of characters. For example, the regex `gen.*ion` would match: `genion`, `generalization`, `generation`, `gentrification`, and so on.

A special character similar to the asterisk is the ***question mark*** (`'?'`), which stands for "the preceding character, zero times or exactly once". You might also rephrase it as "the preceding character may or may not be there". Like the asterisk, it can be combined with a dot. Therefore, the regex `hell.?` would match: `hell`, `hello`, `hells`, `hell4`, etc.

Since characters such as the dot and the asterisk have a special meaning, if you want to match them *literally*, you'll have to *escape* them. This is done by preceding them with another special character, the ***backslash*** (`'\'`). A typical case where the backslash is needed is matching the dot in a filename. For example, to match all files with the filename `license.txt`, you have to use the regex `license\.txt` instead of just `license.txt` &mdash; the latter would also match, for example, `license-txt`.

So, if we put all this together, we can write down a regex that matches all Microsoft Excel files, like so: `.*\.xlsx?`. This regex basically says: An arbitrary sequence of characters, followed by a literal dot, followed by "xls", with an optional "x" at the end.

Matching a sequence of digits: `journal\d+\.doc`
------------------------------------------------
Suppose you want to exclude all Microsoft Word files that start with "journal" and end with a timestamp, like this: "journal2007.doc". Moreover, what you *don't* want is to exclude files like "journalism.doc".

A regex like `journal.*\.doc` isn't going to work here because it would match "journalism.doc" as well. The first step towards the solution is to replace the dot with either `[0-9]` or `\d`, which both match exactly ***one digit***. The expression `[0-9]` is actually a more general notation than `\d`, because you can, for example, write `[4-6]` to match only the digits `4`, `5` and `6`. It even works for letters: `[m-p]` matches all lowercase letters from m through p.

So, if we combine `\d` with an asterisk, we can write down the regex `journal\d*\.doc`, which will match "journal2007.doc", but not "journalism.doc". But wait, that's not quite right: Recall that the asterisk means "the preceding character, repeated *zero* or more times". In this case, we don't want *zero* digits after "journal", we want *at least one* &mdash; otherwise the regex would also match the file "journal.doc".

So here's another special character for you: The ***plus*** symbol (`'+'`) stands for "the preceding character, repeated one or more times". The final version of our regex is therefore: `journal\d+\.doc`
