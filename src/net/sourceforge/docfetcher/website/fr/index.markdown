Description
===========
DocFetcher est une application Open Source pour la recherche de contenu local sur ordinateur: elle vous permet de faire des recherches dans le contenu des fichiers sur votre ordinateur. &mdash; Vous pouvez le voir comme un Google pour vos fichiers locaux. L'application fonctionne sur Windows, Linux et Max OS&nbsp;X. Elle est disponible sous licence  [Eclipse Public License](http://en.wikipedia.org/wiki/Eclipse_Public_License).

Usage
===========
La capture d'écran ci dessous montre l'interface utilisateur principale. Les requêtes sont entrées dans le champ (1). Les résultats de la recherche apparaissent dans le panneau de résultat (2). Le panneau d'aperçu (3) montre seulement un aperçu du texte du fichier sélectionné dans le panneau résultat. Toutes les correspondances dans le fichiers sont surlignées en jaune.

Vous pouvez filtrer les résultats par taille minimale ou maximale (4), par type de fichier (5) et par emplacement (6). Les boutons (7) sont utilisés pour ouvrir le manuel, les menus de réglages et pour minimiser le programme dans la barre système.

<div id="img" style="text-align: center;"><a href="../all/intro-001-results-edited.png"><img style="width: 500px; height: 375px;" src="../all/intro-001-results-edited.png"></a></div>

DocFetcher a besoin que vous définissiez des *index* pour les dossiers dans lesquels vous voulez effectuer des recherches. Ce qu'est l'indexation et comment cela marche est expliqué plus en détails ci-dessous. En résumé, un index permet à DocFetcher de trouver très rapidement (on parle ici de millisecondes) quels fichiers contiennent un ensemble de mots, ainsi accélérant considérablement vos recherches. La capture d'écran qui suit montre la fenêtre de dialogue de DocFetcher pour créer de nouveaux index:

<div id="img" style="text-align: center;"><a href="../all/intro-002-config.png"><img style="width: 500px; height: 375px;" src="../all/intro-002-config.png"></a></div>

Cliquer sur le bouton "Démarrer" en bas à droite permet de commencer l'indexation. L'indexation peut durer un bon moment selon le nombre et la taille des fichiers qui doivent être indexés. En gros, il faut compter 200 fichiers par minute.

La création des index prend du temps mais elle n'a besoin d'être faite qu'une seule fois par dossier. De plus, *mettre à jour* un index après que son contenu ait changé est beaucoup plus rapide que de le créer &mdash; cela prend typiquement seulement quelques secondes.

Fonctions principales
================
* **Une version portable**: il y a une version portable de DocFetcher qui fonctionne sous Windows, Linux *et* Mac OS&nbsp;X. Son utilité est décrite avec plus de détails plus bas sur cette page.
* **support 64-bit**: les versions 32-bit et 64-bit des systèmes d'exploitations sont toutes deux supportées.
* **support Unicode support**: DocFetcher vient avec un support Unicode robuste pour tous les formats principaux, incluant Microsoft Office, OpenOffice.org, PDF, HTML, RTF et les fichiers texte bruts. La seule exception est CHM, pour lequel il n'y a pas encore de support Unicode. 
* **Support des archives**: DocFetcher supporte les formats d'archive suivants: zip, 7z, rar, et la famille complète des  tar.*. Les extensions de fichiers peuvent être configurées afin de vous permettre d'ajouter plus de formats d'archives basés sur le format zip si nécessaire. De plus, DocFetcher gère un nombre illimité d'archives imbriquées (ex: une archive zip qui contient une  archive  7z qui contient une archive rar... etc).
* **Recherche dans les fichiers de code source**: Les extensions que DocFetcher reconnait comme fichiers texte peuvent être configurées, de manière à ce que vous puissiez utiliser DocFetcher pour chercher n'importe quel type de fichier code source et de fichier basés sur du texte. (Ceci marche assez bien en combinaison avec la configuration des extensions zip par exemple pour chercher dans des fichiers code source à l'intérieur de fichiers Jar)
* **Fichiers Outlook PST**: DocFetcher permet de chercher les messages Outlook, que Microsoft Outlook stocke typiquement dans des fichiers PST.
* **Détection de paires HTML **: Par défaut, DocFetcher détecte les paires de fichiers HTML (ex: un fichier est nommé "toto.html" et un dossier "toto_files"), et les traite comme un document unique. Cette fonction peut paraître inutile de prime abord, mais cela augmente considérablement la qualité des résultats de recherche pour les fichiers HTML, dans la mesure ou tout le "bazar" dans le dossier HTML disparaît des résultats.
* **exclusion de fichier à indexer basée des expressions régulières (Regex)**: vous pouvez utiliser des expression régulières pour exclure des fichiers de l'indexation. Par exemple, pour exclure des fichiers Microsoft Excel, vous pouvez utiliser une expression régulière comme ceci: `.*\.xls`
* **Détection des types Mime**: Vous pouvez utiliser des expressions régulières pour activer la détection du type mime pour certains fichiers, ce qui veut dire que DocFetcher essaiera de détecter le vrai type de fichier en pas seulement en se basant sur le nom mais aussi en regardant à l'intérieur. Ceci est utile pour les fichiers qui ont une mauvaise extension.
* **Une syntaxe puissante pour les requêtes**: en plus de constructions basiques comme et, ou et pas (`OR`, `AND` et `NOT`), DocFetcher supporte aussi entre autres: les caractères de remplacement, les recherches de phrase, les recherches floues  ("trouver des mots similaires à..."), la recherche de proximité ("ces deux mots devraient être au plus à 10 mots d'intervalle l'un de l'autre"), "boosting" ("augmenter le score des documents qui contiennent...")

Formats de documents supportés
==========================
* Microsoft Office (doc, xls, ppt)
* Microsoft Office 2007 et versions plus récentes (docx, xlsx, pptx, docm, xlsm, pptm)
* Microsoft Outlook (pst)
* OpenOffice.org (odt, ods, odg, odp, ott, ots, otg, otp)
* Portable Document Format (pdf)
* HTML (html, xhtml, ...)
* Texte brut (configurable)
* Rich Text Format (rtf)
* AbiWord (abw, abw.gz, zabw)
* Microsoft Compiled HTML Help (chm)
* Microsoft Visio (vsd)
* Scalable Vector Graphics (svg)

Comparaison avec d'autres de programme de recherche de fichiers locaux
===============================================
En comparaison avec d'autres programmes de recherche de fichiers locaux, voici où DocFetcher se démarque:

**Sans m...**: Nous travaillons dur pour conserver l'interface de DocFetcher simple et sans bazar. Pas de publicité ou de "Voudriez-vous vous enregistrer...?" qui s'ouvrent. Pas de choses inutiles installées dans votre navigateur, registre ou n'importe où dans votre système.

**Vie privée**: DocFetcher ne collecte pas vos données privées. Jamais. Quiconque en douterait peut vérifier le [code source ](http://docfetcher.sourceforge.net/wiki/doku.php?id=source_code) qui est public.

**Gratuit/Libre pour toujours**: comme DocFetcher est Open Source, vous n'avez pas besoin de vous inquiéter sur le fait que le programme devienne obsolète ou sans support, parce que le code source sera toujours disponible pour être utilisé. En parlant de support, savez vous que Google Desktop, un des compétiteurs commerciaux majeurs de DocFetcher's a été arrêté en  2011? ...

**Multi-plateforme**: Contrairement à ses compétiteurs, DocFetcher ne fonctionne pas seulement sur Windows, mais aussi Linux et Mac OS&nbsp;X. Ainsi, si vous sentiez le besoin de laisser votre base Windows vers Linux ou Mac OS&nbsp;X, DocFetcher vous attendra de l'autre coté.

**Portable**: Une des plus grandes forces de DocFetcher's est sa portabilité. En fait, avec DocFetcher, vous pouvez construire une base de documents complète, qui peut être cherchée, et que vous pouvez emporter sur une clé USB.. Plus d'infos dans la section après.

**Indexez seulement ce dont vous avez besoin**: Parmi les compétiteurs commerciaux de DocFetcher, il semble y avoir une tendance à inciter les utilisateurs à indexer le disque dur complet &mdash; peut être pour ne pas laisser à un utilisateur supposé "stupide" de choix, ou pire, pour collecter le plus de données utilisateur possible. En pratique, il parait raisonnable de penser que la plupart des gens ne *veulent pas* indexer leur disque complet: non seulement c'est une perte de temps et d'espace disque, mais aussi cela pollue les résultats de recherche avec des fichiers non désirés. Aussi, DocFetcher n'indexe que les dossiers que vous voulez explicitement être indexés, et en plus vous avez de nombreuses options de filtrage.

Bases de documents portable
==============================
Une des fonctions remarquables de DocFetcher est qu'il est disponible dans une version qui vous permet de créer une *base de documents portable*  &mdash; complètement indexée et cherchable de tous vos documents importants et que vous pouvez emporter avec vous.

**Exemples d'utilisation**: il y a des tas de choses que vous pouvez faire avec une telle base: vous pouvez l'emporter avec vous sur une clé USB, la graver sur un CD à dessein d'archivage, la mettre sur un volume crypté (recommandé: [TrueCrypt](http://www.truecrypt.org/), la synchroniser entre plusieurs ordinateurs via un service de stockage en ligne comme [DropBox](http://www.dropbox.com/), etc. Mieux, comme DocFetcher est Open Source, vous pouvez même distribuer votre base: mettez en ligne et partagez là avec le monde entier si vous voulez.

**Java: Performance et portabilité**: Un des aspects qui pourrait ennuyer certains est le fait que DocFetcher a été écrit en Java, qui a la réputation d'être "lent". Ceci était en fait vrai il y a dix ans, mais depuis les performances de Java se sont beaucoup améliorées, [selon Wikipedia](http://en.wikipedia.org/wiki/Java_%28software_platform%29#Performance). De toute façon, le point positif sur le fait d'être écrit en Java est que le même package portable DocFetcher peut être exécuté sur Windows, Linux *et* Mac OS&nbsp;X &mdash; beaucoup d'autres programmes demandent des paquets séparés pour chaque plateforme. Il en résulte que vous pouvez, par exemple, mettre votre base sur une clé USB et y accéder de *tous* ces systèmes d’exploitation du moment que Java runtime y est installé.

Comment fonctionne l'indexation
==================
Cette section essaye de donner un aperçu simplifié de ce qu'est l'indexation et de comment elle marche.

**L'approche naïve pour la recherche de fichier**: L'approche la plus basique pour la recherche de fichier est simplement regarder chaque fichier stocké un par un chaque fois qu'une recherche est effectué. Cela marche assez bien quand on recherche un *nom de fichier* seulement, car analyser les noms de fichiers est très rapide. Par contre, si vous vouliez chercher le *contenu* des fichiers, cela ne marcherait pas aussi bien car l'extraction du texte d'un fichier est bien plus coûteuse que la simple analyse de son nom.

**Recherche basée sur un index**: C'est pourquoi DocFetcher, qui permet de chercher le contenu, utilise une approche connue sous le nom d'*indexation*: l'idée de base est que la plupart des fichiers que les gens recherchent (typiquement plus de 95%) ne sont modifiés que très rarement ou pas du tout. Donc, plutôt que d'extraire le texte complet de chaque fichier à chaque recherche, il est beaucoup plus efficace de faire l'extraction *une fois* pour tous les fichiers, et de créer un *index* à partir de tout le texte collecté. Cet index est comme un dictionnaire qui permet rapidement de rechercher des fichiers à partir des mots qu'ils contiennent.

**Analogie d'un annuaire**: comme analogie, voyez comme il est plus efficace de trouver le numéro de téléphone de quelqu'un dans l'annuaire (l'"index") plutôt que d'appeler *chaque* numéro possible juste pour voir si la personne de l'autre coté est celle que vous recherchez. &mdash; Appeler quelqu'un au téléphone et extraire le contenu texte d'un fichier peuvent tous deux être considérés comme des "opérations coûteuses" De plus, le fait que les gens ne changent pas souvent de numéro est similaire au fait que la plupart des fichiers sur un ordinateur ne sont que rarement modifiés.

**Mise à jour de l'Index**: Bien sûr, un index ne correspond à l'état des fichiers indexés que quand il a été crée, et ne prend pas forcément en compte les versions plus récentes des fichiers. Ainsi, si l'index n'est pas conservé à jour, vous pourriez obtenir des résultats de recherche périmés, de la même manière qu'un annuaire peut ne pas être à jour. Toutefois, ceci ne devrait pas être un gros problème si la plupart des fichiers ne changent que rarement. De plus, DocFetcher est capable de mettre à jour *automatiquement* ses index: (1) quand il est ouvert, il détecte les changements de fichiers et met à jour l'index de manière correspondante. (2) quand il n'est pas ouvert, un petit programme en tache de fond détecte les changements et garde une liste des index à mettre à jour; DocFetcher les mettra ensuite à jour au démarrage suivant. Et ne vous inquiétez pas au sujet du programme qui tourne en tâche de fond: il utilise très peu de ressources processeur et très peu de mémoire, dans la mesure où il ne fait rien à part noter quels dossiers ont été mis à jour, et laisse la mise à jour de l'index (qui est plus couteuse) à DocFetcher.
